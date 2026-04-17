#!/bin/bash
# ============================================================
# ICON 배포 스크립트 (Windows → Linux 서버)
# Usage: ./deploy.sh [frontend|backend|all] [서버별칭]
#
# 예시:
#   ./deploy.sh all vm          # VM에 전체 배포
#   ./deploy.sh backend dev4    # 개발서버4에 백엔드만 배포
#   ./deploy.sh frontend dev6   # 개발서버6에 프론트만 배포
# ============================================================

set -euo pipefail

# ── 색상 출력 ─────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
# [2026-04-17] 유니코드 박스문자 → ASCII (Git Bash 호환)
section() { echo -e "\n${CYAN}======================================${NC}"; echo -e "${CYAN}  $*${NC}"; echo -e "${CYAN}======================================${NC}"; }

# ── 로컬 경로 ─────────────────────────────────────────────────
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$BASE_DIR/icon-backend"
FRONTEND_DIR="$BASE_DIR/icon-frontend"
JAR_NAME="icon-api-0.0.1-SNAPSHOT.jar"
JAR_LOCAL="$BACKEND_DIR/icon-api/build/libs/$JAR_NAME"

# ============================================================
# ★ 서버 설정 (환경에 맞게 수정)
# ============================================================
# [2026-04-17] SSH 키 인증 방식으로 변경 (sshpass 제거)
SSH_KEY="/c/Users/dh-le/.ssh/dev_user_ssh/id_rsa"

declare -A SERVER_HOST SERVER_PORT SERVER_USER SERVER_DEPLOY_DIR SERVER_JAVA_BIN

# VM (local_vm_rocky_docker9.6)
SERVER_HOST[vm]="192.168.118.130"
SERVER_PORT[vm]="22"
SERVER_USER[vm]="datasay"
SERVER_DEPLOY_DIR[vm]="/home/datasay/sw/icon-base"
SERVER_JAVA_BIN[vm]="/home/datasay/sw/icon/jdk-17.0.17/bin/java"

# 개발서버 4
SERVER_HOST[dev4]="개발서버4_IP"
SERVER_PORT[dev4]="22"
SERVER_USER[dev4]="dev_user"
SERVER_DEPLOY_DIR[dev4]="/home/datasay/sw/icon"
SERVER_JAVA_BIN[dev4]="/home/datasay/sw/icon/jdk-17.0.17/bin/java"

# 개발서버 6
SERVER_HOST[dev6]="개발서버6_IP"
SERVER_PORT[dev6]="22"
SERVER_USER[dev6]="dev_user"
SERVER_DEPLOY_DIR[dev6]="/home/datasay/sw/icon"
SERVER_JAVA_BIN[dev6]="/home/datasay/sw/icon/jdk-17.0.17/bin/java"

# 개발서버 7
SERVER_HOST[dev7]="개발서버7_IP"
SERVER_PORT[dev7]="22"
SERVER_USER[dev7]="dev_user"
SERVER_DEPLOY_DIR[dev7]="/home/datasay/sw/icon"
SERVER_JAVA_BIN[dev7]="/home/datasay/sw/icon/jdk-17.0.17/bin/java"

# ============================================================

# ── 인자 파싱 ─────────────────────────────────────────────────
COMPONENT="${1:-all}"    # frontend | backend | all
SERVER_ALIAS="${2:-vm}"  # vm | dev4 | dev6 | dev7

usage() {
    echo "Usage: $0 [frontend|backend|all|iconsh] [vm|dev4|dev6|dev7]"
    echo ""
    echo "  Component:"
    echo "    backend   백엔드(JAR) + icon.sh 배포"
    echo "    frontend  프론트엔드만 배포"
    echo "    all       전체 배포 (기본값)"
    echo "    iconsh    icon.sh만 배포"
    echo ""
    echo "  Server:"
    echo "    vm    로컬 VM (기본값)"
    echo "    dev4  개발서버 4"
    echo "    dev6  개발서버 6"
    echo "    dev7  개발서버 7"
    exit 1
}

# 유효성 확인
if [[ ! "$COMPONENT" =~ ^(frontend|backend|all|iconsh)$ ]]; then
    error "잘못된 컴포넌트: $COMPONENT"; usage
fi
if [[ -z "${SERVER_HOST[$SERVER_ALIAS]:-}" ]]; then
    error "알 수 없는 서버: $SERVER_ALIAS"; usage
fi

# ── 서버 접속 정보 ────────────────────────────────────────────
S_HOST="${SERVER_HOST[$SERVER_ALIAS]}"
S_PORT="${SERVER_PORT[$SERVER_ALIAS]}"
S_USER="${SERVER_USER[$SERVER_ALIAS]}"
S_DIR="${SERVER_DEPLOY_DIR[$SERVER_ALIAS]}"
S_JAVA="${SERVER_JAVA_BIN[$SERVER_ALIAS]}"
SSH_OPTS="-i $SSH_KEY -p $S_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10"
SCP_OPTS="-i $SSH_KEY -P $S_PORT -o StrictHostKeyChecking=no"

# [2026-04-17] SSH 키 파일 존재 확인
if [ ! -f "$SSH_KEY" ]; then
    error "SSH 키 파일 없음: $SSH_KEY"; exit 1
fi

ssh_run()  { ssh $SSH_OPTS "$S_USER@$S_HOST" "$@"; }
scp_send() { scp $SCP_OPTS "$@"; }

# ── 접속 확인 ─────────────────────────────────────────────────
check_connection() {
    section "서버 접속 확인: $SERVER_ALIAS ($S_USER@$S_HOST:$S_PORT)"
    if ! ssh $SSH_OPTS "$S_USER@$S_HOST" "echo ok" &>/dev/null; then
        error "서버 접속 실패: $S_USER@$S_HOST:$S_PORT"
        error "SSH 키 또는 비밀번호를 확인하세요."
        exit 1
    fi
    info "접속 성공"
}

# ─────────────────────────────────────────────────────────────
# 백엔드 배포
# ─────────────────────────────────────────────────────────────
deploy_backend() {
    section "백엔드 배포"

    # 로컬 빌드
    info "Gradle 빌드 중..."
    cd "$BACKEND_DIR"
    ./gradlew :icon-api:bootJar -x test --quiet
    cd "$BASE_DIR"

    if [ ! -f "$JAR_LOCAL" ]; then
        error "JAR 빌드 실패: $JAR_LOCAL"; exit 1
    fi
    info "빌드 완료: $JAR_LOCAL ($(du -sh "$JAR_LOCAL" | cut -f1))"

    # 서버에서 백엔드 프로세스 중지
    info "서버 백엔드 중지 중..."
    # [2026-04-17] &>/dev/null → >/dev/null 2>&1 (sh 호환), || true로 set -e 방지
    ssh_run "
        PID=\$(pgrep -f '$JAR_NAME' 2>/dev/null || true)
        if [ -n \"\$PID\" ]; then
            kill \$PID && sleep 3
            pgrep -f '$JAR_NAME' >/dev/null 2>&1 && kill -9 \$PID 2>/dev/null || true
            echo '백엔드 중지 완료'
        else
            echo '백엔드 실행 중이지 않음'
        fi
    " || true

    # JAR 전송
    info "JAR 전송 중..."
    scp_send "$JAR_LOCAL" "$S_USER@$S_HOST:$S_DIR/$JAR_NAME"
    info "JAR 전송 완료"

    # 서버에서 백엔드 기동
    info "서버 백엔드 기동 중..."
    ssh_run "
        cd '$S_DIR'
        mkdir -p logs
        JAVA_OPTS='-XX:MaxDirectMemorySize=8192m -Xmx2048m -XX:+UseG1GC -Dfile.encoding=UTF-8'
        nohup $S_JAVA \$JAVA_OPTS -jar '$S_DIR/$JAR_NAME' > '$S_DIR/logs/icon-backend.log' 2>&1 &
        sleep 4
        PID=\$(pgrep -f '$JAR_NAME' 2>/dev/null || true)
        if [ -n \"\$PID\" ]; then
            echo \"백엔드 기동 성공 (PID: \$PID)\"
        else
            echo '백엔드 기동 실패 — 로그 확인: $S_DIR/logs/icon-backend.log'
            exit 1
        fi
    "
    info "백엔드 배포 완료 → http://$S_HOST:11100"
}

# ─────────────────────────────────────────────────────────────
# 프론트엔드 배포 (standalone 빌드: 작업자 PC에서 빌드 후 실행 파일만 전송)
# 서버에서 npm install / build 불필요 — node 실행만 필요
# ─────────────────────────────────────────────────────────────
deploy_frontend() {
    section "프론트엔드 배포"

    # 로컬 빌드
    info "로컬에서 Next.js standalone 빌드 중..."
    cd "$FRONTEND_DIR"
    npm install --silent
    npm run build
    cd "$BASE_DIR"

    local STANDALONE_DIR="$FRONTEND_DIR/.next/standalone"
    if [ ! -d "$STANDALONE_DIR" ]; then
        error "standalone 빌드 결과 없음: $STANDALONE_DIR"
        error "next.config.ts에 output: 'standalone' 설정을 확인하세요."
        exit 1
    fi
    info "빌드 완료"

    # 서버에서 프론트 프로세스 중지
    info "서버 프론트엔드 중지 중..."
    # [2026-04-17] &>/dev/null → >/dev/null 2>&1 (sh 호환), || true로 set -e 방지
    ssh_run "
        PID=\$(pgrep -f 'node.*server.js' 2>/dev/null || true)
        if [ -n \"\$PID\" ]; then
            kill \$PID && sleep 2
            pgrep -f 'node.*server.js' >/dev/null 2>&1 && kill -9 \$PID 2>/dev/null || true
            echo '프론트엔드 중지 완료'
        else
            echo '프론트엔드 실행 중이지 않음'
        fi
    " || true

    # 배포 파일 전송
    # standalone/        → 실행에 필요한 최소 node_modules 포함
    # .next/static/      → 정적 에셋 (JS, CSS)
    # public/            → 이미지 등 public 파일
    info "빌드 결과물 전송 중..."
    ssh_run "mkdir -p '$S_DIR/frontend/.next/static' '$S_DIR/frontend/public' '$S_DIR/logs'"

    scp_send -r "$STANDALONE_DIR/." "$S_USER@$S_HOST:$S_DIR/frontend/"
    scp_send -r "$FRONTEND_DIR/.next/static/." "$S_USER@$S_HOST:$S_DIR/frontend/.next/static/"
    scp_send -r "$FRONTEND_DIR/public/." "$S_USER@$S_HOST:$S_DIR/frontend/public/"
    info "전송 완료"

    # 서버에서 기동 (node server.js)
    info "서버 프론트엔드 기동 중..."
    ssh_run "
        mkdir -p '$S_DIR/logs'
        cd '$S_DIR/frontend'
        export PORT=5160
        export HOSTNAME=0.0.0.0
        nohup node server.js > '$S_DIR/logs/icon-frontend.log' 2>&1 &
        sleep 3
        PID=\$(pgrep -f 'node.*server.js' 2>/dev/null || true)
        if [ -n \"\$PID\" ]; then
            echo \"프론트엔드 기동 성공 (PID: \$PID)\"
        else
            echo '프론트엔드 기동 실패 — 로그 확인: $S_DIR/logs/icon-frontend.log'
            exit 1
        fi
    "
    info "프론트엔드 배포 완료 → http://$S_HOST:5160"
}

# ─────────────────────────────────────────────────────────────
# icon.sh 배포
# ─────────────────────────────────────────────────────────────
deploy_iconsh() {
    section "icon.sh 배포"
    scp_send "$BASE_DIR/icon.sh" "$S_USER@$S_HOST:$S_DIR/icon.sh"
    ssh_run "chmod +x '$S_DIR/icon.sh'"
    info "icon.sh 배포 완료"
}

# ─────────────────────────────────────────────────────────────
# 메인
# ─────────────────────────────────────────────────────────────
section "ICON 배포 시작"
info "컴포넌트 : $COMPONENT"
info "대상 서버 : $SERVER_ALIAS ($S_USER@$S_HOST)"

check_connection

case "$COMPONENT" in
    backend)
        deploy_backend
        deploy_iconsh
        ;;
    frontend) deploy_frontend ;;
    all)
        deploy_backend
        deploy_frontend
        deploy_iconsh
        ;;
    iconsh) deploy_iconsh ;;
esac

section "배포 완료"
info "  백엔드  → http://$S_HOST:11100"
info "  프론트  → http://$S_HOST:5160"
info "  Swagger → http://$S_HOST:11100/swagger-ui.html"
