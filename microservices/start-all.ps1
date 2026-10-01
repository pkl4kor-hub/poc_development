$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Starting Workbench E-Commerce Microservices Stack" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$services = @(
    @{ Name = "Catalog-Service"; Dir = "catalog-service"; Jar = "catalog-service-0.0.1-SNAPSHOT.jar"; Port = 8082 },
    @{ Name = "Cart-Service";    Dir = "cart-service";    Jar = "cart-service-0.0.1-SNAPSHOT.jar";    Port = 8083 },
    @{ Name = "Auth-Service";    Dir = "auth-service";    Jar = "auth-service-0.0.1-SNAPSHOT.jar";    Port = 8081 },
    @{ Name = "Payment-Service"; Dir = "payment-service"; Jar = "payment-service-0.0.1-SNAPSHOT.jar"; Port = 8085 },
    @{ Name = "Order-Service";   Dir = "order-service";   Jar = "order-service-0.0.1-SNAPSHOT.jar";   Port = 8084 }
)

foreach ($s in $services) {
    Write-Host "Starting $($s.Name) on port $($s.Port)..." -ForegroundColor Yellow
    $workingDir = Join-Path $scriptDir $s.Dir
    $jarPath = Join-Path "target" $s.Jar
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$workingDir'; java -jar '$jarPath'"
}

Start-Sleep -Seconds 5

Write-Host "Starting API Gateway on port 3001..." -ForegroundColor Yellow
$gatewayDir = Join-Path $scriptDir "api-gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$gatewayDir'; java -jar 'target\api-gateway-0.0.1-SNAPSHOT.jar'"

Write-Host "===================================================" -ForegroundColor Green
Write-Host "All microservices launched! API Gateway is on http://localhost:3001" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
