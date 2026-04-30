# ============================================================
# ICON Agent CLI - Interactive Management Shell
# Usage: .\agent-cli.ps1 [-Port 8081]
# Connects to AdminServer at 127.0.0.1:8081
# [2026-04-21]
# ============================================================
param(
    [int]$Port = 8081
)

$BASE_URL = "http://127.0.0.1:$Port"

function Write-Ok   { param($m) Write-Host $m -ForegroundColor Green }
function Write-Warn { param($m) Write-Host $m -ForegroundColor Yellow }
function Write-Err  { param($m) Write-Host $m -ForegroundColor Red }
function Write-Info { param($m) Write-Host $m -ForegroundColor Cyan }
function Write-Dim  { param($m) Write-Host $m -ForegroundColor DarkGray }

function Invoke-Admin {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [hashtable]$Body = $null
    )
    $uri = "$BASE_URL$Path"
    try {
        if ($Body) {
            $json = $Body | ConvertTo-Json -Depth 5
            $resp = Invoke-RestMethod -Method $Method -Uri $uri `
                -ContentType "application/json" -Body $json -ErrorAction Stop
        } else {
            $resp = Invoke-RestMethod -Method $Method -Uri $uri -ErrorAction Stop
        }
        return @{ ok = $true; data = $resp }
    } catch {
        $msg = $_.Exception.Message
        if ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $errBody = $reader.ReadToEnd() | ConvertFrom-Json
                $msg = $errBody.error
            } catch {}
        }
        return @{ ok = $false; error = $msg }
    }
}

function Cmd-Status {
    $r = Invoke-Admin -Path "/status"
    if (-not $r.ok) { Write-Err "  Error: $($r.error)"; return }
    $d = $r.data
    $upMin = [math]::Floor($d.uptimeSec / 60)
    $upSec = $d.uptimeSec % 60
    Write-Host ""
    Write-Host "  Status   : " -NoNewline; Write-Host "RUNNING" -ForegroundColor Green
    Write-Host "  AgentId  : $($d.agentId)"
    Write-Host "  Uptime   : $($upMin)m $($upSec)s"
    Write-Host "  Targets  : $($d.targets)"
    Write-Host ""
}

function Cmd-TargetList {
    $r = Invoke-Admin -Path "/targets"
    if (-not $r.ok) { Write-Err "  Error: $($r.error)"; return }
    $list = $r.data
    if ($list.Count -eq 0) {
        Write-Warn "  No targets registered. Use: target add [id] [endpoint]"
        return
    }
    Write-Host ""
    Write-Host ("  {0,-20} {1,-45} {2,-10} {3,-8} {4}" -f "ID","ENDPOINT","CONNECTED","QUEUE","SPOOL") -ForegroundColor Cyan
    Write-Host ("  " + ("-" * 90)) -ForegroundColor DarkGray
    foreach ($t in $list) {
        $conn  = if ($t.connected) { "YES" } else { "NO" }
        $color = if ($t.connected) { "Green" } else { "Yellow" }
        Write-Host ("  {0,-20} {1,-45} " -f $t.id, $t.endpoint) -NoNewline
        Write-Host ("{0,-10}" -f $conn) -ForegroundColor $color -NoNewline
        Write-Host ("{0,-8} {1}" -f $t.queueDepth, $t.spoolDepth)
    }
    Write-Host ""
}

function Cmd-TargetAdd {
    param([string[]]$Tokens)
    if ($Tokens.Count -lt 2) {
        Write-Err "  Usage: target add [id] [endpoint] [key=value ...]"
        Write-Dim "  Batch options : compress=true  maxBatchSize=1000  maxBatchMs=5000"
        Write-Dim "                  maxBatchBytes=2097152  queueCapacity=20000"
        Write-Dim "                  maxSpoolFiles=5000  maxSpoolSizeMb=1024"
        Write-Dim "                  maxBatchesPerSecond=10"
        Write-Dim "  Reconnect     : reconnectBaseMs=1000  reconnectMaxMs=60000"
        Write-Dim "  TLS (wss://)  : tls.insecureTrustAll=true"
        Write-Dim "                  tls.keystorePath=C:\certs\client.p12  tls.keystorePassword=secret"
        Write-Dim "                  tls.truststorePath=C:\certs\ca.p12    tls.truststorePassword=secret"
        return
    }
    $id       = $Tokens[0]
    $endpoint = $Tokens[1]

    if ($endpoint -notmatch "^wss?://") {
        Write-Err "  Endpoint must start with ws:// or wss://"
        return
    }

    # [2026-04-21] key=value 옵션 파싱 — 배치/TLS/스풀/속도제한 설정 지원
    $body = @{ id = $id; endpoint = $endpoint }
    $tls  = @{}

    for ($i = 2; $i -lt $Tokens.Count; $i++) {
        $kv = $Tokens[$i] -split "=", 2
        if ($kv.Count -ne 2) { continue }
        $k = $kv[0].Trim()
        $v = $kv[1].Trim()

        switch ($k) {
            "compress"            { $body["compress"]            = ($v -eq "true") }
            "maxBatchSize"        { $body["maxBatchSize"]        = [int]$v }
            "maxBatchMs"          { $body["maxBatchMs"]          = [long]$v }
            "maxBatchBytes"       { $body["maxBatchBytes"]       = [long]$v }
            "queueCapacity"       { $body["queueCapacity"]       = [int]$v }
            "maxSpoolFiles"       { $body["maxSpoolFiles"]       = [int]$v }
            "maxSpoolSizeMb"      { $body["maxSpoolSizeMb"]      = [long]$v }
            "maxBatchesPerSecond" { $body["maxBatchesPerSecond"] = [int]$v }
            "reconnectBaseMs"     { $body["reconnectBaseMs"]     = [long]$v }
            "reconnectMaxMs"      { $body["reconnectMaxMs"]      = [long]$v }
            "ackTimeoutMs"        { $body["ackTimeoutMs"]        = [long]$v }
            "tls.insecureTrustAll"   { $tls["insecureTrustAll"]   = ($v -eq "true") }
            "tls.keystorePath"       { $tls["keystorePath"]       = $v }
            "tls.keystorePassword"   { $tls["keystorePassword"]   = $v }
            "tls.truststorePath"     { $tls["truststorePath"]     = $v }
            "tls.truststorePassword" { $tls["truststorePassword"] = $v }
            "tls.keystoreType"       { $tls["keystoreType"]       = $v }
        }
    }
    if ($tls.Count -gt 0) { $body["tls"] = $tls }

    $r = Invoke-Admin -Method "POST" -Path "/targets" -Body $body
    if ($r.ok) {
        Write-Ok "  Target '$id' added -> $endpoint"
    } else {
        Write-Err "  Error: $($r.error)"
    }
}

function Cmd-TargetShow {
    param([string[]]$Tokens)
    if ($Tokens.Count -lt 1 -or $Tokens[0] -eq "") {
        Write-Err "  Usage: target show [id]"
        return
    }
    $id = $Tokens[0]
    $r  = Invoke-Admin -Path "/targets/$id"
    if (-not $r.ok) { Write-Err "  Error: $($r.error)"; return }
    $t = $r.data
    Write-Host ""
    Write-Host "  ID       : $($t.id)"
    Write-Host "  Endpoint : $($t.endpoint)"
    Write-Host "  Compress : $($t.compress)"
    Write-Host "  Reconnect: base=$($t.reconnectBaseMs)ms  max=$($t.reconnectMaxMs)ms"
    Write-Host "  Batch    : size=$($t.maxBatchSize)  ms=$($t.maxBatchMs)  bytes=$($t.maxBatchBytes)"
    Write-Host "  Queue    : capacity=$($t.queueCapacity)"
    Write-Host "  Spool    : files=$($t.maxSpoolFiles)  sizeMb=$($t.maxSpoolSizeMb)"
    Write-Host "  RateLimit: $($t.maxBatchesPerSecond)/sec (0=unlimited)"
    if ($t.tls) {
        Write-Host "  TLS      : insecureTrustAll=$($t.tls.insecureTrustAll)"
        if ($t.tls.keystorePath)   { Write-Host "             keystore=$($t.tls.keystorePath)" }
        if ($t.tls.truststorePath) { Write-Host "             truststore=$($t.tls.truststorePath)" }
    }
    Write-Host ""
}

function Cmd-TargetRemove {
    param([string[]]$Tokens)
    if ($Tokens.Count -lt 1 -or $Tokens[0] -eq "") {
        Write-Err "  Usage: target remove [id]"
        return
    }
    $id = $Tokens[0]
    $r  = Invoke-Admin -Method "DELETE" -Path "/targets/$id"
    if ($r.ok) {
        Write-Ok "  Target '$id' removed."
    } else {
        Write-Err "  Error: $($r.error)"
    }
}

# [2026-04-21] collector list [targetId] — 수집기 상태 및 설정 조회
function Cmd-CollectorList {
    param([string[]]$Tokens)
    if ($Tokens.Count -lt 1 -or $Tokens[0] -eq "") {
        Write-Err "  Usage: collector list [targetId]"
        return
    }
    $id = $Tokens[0]
    $r  = Invoke-Admin -Path "/targets/$id/collectors"
    if (-not $r.ok) { Write-Err "  Error: $($r.error)"; return }
    $list = $r.data
    if ($list.Count -eq 0) {
        Write-Warn "  No collectors registered for target '$id'."
        return
    }
    Write-Host ""
    Write-Host "  Collectors for target: $id" -ForegroundColor Cyan
    Write-Host ("  " + ("-" * 100)) -ForegroundColor DarkGray
    Write-Host ("  {0,-22} {1,-22} {2,-6} {3,-8} {4,-9} {5,-10} {6}" -f "ID","NAME","TYPE","ENABLED","RUNNING","POLL(ms)","CONFIG") -ForegroundColor Cyan
    Write-Host ("  " + ("-" * 100)) -ForegroundColor DarkGray
    foreach ($c in $list) {
        $enabled = if ($c.enabled) { "YES" } else { "NO" }
        $running = if ($c.running) { "YES" } else { "NO" }
        $enabledColor = if ($c.enabled) { "Green"  } else { "DarkGray" }
        $runningColor = if ($c.running) { "Green"  } else { "Yellow" }
        $extra = ""
        if ($c.type -eq "FILE") {
            $extra = "dir=$($c.directory)  pattern=$($c.filePattern)  fmt=$($c.format)"
        } elseif ($c.type -eq "JDBC") {
            $extra = "url=$($c.url)"
        }
        Write-Host ("  {0,-22} {1,-22} {2,-6} " -f $c.id, $c.name, $c.type) -NoNewline
        Write-Host ("{0,-8}" -f $enabled) -ForegroundColor $enabledColor -NoNewline
        Write-Host ("{0,-9}" -f $running) -ForegroundColor $runningColor -NoNewline
        Write-Host ("{0,-10} {1}" -f $c.pollIntervalMs, $extra)
    }
    Write-Host ""
}

function Show-Help {
    Write-Host ""
    Write-Info "  Available commands:"
    Write-Host ""
    Write-Host "  status                               Agent status"
    Write-Host "  target list                          List connected targets"
    Write-Host "  target show [id]                     Show full target settings"
    Write-Host "  target add [id] [endpoint] [opts]    Add and connect a target"
    Write-Host "  target remove [id]                   Remove and disconnect a target"
    Write-Host "  collector list [targetId]            List collectors for a target"
    Write-Host "  help                                 Show this help"
    Write-Host "  exit / quit                          Exit CLI"
    Write-Host ""
    Write-Dim  "  target add options (key=value):"
    Write-Dim  "    compress=true  maxBatchSize=1000  maxBatchMs=5000  maxBatchBytes=2097152"
    Write-Dim  "    queueCapacity=20000  maxSpoolFiles=5000  maxSpoolSizeMb=1024"
    Write-Dim  "    maxBatchesPerSecond=10  reconnectBaseMs=1000  reconnectMaxMs=60000"
    Write-Dim  "    tls.insecureTrustAll=true"
    Write-Dim  "    tls.keystorePath=C:\certs\client.p12  tls.keystorePassword=secret"
    Write-Dim  "    tls.truststorePath=C:\certs\ca.p12    tls.truststorePassword=secret"
    Write-Host ""
    Write-Dim  "  Examples:"
    Write-Dim  "    target add icon-backend ws://192.168.1.1:11100/rpc"
    Write-Dim  "    target add icon-backend ws://192.168.1.1:11100/rpc compress=true maxBatchSize=1000"
    Write-Dim  "    target add icon-backend wss://192.168.1.1:11100/rpc tls.insecureTrustAll=true"
    Write-Dim  "    target remove icon-backend"
    Write-Dim  "    collector list icon-backend"
    Write-Host ""
}

# -- Connection check -----------------------------------------
$ping = Invoke-Admin -Path "/status"
if (-not $ping.ok) {
    Write-Err ""
    Write-Err "  Cannot connect to agent at 127.0.0.1:$Port"
    Write-Err "  Make sure the agent is running: .\icon-agent.bat status"
    Write-Err ""
    exit 1
}

# -- Banner ---------------------------------------------------
$d = $ping.data
Write-Host ""
Write-Host "  +=======================================+" -ForegroundColor Cyan
Write-Host "  |     ICON Agent CLI  (port $Port)       |" -ForegroundColor Cyan
Write-Host "  +=======================================+" -ForegroundColor Cyan
Write-Host ""
Write-Host "  AgentId : $($d.agentId)"
Write-Host "  Targets : $($d.targets) connected"
Write-Host ""
Write-Dim  "  Type 'help' for available commands."
Write-Host ""

# -- REPL loop ------------------------------------------------
while ($true) {
    $raw = Read-Host "agent"
    if ($null -eq $raw) { break }
    $line = $raw.Trim()
    if ($line -eq "") { continue }

    $parts = $line -split '\s+'
    $cmd   = $parts[0].ToLower()
    $sub   = if ($parts.Count -gt 1) { $parts[1].ToLower() } else { "" }
    $rest  = if ($parts.Count -gt 2) { $parts[2..($parts.Count - 1)] } else { @() }

    switch ($cmd) {
        "exit"   { Write-Host "  Bye."; exit 0 }
        "quit"   { Write-Host "  Bye."; exit 0 }
        "help"   { Show-Help }
        "status" { Cmd-Status }
        "target" {
            switch ($sub) {
                "list"   { Cmd-TargetList }
                "show"   { Cmd-TargetShow -Tokens $rest }
                "add"    { Cmd-TargetAdd -Tokens $rest }
                "remove" { Cmd-TargetRemove -Tokens $rest }
                default  { Write-Err "  Unknown target sub-command: '$sub'. Type 'help'." }
            }
        }
        # [2026-04-21] collector list [targetId] — 수집기 상태 조회
        "collector" {
            switch ($sub) {
                "list"  { Cmd-CollectorList -Tokens $rest }
                default { Write-Err "  Unknown collector sub-command: '$sub'. Try: collector list [targetId]" }
            }
        }
        default { Write-Err "  Unknown command: '$cmd'. Type 'help'." }
    }
}
