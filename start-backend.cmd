@echo off
cd /d "%~dp0"
java -jar backend\target\backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
