#!/bin/bash
# =============================================================
# HTTPS 활성화 스크립트 (Let's Encrypt + certbot)
#
# 사전 조건:
#   1. 도메인 등록 완료
#   2. 도메인 DNS A 레코드 → EC2 퍼블릭 IP 등록 + 전파 완료
#      (`dig <domain> +short` 으로 IP 확인)
#   3. 보안 그룹에 80, 443 인바운드 허용
#   4. ec2-setup.sh 실행 완료 + 앱이 정상 기동 중
#
# 사용법:
#   ./enable-https.sh <domain> [email]
#
# 예:
#   ./enable-https.sh tokyolasttrain.com admin@tokyolasttrain.com
# =============================================================
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "사용법: $0 <domain> [email]"
    echo "예: $0 tokyolasttrain.com admin@tokyolasttrain.com"
    exit 1
fi

DOMAIN="$1"
EMAIL="${2:-}"
APP_NAME="tokyo-last-train"
NGINX_CONF="/etc/nginx/conf.d/${APP_NAME}.conf"

echo "=== 1. DNS 전파 확인 ==="
RESOLVED=$(dig +short "$DOMAIN" | head -n1)
EXPECTED=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 || echo "")
if [ -n "$EXPECTED" ] && [ "$RESOLVED" != "$EXPECTED" ]; then
    echo "⚠  경고: ${DOMAIN}이 ${RESOLVED}로 해석됨 (이 EC2: ${EXPECTED})"
    echo "   DNS 전파가 끝났는지 확인 후 재실행하세요."
    read -p "그래도 진행할까요? (y/N) " yn
    [[ "$yn" =~ ^[Yy]$ ]] || exit 1
fi

echo "=== 2. Certbot 설치 ==="
sudo dnf install -y certbot python3-certbot-nginx

echo "=== 3. Nginx server_name 변경 ==="
sudo sed -i "s/server_name _;/server_name ${DOMAIN};/" "$NGINX_CONF"
sudo nginx -t && sudo systemctl reload nginx

echo "=== 4. Let's Encrypt 인증서 발급 + Nginx 자동 적용 ==="
if [ -n "$EMAIL" ]; then
    sudo certbot --nginx -d "$DOMAIN" -m "$EMAIL" --agree-tos --non-interactive --redirect
else
    sudo certbot --nginx -d "$DOMAIN" --register-unsafely-without-email --agree-tos --non-interactive --redirect
fi

echo "=== 5. 자동 갱신 타이머 활성화 ==="
sudo systemctl enable --now certbot-renew.timer 2>/dev/null \
    || sudo systemctl enable --now certbot.timer

echo ""
echo "=== HTTPS 활성화 완료 ==="
echo "https://${DOMAIN} 으로 접속하세요"
echo "인증서는 90일마다 자동 갱신됩니다."
