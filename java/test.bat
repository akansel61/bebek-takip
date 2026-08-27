@echo off
rem Dogrulama betigi.
rem   test.bat            -> veri ve islev testlerini calistirir
rem   test.bat onizleme   -> ekranlarin PNG goruntusunu build\onizleme icine yazar
cd /d "%~dp0"
set "KOK=%CD%"
chcp 65001 >nul

call "%KOK%\jdk-bul.bat"
if not defined JDK goto :jdkYok

if not exist "build\classes\com\akansel\bebektakip\App.class" (
    call "%KOK%\derle.bat"
    if errorlevel 1 exit /b 1
)

echo.
echo   Testler derleniyor...
"%JDK%\bin\javac.exe" -encoding UTF-8 -cp "build\classes" -d "build\classes" ^
    "test\Deneme.java" "test\Islev.java" "test\Onizleme.java"
if errorlevel 1 goto :hata

if /i "%~1"=="onizleme" goto :onizleme

echo.
echo   === Veri katmanı testleri ===
"%JDK%\bin\java.exe" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 ^
    -Djava.awt.headless=true -cp "build\classes" Deneme
if errorlevel 1 goto :hata

echo.
echo   === İşlevsel testler (gerçek pencere, ekran dışında) ===
"%JDK%\bin\java.exe" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 ^
    -cp "build\classes" Islev
if errorlevel 1 goto :hata

echo.
echo   TÜM TESTLER GEÇTİ
echo.
exit /b 0

:onizleme
echo.
echo   Ekran görüntüleri üretiliyor...
"%JDK%\bin\java.exe" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 ^
    -cp "build\classes" Onizleme "build\onizleme"
if errorlevel 1 goto :hata
echo.
echo   TAMAM -^> build\onizleme
echo.
exit /b 0

:jdkYok
echo.
echo   HATA: JDK 17 veya üstü bulunamadı. https://adoptium.net
echo.
exit /b 1

:hata
echo.
echo   TEST BAŞARISIZ
echo.
exit /b 1
