#!/usr/bin/env bash
# 启动自建后端（需先复制 .env.example 为 .env 并填值）
set -euo pipefail
cd "$(dirname "$0")/../backend/flowclone-server"
if [ -f .env ]; then set -a; . ./.env; set +a; fi
: "${GATEWAY_URL:?GATEWAY_URL required}"
: "${GATEWAY_KEY:?GATEWAY_KEY required}"
: "${TOKEN_SECRET:?TOKEN_SECRET required}"
python3 server.py
