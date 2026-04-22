@echo off
:: ============================================================
:: ICON Collector Agent — Windows 실행 스크립트
:: Usage: icon-agent.bat {start|stop|status|restart}
:: 에이전트(icon-agent)는 수집 대상 시스템(Windows)에서 직접 실행
:: 내부통제시스템 서버(icon.sh)와 별도 관리
:: ============================================================
setlocal EnableDelayedExpansion

:: [2026-04-21] 경로 설정
set "BASE_DIR=%~dp0icon-agent"
set "JAR_NAME=collector-agent-1.0.0-all.jar"
set "JAR_PATH=%BASE_DIR%\build\libs\%JAR_NAME%"
set "CONFIG_PATH=%BASE_DIR%\config.yaml"
set "LOG_DIR=%BASE_DIR%\logs"
set "PID_FILE=%BASE_DIR%\agent.pid"

:: [2026-04-21] JVM 옵션 (경량 수집기)
set "JAVA_OPTS=-Xms64m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=%LOG_DIR%\heapdump -Dfile.encoding=UTF-8"

if "%~1"=="" goto usage
if /i "%~1"=="start"   goto do_start
if /i "%~1"=="stop"    goto do_stop
if /i "%~1"=="status"  goto do_status
if /i "%~1"=="restart" goto do_restart
goto usage

:: ── start ────────────────────────────────────────────────────
:do_start
call :check_running
if !ERRORLEVEL!==0 (
    echo [INFO]  에이전트 이미 실행 중입니다.
    goto end
)
if not exist "%JAR_PATH%" (
    echo [ERROR] JAR 없음: %JAR_PATH%
    echo [ERROR] gradlew shadowJar 로 빌드하거나 배포 파일을 확인하세요.
    exit /b 1
)
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [INFO]  에이전트 기동 중...
start /b "" java %JAVA_OPTS% -jar "%JAR_PATH%" -c "%CONFIG_PATH%" >> "%LOG_DIR%\collector-agent.log" 2>&1

:: PID 획득 (최대 5초 대기)
set "PID="
for /l %%i in (1,1,5) do (
    if "!PID!"=="" (
        for /f "tokens=2" %%p in ('tasklist /fi "imagename eq java.exe" /fo list ^| findstr /i "PID"') do (
            set "PID=%%p"
        )
        timeout /t 1 /nobreak >/dev/null
    )
)
if not "!PID!"=="" (
    echo !PID! > "%PID_FILE%"
    echo [INFO]  에이전트 기동 성공 (PID: !PID!)
) else (
    echo [WARN]  PID 확인 불가. 로그를 확인하세요: %LOG_DIR%\collector-agent.log
)
goto end

:: ── stop ─────────────────────────────────────────────────────
:do_stop
call :check_running
if !ERRORLEVEL!==1 (
    echo [INFO]  에이전트가 실행 중이지 않습니다.
    goto end
)
set /p STORED_PID=<"%PID_FILE%"
echo [INFO]  에이전트 종료 중 (PID: !STORED_PID!)...
taskkill /pid !STORED_PID! /f >/dev/null 2>&1
del /f "%PID_FILE%" >/dev/null 2>&1
echo [INFO]  에이전트 종료 완료
goto end

:: ── status ───────────────────────────────────────────────────
:do_status
call :check_running
if !ERRORLEVEL!==0 (
    set /p STORED_PID=<"%PID_FILE%"
    echo [INFO]  에이전트 실행 중 (PID: !STORED_PID!)
) else (
    echo [INFO]  에이전트 정지 상태
)
goto end

:: ── restart ──────────────────────────────────────────────────
:do_restart
call :do_stop
timeout /t 2 /nobreak >/dev/null
goto do_start

:: ── 내부: 실행 여부 확인 ──────────────────────────────────────
:check_running
if not exist "%PID_FILE%" exit /b 1
set /p CHECK_PID=<"%PID_FILE%"
tasklist /fi "pid eq !CHECK_PID!" /fo csv 2>/dev/null | findstr /i "java" >/dev/null
if !ERRORLEVEL!==0 (exit /b 0) else (
    del /f "%PID_FILE%" >/dev/null 2>&1
    exit /b 1
)

:: ── usage ─────────────────────────────────────────────────────
:usage
echo.
echo  사용법: icon-agent.bat {start^|stop^|status^|restart}
echo.
echo  start    에이전트 기동
echo  stop     에이전트 종료
echo  status   실행 상태 확인
echo  restart  재기동
echo.
exit /b 1

:end
endlocal
