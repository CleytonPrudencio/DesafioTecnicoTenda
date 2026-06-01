#!/usr/bin/env sh
set -e

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker nao encontrado. Instale o Docker Desktop e tente novamente."
  exit 1
fi

docker compose version >/dev/null 2>&1 || {
  echo "Docker Compose plugin nao encontrado."
  exit 1
}

cmd="${1:-up}"

case "$cmd" in
  up)
    docker compose up --build -d
    docker compose ps
    echo ""
    echo "Frontend  : http://localhost:3000"
    echo "API       : http://localhost:8080"
    echo "Swagger   : http://localhost:8080/swagger-ui/index.html"
    echo "H2        : http://localhost:8080/h2-console"
    ;;
  down)
    docker compose down
    ;;
  logs)
    docker compose logs -f "${2:-}"
    ;;
  rebuild)
    docker compose down
    docker compose build --no-cache
    docker compose up -d
    ;;
  *)
    echo "Uso: $0 [up|down|logs|rebuild]"
    exit 1
    ;;
esac
