@echo off
rem Abre Social UNA. Requiere Java 17 o superior.
rem Usa la carpeta de este archivo como directorio de trabajo para leer data\ correctamente.
cd /d "%~dp0"

if exist "SocialUNA.jar" (set "JAR=SocialUNA.jar") else (set "JAR=target\SocialUNA.jar")
if not exist "%JAR%" (
    echo No se encontro SocialUNA.jar.
    echo Si esta usando el codigo fuente, generelo primero con: mvn package
    pause
    exit /b 1
)

java -jar "%JAR%"
if errorlevel 1 (
    echo.
    echo No se pudo abrir Social UNA. Verifique que tenga Java 17 o superior con: java -version
    pause
)
