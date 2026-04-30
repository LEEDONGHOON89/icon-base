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
# [2026-04-24] 마이그레이션 단일 정본: BASE_DIR/db/migrations (개발·서버 환경 공통)
# src/main/resources/db/migrations 는 Flyway 미사용으로 참조하지 않음
MIGRATION_DIR="${BASE_DIR}/db/migrations"
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
# [2026-04-30] psql 연결 명령 전역 변수 — run_migrations 에서 결정 후 inline migration 공유
PSQL_CMD=""

# [2026-04-21] JVM 옵션 경량화 (백엔드 전용 — 에이전트는 icon-agent.bat / icon-agent.ps1 참조)
JAVA_OPTS=(
    -Xms256m
    -Xmx512m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:G1ReservePercent=10
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

    # [2026-04-29] JAVA_HOME이 설정됐지만 실제 바이너리가 없으면 무시 (잘못된 경로 방지)
    if [ -n "${JAVA_HOME:-}" ] && [ ! -x "$JAVA_HOME/bin/java" ]; then
        warn "JAVA_HOME($JAVA_HOME) 에 java 바이너리 없음 → JAVA_HOME 무시하고 재탐색"
        unset JAVA_HOME
    fi

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
            warn "Java $JAVA_VER 감지 (17 이상 필요) → Java 17 설치를 진행합니다"
        fi
    fi

    warn "Java 17 미설치 → 설치를 진행합니다"
    local os
    os=$(detect_os)
    if [ "$os" = "debian" ]; then
        sudo apt-get update -qq
        sudo apt-get install -y openjdk-17-jdk
        # [2026-04-29] 설치 후 Java 17 바이너리 명시적 탐색 (command -v 는 옛 버전 반환 가능)
        JAVA_BIN=$(find /usr/lib/jvm -name "java" -path "*java-17*" 2>/dev/null | head -1)
        [ -z "$JAVA_BIN" ] && JAVA_BIN=$(command -v java)
    elif [ "$os" = "redhat" ]; then
        sudo yum install -y java-17-openjdk-devel
        # [2026-04-29] 설치 후 Java 17 바이너리 명시적 탐색
        JAVA_BIN=$(find /usr/lib/jvm -name "java" -path "*java-17*" 2>/dev/null | head -1)
        if [ -z "$JAVA_BIN" ]; then
            JAVA_BIN=$(alternatives --list 2>/dev/null | awk '$1=="java" && /java-17/{print $3}' | head -1)
        fi
        [ -z "$JAVA_BIN" ] && JAVA_BIN=$(command -v java)
    else
        error "Java 17을 수동으로 설치 후 JAVA_HOME을 설정하세요."; exit 1
    fi

    # [2026-04-29] 설치 후 바이너리 존재 및 버전 최종 확인
    if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
        error "Java 17 설치 후 바이너리를 찾을 수 없습니다. 수동으로 JAVA_HOME을 설정하세요."
        exit 1
    fi
    JAVA_VER=$("$JAVA_BIN" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
    info "Java $JAVA_VER 설치 완료: $JAVA_BIN"
}

check_node() {
    section "Node.js 확인"
    if command -v node &>/dev/null; then
        NODE_VER=$(node -v | sed 's/v//' | cut -d. -f1)
        if [ "$NODE_VER" -ge 18 ] 2>/dev/null; then
            # [2026-04-30] node -v 가 이미 "v21.x" 형태를 반환하므로 추가 v 제거
            info "Node.js $(node -v) 확인됨"; return
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

# [2026-04-17] init.sql 경로: 소스 없는 서버는 BASE_DIR/db/init.sql 사용
INIT_SQL="${BASE_DIR}/db/init.sql"

setup_database() {
    section "DB 초기화"

    # postgres 슈퍼유저로 DB 존재 확인
    DB_EXISTS=$(sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>/dev/null || true)

    if [ "$DB_EXISTS" != "1" ]; then
        info "DB '$DB_NAME' 생성 중..."
        sudo -u postgres psql -c "CREATE DATABASE $DB_NAME ENCODING 'UTF8' LC_COLLATE 'en_US.UTF-8' LC_CTYPE 'en_US.UTF-8' TEMPLATE template0;" 2>/dev/null || \
        sudo -u postgres psql -c "CREATE DATABASE $DB_NAME;"
        info "DB '$DB_NAME' 생성 완료"
        DB_IS_NEW=true
    else
        info "DB '$DB_NAME' 이미 존재함"
        DB_IS_NEW=false
    fi

    # [2026-04-30] postgres 비밀번호 항상 동기화 — 신규·기존 DB 모두
    #   run_migrations 의 TCP 인증(PGPASSWORD)이 반드시 성공해야 하므로
    #   peer auth(sudo)로 비밀번호를 설정해 TCP 인증을 보장
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

    # [2026-04-17] DB 신규 생성 시 init.sql로 전체 스키마 한 번에 초기화
    if [ "$DB_IS_NEW" = true ]; then
        run_init_sql
    fi
}

# [2026-04-17] 통합 init.sql 적용 (DB 버전 1.0.0 기준선)
run_init_sql() {
    local psql_cmd="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"

    if [ ! -f "$INIT_SQL" ]; then
        warn "init.sql 없음: $INIT_SQL (건너뜀 — 마이그레이션으로 대체됨)"
        return
    fi

    info "DB 스키마 초기화 중 (v1.0.0): $INIT_SQL"
    if $psql_cmd --set=ON_ERROR_STOP=on -f "$INIT_SQL" &>/dev/null; then
        local tbl_count
        tbl_count=$(grep -c 'CREATE TABLE IF NOT EXISTS' "$INIT_SQL")
        info "DB 스키마 초기화 완료 — ${tbl_count}개 테이블 (v1.0.0)"

        # [2026-04-17] init.sql 적용 후 v1.0.0을 icon_migrations에 기록
        # → 이후 run_migrations()가 동일 버전을 재적용하지 않도록 방지
        local init_checksum
        init_checksum=$(md5sum "$INIT_SQL" | awk '{print $1}')
        $psql_cmd -c "
            CREATE TABLE IF NOT EXISTS icon_migrations (
                id          SERIAL       PRIMARY KEY,
                version     VARCHAR(50)  NOT NULL UNIQUE,
                filename    VARCHAR(255) NOT NULL,
                applied_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                checksum    VARCHAR(64)
            );
            INSERT INTO icon_migrations (version, filename, checksum)
            VALUES ('1.0.0', 'init.sql', '$init_checksum')
            ON CONFLICT (version) DO NOTHING;
        " &>/dev/null || true
    else
        warn "init.sql 일부 오류 발생 — 상세 내용:"
        $psql_cmd -f "$INIT_SQL" 2>&1 | grep -i "error" | head -10 || true
        error "DB 초기화 실패. 수동으로 확인하세요: $INIT_SQL"
        exit 1
    fi
}

# ─────────────────────────────────────────────────────────────
# 3. DB 마이그레이션 (v1.0.1 이상 증분 변경 적용)
# 파일명 형식: V1_0_1__description.sql (버전 점 → 언더스코어)
# ─────────────────────────────────────────────────────────────
run_migrations() {
    section "DB 마이그레이션"

    local psql_cmd="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"

    # [2026-04-30] 마이그레이션 추적 테이블 생성 — || true 로 set -e 탈출 방지
    #   psql 인증 실패 시 명확한 에러 메시지 출력 후 종료
    if ! $psql_cmd -c "
        CREATE TABLE IF NOT EXISTS icon_migrations (
            id          SERIAL      PRIMARY KEY,
            version     VARCHAR(50) NOT NULL UNIQUE,
            filename    VARCHAR(255) NOT NULL,
            applied_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
            checksum    VARCHAR(64)
        );
    " &>/dev/null; then
        error "DB 마이그레이션 테이블 생성 실패 — psql 인증 오류 가능성"
        error "  psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME 연결을 수동 확인하세요."
        # [2026-04-30] sudo peer auth 로 폴백 시도
        warn "sudo peer 인증으로 폴백 시도..."
        psql_cmd="sudo -u postgres psql -d $DB_NAME"
        $psql_cmd -c "
            CREATE TABLE IF NOT EXISTS icon_migrations (
                id          SERIAL      PRIMARY KEY,
                version     VARCHAR(50) NOT NULL UNIQUE,
                filename    VARCHAR(255) NOT NULL,
                applied_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                checksum    VARCHAR(64)
            );" &>/dev/null || { error "DB 마이그레이션 테이블 생성 최종 실패"; exit 1; }
        warn "sudo peer 인증으로 마이그레이션 진행"
    fi
    # [2026-04-30] 결정된 psql_cmd 을 전역 PSQL_CMD 에 저장 (apply_inline_migration 공유)
    PSQL_CMD="$psql_cmd"

    if [ ! -d "$MIGRATION_DIR" ]; then
        info "마이그레이션 디렉토리 없음: $MIGRATION_DIR (건너뜀)"
        run_inline_migrations
        return
    fi

    local sql_files
    sql_files=$(ls "$MIGRATION_DIR"/V*.sql 2>/dev/null | sort || true)

    if [ -z "$sql_files" ]; then
        info "적용할 마이그레이션 파일 없음"
        run_inline_migrations
        return
    fi

    # V1_0_1__xxx.sql 파일을 정렬하여 순서대로 실행
    local applied=0
    local skipped=0

    for sql_file in $sql_files; do
        local filename
        filename=$(basename "$sql_file")
        # [2026-04-17] 버전 추출: V1_0_1__desc.sql → 1.0.1 (언더스코어 → 점 변환)
        local version
        version=$(echo "$filename" | sed -E 's/^V([0-9]+)_([0-9]+)_([0-9]+)__.*/\1.\2.\3/')
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
        if $psql_cmd --set=ON_ERROR_STOP=on -f "$sql_file" &>/dev/null; then
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

    # [2026-04-21] 파일 기반 마이그레이션 이후 인라인 마이그레이션 추가 적용
    run_inline_migrations
}

# ─────────────────────────────────────────────────────────────
# 3-1. 인라인 마이그레이션
# SQL 파일이 서버에 없는 경우에도 icon.sh 단독으로 스키마 변경을 적용할 수 있도록
# 버전별 DDL을 직접 내장한다.
# 새 마이그레이션 추가 시: apply_inline_migration 블록을 순서대로 추가할 것.
# ─────────────────────────────────────────────────────────────

# [2026-04-21] 인라인 마이그레이션 헬퍼
# 사용법: apply_inline_migration "1.0.6" "V1_0_6__desc" "SQL 문장"
# [2026-04-30] PSQL_CMD 전역 변수 사용 (resolve_psql_cmd 로 결정된 명령)
apply_inline_migration() {
    local version="$1"
    local filename="$2"
    local sql="$3"
    local psql_cmd="${PSQL_CMD:-psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME}"

    local already_applied
    already_applied=$($psql_cmd -tAc "SELECT COUNT(*) FROM icon_migrations WHERE version='$version'" 2>/dev/null || echo "0")

    if [ "$already_applied" = "1" ]; then
        info "  SKIP  $filename (이미 적용됨)"
        return
    fi

    info "  APPLY $filename (inline)"
    if $psql_cmd -c "$sql" &>/dev/null; then
        $psql_cmd -c "
            INSERT INTO icon_migrations (version, filename, checksum)
            VALUES ('$version', '$filename', 'inline')
            ON CONFLICT (version) DO NOTHING;
        " &>/dev/null
        info "  ✓     $filename 적용 완료"
    else
        error "인라인 마이그레이션 실패: $filename"
        exit 1
    fi
}

run_inline_migrations() {
    # [2026-04-30] PSQL_CMD 전역 변수 사용
    local psql_cmd="${PSQL_CMD:-psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME}"

    # icon_migrations 테이블이 없으면 인라인 적용 불가 — 건너뜀
    local tbl_exists
    tbl_exists=$($psql_cmd -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='icon_migrations'" 2>/dev/null || echo "0")
    if [ "$tbl_exists" != "1" ]; then
        return
    fi

    # ── V1_0_6 : ds_database_config — 증분 컬럼 초기값 추가 ──────────────
    # [2026-04-21] 첫 수집 시 하이워터마크 시작점 설정 필드
    apply_inline_migration "1.0.6" \
        "V1_0_6__add_incremental_column_initial_value" \
        "ALTER TABLE public.ds_database_config
             ADD COLUMN IF NOT EXISTS incremental_column_initial_value VARCHAR(200);"

    # ── V1_0_7 : ds_file_system_config — 에이전트 폴링 설정 추가 ─────────
    # [2026-04-21] 에이전트가 파일 폴링 시 사용하는 간격·크기 설정
    #   poll_interval_ms   : 폴링 간격 (ms), NULL → scan_interval_minutes × 60000
    #   max_lines_per_poll : 폴링 1회 최대 라인 수, NULL → 1000
    #   max_record_bytes   : 단일 레코드 최대 바이트, NULL → 524288 (512 KB)
    apply_inline_migration "1.0.7" \
        "V1_0_7__add_poll_settings_to_file_system_config" \
        "ALTER TABLE public.ds_file_system_config
             ADD COLUMN IF NOT EXISTS poll_interval_ms   BIGINT,
             ADD COLUMN IF NOT EXISTS max_lines_per_poll INTEGER,
             ADD COLUMN IF NOT EXISTS max_record_bytes   INTEGER;"

    # ── V1_0_8 : ds_database_config — 에이전트 폴링 설정 추가 ────────────
    # [2026-04-21] 에이전트가 JDBC 폴링 시 사용하는 간격·크기 설정
    #   poll_interval_ms   : 폴링 간격 (ms), NULL → 300000 (5분)
    #   max_lines_per_poll : 폴링 1회 최대 행 수, NULL → 1000
    #   max_record_bytes   : 단일 레코드 최대 바이트, NULL → 524288 (512 KB)
    apply_inline_migration "1.0.8" \
        "V1_0_8__add_poll_settings_to_database_config" \
        "ALTER TABLE public.ds_database_config
             ADD COLUMN IF NOT EXISTS poll_interval_ms   BIGINT,
             ADD COLUMN IF NOT EXISTS max_lines_per_poll INTEGER,
             ADD COLUMN IF NOT EXISTS max_record_bytes   INTEGER;"

    # ── V1_0_9 : agent_target_configs — 초당 배치 전송 제한 추가 ─────────
    # [2026-04-22] 초당 최대 배치 전송 수. 0 = 무제한 (기본값 10)
    apply_inline_migration "1.0.9" \
        "V1_0_9__add_max_batches_per_second_to_agent_target_configs" \
        "ALTER TABLE public.agent_target_configs
             ADD COLUMN IF NOT EXISTS max_batches_per_second INTEGER NOT NULL DEFAULT 10;"

    # ── V1_0_10 : 구 수집기 설정 테이블 삭제 ─────────────────────────────
    # [2026-04-22] ds_file_system_config / ds_database_config 가 단일 진실 공급원으로 전환
    #              agent_collector_* 테이블은 더 이상 사용하지 않음
    apply_inline_migration "1.0.10" \
        "V1_0_10__drop_agent_collector_tables" \
        "DROP TABLE IF EXISTS agent_collector_file_configs;
         DROP TABLE IF EXISTS agent_collector_jdbc_configs;
         DROP TABLE IF EXISTS agent_collector_configs;"

    # ── V1_0_11 : mapped_storages 성능 인덱스 추가 ───────────────────────
    # [2026-04-22] reg_dt·landing_record_id·processing_status·transaction_id 기반 인덱스
    apply_inline_migration "1.0.11" \
        "V1_0_11__add_mapped_storages_performance_indexes" \
        "CREATE INDEX IF NOT EXISTS idx_mapped_storages_landing_reg_dt
             ON public.mapped_storages (landing_record_id, reg_dt DESC);
         CREATE INDEX IF NOT EXISTS idx_mapped_storages_reg_dt
             ON public.mapped_storages (reg_dt DESC);
         CREATE INDEX IF NOT EXISTS idx_mapped_storages_processing_status
             ON public.mapped_storages (processing_status);
         CREATE INDEX IF NOT EXISTS idx_mapped_storages_transaction_id
             ON public.mapped_storages (transaction_id);"

    # ── V1_0_12 : ds_database_config — 보조 증분 컬럼 추가 ──────────────
    # [2026-04-22] 복합 키 기반 증분 수집 지원 (예: updated_at + seq_id 조합)
    apply_inline_migration "1.0.12" \
        "V1_0_12__add_secondary_incremental_column" \
        "ALTER TABLE public.ds_database_config
             ADD COLUMN IF NOT EXISTS secondary_incremental_column               VARCHAR(200),
             ADD COLUMN IF NOT EXISTS secondary_incremental_column_type          VARCHAR(50),
             ADD COLUMN IF NOT EXISTS secondary_incremental_column_initial_value VARCHAR(200),
             ADD COLUMN IF NOT EXISTS last_secondary_processed_value             VARCHAR(200);"
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
    # [2026-04-24] stdout을 /dev/null로 버려 콘솔 로그 중복 방지 (FileAppender → logs/icon-api.log에만 기록)
    nohup "$JAVA_BIN" "${JAVA_OPTS[@]}" -jar "$JAR_PATH" \
        > /dev/null 2>&1 &
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

# [2026-04-29] 포트 감지: ss → netstat → lsof 순서로 폴백 (서버 환경 호환성)
fe_port_open() {
    ss      -tlnp 2>/dev/null | grep -q ":5160" && return 0
    netstat -tlnp 2>/dev/null | grep -q ":5160" && return 0
    lsof -i :5160 -sTCP:LISTEN 2>/dev/null | grep -q "."  && return 0
    return 1
}

fe_is_running() {
    fe_port_open
}

fe_get_pid() {
    # ss 방식
    local pid
    pid=$(ss -tlnp 2>/dev/null | grep ":5160" | grep -oP 'pid=\K[0-9]+' | head -1)
    [ -n "$pid" ] && echo "$pid" && return
    # netstat 방식
    pid=$(netstat -tlnp 2>/dev/null | grep ":5160" | awk '{print $7}' | cut -d/ -f1 | head -1)
    [ -n "$pid" ] && echo "$pid" && return
    # lsof 방식
    pid=$(lsof -i :5160 -sTCP:LISTEN 2>/dev/null | awk 'NR>1{print $2}' | head -1)
    echo "${pid:-}"
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

    # [2026-04-29] 단일 sleep 대신 재시도 루프 (최대 15초 대기)
    local i=0
    while [ $i -lt 15 ]; do
        sleep 1
        if fe_is_running; then
            info "프론트엔드 기동 성공 (PID: $(fe_get_pid)) → 포트 5160"
            return
        fi
        ((i++)) || true
    done

    error "프론트엔드 기동 실패. 로그 확인: $LOG_DIR/icon-frontend.log"; exit 1
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

# [2026-04-21] 프론트엔드 단독 제어
cmd_start_fe() {
    section "프론트엔드 기동"
    start_frontend
    echo ""
    info "프론트엔드 기동 완료 → http://localhost:5160"
}

cmd_stop_fe() {
    section "프론트엔드 중지"
    stop_frontend
    info "프론트엔드 중지 완료"
}

cmd_restart_fe() {
    section "프론트엔드 재기동"
    stop_frontend
    sleep 1
    start_frontend
    echo ""
    info "프론트엔드 재기동 완료 → http://localhost:5160"
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
    start)      cmd_start      ;;
    stop)       cmd_stop       ;;
    restart)    cmd_restart    ;;
    rebuild)    cmd_rebuild    ;;
    status)     cmd_status     ;;
    setup)      cmd_setup      ;;
    # [2026-04-21] 프론트엔드 단독 제어
    start-fe)   cmd_start_fe   ;;
    stop-fe)    cmd_stop_fe    ;;
    restart-fe) cmd_restart_fe ;;
    *)
        echo "Usage: $0 {start|stop|restart|rebuild|status|setup|start-fe|stop-fe|restart-fe}"
        echo ""
        echo "  start       의존성 확인 → DB 초기화 → 마이그레이션 → 백엔드+프론트 기동"
        echo "  stop        백엔드+프론트 종료"
        echo "  restart     재시작"
        echo "  rebuild     소스 재빌드 후 재시작"
        echo "  status      실행 상태 확인"
        echo "  setup       환경 설정만 실행 (기동 없이)"
        echo ""
        echo "  start-fe    프론트엔드만 기동"
        echo "  stop-fe     프론트엔드만 중지"
        echo "  restart-fe  프론트엔드만 재기동"
        exit 1
        ;;
esac
