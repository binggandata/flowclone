package com.example.flowclone

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * NGSFlow AI Gateway 客户端（https://your-gateway.example）
 * 生图：grok-imagine-image-1.5；生视频：grok-imagine-video-1.5
 * 流程：POST /v1/generations → 轮询 GET /v1/jobs/{id} → GET /v1/jobs/{id}/outputs/{i}/content
 */
object Gateway {
    private val base = BuildConfig.GATEWAY_URL.trimEnd('/')
    private val key = BuildConfig.GATEWAY_KEY

    fun isConfigured(): Boolean = key.isNotBlank() || AuthStore.isLoggedIn

    /** 自建 Server 模式用用户 token；未登录时回退到内置网关 Key（本地调试直连） */
    private fun authHeader(): String {
        val t = AuthStore.token
        return if (!t.isNullOrBlank()) "Bearer $t" else "Bearer $key"
    }

    suspend fun probe(timeoutMs: Int = 1500): Boolean = withContext(Dispatchers.IO) {
        try {
            val c = (java.net.URL("$base/v1/models").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("Authorization", authHeader())
            }
            val code = c.responseCode
            c.disconnect()
            code in 200..499
        } catch (e: Exception) { false }
    }

    private fun open(path: String, method: String = "GET"): HttpURLConnection {
        val c = URL("$base$path").openConnection() as HttpURLConnection
        c.requestMethod = method
        c.setRequestProperty("Authorization", authHeader())
        c.connectTimeout = 30_000
        c.readTimeout = 600_000
        return c
    }

    private fun readBody(c: HttpURLConnection): String {
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    private suspend fun postJson(path: String, body: JSONObject, idempotencyKey: String? = null): JSONObject {
        var c = open(path, "POST")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        if (idempotencyKey != null) c.setRequestProperty("Idempotency-Key", idempotencyKey)
        c.outputStream.use { os: OutputStream -> os.write(body.toString().toByteArray()) }
        var text = readBody(c)
        if (c.responseCode == 401 && refreshAccess()) {
            c = open(path, "POST")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            if (idempotencyKey != null) c.setRequestProperty("Idempotency-Key", idempotencyKey)
            c.outputStream.use { os: OutputStream -> os.write(body.toString().toByteArray()) }
            text = readBody(c)
        }
        if (c.responseCode !in 200..299) throw RuntimeException("HTTP ${c.responseCode}: ${text.take(260)}")
        return JSONObject(text)
    }

    // ---------- 账号（自建 Server） ----------
    data class AuthResult(
        val token: String, val refreshToken: String?, val email: String?,
        val userId: String?, val credits: Int,
    )

    private suspend fun authPost(path: String, body: JSONObject): AuthResult = withContext(Dispatchers.IO) {
        val c = URL("$base$path").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { os: OutputStream -> os.write(body.toString().toByteArray()) }
        val text = readBody(c)
        if (c.responseCode !in 200..299) {
            val msg = try { JSONObject(text).optString("error") } catch (e: Exception) { "" }
            throw RuntimeException(msg.ifBlank { "请求失败 HTTP ${c.responseCode}" })
        }
        val o = JSONObject(text)
        AuthResult(
            o.optString("token"), o.optString("refreshToken").ifBlank { null },
            o.optString("email").ifBlank { null },
            o.optString("userId").ifBlank { null }, o.optInt("credits", 0),
        )
    }

    /** 用 refreshToken 换新 access token（轮换 refresh） */
    suspend fun refreshAccess(): Boolean = withContext(Dispatchers.IO) {
        val rt = AuthStore.refreshToken ?: return@withContext false
        try {
            val c = URL("$base/v1/auth/refresh").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { os: OutputStream ->
                os.write(JSONObject().put("refreshToken", rt).toString().toByteArray())
            }
            val text = readBody(c)
            if (c.responseCode !in 200..299) return@withContext false
            val o = JSONObject(text)
            val newToken = o.optString("token")
            if (newToken.isBlank()) return@withContext false
            AuthStore.save(
                newToken, o.optString("refreshToken").ifBlank { rt },
                AuthStore.email, o.optString("userId").ifBlank { AuthStore.userId },
                o.optInt("credits", AuthStore.credits),
            )
            true
        } catch (e: Exception) { false }
    }

    /** 服务端通知列表 */
    data class ServerNotification(val title: String, val body: String, val createdAt: Long)

    suspend fun notifications(limit: Int = 20): List<ServerNotification> = withContext(Dispatchers.IO) {
        try {
            var c = open("/v1/notifications?limit=$limit")
            var text = readBody(c)
            if (c.responseCode == 401 && refreshAccess()) {
                c = open("/v1/notifications?limit=$limit")
                text = readBody(c)
            }
            if (c.responseCode !in 200..299) return@withContext emptyList()
            val arr = JSONObject(text).optJSONArray("data") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ServerNotification(o.optString("title"), o.optString("body"), o.optLong("created_at"))
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun register(email: String, password: String): AuthResult =
        authPost("/v1/auth/register", JSONObject().put("email", email).put("password", password))

    suspend fun login(email: String, password: String): AuthResult =
        authPost("/v1/auth/login", JSONObject().put("email", email).put("password", password))

    suspend fun deviceLogin(deviceId: String): AuthResult =
        authPost("/v1/auth/device", JSONObject().put("deviceId", deviceId))

    data class JobInfo(
        val id: String, val model: String, val modality: String,
        val status: String, val prompt: String, val createdAt: Long,
    )

    suspend fun jobs(limit: Int = 20): List<JobInfo> = withContext(Dispatchers.IO) {
        try {
            var c = open("/v1/jobs?limit=$limit")
            var text = readBody(c)
            if (c.responseCode == 401 && refreshAccess()) {
                c = open("/v1/jobs?limit=$limit")
                text = readBody(c)
            }
            if (c.responseCode !in 200..299) return@withContext emptyList()
            val arr = JSONObject(text).optJSONArray("data") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                JobInfo(
                    o.optString("id"), o.optString("model"), o.optString("modality"),
                    o.optString("status"), o.optString("prompt"), o.optLong("created_at"),
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteAccount(): Boolean = withContext(Dispatchers.IO) {
        try {
            var c = open("/v1/account", "DELETE")
            var text = readBody(c)
            if (c.responseCode == 401 && refreshAccess()) {
                c = open("/v1/account", "DELETE")
                text = readBody(c)
            }
            c.responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun cancelJob(jobId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var c = open("/v1/jobs/$jobId/cancel", "POST")
            var text = readBody(c)
            if (c.responseCode == 401 && refreshAccess()) {
                c = open("/v1/jobs/$jobId/cancel", "POST")
                text = readBody(c)
            }
            c.responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun agentChat(messages: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        fun call(): Pair<Int, String> {
            val arr = JSONArray()
            messages.forEach { (role, content) ->
                arr.put(JSONObject().put("role", role).put("content", content))
            }
            val body = JSONObject().put("messages", arr)
            val c = open("/v1/agent/chat", "POST")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { os -> os.write(body.toString().toByteArray()) }
            return c.responseCode to readBody(c)
        }
        var res = call()
        if (res.first == 401 && refreshAccess()) res = call()
        if (res.first !in 200..299) throw RuntimeException("智能体暂时不可用（HTTP ${res.first}）")
        JSONObject(res.second).optString("reply")
    }

    suspend fun meCredits(): Int = withContext(Dispatchers.IO) {
        try {
            var c = open("/v1/auth/me")
            var text = readBody(c)
            if (c.responseCode == 401 && refreshAccess()) {
                c = open("/v1/auth/me")
                text = readBody(c)
            }
            if (c.responseCode !in 200..299) return@withContext -1
            JSONObject(text).optInt("credits", -1)
        } catch (e: Exception) { -1 }
    }

    suspend fun submitImage(
        prompt: String,
        size: String,
        count: Int,
        referencePath: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val inputs = JSONArray()
        if (referencePath != null && File(referencePath).exists()) {
            val fileId = upload(File(referencePath))
            inputs.put(JSONObject().put("role", "image-reference").put("fileId", fileId))
        }
        val body = JSONObject().apply {
            put("model", "grok-imagine-image-1.5")
            put("modality", "image")
            put("prompt", prompt)
            put("inputs", inputs)
            put("parameters", JSONObject().put("count", count.coerceIn(1, 4)).put("size", size))
        }
        val accepted = postJson("/v1/generations", body, UUID.randomUUID().toString())
        val job = accepted.optJSONObject("data") ?: accepted
        job.optString("id").also { if (it.isBlank()) throw RuntimeException("网关未返回任务 ID") }
    }

    suspend fun submitVideo(
        prompt: String,
        aspectRatio: String,
        durationSeconds: Int,
        quality: String,
        referencePath: String?,
    ): String = withContext(Dispatchers.IO) {
        val inputs = JSONArray()
        if (referencePath != null && File(referencePath).exists()) {
            val fileId = upload(File(referencePath))
            inputs.put(JSONObject().put("role", "image-reference").put("fileId", fileId))
        }
        val body = JSONObject().apply {
            put("model", "grok-imagine-video-1.5")
            put("modality", "video")
            put("prompt", prompt)
            put("inputs", inputs)
            put("parameters", JSONObject()
                .put("durationSeconds", durationSeconds.coerceIn(6, 15))
                .put("aspectRatio", aspectRatio)
                .put("quality", quality))
        }
        val accepted = postJson("/v1/generations", body, UUID.randomUUID().toString())
        val job = accepted.optJSONObject("data") ?: accepted
        job.optString("id").also { if (it.isBlank()) throw RuntimeException("网关未返回任务 ID") }
    }

    suspend fun waitAndDownload(
        jobId: String,
        isVideo: Boolean,
        outDir: File,
        onProgress: (Float) -> Unit = {},
    ): List<File> = withContext(Dispatchers.IO) {
        val ext = if (isVideo) "mp4" else "jpg"
        val estimateMs = if (isVideo) 300_000L else 25_000L
        val started = System.currentTimeMillis()
        val deadline = started + estimateMs + 8 * 60_000
        var job: JSONObject = JSONObject()
        var lastStatus = "queued"
        // 首次拉取状态
        run {
            val c = open("/v1/jobs/$jobId")
            val text = readBody(c)
            if (c.responseCode == 404) throw RuntimeException("任务不存在或已过期")
            if (c.responseCode in 200..299) {
                job = JSONObject(text).optJSONObject("data") ?: JSONObject(text)
                lastStatus = job.optString("status")
            }
        }
        while (job.optString("status") !in listOf("succeeded", "failed", "canceled")) {
            delay(3_000)
            if (System.currentTimeMillis() > deadline) throw RuntimeException("等待网关超时")
            val c = open("/v1/jobs/$jobId")
            val text = readBody(c)
            if (c.responseCode == 404) throw RuntimeException("任务不存在或已过期")
            if (c.responseCode !in 200..299) throw RuntimeException("HTTP ${c.responseCode}: ${text.take(200)}")
            job = JSONObject(text).optJSONObject("data") ?: JSONObject(text)
            lastStatus = job.optString("status")
            val elapsed = (System.currentTimeMillis() - started).toFloat()
            onProgress((elapsed / estimateMs * 90f).coerceIn(5f, 90f))
        }
        if (job.optString("status") != "succeeded") {
            val err = job.optJSONObject("error")
            throw RuntimeException("生成失败($lastStatus): ${err?.optString("message") ?: err?.optString("code") ?: ""}")
        }
        val results = job.optJSONArray("results") ?: JSONArray()
        if (results.length() == 0) {
            val keys = job.keys().asSequence().joinToString(",")
            throw RuntimeException("网关没有返回结果 status=${job.optString("status")} resultExists=${job.has("result")} keys=[$keys]")
        }
        outDir.mkdirs()
        val files = mutableListOf<File>()
        for (i in 0 until results.length()) {
            val out = File(outDir, "gen_${jobId.take(8)}_${System.currentTimeMillis()}_$i.$ext")
            var ok = false
            var tries = 0
            while (!ok && tries < 150) {
                tries++
                if (out.exists()) out.delete()
                val c = open("/v1/jobs/$jobId/outputs/$i/content")
                val code = c.responseCode
                if (code == 200 || code == 201) {
                    c.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
                    // 校验文件头（防止把 JSON/错误页当成媒体保存）
                    val head = ByteArray(16)
                    var headLen = 0
                    if (out.length() >= 8) {
                        out.inputStream().use { ins ->
                            while (headLen < head.size) {
                                val n = ins.read(head, headLen, head.size - headLen)
                                if (n <= 0) break
                                headLen += n
                            }
                        }
                    }
                    val looksMedia = headLen >= 8 && (
                        (head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte()) || // JPEG
                        (head[0] == 0x89.toByte() && head[1] == 0x50.toByte()) || // PNG
                        (String(head, 0, 4, Charsets.ISO_8859_1) == "RIFF") ||   // WebP/WAV
                        (String(head, 0, 4, Charsets.ISO_8859_1) == "GIF8") ||
                        (String(head, 0, 4, Charsets.ISO_8859_1) == "fLaC") ||
                        (String(head, 0, 4, Charsets.ISO_8859_1) == "OggS") ||
                        (String(head, 4, 4, Charsets.ISO_8859_1) in listOf("ftyp", "moov", "mdat"))
                        )
                    if (out.length() > 1024 && looksMedia) { files.add(out); ok = true } else out.delete()
                } else if (code == 202 || code == 404 || code == 409 || code == 503) {
                    readBody(c)
                    onProgress((91 + tries / 2).coerceAtMost(99).toFloat())
                    delay(4_000)
                } else {
                    throw RuntimeException("下载失败 HTTP $code")
                }
            }
            if (!ok) throw RuntimeException("下载超时（服务端仍在缓存文件）")
        }
        onProgress(100f)
        files
    }

    private suspend fun upload(file: File): String {
        var result = uploadOnce(file)
        if (result.first == 401 && refreshAccess()) result = uploadOnce(file)
        if (result.first !in 200..299) throw RuntimeException("上传失败 HTTP ${result.first}")
        val parsed = JSONObject(result.second)
        val data = parsed.optJSONObject("data") ?: parsed
        return data.optString("id").ifBlank { data.optString("fileId") }
    }

    private suspend fun uploadOnce(file: File): Pair<Int, String> {
        val boundary = "----flowclone" + UUID.randomUUID().toString().replace("-", "")
        val c = open("/v1/files", "POST")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        c.outputStream.use { os ->
            os.write(("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n").toByteArray())
            os.write(file.readBytes())
            os.write("\r\n--$boundary--\r\n".toByteArray())
        }
        return c.responseCode to readBody(c)
    }
}
