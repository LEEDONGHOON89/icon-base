#!/bin/bash

JAVA_BIN="/home/icon-sw/jdk-17.0.17/bin/java"
JAR_NAME="icon-api-0.0.1-SNAPSHOT.jar"
APP_DIR="/home/icon-sw"   # jar 위치에 맞게 수정
#LOG_FILE="$APP_DIR/icon-api.log"

JAVA_OPTS="
-XX:MaxDirectMemorySize=8192m
-Xmx2048m
-XX:+UseG1GC
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=$APP_DIR/heapdump
-Dfile.encoding=UTF-8
"

start() {
    PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')

    if [ -n "$PID" ]; then
        echo "icon-api is already running (PID: $PID)"
        exit 0
    fi

    echo "Starting icon-api...$JAVA_BIN -jar $APP_DIR/$JAR_NAME"
    nohup $JAVA_BIN $JAVA_OPTS -jar $APP_DIR/$JAR_NAME 2>&1 &
    sleep 2

    PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
    if [ -n "$PID" ]; then
        echo "icon-api started successfully (PID: $PID)"
    else
        echo "Failed to start icon-api"
    fi
}

stop() {
    PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')

    if [ -z "$PID" ]; then
        echo "icon-api is not running"
        exit 0
    fi

    echo "Stopping icon-api (PID: $PID)..."
    kill $PID
    sleep 2

    PID_CHECK=$(ps -ef | grep "$JAR_NAME" | grep -v grep)
    if [ -z "$PID_CHECK" ]; then
        echo "icon-api stopped successfully"
    else
        echo "icon-api did not stop, force killing..."
        kill -9 $PID
        echo "icon-api force stopped"
    fi
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    status)
        ps -ef | grep "$JAR_NAME" | grep -v grep
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac

