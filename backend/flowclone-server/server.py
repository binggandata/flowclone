#!/usr/bin/env python3
"""
FlowClone Server —— 商业化最小后端骨架（零第三方依赖，标准库实现）

作用：
  1) App 只连本服务，AI 网关密钥只存在服务端（不再打进 APK）
  2) 生成任务全部在服务端异步执行 + 本地 SQLite 持久化（App 退出/被杀不影响）
  3) 预留账号、额度、推送、审核、运营后台的扩展点

运行：
  export GATEWAY_URL=https://your-gateway.example
  export GATEWAY_KEY=<gateway-key>
  python3 server.py            # 默认 0.0.0.0:8081
手机联调：
  adb reverse tcp:8081 tcp:8081
  App 的 GATEWAY_URL 改为 http://127.0.0.1:8081 重新构建

接口（对 App 完全兼容现有 /v1/* 协议）：
  GET  /health
  POST /v1/auth/device           {deviceId} -> {token, userId, credits}
  POST /v1/files                 multipart 透传
  POST /v1/generations           JSON 透传 + 记账
  GET  /v1/jobs/{id}             透传 + 落库
  GET  /v1/jobs/{id}/outputs/{i}/content   透传下载
  GET  /v1/me                    -> {userId, credits}
  POST /v1/push/register         {token, platform}
"""
import base64
import hashlib
import hmac
import json
import os
import sqlite3
import ssl
import threading
import time
import urllib.request
import urllib.error
import uuid
from collections import defaultdict, deque
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

GATEWAY_URL = os.environ.get("GATEWAY_URL", "").rstrip("/")
GATEWAY_KEY = os.environ.get("GATEWAY_KEY", "")
PORT = int(os.environ.get("PORT", "8081"))
DB_PATH = os.environ.get("DB_PATH", os.path.join(os.path.dirname(os.path.abspath(__file__)), "flowclone.db"))
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "<admin-token>")
RATE_LIMIT_PER_MIN = int(os.environ.get("RATE_LIMIT_PER_MIN", "60"))
_RL = defaultdict(deque)
SSL_CTX = None
if os.environ.get("GATEWAY_INSECURE", "1") == "1":
    SSL_CTX = ssl.create_default_context()
    SSL_CTX.check_hostname = False
    SSL_CTX.verify_mode = ssl.CERT_NONE


TOKEN_SECRET = os.environ.get("TOKEN_SECRET", "flowclone-dev-secret-change-me").encode()


def hash_password(password: str, salt: bytes = None) -> str:
    salt = salt or os.urandom(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 120_000)
    return "pbkdf2$" + base64.b64encode(salt).decode() + "$" + base64.b64encode(dk).decode()


def verify_password(password: str, stored: str) -> bool:
    try:
        _, salt_b64, hash_b64 = stored.split("$")
        salt = base64.b64decode(salt_b64)
        expected = base64.b64decode(hash_b64)
        dk = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 120_000)
        return hmac.compare_digest(dk, expected)
    except Exception:
        return False


ACCESS_TTL = int(os.environ.get("ACCESS_TTL_SECONDS", "3600"))


def issue_token(user_id: str, ttl: int = None) -> str:
    """无状态 access token: uid.exp.sig（HMAC-SHA256），默认 1 小时"""
    exp = int(time.time()) + (ttl or ACCESS_TTL)
    payload = f"{user_id}.{exp}"
    sig = hmac.new(TOKEN_SECRET, payload.encode(), hashlib.sha256).hexdigest()
    return base64.urlsafe_b64encode(f"{payload}.{sig}".encode()).decode()


def parse_token(token: str) -> str:
    try:
        raw = base64.urlsafe_b64decode(token.encode()).decode()
        user_id, exp, sig = raw.rsplit(".", 2)
        payload = f"{user_id}.{exp}"
        expect = hmac.new(TOKEN_SECRET, payload.encode(), hashlib.sha256).hexdigest()
        if not hmac.compare_digest(sig, expect):
            return ""
        if int(exp) < time.time():
            return ""
        return user_id
    except Exception:
        return ""


@contextmanager
def db():
    conn = sqlite3.connect(DB_PATH, timeout=30)
    conn.row_factory = sqlite3.Row
    try:
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA busy_timeout=30000")
    except Exception:
        pass
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def init_db():
    with db() as c:
        c.executescript("""
        CREATE TABLE IF NOT EXISTS users(
          id TEXT PRIMARY KEY, device_id TEXT UNIQUE, credits INTEGER DEFAULT 100,
          created_at INTEGER
        );
        CREATE TABLE IF NOT EXISTS sessions(
          token TEXT PRIMARY KEY, user_id TEXT, created_at INTEGER
        );
        CREATE TABLE IF NOT EXISTS jobs(
          id TEXT PRIMARY KEY, user_id TEXT, model TEXT, modality TEXT,
          status TEXT, remote_job_id TEXT, prompt TEXT, created_at INTEGER, updated_at INTEGER
        );
        CREATE TABLE IF NOT EXISTS push_tokens(
          token TEXT PRIMARY KEY, user_id TEXT, platform TEXT, created_at INTEGER
        );
        CREATE TABLE IF NOT EXISTS refresh_tokens(
          token TEXT PRIMARY KEY, user_id TEXT, expires_at INTEGER, created_at INTEGER, revoked INTEGER DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS notifications(
          id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, job_id TEXT, kind TEXT,
          title TEXT, body TEXT, created_at INTEGER
        );
        CREATE TABLE IF NOT EXISTS ledger(
          id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, job_id TEXT,
          delta INTEGER, reason TEXT, created_at INTEGER
        );
        """)
        # 兼容升级：老库补列
        cols = {r[1] for r in c.execute("PRAGMA table_info(users)").fetchall()}
        if "email" not in cols:
            c.execute("ALTER TABLE users ADD COLUMN email TEXT")
        if "password_hash" not in cols:
            c.execute("ALTER TABLE users ADD COLUMN password_hash TEXT")


def issue_tokens(user_id: str) -> dict:
    access = issue_token(user_id)
    refresh = "r_" + uuid.uuid4().hex + uuid.uuid4().hex
    with db() as c:
        c.execute("INSERT INTO refresh_tokens(token,user_id,expires_at,created_at) VALUES(?,?,?,?)",
                  (refresh, user_id, int(time.time()) + 90 * 24 * 3600, int(time.time())))
    return {"token": access, "refreshToken": refresh, "expiresIn": ACCESS_TTL}


def rate_limited(key: str, per_min: int = None) -> bool:
    """简单滑动窗口限流：key 一般用 user_id/ip"""
    per_min = per_min or RATE_LIMIT_PER_MIN
    now = time.time()
    q = _RL[key]
    while q and now - q[0] > 60:
        q.popleft()
    if len(q) >= per_min:
        return True
    q.append(now)
    return False


def gateway(method, path, body=None, headers=None, raw=False, timeout=180):
    """转发到 NGSFlow 网关；返回 (status, body_bytes_or_json)"""
    url = GATEWAY_URL + path
    data = None
    hdrs = {"Authorization": f"Bearer {GATEWAY_KEY}"}
    if headers:
        hdrs.update(headers)
    if body is not None:
        data = body if isinstance(body, bytes) else json.dumps(body).encode()
        hdrs.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=SSL_CTX) as r:
            content = r.read()
            return r.status, content if raw else json.loads(content.decode() or "{}")
    except urllib.error.HTTPError as e:
        return e.code, {"error": e.read().decode()[:500]}
    except Exception as e:  # noqa
        return 502, {"error": str(e)}



# ---------- 输出文件本地缓存（网关内容下载很慢，先缓存再给客户端） ----------
OUTPUT_CACHE_DIR = os.path.join(os.path.dirname(os.path.abspath(DB_PATH or "flowclone.db")), "outputs")
_caching = set()
_caching_lock = threading.Lock()


def output_cache_path(local_id, idx):
    return os.path.join(OUTPUT_CACHE_DIR, f"{local_id}_{idx}.bin")


def cache_outputs_async(local_id, remote, count=1):
    with _caching_lock:
        if local_id in _caching:
            return
        _caching.add(local_id)

    def run():
        try:
            os.makedirs(OUTPUT_CACHE_DIR, exist_ok=True)
            for i in range(max(1, count)):
                path = output_cache_path(local_id, i)
                if os.path.exists(path):
                    continue
                # 网关内容下载偶尔很慢/中断：最多尝试 3 次
                for attempt in range(1, 4):
                    print("[server] cache fetch start", local_id, i, "attempt", attempt, flush=True)
                    st, data = gateway("GET", f"/v1/jobs/{remote}/outputs/{i}/content", raw=True, timeout=900)
                    print("[server] cache fetch done", local_id, i, "status", st,
                          "bytes", len(data) if isinstance(data, (bytes, bytearray)) else -1, flush=True)
                    if st == 200 and isinstance(data, (bytes, bytearray)) and len(data) > 1024:
                        tmp = path + ".tmp"
                        with open(tmp, "wb") as f:
                            f.write(data)
                        os.replace(tmp, path)
                        break
                    time.sleep(3)
        except Exception as e:  # noqa
            print("[server] cache error", local_id, e, flush=True)
        finally:
            with _caching_lock:
                _caching.discard(local_id)

    threading.Thread(target=run, daemon=True).start()


class Handler(BaseHTTPRequestHandler):
    server_version = "FlowCloneServer/0.1"

    def log_message(self, fmt, *args):
        print("[server]", self.address_string(), fmt % args, flush=True)

    # ---------- helpers ----------
    def _cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type, Idempotency-Key")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")

    def send_json(self, code, obj):
        data = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self._cors()
        self.end_headers()
        self.wfile.write(data)

    def do_OPTIONS(self):
        self.send_response(204)
        self._cors()
        self.end_headers()

    def body(self):
        n = int(self.headers.get("Content-Length") or 0)
        return self.rfile.read(n) if n else b""

    def user_id(self):
        auth = self.headers.get("Authorization", "")
        token = auth.replace("Bearer ", "").strip()
        if not token:
            return None
        uid = parse_token(token)
        if uid:
            return uid
        with db() as c:
            row = c.execute("SELECT user_id FROM sessions WHERE token=?", (token,)).fetchone()
            return row["user_id"] if row else None

    def authed(self):
        uid = self.user_id()
        if not uid:
            self.send_json(401, {"error": "unauthorized"})
        return uid

    # ---------- routes ----------
    def do_GET(self):
        if self.path == "/health":
            return self.send_json(200, {"ok": True, "gateway": bool(GATEWAY_URL and GATEWAY_KEY)})
        if self.path.startswith("/v1/jobs/"):
            return self.handle_job_get()
        if self.path.split("?")[0] == "/v1/jobs":
            return self.handle_jobs_list()
        if self.path.startswith("/admin/"):
            return self.handle_admin()
        if self.path == "/v1/auth/me":
            return self.handle_me()
        if self.path.split("?")[0] == "/v1/notifications":
            return self.handle_notifications_list()
        if self.path == "/v1/me":
            uid = self.authed()
            if not uid:
                return
            with db() as c:
                row = c.execute("SELECT credits FROM users WHERE id=?", (uid,)).fetchone()
            return self.send_json(200, {"userId": uid, "credits": row["credits"] if row else 0})
        return self.send_json(404, {"error": "not found"})

    def do_DELETE(self):
        if self.path.split("?")[0] == "/v1/account":
            return self.handle_delete_account()
        return self.send_json(404, {"error": "not found"})

    def do_POST(self):
        if self.path == "/v1/auth/device":
            return self.handle_auth()
        if self.path == "/v1/auth/register":
            return self.handle_register()
        if self.path == "/v1/auth/login":
            return self.handle_login()
        if self.path == "/v1/auth/refresh":
            return self.handle_refresh()
        if self.path == "/v1/auth/logout":
            return self.handle_logout()
        if self.path == "/v1/auth/me":
            return self.handle_me()
        if self.path.split("?")[0] == "/v1/notifications":
            return self.handle_notifications_list()
        if self.path == "/v1/files":
            return self.handle_upload()
        if self.path == "/v1/generations":
            return self.handle_generation()
        if self.path == "/v1/agent/chat":
            return self.handle_agent_chat()
        if self.path == "/v1/push/register":
            return self.handle_push()
        if self.path.endswith("/cancel") and self.path.startswith("/v1/jobs/"):
            return self.handle_job_cancel()
        return self.send_json(404, {"error": "not found"})

    # ---------- handlers ----------
    def handle_auth(self):
        try:
            body = json.loads(self.body() or b"{}")
        except Exception:
            body = {}
        device_id = body.get("deviceId") or hashlib.sha256(str(uuid.uuid4()).encode()).hexdigest()[:16]
        with db() as c:
            row = c.execute("SELECT id FROM users WHERE device_id=?", (device_id,)).fetchone()
            if row:
                uid = row["id"]
            else:
                uid = "u_" + uuid.uuid4().hex[:12]
                c.execute("INSERT INTO users(id, device_id, credits, created_at) VALUES(?,?,?,?)",
                          (uid, device_id, 100, int(time.time())))
            credits = c.execute("SELECT credits FROM users WHERE id=?", (uid,)).fetchone()["credits"]
        tok = issue_tokens(uid)
        return self.send_json(200, {"token": tok["token"], "refreshToken": tok["refreshToken"],
                                    "expiresIn": tok["expiresIn"], "userId": uid, "credits": credits})

    def handle_register(self):
        try:
            body = json.loads(self.body() or b"{}")
        except Exception:
            return self.send_json(400, {"error": "bad json"})
        email = (body.get("email") or "").strip().lower()
        password = body.get("password") or ""
        if "@" not in email or len(email) < 5:
            return self.send_json(400, {"error": "请输入有效邮箱"})
        if len(password) < 8:
            return self.send_json(400, {"error": "密码至少 8 位"})
        with db() as c:
            if c.execute("SELECT id FROM users WHERE email=?", (email,)).fetchone():
                return self.send_json(409, {"error": "该邮箱已注册"})
            uid = "u_" + uuid.uuid4().hex[:12]
            c.execute("INSERT INTO users(id, device_id, credits, created_at, email, password_hash) VALUES(?,?,?,?,?,?)",
                      (uid, None, 100, int(time.time()), email, hash_password(password)))
            credits = 100
        tok = issue_tokens(uid)
        return self.send_json(200, {"token": tok["token"], "refreshToken": tok["refreshToken"],
                                    "expiresIn": tok["expiresIn"], "userId": uid, "email": email, "credits": credits})

    def handle_login(self):
        try:
            body = json.loads(self.body() or b"{}")
        except Exception:
            return self.send_json(400, {"error": "bad json"})
        email = (body.get("email") or "").strip().lower()
        password = body.get("password") or ""
        if rate_limited(f"login:{email or self.client_address[0]}", 10):
            return self.send_json(429, {"error": "尝试过于频繁，请稍后再试"})
        with db() as c:
            row = c.execute("SELECT * FROM users WHERE email=?", (email,)).fetchone()
        if not row or not row["password_hash"] or not verify_password(password, row["password_hash"]):
            return self.send_json(401, {"error": "邮箱或密码错误"})
        tok = issue_tokens(row["id"])
        return self.send_json(200, {"token": tok["token"], "refreshToken": tok["refreshToken"],
                                    "expiresIn": tok["expiresIn"], "userId": row["id"], "email": email,
                                    "credits": row["credits"]})

    def handle_me(self):
        uid = self.authed()
        if not uid:
            return
        with db() as c:
            row = c.execute("SELECT id, email, credits FROM users WHERE id=?", (uid,)).fetchone()
        if not row:
            return self.send_json(401, {"error": "user not found"})
        return self.send_json(200, {"userId": row["id"], "email": row["email"], "credits": row["credits"]})

    def handle_upload(self):
        uid = self.authed()
        if not uid:
            return
        raw = self.body()
        ctype = self.headers.get("Content-Type", "")
        st, obj = gateway("POST", "/v1/files", raw, {"Content-Type": ctype})
        return self.send_json(st, obj)

    def handle_generation(self):
        uid = self.authed()
        if not uid:
            return
        if rate_limited(f"gen:{uid}", 30):
            return self.send_json(429, {"error": "请求过于频繁，请稍后再试"})
        raw = self.body()
        try:
            body = json.loads(raw or b"{}")
        except Exception:
            return self.send_json(400, {"error": "bad json"})
        model = body.get("model", "")
        # 额度规则（对齐原版展示口径）：图片 0 点；视频按 720p 3 点/秒、480p 1 点/秒
        if "video" in model:
            params = body.get("parameters") or {}
            secs = int(params.get("durationSeconds") or 8)
            qual = str(params.get("quality") or "720p")
            cost = (3 if qual == "720p" else 1) * secs
        else:
            cost = 0
        with db() as c:
            credits = c.execute("SELECT credits FROM users WHERE id=?", (uid,)).fetchone()["credits"]
        if credits < cost:
            return self.send_json(402, {"error": "insufficient credits"})
        st, obj = gateway("POST", "/v1/generations", raw,
                          {"Content-Type": "application/json",
                           "Idempotency-Key": self.headers.get("Idempotency-Key") or uuid.uuid4().hex})
        if st in (200, 201, 202):
            remote = (obj.get("data") or {}).get("id") or obj.get("id")
            local_id = "job_" + uuid.uuid4().hex[:12]
            with db() as c:
                c.execute("INSERT INTO jobs(id,user_id,model,modality,status,remote_job_id,prompt,created_at,updated_at)"
                          " VALUES(?,?,?,?,?,?,?,?,?)",
                          (local_id, uid, model, body.get("modality", ""), "queued", remote,
                           body.get("prompt", "")[:2000], int(time.time()), int(time.time())))
                c.execute("UPDATE users SET credits=credits-? WHERE id=?", (cost, uid))
                c.execute("INSERT INTO ledger(user_id,job_id,delta,reason,created_at) VALUES(?,?,?,?,?)",
                          (uid, local_id, -cost, "generation", int(time.time())))
            obj["data"] = obj.get("data") if isinstance(obj.get("data"), dict) else {}
            obj["data"]["id"] = local_id
            obj["data"]["remoteJobId"] = remote
        return self.send_json(st, obj)

    def handle_job_get(self):
        uid = self.authed()
        if not uid:
            return
        parts = self.path.split("/")
        # /v1/jobs/{id} 或 /v1/jobs/{id}/outputs/{i}/content
        local_id = parts[3] if len(parts) > 3 else ""
        with db() as c:
            row = c.execute("SELECT * FROM jobs WHERE id=? AND user_id=?", (local_id, uid)).fetchone()
        if not row:
            return self.send_json(404, {"error": "job not found"})
        remote = row["remote_job_id"]
        suffix = self.path[len(f"/v1/jobs/{local_id}"):]
        if suffix.startswith("/outputs/"):
            try:
                idx = int(suffix.rstrip("/").split("/")[-2])
            except Exception:
                idx = 0
            cached = output_cache_path(local_id, idx)
            if os.path.exists(cached):
                data = open(cached, "rb").read()
                self.send_response(200)
                self.send_header("Content-Type", "application/octet-stream")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return
            # 网关内容下载慢：先尝试直接透传（客户端读超时 10 分钟），并同时后台缓存供下次秒开
            cache_outputs_async(local_id, remote, 1)
            st, data = gateway("GET", f"/v1/jobs/{remote}{suffix}", raw=True, timeout=900)
            if st == 200 and isinstance(data, (bytes, bytearray)) and len(data) > 1024:
                try:
                    os.makedirs(OUTPUT_CACHE_DIR, exist_ok=True)
                    tmp = cached + ".tmp"
                    with open(tmp, "wb") as f:
                        f.write(data)
                    os.replace(tmp, cached)
                except Exception:
                    pass
                self.send_response(200)
                self.send_header("Content-Type", "application/octet-stream")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return
            return self.send_json(202, {"status": "caching", "retryAfterMs": 5000})
        st, obj = gateway("GET", f"/v1/jobs/{remote}")
        if st == 200:
            data = obj.get("data") or obj
            status = data.get("status", "queued")
            with db() as c:
                prev = c.execute("SELECT status FROM jobs WHERE id=?", (local_id,)).fetchone()
                c.execute("UPDATE jobs SET status=?, updated_at=? WHERE id=?", (status, int(time.time()), local_id))
            if status == "succeeded":
                try:
                    cnt = int(data.get("requestedOutputCount") or 1)
                except Exception:
                    cnt = 1
                cache_outputs_async(local_id, remote, cnt)
            if status in ("succeeded", "failed") and (prev is None or prev["status"] != status):
                with db() as c:
                    row = c.execute("SELECT user_id, prompt FROM jobs WHERE id=?", (local_id,)).fetchone()
                if row:
                    self.notify_job(row["user_id"], local_id, status, row["prompt"] or "")
            data["id"] = local_id
            data["remoteJobId"] = remote
            obj["data"] = data
        return self.send_json(st, obj)

    def handle_refresh(self):
        try:
            body = json.loads(self.body() or b"{}")
        except Exception:
            return self.send_json(400, {"error": "bad json"})
        rt = body.get("refreshToken") or ""
        with db() as c:
            row = c.execute("SELECT * FROM refresh_tokens WHERE token=? AND revoked=0", (rt,)).fetchone()
            if not row or row["expires_at"] < time.time():
                return self.send_json(401, {"error": "refresh token 无效或已过期"})
            uid = row["user_id"]
            # 轮换 refresh token
            c.execute("UPDATE refresh_tokens SET revoked=1 WHERE token=?", (rt,))
            credits = c.execute("SELECT credits FROM users WHERE id=?", (uid,)).fetchone()["credits"]
        tok = issue_tokens(uid)
        return self.send_json(200, {"token": tok["token"], "refreshToken": tok["refreshToken"],
                                    "expiresIn": tok["expiresIn"], "userId": uid, "credits": credits})

    def handle_logout(self):
        try:
            body = json.loads(self.body() or b"{}")
        except Exception:
            body = {}
        rt = body.get("refreshToken") or ""
        with db() as c:
            c.execute("UPDATE refresh_tokens SET revoked=1 WHERE token=?", (rt,))
        return self.send_json(200, {"ok": True})

    def notify_job(self, uid: str, job_id: str, status: str, prompt: str):
        with db() as c:
            if c.execute("SELECT id FROM notifications WHERE job_id=?", (job_id,)).fetchone():
                return
            ok = status == "succeeded"
            title = "成功！" if ok else "生成失败"
            # 原版实测：仅视频生成会发通知；图片（0 点）不发
            with db() as c2:
                jr = c2.execute("SELECT modality FROM jobs WHERE id=?", (job_id,)).fetchone()
                if not jr or jr["modality"] != "video":
                    return
            kind = "视频"
            body = (f"您的{kind}“{prompt[:40]}”已生成完毕。点按即可查看。" if ok
                    else f"您的{kind}“{prompt[:40]}”生成失败，可重试。")
            c.execute("INSERT INTO notifications(user_id,job_id,kind,title,body,created_at) VALUES(?,?,?,?,?,?)",
                      (uid, job_id, "job_succeeded" if ok else "job_failed", title, body, int(time.time())))

    def handle_notifications_list(self):
        uid = self.authed()
        if not uid:
            return
        with db() as c:
            rows = [dict(r) for r in c.execute(
                "SELECT id,kind,title,body,created_at FROM notifications WHERE user_id=? ORDER BY id DESC LIMIT 20",
                (uid,))]
        return self.send_json(200, {"data": rows})

    def handle_jobs_list(self):
        uid = self.authed()
        if not uid:
            return
        limit = 20
        try:
            qs = self.path.split("?", 1)[1] if "?" in self.path else ""
            for kv in qs.split("&"):
                if kv.startswith("limit="):
                    limit = min(max(int(kv.split("=")[1]), 1), 100)
        except Exception:
            pass
        with db() as c:
            rows = c.execute(
                "SELECT id, model, modality, status, created_at, updated_at, prompt"
                " FROM jobs WHERE user_id=? ORDER BY created_at DESC LIMIT ?",
                (uid, limit)).fetchall()
            credits = c.execute("SELECT credits FROM users WHERE id=?", (uid,)).fetchone()["credits"]
        return self.send_json(200, {"data": [dict(r) for r in rows], "credits": credits})

    def handle_admin(self):
        if self.headers.get("X-Admin-Token", "") != ADMIN_TOKEN:
            return self.send_json(401, {"error": "admin unauthorized"})
        path = self.path.split("?")[0]
        with db() as c:
            if path == "/admin/users":
                rows = [dict(r) for r in c.execute(
                    "SELECT id,email,credits,created_at FROM users ORDER BY created_at DESC LIMIT 100")]
            elif path == "/admin/jobs":
                rows = [dict(r) for r in c.execute(
                    "SELECT id,user_id,model,status,created_at FROM jobs ORDER BY created_at DESC LIMIT 100")]
            elif path == "/admin/ledger":
                rows = [dict(r) for r in c.execute(
                    "SELECT * FROM ledger ORDER BY id DESC LIMIT 100")]
            elif path == "/admin/stats":
                users = c.execute("SELECT COUNT(*) c FROM users").fetchone()["c"]
                jobs = c.execute("SELECT COUNT(*) c FROM jobs").fetchone()["c"]
                ok = c.execute("SELECT COUNT(*) c FROM jobs WHERE status='succeeded'").fetchone()["c"]
                return self.send_json(200, {"users": users, "jobs": jobs, "succeeded": ok})
            else:
                return self.send_json(404, {"error": "not found"})
        return self.send_json(200, {"data": rows})

    def handle_delete_account(self):
        uid = self.authed()
        if not uid:
            return
        with db() as c:
            for table in ("notifications", "jobs", "ledger", "refresh_tokens", "sessions", "push_tokens"):
                c.execute(f"DELETE FROM {table} WHERE user_id=?", (uid,))
            c.execute("DELETE FROM users WHERE id=?", (uid,))
        return self.send_json(200, {"ok": True, "deleted": uid})

    def handle_job_cancel(self):
        uid = self.authed()
        if not uid:
            return
        local_id = self.path.split("/")[3]
        with db() as c:
            row = c.execute("SELECT * FROM jobs WHERE id=? AND user_id=?", (local_id, uid)).fetchone()
        if not row or not row["remote_job_id"]:
            return self.send_json(404, {"error": "job not found"})
        st, obj = gateway("POST", f"/v1/jobs/{row['remote_job_id']}/cancel")
        if st in (200, 201, 202, 204):
            with db() as c:
                c.execute("UPDATE jobs SET status='canceled', updated_at=? WHERE id=?", (int(time.time()), local_id))
                # 取消退款：按该 job 的扣费记录反冲
                led = c.execute("SELECT SUM(delta) s FROM ledger WHERE job_id=?", (local_id,)).fetchone()
                spent = led["s"] if led and led["s"] is not None else 0
                if spent < 0:
                    c.execute("UPDATE users SET credits=credits-? WHERE id=?", (spent, uid))
                    c.execute("INSERT INTO ledger(user_id,job_id,delta,reason,created_at) VALUES(?,?,?,?,?)",
                              (uid, local_id, -spent, "cancel_refund", int(time.time())))
        return self.send_json(st if st in (200, 201, 202, 204) else st, obj)

    def handle_agent_chat(self):
        """真实 LLM 代理：把对话转发给网关 Grok 文本模型（Key 只在服务端）。"""
        uid = self.authed()
        if not uid:
            return
        try:
            body = json.loads(self.body() or b"{}")
        except Exception:
            return self.send_json(400, {"error": "bad json"})
        if rate_limited(f"agent:{uid}", 30):
            return self.send_json(429, {"error": "请求过于频繁"})
        messages = body.get("messages") or []
        system = body.get("system") or (
            "你是 Flow 视频创作 App 内的智能体助手。用简体中文简洁回答；"
            "涉及创作建议时给出具体可执行的提示词或步骤；回答控制在 300 字以内。"
        )
        lines = [system, ""]
        for m in messages[-12:]:
            role = "用户" if m.get("role") == "user" else "助手"
            lines.append(f"{role}：{m.get('content','')}")
        lines.append("助手：")
        prompt = "\n".join(lines)
        st, obj = gateway("POST", "/v1/generations", {
            "model": "grok-4.3", "modality": "text", "prompt": prompt,
            "parameters": {},
        })
        if st not in (200, 201, 202):
            return self.send_json(st if 400 <= st < 600 else 502, {"error": "LLM 调用失败", "detail": obj})
        job_id = ((obj.get("data") or {}).get("id")) or ""
        reply = ""
        for _ in range(30):
            time.sleep(1.2)
            st2, obj2 = gateway("GET", f"/v1/jobs/{job_id}")
            if st2 != 200:
                continue
            data = obj2.get("data") or {}
            if data.get("status") == "succeeded":
                res = data.get("result") or {}
                reply = res.get("content") or ""
                if not reply:
                    results = data.get("results") or []
                    if results:
                        reply = results[0].get("content") or ""
                break
            if data.get("status") in ("failed", "canceled"):
                return self.send_json(502, {"error": "LLM 生成失败"})
        if not reply:
            return self.send_json(504, {"error": "LLM 超时"})
        return self.send_json(200, {"reply": reply.strip()})

    def handle_push(self):
        uid = self.authed()
        if not uid:
            return
        try:
            body = json.loads(self.body() or b"{}")
        except Exception:
            body = {}
        token = body.get("token", "")
        if not token:
            return self.send_json(400, {"error": "token required"})
        with db() as c:
            c.execute("INSERT OR REPLACE INTO push_tokens(token,user_id,platform,created_at) VALUES(?,?,?,?)",
                      (token, uid, body.get("platform", "android"), int(time.time())))
        return self.send_json(200, {"ok": True})


if __name__ == "__main__":
    init_db()
    print(f"FlowClone Server on :{PORT}  gateway={GATEWAY_URL or '(unset)'}", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
