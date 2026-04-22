@echo off
:: ICON Agent CLI — AdminServer(127.0.0.1:8081) 인터랙티브 관리 셸
:: [2026-04-21] 신규 생성
powershell -ExecutionPolicy Bypass -File "%~dp0agent-cli.ps1" %*
