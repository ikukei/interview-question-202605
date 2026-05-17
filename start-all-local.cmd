@echo off
setlocal
cd /d "%~dp0"

if not exist logs mkdir logs

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd"') do set RELEASE_DATE=%%i

echo Starting backend on http://127.0.0.1:8080
start "feature-backend" /min cmd /c "java @backend-run.args 1> logs\backend.out.log 2> logs\backend.err.log"

echo Starting web-admin on http://127.0.0.1:5173
start "feature-web-admin" /min cmd /c "cd /d web-admin && npm.cmd run dev -- --host 127.0.0.1 --port 5173 1> ..\logs\web-admin.out.log 2> ..\logs\web-admin.err.log"

echo Starting vue-demo on http://127.0.0.1:5174
start "feature-vue-demo" /min cmd /c "cd /d vue-demo && npm.cmd run dev -- --host 127.0.0.1 --port 5174 1> ..\logs\vue-demo.out.log 2> ..\logs\vue-demo.err.log"

echo Starting java-demo polling client
start "feature-java-demo" /min cmd /c "java -cp java-demo\target\classes;java-sdk\target\classes;D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-annotations\2.21\jackson-annotations-2.21.jar;D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-core\2.21.2\jackson-core-2.21.2.jar;D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-databind\2.21.2\jackson-databind-2.21.2.jar com.example.featuredemo.JavaFeatureDemo http://127.0.0.1:8080 java-demo-user Asia vip %RELEASE_DATE% 5 1> logs\java-demo.out.log 2> logs\java-demo.err.log"

echo Started. Logs are under %cd%\logs
endlocal
