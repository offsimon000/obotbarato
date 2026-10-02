@echo off
cd /d "%~dp0"
echo Feche o aplicativo antes deste teste.
echo O teste usa dados ficticios e preserva o banco PostgreSQL original.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\testar-integracao.ps1"
if errorlevel 1 goto erro
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\verificar-base.ps1"
if errorlevel 1 goto erro
echo.
echo APROVADO. Consulte os resultados na pasta docs.
pause
exit /b 0
:erro
echo.
echo Teste nao concluido. Anote a mensagem acima.
pause
exit /b 1
