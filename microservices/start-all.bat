@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo Starting Workbench E-Commerce Microservices Stack
echo ===================================================

echo Starting Catalog Service on port 8082...
start "Catalog-Service" cmd /k "cd /d "%~dp0catalog-service" && java -jar target\catalog-service-0.0.1-SNAPSHOT.jar"

echo Starting Cart Service on port 8083...
start "Cart-Service" cmd /k "cd /d "%~dp0cart-service" && java -jar target\cart-service-0.0.1-SNAPSHOT.jar"

echo Starting Auth Service on port 8081...
start "Auth-Service" cmd /k "cd /d "%~dp0auth-service" && java -jar target\auth-service-0.0.1-SNAPSHOT.jar"

echo Starting Payment Service on port 8085...
start "Payment-Service" cmd /k "cd /d "%~dp0payment-service" && java -jar target\payment-service-0.0.1-SNAPSHOT.jar"

echo Starting Order Service on port 8084...
start "Order-Service" cmd /k "cd /d "%~dp0order-service" && java -jar target\order-service-0.0.1-SNAPSHOT.jar"

timeout /t 5 >nul

echo Starting API Gateway on port 3001...
start "API-Gateway" cmd /k "cd /d "%~dp0api-gateway" && java -jar target\api-gateway-0.0.1-SNAPSHOT.jar"

echo All microservices launched! API Gateway is on http://localhost:3001.
