# Test all services
Write-Host "Testing Papairs Services..." -ForegroundColor Green

# Test Auth Service
try {
    $authResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/health" -Method GET
    Write-Host "✓ Auth Service: " -ForegroundColor Green -NoNewline
    Write-Host $authResponse.message -ForegroundColor White
} catch {
    Write-Host "✗ Auth Service: Not responding" -ForegroundColor Red
}

# Test Docs Service  
try {
    $docsResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/docs/health" -Method GET
    Write-Host "✓ Docs Service: " -ForegroundColor Green -NoNewline
    Write-Host $docsResponse.message -ForegroundColor White
} catch {
    Write-Host "✗ Docs Service: Not responding" -ForegroundColor Red
}

# Test Frontend
try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost:3000" -Method GET -UseBasicParsing
    Write-Host "✓ Frontend: Running (Status: $($frontendResponse.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "✗ Frontend: Not responding" -ForegroundColor Red
}

Write-Host "`nIf all services are running:" -ForegroundColor Yellow
Write-Host "- Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "- Auth API: http://localhost:8081/api/auth/health" -ForegroundColor Cyan  
Write-Host "- Docs API: http://localhost:8082/api/docs/health" -ForegroundColor Cyan