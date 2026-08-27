@echo off
rem Uygulamayi JAR olarak calistirir; JAR yoksa once derler.
cd /d "%~dp0"
set "KOK=%CD%"
chcp 65001 >nul

call "%KOK%\jdk-bul.bat"
if not defined JDK goto :jdkYok

if not exist "build\jar\BebekTakip.jar" (
    echo   JAR bulunamadı, önce derleniyor...
    call "%KOK%\derle.bat"
    if errorlevel 1 exit /b 1
)

start "Bebek Takip" "%JDK%\bin\javaw.exe" ^
    -Dfile.encoding=UTF-8 ^
    -Dawt.useSystemAAFontSettings=on ^
    -Dswing.aatext=true ^
    -jar "build\jar\BebekTakip.jar"
exit /b 0

:jdkYok
echo.
echo   HATA: JDK 17 veya üstü bulunamadı. https://adoptium.net
echo.
pause
exit /b 1
