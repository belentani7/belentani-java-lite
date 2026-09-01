@echo off
echo ╔═══════════════════════════════════════════════════════════╗
echo ║                                                           ║
echo ║   BELENTANI LITE SERVER - Iniciando...                   ║
echo ║                                                           ║
echo ╚═══════════════════════════════════════════════════════════╝
echo.

cd /d "%~dp0"
java -cp out com.belentani.lite.BelentaniLiteServer %*

pause
