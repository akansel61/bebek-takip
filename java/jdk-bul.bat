@echo off
rem Sistemdeki uygun JDK'yi bulup JDK degiskenine yazar.
rem Diger betikler bunu "call jdk-bul.bat" ile kullanir.
rem Burada setlocal YOK; degiskenin cagirana donmesi gerekiyor.

set "JDK="

if defined JAVA_HOME call :dene "%JAVA_HOME%"
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk*") do call :dene "%%~fD"
for /d %%D in ("C:\Program Files\Java\jdk*") do call :dene "%%~fD"
for /d %%D in ("C:\Program Files\Microsoft\jdk*") do call :dene "%%~fD"
for /d %%D in ("C:\Program Files\Amazon Corretto\jdk*") do call :dene "%%~fD"
for /d %%D in ("C:\Program Files\Zulu\zulu*") do call :dene "%%~fD"
for /d %%D in ("C:\Program Files\BellSoft\LibericaJDK*") do call :dene "%%~fD"

exit /b 0

rem Aday klasorde hem javac hem jpackage varsa JDK 17 veya ustu demektir.
:dene
if defined JDK exit /b 0
if not exist "%~1\bin\javac.exe" exit /b 0
if not exist "%~1\bin\jpackage.exe" exit /b 0
set "JDK=%~1"
exit /b 0
