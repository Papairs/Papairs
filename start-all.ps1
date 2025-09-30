# Papairs - Start All Services
# This script starts the Vue frontend and both Spring Boot backends

Write-Host "Starting Papairs Services..." -ForegroundColor Green
Write-Host "=========================" -ForegroundColor Green

# Function to start a service in a new PowerShell window
function Start-Service {
    param(
        [string]$ServiceName,
        [string]$Path,
        [string]$Command,
        [string]$Color
    )
    
    Write-Host "Starting $ServiceName..." -ForegroundColor $Color
    
    $scriptBlock = "
        Set-Location '$Path'
        Write-Host 'Starting $ServiceName in $Path' -ForegroundColor $Color
        $Command
        Write-Host '$ServiceName stopped. Press any key to close...' -ForegroundColor Yellow
        Read-Host
    "
    
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $scriptBlock
}

# Start Auth Service (Port 8081)
Start-Service -ServiceName "Auth Service" -Path "E:\C code\Papairs\backend\auth-service" -Command "mvn spring-boot:run" -Color "Blue"
Start-Sleep -Seconds 2

# Start Docs Service (Port 8082)  
Start-Service -ServiceName "Docs Service" -Path "E:\C code\Papairs\backend\docs-service" -Command "mvn spring-boot:run" -Color "Magenta"
Start-Sleep -Seconds 2

# Start Frontend (Port 3000)
Start-Service -ServiceName "Vue Frontend" -Path "E:\C code\Papairs\frontend" -Command "npm run serve" -Color "Green"

Write-Host ""
Write-Host "All services are starting up!" -ForegroundColor Green
Write-Host "Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "Auth Service: http://localhost:8081" -ForegroundColor Cyan  
Write-Host "Docs Service: http://localhost:8082" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop this script (services will continue running)" -ForegroundColor Yellow
Write-Host "Close individual service windows to stop each service" -ForegroundColor Yellow

# Keep the main script running
try {
    while ($true) {
        Start-Sleep -Seconds 30
        Write-Host "Services running... $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Gray
    }
}
catch {
    Write-Host "Script interrupted" -ForegroundColor Red
}