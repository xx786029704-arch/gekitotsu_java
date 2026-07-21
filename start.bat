@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 正在启动对战模拟服务...
java -jar target\gekitotsu_java-1.3.4.jar --mode server %*
pause
