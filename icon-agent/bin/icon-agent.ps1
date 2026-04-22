# ============================================================
# ICON Collector Agent — Windows 실행 스크립트 (PowerShell)
# Usage: .\icon-agent.ps1 { start | stop | restart | status | build | log }
#
# 에이전트(icon-agent)는 수집 대상 Windows 시스템에서 직접 실행
# 내부통제시스템 서버는 icon.sh 로 별도 관리
#
# Requires: Java 17+
# [2026-04-21] JVM 옵션 경량화 (Xms64m/Xmx256m, G1GC)
# ============================================================

# -- Path setup -----------------------------------------------
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition
$APP_HOME   = Split-Path -Parent $SCRIPT_DIR

$JAR_NAME   = "collector-agent.jar"
$JAR_PATH   = Join-Path $APP_HOME "build\libs\$JAR_NAME"
$CONFIG_PATH= Join-Path $APP_HOME "config.yaml"
$PID_FILE   = Join-Path $APP_HOME "data\icon-agent.pid"
$LOG_DIR    = Join-Path $APP_HOME "logs"
$STDOUT_LOG = Join-Path $LOG_DIR  "icon-agent.out"
$STDERR_LOG = Join-Path $LOG_DIR  "icon-agent.err"

# [2026-04-21] JVM 옵션 경량화 — 환경변수 JVM_OPTS로 재정의 가능
$_heapDump = Join-Path $LOG_DIR "heapdump"
if ($env:JVM_OPTS) {
    $JVM_OPTS = $env:JVM_OPTS
} else {
    $JVM_OPTS = "-Xms64m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=`"$_heapDump`" -Dfile.encoding=UTF-8"
}

# -- Java detection -------------------------------------------
function Find-Java {
    # 1) JAVA_HOME env var
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        return "$env:JAVA_HOME\bin\java.exe"
    }
    # 2) java in PATH
    $found = Get-Command java -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    # 3) Common install paths
    $candidates = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft",
        "C:\Program Files\BellSoft"
    )
    foreach ($base in $candidates) {
        if (Test-Path $base) {
            $javaExe = Get-ChildItem -Path $base -Filter "java.exe" -Recurse -ErrorAction SilentlyContinue |
                       Where-Object { $_.FullName -notmatch "jre\\bin" } |
                       Sort-Object LastWriteTime -Descending |
                       Select-Object -First 1
            if ($javaExe) { return $javaExe.FullName }
        }
    }
    return $null
}

# -- Color output helpers -------------------------------------
function Write-Info  ($msg) { Write-Host "[INFO]  $msg" -ForegroundColor Green }
function Write-Warn  ($msg) { Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err   ($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red }
function Write-Title ($msg) {
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
}

# -- PID utilities --------------------------------------------
function Get-AgentPid {
    if (-not (Test-Path $PID_FILE)) { return $null }
    $pidVal = Get-Content $PID_FILE -ErrorAction SilentlyContinue
    if ($pidVal -match '^\d+$') { return [int]$pidVal }
    return $null
}

function Test-AgentRunning ($agentPid) {
    if (-not $agentPid) { return $false }
    try {
        $proc = Get-Process -Id $agentPid -ErrorAction Stop
        return ($proc -ne $null)
    } catch {
        return $false
    }
}

# -- start ----------------------------------------------------
function Start-Agent {
    Write-Title "icon-agent start"

    $javaExe = Find-Java
    if (-not $javaExe) {
        Write-Err "Java not found."
        Write-Err "Set JAVA_HOME or install Java 17+."
        exit 1
    }
    $javaVer = & "$javaExe" -version 2>&1 | Select-String -Pattern '\d+' | ForEach-Object { $_.Matches[0].Value } | Select-Object -First 1
    Write-Info "Java: $javaExe (version $javaVer)"

    if (-not (Test-Path $JAR_PATH)) {
        Write-Err "JAR not found: $JAR_PATH"
        Write-Warn "Build first: .\icon-agent.bat build"
        exit 1
    }

    if (-not (Test-Path $CONFIG_PATH)) {
        Write-Err "Config not found: $CONFIG_PATH"
        exit 1
    }

    $existPid = Get-AgentPid
    if (Test-AgentRunning $existPid) {
        Write-Warn "Already running (PID: $existPid)"
        return
    }

    New-Item -ItemType Directory -Force -Path (Split-Path $PID_FILE) | Out-Null
    New-Item -ItemType Directory -Force -Path $LOG_DIR | Out-Null

    $jvmArgs = $JVM_OPTS -split '\s+' | Where-Object { $_ -ne '' }

    Write-Info "Starting..."
    Write-Info "  JAR    : $JAR_PATH"
    Write-Info "  CONFIG : $CONFIG_PATH"
    Write-Info "  JVM    : $JVM_OPTS"
    Write-Info "  LOG    : $STDOUT_LOG"

    $startArgs = @{
        FilePath               = $javaExe
        ArgumentList           = ($jvmArgs + @("-jar", "`"$JAR_PATH`"", "-c", "`"$CONFIG_PATH`""))
        RedirectStandardOutput = $STDOUT_LOG
        RedirectStandardError  = $STDERR_LOG
        WindowStyle            = "Hidden"
        PassThru               = $true
        WorkingDirectory       = $APP_HOME
    }

    $proc = Start-Process @startArgs
    $proc.Id | Out-File -FilePath $PID_FILE -Encoding ascii

    Start-Sleep -Seconds 2

    if (Test-AgentRunning $proc.Id) {
        Write-Info "Started successfully (PID: $($proc.Id))"
        Write-Info "View log: Get-Content -Wait '$STDOUT_LOG'"
    } else {
        Write-Err "Start failed. Error log:"
        if (Test-Path $STDERR_LOG) {
            Get-Content $STDERR_LOG -Tail 20 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
        }
        Remove-Item $PID_FILE -Force -ErrorAction SilentlyContinue
        exit 1
    }
}

# -- stop -----------------------------------------------------
function Stop-Agent {
    Write-Title "icon-agent stop"

    $agentPid = Get-AgentPid
    if (-not $agentPid) {
        Write-Info "No PID file. Not running."
        return
    }

    if (-not (Test-AgentRunning $agentPid)) {
        Write-Info "Process not found (PID: $agentPid). Cleaning PID file."
        Remove-Item $PID_FILE -Force -ErrorAction SilentlyContinue
        return
    }

    Write-Info "Stopping (PID: $agentPid)..."
    Stop-Process -Id $agentPid -ErrorAction SilentlyContinue

    for ($i = 1; $i -le 15; $i++) {
        Start-Sleep -Seconds 1
        if (-not (Test-AgentRunning $agentPid)) {
            Remove-Item $PID_FILE -Force -ErrorAction SilentlyContinue
            Write-Info "Stopped."
            return
        }
        Write-Host "  Waiting... ($i/15)" -ForegroundColor Gray
    }

    Write-Warn "Graceful stop timed out. Force killing..."
    Stop-Process -Id $agentPid -Force -ErrorAction SilentlyContinue
    Remove-Item $PID_FILE -Force -ErrorAction SilentlyContinue
    Write-Info "Force stopped."
}

# -- status ---------------------------------------------------
function Get-AgentStatus {
    Write-Title "icon-agent status"

    $agentPid = Get-AgentPid

    if (-not $agentPid) {
        Write-Host "  Status : " -NoNewline
        Write-Host "Stopped (no PID file)" -ForegroundColor Red
    } elseif (Test-AgentRunning $agentPid) {
        $proc    = Get-Process -Id $agentPid
        $mem     = [math]::Round($proc.WorkingSet64 / 1MB, 1)
        $cpu     = [math]::Round($proc.CPU, 1)
        $elapsed = (Get-Date) - $proc.StartTime

        Write-Host "  Status : " -NoNewline; Write-Host "Running" -ForegroundColor Green
        Write-Host "  PID    : $agentPid"
        Write-Host "  Memory : ${mem} MB"
        Write-Host "  CPU    : ${cpu} sec (total)"
        Write-Host "  Uptime : $([int]$elapsed.TotalMinutes) min"
    } else {
        Write-Host "  Status : " -NoNewline
        Write-Host "Stopped (stale PID: $agentPid)" -ForegroundColor Yellow
        Remove-Item $PID_FILE -Force -ErrorAction SilentlyContinue
        Write-Info "PID file removed."
    }

    Write-Host ""
    Write-Host "  JAR    : $JAR_PATH"
    Write-Host "  CONFIG : $CONFIG_PATH"
    Write-Host "  LOGS   : $LOG_DIR"

    if (Test-Path $STDOUT_LOG) {
        Write-Host ""
        Write-Host "-- Recent log (stdout, last 10 lines) --" -ForegroundColor DarkGray
        Get-Content $STDOUT_LOG -Tail 10 | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
    }
}

# -- build ----------------------------------------------------
function Build-Agent {
    Write-Title "icon-agent build"

    $gradlew = Join-Path $APP_HOME "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        Write-Err "gradlew.bat not found: $gradlew"
        exit 1
    }

    Write-Info "Running Gradle shadowJar..."
    Push-Location $APP_HOME
    & "$gradlew" shadowJar -x test
    $exitCode = $LASTEXITCODE
    Pop-Location

    if ($exitCode -eq 0) {
        Write-Info "Build successful: $JAR_PATH"
    } else {
        Write-Err "Build failed (exit code: $exitCode)"
        exit 1
    }
}

# -- log tail -------------------------------------------------
function Watch-Log {
    if (-not (Test-Path $STDOUT_LOG)) {
        Write-Warn "Log file not found: $STDOUT_LOG"
        exit 1
    }
    Write-Info "Tailing log (Ctrl+C to stop)..."
    Get-Content -Path $STDOUT_LOG -Wait -Tail 50
}

# -- main -----------------------------------------------------
$cmd = if ($args.Count -gt 0) { $args[0] } else { "" }

switch ($cmd) {
    "start"   { Start-Agent }
    "stop"    { Stop-Agent }
    "restart" { Stop-Agent; Start-Sleep -Seconds 2; Start-Agent }
    "status"  { Get-AgentStatus }
    "build"   { Build-Agent }
    "log"     { Watch-Log }
    default {
        Write-Host ""
        Write-Host "Usage: .\icon-agent.bat { start | stop | restart | status | build | log }" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  start    Start agent (background)"
        Write-Host "  stop     Stop agent"
        Write-Host "  restart  Restart agent"
        Write-Host "  status   Show status + recent log"
        Write-Host "  build    Build via Gradle shadowJar"
        Write-Host "  log      Tail live log (Ctrl+C to stop)"
        Write-Host ""
        Write-Host "Environment variables:"
        Write-Host "  JAVA_HOME  Java install path (auto-detected if not set)"
        Write-Host "  JVM_OPTS   JVM options (default: $JVM_OPTS)"
        Write-Host ""
        Write-Host "Paths:"
        Write-Host "  APP_HOME : $APP_HOME"
        Write-Host "  JAR      : $JAR_PATH"
        Write-Host "  CONFIG   : $CONFIG_PATH"
        Write-Host "  LOGS     : $LOG_DIR"
        exit 1
    }
}
