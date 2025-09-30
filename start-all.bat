@echo off
echo Starting Papairs Services...
echo =========================

echo Starting Auth Service on port 8081...
start "Auth Service" cmd /k "cd /d "E:\C code\Papairs\backend\auth-service" && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo Starting Docs Service on port 8082...
start "Docs Service" cmd /k "cd /d "E:\C code\Papairs\backend\docs-service" && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo Starting Vue Frontend on port 3000...
start "Vue Frontend" cmd /k "cd /d "E:\C code\Papairs\frontend" && npm run serve"

echo.
echo All services are starting up!
echo Frontend: http://localhost:3000
echo Auth Service: http://localhost:8081
echo Docs Service: http://localhost:8082
echo.
echo Close individual windows to stop each service
echo Press any key to exit this launcher...
pause >nul