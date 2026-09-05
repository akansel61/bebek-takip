@echo off
rem Windows icin kendi Java calisma zamanini iceren .exe uretir (jpackage).
rem
rem   paketle.bat                 -> %USERPROFILE%\BebekTakip icine uretir
rem   paketle.bat "D:\Programlar" -> verilen klasore uretir
rem   paketle.bat kurulum         -> MSI/EXE kurulum dosyasi (WiX 3.x gerekir)
rem
rem Uyari: uretilen .exe, yolunda Windows ANSI kod sayfasinda bulunmayan bir
rem karakter (i-noktasiz, g-yumusak, s-cedilla) gecen klasorden calismaz;
rem bu yuzden varsayilan hedef ASCII bir klasor.
setlocal
cd /d "%~dp0"
set "KOK=%CD%"
chcp 65001 >nul

set "AD=Bebek Takip"
set "SURUM=1.2"
set "KURULUM=0"
set "CIKTI=%USERPROFILE%\BebekTakip"

if /i "%~1"=="kurulum" (
    set "KURULUM=1"
) else (
    if not "%~1"=="" set "CIKTI=%~1"
)

call "%KOK%\jdk-bul.bat"
if not defined JDK goto :jdkYok

echo.
echo   Bebek Takip - paketleme
echo   -----------------------
echo   JDK   : %JDK%
echo   Hedef : %CIKTI%
echo.

call "%KOK%\derle.bat"
if errorlevel 1 exit /b 1

if not exist "%CIKTI%" mkdir "%CIKTI%"
if exist "%CIKTI%\%AD%" rmdir /s /q "%CIKTI%\%AD%"

if "%KURULUM%"=="1" goto :kurulumUret

echo   [4/4] Windows uygulaması paketleniyor (biraz sürer)...
"%JDK%\bin\jpackage.exe" ^
    --type app-image ^
    --name "%AD%" ^
    --app-version %SURUM% ^
    --vendor "AKANSEL" ^
    --copyright "© 2026 by AKANSEL" ^
    --description "Bebek Beslenme Takibi" ^
    --input "build\jar" ^
    --main-jar BebekTakip.jar ^
    --main-class com.akansel.bebektakip.App ^
    --icon "build\bebek-takip.ico" ^
    --dest "%CIKTI%" ^
    --add-modules java.base,java.desktop ^
    --jlink-options "--strip-debug --no-header-files --no-man-pages" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --java-options "-Dawt.useSystemAAFontSettings=on" ^
    --java-options "-Dswing.aatext=true"
if errorlevel 1 goto :hata

echo.
echo   TAMAM
echo     %CIKTI%\%AD%\%AD%.exe
echo.
echo   Klasör kendi Java çalışma zamanını içeriyor; bilgisayarda Java kurulu
echo   olmasına gerek yok. Olduğu gibi kopyalayıp taşıyabilirsiniz.
echo.

set /p KISAYOL=  Masaüstüne kısayol oluşturulsun mu? (E/H):
if /i "%KISAYOL%"=="E" (
    powershell -NoProfile -Command ^
      "$s=(New-Object -COM WScript.Shell).CreateShortcut([Environment]::GetFolderPath('Desktop')+'\Bebek Takip.lnk'); $s.TargetPath='%CIKTI%\%AD%\%AD%.exe'; $s.WorkingDirectory='%CIKTI%\%AD%'; $s.Description='Bebek Beslenme Takibi'; $s.Save()"
    echo   Kısayol masaüstüne eklendi.
)
echo.
exit /b 0

:kurulumUret
echo   [4/4] Kurulum dosyası üretiliyor (WiX gerekir)...
"%JDK%\bin\jpackage.exe" ^
    --type exe ^
    --name "%AD%" ^
    --app-version %SURUM% ^
    --vendor "AKANSEL" ^
    --copyright "© 2026 by AKANSEL" ^
    --description "Bebek Beslenme Takibi" ^
    --input "build\jar" ^
    --main-jar BebekTakip.jar ^
    --main-class com.akansel.bebektakip.App ^
    --icon "build\bebek-takip.ico" ^
    --dest "%CIKTI%" ^
    --add-modules java.base,java.desktop ^
    --jlink-options "--strip-debug --no-header-files --no-man-pages" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --java-options "-Dawt.useSystemAAFontSettings=on" ^
    --java-options "-Dswing.aatext=true" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --win-per-user-install
if errorlevel 1 goto :wixYok

echo.
echo   TAMAM - kurulum dosyası: %CIKTI%
echo.
exit /b 0

:wixYok
echo.
echo   Kurulum dosyası üretilemedi.
echo   jpackage, Windows kurulum paketi için WiX Toolset 3.x istiyor.
echo.
echo     1) https://github.com/wixtoolset/wix3/releases adresinden
echo        WiX 3.11 (wix311.exe) kurun
echo     2) WiX bin klasörünü PATH'e ekleyin
echo     3) paketle.bat kurulum  komutunu tekrar çalıştırın
echo.
echo   Alternatif: WiX olmadan da  paketle.bat  ile çalışan bir .exe
echo   üretebilirsiniz (kurulum dosyası değil, taşınabilir klasör).
echo.
exit /b 1

:jdkYok
echo.
echo   HATA: JDK 17 veya üstü bulunamadı. https://adoptium.net
echo.
exit /b 1

:hata
echo.
echo   HATA: Paketleme başarısız oldu.
echo.
exit /b 1
