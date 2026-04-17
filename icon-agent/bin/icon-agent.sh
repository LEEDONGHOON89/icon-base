#!/bin/bash
#
# icon-agent control script (Linux): start | stop | restart | status
# Usage: ./icon-agent.sh { start | stop | restart | status }
#

# --- Config (edit as needed) ---
# JDK path (default: /home/datasay/sw/jdks/jdk-21.0.4). Override with env: export JDK_HOME=/path/to/jdk
export JDK_HOME="${JDK_HOME:-/home/datasay/sw/jdks/jdk-21.0.4}"

# App home: parent of directory containing this script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(cd "$SCRIPT_DIR/.." && pwd)"

JAR_NAME="collector-agent.jar"
CONFIG_PATH="${APP_HOME}/config.yaml"
PID_FILE="${APP_HOME}/data/icon-agent.pid"
LOG_DIR="${APP_HOME}/logs"
STDOUT_LOG="${LOG_DIR}/icon-agent.out"
STDERR_LOG="${LOG_DIR}/icon-agent.err"

# JVM options (override with JVM_OPTS env)
JVM_OPTS="${JVM_OPTS:--Xms4g -Xmx4g}"
# --- Derived ---
JAVA_EXE="${JDK_HOME}/bin/java"
JAR_PATH="${APP_HOME}/build/libs/${JAR_NAME}"

check_java() {
  if [ ! -x "$JAVA_EXE" ]; then
    echo "[ERROR] Java not found: $JAVA_EXE (set JDK_HOME)"
    exit 1
  fi
}

check_jar() {
  if [ ! -f "$JAR_PATH" ]; then
    echo "[ERROR] JAR not found: $JAR_PATH"
    exit 1
  fi
}

pid_exists() {
  [ -n "$1" ] && kill -0 "$1" 2>/dev/null
}

get_pid() {
  [ -f "$PID_FILE" ] && cat "$PID_FILE" || echo ""
}

start_agent() {
  check_java
  check_jar
  local pid
  pid=$(get_pid)
  if pid_exists "$pid"; then
    echo "[WARN] Already running (PID=$pid)"
    return 0
  fi
  mkdir -p "$(dirname "$PID_FILE")" "$LOG_DIR"
  echo "[INFO] Starting: $JAR_PATH"
  nohup "$JAVA_EXE" $JVM_OPTS -jar "$JAR_PATH" -c "$CONFIG_PATH" >> "$STDOUT_LOG" 2>> "$STDERR_LOG" &
  echo $! > "$PID_FILE"
  sleep 1
  pid=$(get_pid)
  if pid_exists "$pid"; then
    echo "[INFO] Started. PID=$pid"
  else
    echo "[ERROR] Process exited. Check $STDERR_LOG"
    rm -f "$PID_FILE"
    return 1
  fi
}

stop_agent() {
  local pid
  pid=$(get_pid)
  if [ -z "$pid" ]; then
    echo "[INFO] No PID file. Nothing to stop."
    return 0
  fi
  if ! pid_exists "$pid"; then
    echo "[INFO] Process not running (stale PID=$pid). Removing PID file."
    rm -f "$PID_FILE"
    return 0
  fi
  echo "[INFO] Stopping (PID=$pid)..."
  kill "$pid" 2>/dev/null || true
  for i in $(seq 1 15); do
    if ! pid_exists "$pid"; then
      rm -f "$PID_FILE"
      echo "[INFO] Stopped."
      return 0
    fi
    sleep 1
  done
  echo "[WARN] Graceful shutdown timeout. Sending SIGKILL."
  kill -9 "$pid" 2>/dev/null || true
  rm -f "$PID_FILE"
  echo "[INFO] Killed."
}

status_agent() {
  local pid
  pid=$(get_pid)
  if [ -z "$pid" ]; then
    echo "[INFO] Stopped (no PID file)"
    return 0
  fi
  if pid_exists "$pid"; then
    echo "[INFO] Running (PID=$pid)"
    return 0
  fi
  echo "[INFO] Stopped (stale PID=$pid). Removing PID file."
  rm -f "$PID_FILE"
}

case "${1:-}" in
  start)   start_agent ;;
  stop)    stop_agent ;;
  restart) stop_agent; sleep 2; start_agent ;;
  status)  status_agent ;;
  *)
    echo "Usage: $0 { start | stop | restart | status }"
    echo ""
    echo "  JDK_HOME = $JDK_HOME"
    echo "  APP_HOME = $APP_HOME"
    echo "  JAR      = $JAR_PATH"
    echo "  CONFIG   = $CONFIG_PATH"
    exit 1
    ;;
esac
