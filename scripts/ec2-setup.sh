#!/bin/bash
# =============================================================
# EC2 초기 설정 스크립트
#
# 권장 인스턴스:
#   - 리전: ap-northeast-1 (Tokyo)
#   - AMI:  Amazon Linux 2023 (x86_64)
#   - 타입: t3.small (최소, 2GB) / t3.medium (권장, 4GB)
#   - 디스크: gp3 20GB
#   - 보안 그룹: 22(본인 IP), 80, 443
#
# 인스턴스 생성 후 한 번만 실행.
# =============================================================
set -euo pipefail

APP_NAME="tokyo-last-train"

echo "=== 1. 시스템 패키지 업데이트 ==="
sudo dnf update -y

echo "=== 2. Java 21 설치 ==="
sudo dnf install -y java-21-amazon-corretto-headless

echo "=== 3. Nginx 설치 ==="
sudo dnf install -y nginx
sudo systemctl enable nginx

echo "=== 4. 앱 디렉토리 생성 ==="
sudo mkdir -p /opt/${APP_NAME}
sudo mkdir -p /var/www/${APP_NAME}
sudo chown ec2-user:ec2-user /opt/${APP_NAME}
sudo chown ec2-user:ec2-user /var/www/${APP_NAME}

echo "=== 5. systemd 서비스 등록 ==="
# JVM 힙: ODPT 전체 데이터(역/시간표/요금) 인메모리 캐시 + Spring 오버헤드 → 2GB
sudo tee /etc/systemd/system/${APP_NAME}.service > /dev/null << 'EOF'
[Unit]
Description=Tokyo Last Train Finder
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/tokyo-last-train
ExecStart=/usr/bin/java -Xms512m -Xmx2g -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Tokyo -jar /opt/tokyo-last-train/app.jar
EnvironmentFile=/opt/tokyo-last-train/.env
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable ${APP_NAME}

echo "=== 6. Nginx 설정 ==="
sudo tee /etc/nginx/conf.d/${APP_NAME}.conf > /dev/null << 'EOF'
# 액세스 로그에서 쿼리 스트링을 제거한다.
# /api/v1/stations/nearest?lat=&lng= 같은 GPS 좌표가 디스크에 남지 않도록 $request 대신 $uri를 사용.
log_format mask_qs '$remote_addr - $remote_user [$time_local] '
                   '"$request_method $uri $server_protocol" '
                   '$status $body_bytes_sent '
                   '"$http_referer" "$http_user_agent"';

server {
    listen 80 default_server;
    # 도메인 발급 후 enable-https.sh가 server_name을 교체
    server_name _;

    # http 컨텍스트의 기본 access_log를 이 server 한정으로 mask_qs 포맷으로 덮어씀
    access_log /var/log/nginx/access.log mask_qs;

    root /var/www/tokyo-last-train;
    index index.html;

    # SPA 라우팅
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 프록시
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 180s;
    }

    # GPS 좌표 엔드포인트는 한층 더 보수적으로 액세스 로그 자체를 끔
    location = /api/v1/stations/nearest {
        access_log off;
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
    }

    # Actuator는 외부 노출 차단 (헬스체크는 localhost에서만 사용)
    location /actuator/ {
        deny all;
        return 403;
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript application/xml text/xml image/svg+xml;
}
EOF

# 기본 nginx.conf의 default server 블록을 안전하게 제거
# (sed로 잘라내면 중첩 } 매칭 때문에 location 블록이 server 밖에 남는 문제 발생)
sudo python3 -c "
import re, sys
p = '/etc/nginx/nginx.conf'
with open(p) as f: s = f.read()
# http { ... } 안의 server { ... } 블록을 통째로 제거 (중첩 brace 정확히 매칭)
def strip_server(text):
    i = text.find('server {')
    if i < 0: return text
    depth = 0; j = i
    while j < len(text):
        if text[j] == '{': depth += 1
        elif text[j] == '}':
            depth -= 1
            if depth == 0: break
        j += 1
    return text[:i] + text[j+1:]
new = strip_server(s)
if new != s:
    with open(p, 'w') as f: f.write(new)
    print('default server block removed')
else:
    print('no default server block found (already clean)')
"

sudo nginx -t && sudo systemctl enable --now nginx

echo "=== 7. 환경변수 파일 생성 ==="
if [ ! -f /opt/${APP_NAME}/.env ]; then
    cat > /opt/${APP_NAME}/.env << 'EOF'
ODPT_API_KEY=여기에_ODPT_API_키_입력
EOF
    echo "⚠  /opt/${APP_NAME}/.env 파일에 ODPT_API_KEY를 설정하세요"
fi

echo ""
echo "=== 설정 완료 ==="
echo ""
echo "다음 단계:"
echo "  1. /opt/${APP_NAME}/.env 에 ODPT_API_KEY 설정"
echo "  2. 보안 그룹에서 80, 443 포트 인바운드 허용"
echo "  3. Jenkins에서 파이프라인 실행 → 첫 배포"
echo "  4. 도메인 DNS A 레코드 → EC2 퍼블릭 IP 등록"
echo "  5. scripts/enable-https.sh <도메인> [이메일] 실행 → HTTPS 활성화"
