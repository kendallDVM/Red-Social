@echo off
cd /d "%~dp0"
java -cp "target/classes;lib/gson-2.15.0.jar;lib/bcrypt-0.10.2.jar;lib/bytes-1.5.0.jar" socialuna.Main
pause
