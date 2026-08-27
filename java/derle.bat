@echo off
rem Kaynaklari derler, calistirilabilir JAR'i ve uygulama simgesini uretir.
rem Sira onemli: once cd, sonra chcp. Tersi olursa cmd betigin kendi
rem yolundaki Turkce harfleri bozuyor ve dosyalari bulamiyor.
cd /d "%~dp0"
set "KOK=%CD%"
chcp 65001 >nul

call "%KOK%\jdk-bul.bat"
if not defined JDK goto :jdkYok

echo.
echo   Bebek Takip - derleme
echo   ---------------------
echo   JDK: %JDK%
echo.

if exist "build\classes" rmdir /s /q "build\classes"
if exist "build\jar" rmdir /s /q "build\jar"
if not exist "build" mkdir "build"
mkdir "build\classes"
mkdir "build\jar"

echo   [1/3] Kaynaklar derleniyor...
"%JDK%\bin\javac.exe" -encoding UTF-8 --release 17 -sourcepath "src" -d "build\classes" ^
    "src\com\akansel\bebektakip\App.java" ^
    "src\com\akansel\bebektakip\arac\IkonUret.java"
if errorlevel 1 goto :hata

echo   [2/3] JAR oluşturuluyor...
"%JDK%\bin\jar.exe" --create --file "build\jar\BebekTakip.jar" ^
    --main-class com.akansel.bebektakip.App -C "build\classes" .
if errorlevel 1 goto :hata

echo   [3/3] Uygulama simgesi üretiliyor...
"%JDK%\bin\java.exe" -Djava.awt.headless=true -cp "build\classes" ^
    com.akansel.bebektakip.arac.IkonUret "build\bebek-takip.ico"
if errorlevel 1 goto :hata

echo.
echo   TAMAM
echo     JAR   : build\jar\BebekTakip.jar
echo     Simge : build\bebek-takip.ico
echo.
echo   Çalıştırmak için  : calistir.bat
echo   .exe üretmek için : paketle.bat
echo.
exit /b 0

:jdkYok
echo.
echo   HATA: JDK 17 veya üstü bulunamadı.
echo.
echo   JAVA_HOME ayarlı değilse şu klasörler taranır:
echo     C:\Program Files\Eclipse Adoptium\jdk*
echo     C:\Program Files\Java\jdk*
echo     C:\Program Files\Microsoft\jdk*
echo.
echo   JDK indirmek için: https://adoptium.net
echo.
exit /b 1

:hata
echo.
echo   HATA: Derleme başarısız oldu. Yukarıdaki mesajlara bakın.
echo.
exit /b 1
