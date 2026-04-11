#!/usr/bin/env bash
# 生成自签证书 + 配置 Nginx HTTPS 反代
# 用法: sudo bash setup-https.sh

set -euo pipefail

SERVER_IP="86.53.161.33"
SSL_DIR="/etc/nginx/ssl"
NGINX_CONF="/etc/nginx/sites-available/vocata-https"

echo "=== 1. 生成自签 SSL 证书 ==="
mkdir -p "$SSL_DIR"

# 生成含 IP SAN 的证书（现代浏览器要求 SAN，不认纯 CN）
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "$SSL_DIR/vocata.key" \
  -out "$SSL_DIR/vocata.crt" \
  -subj "/C=CN/ST=Beijing/O=VocaTa/CN=$SERVER_IP" \
  -addext "subjectAltName=IP:$SERVER_IP"

echo "证书已生成: $SSL_DIR/vocata.crt"

echo ""
echo "=== 2. 写入 Nginx 配置 ==="
cat > "$NGINX_CONF" <<'NGINX'
# VocaTa HTTPS 反向代理
# 前端: https://86.53.161.33  →  localhost:3000
# 后端: https://86.53.161.33/api  →  localhost:9009
# WebSocket: wss://86.53.161.33/ws  →  localhost:9009

server {
    listen 443 ssl;
    server_name 86.53.161.33;

    ssl_certificate     /etc/nginx/ssl/vocata.crt;
    ssl_certificate_key /etc/nginx/ssl/vocata.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # 前端
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Vite HMR WebSocket (开发模式)
    location /vite-hmr {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:9009/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket (AI 语音对话)
    location /ws/ {
        proxy_pass http://127.0.0.1:9009/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }
}

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name 86.53.161.33;
    return 301 https://$host$request_uri;
}
NGINX

echo "Nginx 配置已写入: $NGINX_CONF"

echo ""
echo "=== 3. 启用站点并检测配置 ==="
ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/vocata-https
nginx -t

echo ""
echo "=== 4. 重载 Nginx ==="
systemctl reload nginx

echo ""
echo "✅ 完成！访问 https://86.53.161.33"
echo "   浏览器会提示不安全（自签证书），点「高级」→「继续访问」即可"
echo "   mediaDevices.getUserMedia 在 HTTPS 下可正常使用"
