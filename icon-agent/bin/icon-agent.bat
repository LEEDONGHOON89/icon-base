@echo off
:: ICON Collector Agent — Windows 실행 진입점
:: 에이전트(icon-agent)는 수집 대상 Windows 시스템에서 직접 실행
:: 내부통제시스템 서버는 icon.sh 로 별도 관리
:: [2026-04-21] 헤더 정리
powershell -ExecutionPolicy Bypass -File "%~dp0icon-agent.ps1" %*
