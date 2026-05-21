@echo off
setlocal
cd /d "%~dp0"

if not exist logs mkdir logs

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd"') do set RELEASE_DATE=%%i

echo Starting backend on http://127.0.0.1:8080
start "feature-backend" /min cmd /c "%~dp0start-backend.cmd 1> %~dp0logs\backend.out.log 2> %~dp0logs\backend.err.log"

echo Starting web-admin on http://127.0.0.1:5173
start "feature-web-admin" /min cmd /c "cd /d %~dp0web-admin && npm.cmd run dev -- --host 127.0.0.1 --port 5173 1> %~dp0logs\web-admin.out.log 2> %~dp0logs\web-admin.err.log"

echo Starting vue-demo on http://127.0.0.1:5174
start "feature-vue-demo" /min cmd /c "cd /d %~dp0vue-demo && npm.cmd run dev -- --host 127.0.0.1 --port 5174 1> %~dp0logs\vue-demo.out.log 2> %~dp0logs\vue-demo.err.log"

echo Starting java-demo polling client
start "feature-java-demo" cmd /c "cd /d %~dp0 && java -cp java-demo\target\classes;java-sdk\target\classes;D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-annotations\2.21\jackson-annotations-2.21.jar;D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-core\2.21.2\jackson-core-2.21.2.jar;D:\Java\maven-repository\com\fasterxml\jackson\core\jackson-databind\2.21.2\jackson-databind-2.21.2.jar com.example.featuredemo.JavaFeatureDemo http://127.0.0.1:8080 java-demo-user Asia vip %RELEASE_DATE% 5"

echo Starting python-demo polling client
start "feature-python-demo" cmd /c "cd /d %~dp0python-demo && python main.py http://127.0.0.1:8080 python-demo-user Asia vip %RELEASE_DATE% 5"

echo Started. Logs: %~dp0logs
endlocal
