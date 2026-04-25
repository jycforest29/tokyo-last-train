#!/bin/bash
set -euo pipefail

APP_NAME="tokyo-last-train"
APP_DIR="/opt/${APP_NAME}"
JAR_FILE="${APP_DIR}/app.jar"
SERVICE_NAME="${APP_NAME}"

echo "=== Deploying ${APP_NAME} ==="

if [ -f "${APP_DIR}/app.jar.new" ]; then
    mv "${APP_DIR}/app.jar.new" "${JAR_FILE}"
    echo "JAR updated"
fi

if [ -d "${APP_DIR}/frontend" ]; then
    rm -rf /var/www/${APP_NAME}/*
    cp -r ${APP_DIR}/frontend/* /var/www/${APP_NAME}/
    echo "Frontend files deployed to Nginx"
fi

systemctl restart ${SERVICE_NAME}

# Actuator 헬스체크 — 캐시 로딩(약 2~3분) 완료 후 UP 반환
echo "Waiting for application to become healthy..."
for i in $(seq 1 48); do
    if curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
        echo "Application is healthy!"
        exit 0
    fi
    sleep 5
done

echo "ERROR: Application failed to become healthy within 4 minutes"
systemctl status ${SERVICE_NAME} --no-pager
journalctl -u ${SERVICE_NAME} --no-pager -n 50
exit 1
