@echo off
chcp 65001 >nul
cd /d "%~dp0..\.."
echo GAIN-Z 도트 공방 실행 중... 브라우저가 자동으로 열립니다. (종료: 탭 닫기 또는 Ctrl+C)
node tools\dot-studio\server.cjs
pause
