#!/bin/bash
# ============================================================
# ICON Platform 통합 실행 스크립트
# Usage: ./icon.sh {start|stop|restart|status|setup}
# ============================================================

set -euo pipefail

# ── 경로 설정 ───────────────────────────────────────────────
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$BASE_DIR/icon-backend"
FRONTEND_DIR="$BASE_DIR/icon-frontend"
LOG_DIR="$BASE_DIR/logs"
MIGRATION_DIR="$BACKEND_DIR/icon-api/src/main/resources/db/migrations"
JAR_NAME="icon-api-0.0.1-SNAPSHOT.jar"
# [2026-04-17] 배포 시 JAR는 BASE_DIR에 위치 (deploy.sh와 일치)
JAR_PATH="$BASE_DIR/$JAR_NAME"
JAR_BUILD_PATH="$BACKEND_DIR/icon-api/build/libs/$JAR_NAME"
# [2026-04-17] standalone 빌드 배포 경로 (deploy.sh와 일치)
FRONTEND_STANDALONE_DIR="$BASE_DIR/frontend"

# ── DB 접속 정보 ─────────────────────────────────────────────
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="icon"
DB_USER="postgres"
DB_PASS="1234"
export PGPASSWORD="$DB_PASS"

# ── JVM 옵션 ─────────────────────────────────────────────────
JAVA_OPTS=(
    -XX:MaxDirectMemorySize=8192m
    -Xmx2048m
    -XX:+UseG1GC
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath="$LOG_DIR/heapdump"
    -Dfile.encoding=UTF-8
)

# ── 색상 출력 ─────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
section() { echo -e "\n${GREEN}══════════════════════════════════════${NC}"; echo -e "${GREEN}  $*${NC}"; echo -e "${GREEN}══════════════════════════════════════${NC}"; }

# ─────────────────────────────────────────────────────────────
# 1. 의존성 확인 및 설치
# ─────────────────────────────────────────────────────────────
detect_os() {
    if [ -f /etc/debian_version ]; then echo "debian"
    elif [ -f /etc/redhat-release ]; then echo "redhat"
    else echo "unknown"
    fi
}

install_pkg() {
    local pkg="$1"
    local os
    os=$(detect_os)
    info "패키지 설치 중: $pkg"
    if [ "$os" = "debian" ]; then
        sudo apt-get update -qq && sudo apt-get install -y "$pkg"
    elif [ "$os" = "redhat" ]; then
        sudo yum install -y "$pkg"
    else
        error "지원하지 않는 OS입니다. 수동으로 $pkg 를 설치하세요."; exit 1
    fi
}

check_java() {
    section "Java 17 확인"
    # JAVA_HOME 또는 PATH에서 java 탐색
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA_BIN="$JAVA_HOME/bin/java"
    elif command -v java &>/dev/null; then
        JAVA_BIN=$(command -v java)
    else
        JAVA_BIN=""
    fi

    if [ -n "$JAVA_BIN" ]; then
        JAVA_VER=$("$JAVA_BIN" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
        if [ "$JAVA_VER" -ge 17 ] 2>/dev/null; then
            info "Java $JAVA_VER 확인됨: $JAVA_BIN"; return
        else
            warn "Java $JAVA_VER 감지 (17 이상 필요)"
        fi
    fi

    warn "Java 17 미설치 → 설치를 진행합니다"
    local os
    os=$(detect_os)
    if [ "$os" = "debian" ]; then
        sudo apt-get update -qq
        sudo apt-get install -y openjdk-17-jdk
    elif [ "$os" = "redhat" ]; then
        sudo yum install -y java-17-openjdk-devel
    else
        error "Java 17을 수동으로 설치 후 JAVA_HOME을 설정하세요."; exit 1
    fi
    JAVA_BIN=$(command -v java)
    info "Java 설치 완료: $JAVA_BIN"
}

check_node() {
    section "Node.js 확인"
    if command -v node &>/dev/null; then
        NODE_VER=$(node -v | sed 's/v//' | cut -d. -f1)
        if [ "$NODE_VER" -ge 18 ] 2>/dev/null; then
            info "Node.js v$(node -v) 확인됨"; return
        else
            warn "Node.js v$(node -v) 감지 (18 이상 필요)"
        fi
    fi

    warn "Node.js 18 이상 미설치 → 설치를 진행합니다"
    local os
    os=$(detect_os)
    if [ "$os" = "debian" ]; then
        curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
        sudo apt-get install -y nodejs
    elif [ "$os" = "redhat" ]; then
        curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
        # [2026-04-17] 기존 nodejs/npm 제거 후 설치 (appstream 버전 충돌 방지)
        if command -v dnf &>/dev/null; then
            sudo dnf remove -y nodejs npm nodejs-full-i18n 2>/dev/null || true
            sudo dnf install -y nodejs --allowerasing
        else
            sudo yum remove -y nodejs npm 2>/dev/null || true
            sudo yum install -y nodejs --allowerasing
        fi
    else
        error "Node.js를 수동으로 설치하세요 (https://nodejs.org)"; exit 1
    fi
    info "Node.js 설치 완료: $(node -v)"
}

check_postgres() {
    section "PostgreSQL 확인"
    if command -v psql &>/dev/null; then
        info "PostgreSQL 클라이언트 확인됨: $(psql --version | head -1)"
    else
        warn "PostgreSQL 미설치 → 설치를 진행합니다"
        local os
        os=$(detect_os)
        if [ "$os" = "debian" ]; then
            sudo apt-get update -qq
            sudo apt-get install -y postgresql postgresql-client
        elif [ "$os" = "redhat" ]; then
            sudo yum install -y postgresql-server postgresql
            sudo postgresql-setup initdb
        fi
    fi

    # [2026-04-17] pg_isready: TCP(-h) 실패 시 unix socket으로 재시도
    pg_is_running() {
        pg_isready -h "$DB_HOST" -p "$DB_PORT" -q 2>/dev/null || \
        pg_isready -q 2>/dev/null || \
        pgrep -f "postgres.*-D" >/dev/null 2>&1
    }

    if ! pg_is_running; then
        warn "PostgreSQL 서비스가 실행 중이지 않습니다 → 시작합니다"
        # [2026-04-17] Rocky/RHEL 서비스명 탐색: systemctl로 active 여부 확인
        local pg_service=""
        for svc in postgresql-17 postgresql-16 postgresql-15 postgresql-14 postgresql; do
            if systemctl list-unit-files 2>/dev/null | grep -q "^${svc}\.service"; then
                pg_service="$svc"; break
            fi
        done

        if [ -n "$pg_service" ]; then
            sudo systemctl start "$pg_service"
            sudo systemctl enable "$pg_service"
        else
            error "PostgreSQL 서비스를 찾을 수 없습니다. 수동으로 시작하세요."; exit 1
        fi
        sleep 2
        if ! pg_is_running; then
            error "PostgreSQL 시작 실패. 수동으로 확인하세요."; exit 1
        fi
    fi
    info "PostgreSQL 서비스 실행 중"
}

check_deps() {
    check_java
    check_node
    check_postgres
}

# ─────────────────────────────────────────────────────────────
# 2. DB 생성 및 초기화
# ─────────────────────────────────────────────────────────────
setup_database() {
    section "DB 초기화"

    # postgres 슈퍼유저로 DB 존재 확인
    DB_EXISTS=$(sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>/dev/null || true)

    if [ "$DB_EXISTS" != "1" ]; then
        info "DB '$DB_NAME' 생성 중..."
        sudo -u postgres psql -c "CREATE DATABASE $DB_NAME ENCODING 'UTF8' LC_COLLATE 'en_US.UTF-8' LC_CTYPE 'en_US.UTF-8' TEMPLATE template0;" 2>/dev/null || \
        sudo -u postgres psql -c "CREATE DATABASE $DB_NAME;"
        info "DB '$DB_NAME' 생성 완료"
    else
        info "DB '$DB_NAME' 이미 존재함"
    fi

    # postgres 사용자 비밀번호 설정
    sudo -u postgres psql -c "ALTER USER $DB_USER PASSWORD '$DB_PASS';" &>/dev/null || true

    # pg_hba.conf md5 인증 확인 (password 접속 가능하도록)
    PG_HBA=$(sudo -u postgres psql -tAc "SHOW hba_file;" 2>/dev/null || true)
    if [ -n "$PG_HBA" ]; then
        if ! sudo grep -q "host.*$DB_NAME.*$DB_USER.*md5" "$PG_HBA" 2>/dev/null; then
            info "pg_hba.conf에 md5 인증 규칙 추가 중..."
            echo "host    $DB_NAME    $DB_USER    127.0.0.1/32    md5" | sudo tee -a "$PG_HBA" > /dev/null
            # [2026-04-17] pg_lsclusters 제거 (Debian 전용) → systemctl reload 사용
            for svc in postgresql-17 postgresql-16 postgresql-15 postgresql-14 postgresql; do
                if systemctl list-unit-files 2>/dev/null | grep -q "^${svc}\.service"; then
                    sudo systemctl reload "$svc" 2>/dev/null || true; break
                fi
            done
        fi
    fi
}

# ─────────────────────────────────────────────────────────────
# 3. DB 마이그레이션
# ─────────────────────────────────────────────────────────────
run_migrations() {
    section "DB 마이그레이션"

    local psql_cmd="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"

    # 마이그레이션 추적 테이블 생성
    $psql_cmd -c "
        CREATE TABLE IF NOT EXISTS icon_migrations (
            id          SERIAL      PRIMARY KEY,
            version     VARCHAR(50) NOT NULL UNIQUE,
            filename    VARCHAR(255) NOT NULL,
            applied_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
            checksum    VARCHAR(64)
        );
    " &>/dev/null

    if [ ! -d "$MIGRATION_DIR" ]; then
        warn "마이그레이션 디렉토리 없음: $MIGRATION_DIR (건너뜀)"
        return
    fi

    # V숫자__xxx.sql 파일을 정렬하여 순서대로 실행
    local applied=0
    local skipped=0

    for sql_file in $(ls "$MIGRATION_DIR"/V*.sql 2>/dev/null | sort); do
        local filename
        filename=$(basename "$sql_file")
        local version
        version=$(echo "$filename" | sed -E 's/(V[0-9]+).*/\1/')
        local checksum
        checksum=$(md5sum "$sql_file" | awk '{print $1}')

        # 이미 적용된 버전 확인
        local already_applied
        already_applied=$($psql_cmd -tAc "SELECT COUNT(*) FROM icon_migrations WHERE version='$version'" 2>/dev/null || echo "0")

        if [ "$already_applied" = "1" ]; then
            info "  SKIP  $filename (이미 적용됨)"
            ((skipped++)) || true
            continue
        fi

        info "  APPLY $filename"
        if $psql_cmd -f "$sql_file" &>/dev/null; then
            $psql_cmd -c "
                INSERT INTO icon_migrations (version, filename, checksum)
                VALUES ('$version', '$filename', '$checksum');
            " &>/dev/null
            info "  ✓     $filename 적용 완료"
            ((applied++)) || true
        else
            error "마이그레이션 실패: $filename"
            error "수동으로 확인하세요: $sql_file"
            exit 1
        fi
    done

    info "마이그레이션 완료 (신규: $applied, 기존: $skipped)"
}

# ─────────────────────────────────────────────────────────────
# 4. 백엔드 빌드 및 기동
# ─────────────────────────────────────────────────────────────
build_backend() {
    section "백엔드 빌드"
    if [ ! -d "$BACKEND_DIR" ]; then
        error "백엔드 소스 없음: $BACKEND_DIR"
        error "서버에서는 rebuild 불가. 작업자 PC에서 배포하세요."; exit 1
    fi
    info "Gradle 빌드 중... (시간이 걸릴 수 있습니다)"
    cd "$BACKEND_DIR"
    ./gradlew :icon-api:bootJar -x test --quiet
    cd "$BASE_DIR"
    # [2026-04-17] 빌드 후 JAR를 BASE_DIR로 복사 (배포 경로와 일치)
    cp "$JAR_BUILD_PATH" "$JAR_PATH"
    info "백엔드 빌드 완료: $JAR_PATH"
}

start_backend() {
    local PID
    PID=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)
    if [ -n "$PID" ]; then
        info "백엔드 이미 실행 중 (PID: $PID)"; return
    fi

    if [ ! -f "$JAR_PATH" ]; then
        # [2026-04-17] 서버에서 빌드 시도 제거 → 배포 안내
        error "JAR 파일 없음: $JAR_PATH"
        error "작업자 PC에서 './deploy.sh backend vm' 을 실행하여 배포하세요."
        exit 1
    fi

    mkdir -p "$LOG_DIR"
    section "백엔드 기동"
    nohup "$JAVA_BIN" "${JAVA_OPTS[@]}" -jar "$JAR_PATH" \
        > "$LOG_DIR/icon-backend.log" 2>&1 &
    sleep 3

    PID=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)
    if [ -n "$PID" ]; then
        info "백엔드 기동 성공 (PID: $PID) → 포트 11100"
    else
        error "백엔드 기동 실패. 로그 확인: $LOG_DIR/icon-backend.log"; exit 1
    fi
}

stop_backend() {
    local PID
    PID=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)
    if [ -z "$PID" ]; then info "백엔드 실행 중이지 않음"; return; fi

    info "백엔드 종료 중 (PID: $PID)..."
    kill "$PID"
    sleep 3
    if pgrep -f "$JAR_NAME" &>/dev/null; then
        warn "강제 종료 중..."
        kill -9 "$PID" 2>/dev/null || true
    fi
    info "백엔드 종료 완료"
}

# ─────────────────────────────────────────────────────────────
# 5. 프론트엔드 빌드 및 기동
# ─────────────────────────────────────────────────────────────
build_frontend() {
    section "프론트엔드 빌드"
    cd "$FRONTEND_DIR"
    info "npm install 중..."
    npm install --silent
    info "Next.js 빌드 중..."
    npm run build
    cd "$BASE_DIR"
    info "프론트엔드 빌드 완료"
}

fe_is_running() {
    # [2026-04-17] Next.js standalone은 프로세스명을 'next-server'로 변경 → 포트로 체크
    ss -tlnp 2>/dev/null | grep -q ":5160"
}

fe_get_pid() {
    ss -tlnp 2>/dev/null | grep ":5160" | grep -oP 'pid=\K[0-9]+' | head -1 || true
}

start_frontend() {
    if fe_is_running; then
        info "프론트엔드 이미 실행 중 (PID: $(fe_get_pid))"; return
    fi

    if [ ! -f "$FRONTEND_STANDALONE_DIR/server.js" ]; then
        error "프론트엔드 배포 파일 없음: $FRONTEND_STANDALONE_DIR/server.js"
        error "작업자 PC에서 './deploy.sh frontend vm' 을 실행하여 배포하세요."
        exit 1
    fi

    mkdir -p "$LOG_DIR"
    section "프론트엔드 기동"
    cd "$FRONTEND_STANDALONE_DIR"
    PORT=5160 HOSTNAME=0.0.0.0 nohup node server.js > "$LOG_DIR/icon-frontend.log" 2>&1 &
    cd "$BASE_DIR"
    sleep 4

    if fe_is_running; then
        info "프론트엔드 기동 성공 (PID: $(fe_get_pid)) → 포트 5160"
    else
        error "프론트엔드 기동 실패. 로그 확인: $LOG_DIR/icon-frontend.log"; exit 1
    fi
}

stop_frontend() {
    local PID
    PID=$(fe_get_pid)
    if [ -z "$PID" ]; then info "프론트엔드 실행 중이지 않음"; return; fi

    info "프론트엔드 종료 중 (PID: $PID)..."
    kill "$PID" 2>/dev/null || true
    sleep 2
    if fe_is_running; then
        kill -9 "$PID" 2>/dev/null || true
    fi
    info "프론트엔드 종료 완료"
}

# ─────────────────────────────────────────────────────────────
# 6. 메인 커맨드
# ─────────────────────────────────────────────────────────────
cmd_setup() {
    section "ICON 환경 설정"
    check_deps
    setup_database
    run_migrations
    info "환경 설정 완료"
}

cmd_start() {
    section "ICON 플랫폼 시작"
    check_deps
    setup_database
    run_migrations
    start_backend
    start_frontend
    echo ""
    info "ICON 플랫폼 기동 완료"
    info "  백엔드  → http://localhost:11100"
    info "  프론트  → http://localhost:5160"
    info "  Swagger → http://localhost:11100/swagger-ui.html"
}

cmd_stop() {
    section "ICON 플랫폼 중지"
    stop_backend
    stop_frontend
    info "ICON 플랫폼 중지 완료"
}

cmd_restart() {
    cmd_stop
    sleep 1
    cmd_start
}

cmd_rebuild() {
    section "ICON 강제 재빌드 후 시작"
    cmd_stop
    build_backend
    build_frontend
    check_deps
    setup_database
    run_migrations
    start_backend
    start_frontend
    info "재빌드 및 기동 완료"
}

cmd_status() {
    section "ICON 플랫폼 상태"
    local be_pid fe_pid
    be_pid=$(pgrep -f "$JAR_NAME" 2>/dev/null || true)
    fe_pid=$(fe_get_pid)

    if [ -n "$be_pid" ]; then
        echo -e "  백엔드  : ${GREEN}실행 중${NC} (PID: $be_pid)"
    else
        echo -e "  백엔드  : ${RED}중지됨${NC}"
    fi

    if [ -n "$fe_pid" ]; then
        echo -e "  프론트  : ${GREEN}실행 중${NC} (PID: $fe_pid)"
    else
        echo -e "  프론트  : ${RED}중지됨${NC}"
    fi

    echo ""
    if pg_isready -h "$DB_HOST" -p "$DB_PORT" -q 2>/dev/null; then
        echo -e "  PostgreSQL: ${GREEN}실행 중${NC}"
    else
        echo -e "  PostgreSQL: ${RED}중지됨${NC}"
    fi
}

# ─────────────────────────────────────────────────────────────
case "${1:-}" in
    start)   cmd_start   ;;
    stop)    cmd_stop    ;;
    restart) cmd_restart ;;
    rebuild) cmd_rebuild ;;
    status)  cmd_status  ;;
    setup)   cmd_setup   ;;
    *)
        echo "Usage: $0 {start|stop|restart|rebuild|status|setup}"
        echo ""
        echo "  start    의존성 확인 → DB 초기화 → 마이그레이션 → 백엔드+프론트 기동"
        echo "  stop     백엔드+프론트 종료"
        echo "  restart  재시작"
        echo "  rebuild  소스 재빌드 후 재시작"
        echo "  status   실행 상태 확인"
        echo "  setup    환경 설정만 실행 (기동 없이)"
        exit 1
        ;;
esac
