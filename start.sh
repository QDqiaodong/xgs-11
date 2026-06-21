#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

find_port() {
  local port="$1"
  while lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; do
    port=$((port + 1))
  done
  printf '%s\n' "$port"
}

APP_HOST_PORT=$(find_port "${APP_HOST_PORT:-18121}")
MYSQL_HOST_PORT=$(find_port "${MYSQL_HOST_PORT:-33121}")

cat > .env.runtime <<EOF
APP_HOST_PORT=${APP_HOST_PORT}
MYSQL_HOST_PORT=${MYSQL_HOST_PORT}
EOF

docker compose --env-file .env.runtime up --build -d

echo "xgs-11 已启动"
echo "前端访问地址: http://127.0.0.1:${APP_HOST_PORT}"
echo "MySQL端口: 127.0.0.1:${MYSQL_HOST_PORT}"
