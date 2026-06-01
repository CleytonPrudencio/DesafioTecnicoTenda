@echo off
setlocal

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker nao encontrado. Instale o Docker Desktop e tente novamente.
  exit /b 1
)

set CMD=%1
if "%CMD%"=="" set CMD=up

if /I "%CMD%"=="up" (
  docker compose up --build -d
  docker compose ps
  echo.
  echo Frontend  : http://localhost:3000
  echo API       : http://localhost:8080
  echo Swagger   : http://localhost:8080/swagger-ui/index.html
  echo H2        : http://localhost:8080/h2-console
  goto :eof
)
if /I "%CMD%"=="down" (
  docker compose down
  goto :eof
)
if /I "%CMD%"=="logs" (
  docker compose logs -f %2
  goto :eof
)
if /I "%CMD%"=="rebuild" (
  docker compose down
  docker compose build --no-cache
  docker compose up -d
  goto :eof
)

echo Uso: %~nx0 [up^|down^|logs^|rebuild]
exit /b 1
