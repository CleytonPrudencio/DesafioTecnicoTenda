# Coupon API — Desafio Técnico Tenda

API REST de gerenciamento de cupons em Java 21 + Spring Boot 3.5 com arquitetura em camadas (Domínio · Aplicação · Infraestrutura · API), regras de negócio encapsuladas em objetos de domínio puros, banco H2 em memória, documentação Swagger, frontend web estilizado e tudo orquestrado por Docker Compose.

## Sumário

- [Stack](#stack)
- [Como rodar — com Docker](#como-rodar--com-docker)
- [Como rodar — sem Docker (local)](#como-rodar--sem-docker-local)
- [Endpoints](#endpoints)
- [Regras de negócio](#regras-de-negócio-implementadas)
- [Arquitetura](#arquitetura)
- [Testes e cobertura](#testes)
- [Postman / Insomnia](#postman--insomnia)
- [Decisões de design](#decisões-de-design)

## Stack

- Java 21 · Spring Boot 3.5 (Web · Data JPA · Validation · Actuator)
- H2 (banco em memória)
- SpringDoc OpenAPI 2.8.6 · Swagger UI
- JUnit 5 · Mockito · AssertJ · JaCoCo (gate ≥ 80%)
- Docker · Docker Compose
- Frontend: HTML + Tailwind CSS + JavaScript vanilla servido por Nginx (reverse proxy)

---

## Como rodar — com Docker

Pré-requisito: **Docker Desktop** (ou Docker Engine + plugin Compose) instalado e rodando.

### Linha única

```bash
docker compose up --build
```

Ou usando os scripts wrapper:

```bash
# Linux/Mac/Git Bash
./docker-run.sh up

# Windows (cmd ou PowerShell)
docker-run.cmd up
```

### URLs disponíveis

| Serviço | URL |
| --- | --- |
| Frontend (Nginx + reverse proxy `/api/*`) | http://localhost:3000 |
| API REST direta | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 Console | http://localhost:8080/h2-console |
| Health Actuator | http://localhost:8080/actuator/health |

H2 Console: JDBC `jdbc:h2:mem:coupondb`, user `sa`, senha vazia.

### O que o Docker Compose sobe

- `coupon-api` (porta 8080): jar Spring Boot rodando sob JRE 21 Alpine, usuário não-root, com healthcheck `wget` ao `/actuator/health`. Memória limitada via `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`.
- `coupon-web` (porta 3000): Nginx Alpine servindo o frontend estático e fazendo reverse proxy de `/api/*`, `/swagger-ui/*` e `/v3/api-docs` para a API. Espera o `coupon-api` ficar **saudável** antes de subir (`depends_on.service_healthy`).
- Rede dedicada `coupon-net` (bridge).

### Comandos úteis

```bash
docker compose up --build -d         # sobe em background
docker compose ps                    # estado dos containers
docker compose logs -f coupon-api    # logs ao vivo da API
docker compose down                  # encerra tudo
docker compose down -v               # encerra e remove volumes
./docker-run.sh rebuild              # rebuild sem cache + sobe
```

---

## Como rodar — sem Docker (local)

Pré-requisitos:

- **Java 21** (instalado e no PATH — `java -version` deve indicar 21)
- **Maven 3.9+** (opcional; o wrapper `./mvnw` baixa Maven se necessário)
- **Python 3.8+** (apenas se quiser usar o servidor de desenvolvimento para o frontend; opcional)

### Subir só a API

```bash
./mvnw spring-boot:run
```

Ou empacotando o jar e rodando:

```bash
./mvnw -DskipTests package
java -jar target/coupon-api-0.0.1-SNAPSHOT.jar
```

A API fica em `http://localhost:8080`. Trocar de porta:

```bash
java -jar target/coupon-api-0.0.1-SNAPSHOT.jar --server.port=18080
```

### Subir o frontend localmente

O frontend é um conjunto de arquivos estáticos em `frontend/` que conversam com a API via `/api/*`. Há um pequeno servidor Python (`frontend/dev_server.py`) que serve os estáticos **e** faz proxy reverso para a API — replicando o comportamento do Nginx do Docker Compose.

```bash
cd frontend
API_TARGET=http://localhost:8080 PORT=3000 python dev_server.py
```

Variáveis de ambiente:

- `API_TARGET` — URL da API (default `http://localhost:8081`)
- `PORT` — porta do servidor estático (default `3001`)

Depois acesse http://localhost:3000.

Alternativa: abrir `frontend/index.html` direto no navegador funciona porque a API tem CORS habilitada — mas o link do Swagger e os endpoints proxy `/api/*` não funcionarão sem o servidor de proxy.

### Testes

```bash
./mvnw verify
```

Executa os testes (JUnit 5) e o gate de cobertura JaCoCo (≥ 80% por instrução).
Relatório HTML em `target/site/jacoco/index.html`.

---

## Endpoints

| Método | Path             | Descrição                                |
| ------ | ---------------- | ---------------------------------------- |
| POST   | `/coupon`        | Cria um cupom (aplica as regras).        |
| GET    | `/coupon`        | Lista todos os cupons (decrescente por id).|
| GET    | `/coupon/{id}`   | Busca um cupom pelo identificador.       |
| DELETE | `/coupon/{id}`   | Realiza o soft delete de um cupom.       |

> `POST` e `DELETE` correspondem às operações definidas no desafio.
> `GET` foi adicionado para alimentar o frontend.

### Códigos de resposta

| Status | Quando                                                       |
| ------ | ------------------------------------------------------------ |
| 201    | Cupom criado (header `Location` aponta para o recurso).      |
| 200    | Listagem / consulta retornadas.                              |
| 204    | Cupom deletado (soft delete) com sucesso.                    |
| 400    | Violação de regra de negócio ou validação de payload.        |
| 404    | Cupom não encontrado.                                        |
| 409    | Tentativa de deletar um cupom já deletado.                   |

Todos os erros seguem o formato:

```json
{
  "timestamp": "2026-05-31T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "discountValue must be greater than or equal to 0.5 (got 0.10)",
  "path": "/coupon",
  "violations": [{ "field": "code", "message": "code is required" }]
}
```

## Regras de negócio implementadas

### Create

- Campos obrigatórios: `code`, `description`, `discountValue`, `expirationDate`.
- `code`: alfanumérico, exatamente 6 caracteres após sanitização. Caracteres especiais são removidos. Entrada bruta limitada a 60 caracteres (`@Size`).
- `description`: obrigatória, máximo 255 caracteres (`@Size`).
- `discountValue`: saldo absoluto, mínimo 0,5, sem máximo.
- `expirationDate`: nunca pode estar no passado.
- `published`: opcional (default `false`). Permite criar o cupom já publicado.

### Delete

- Soft delete (marca `deleted=true` e registra `deletedAt`).
- Cupom já deletado → `409 Conflict`.
- Cupom inexistente → `404 Not Found`.

## Arquitetura

```
src/main/java/com/tenda/coupon/
├── CouponApiApplication.java
├── domain/                  ← regras de negócio puras, sem dependência de Spring/JPA
│   ├── Coupon.java          ← aggregate root, imutável
│   ├── CouponCode.java
│   ├── DiscountValue.java
│   ├── ExpirationDate.java
│   ├── Description.java
│   └── exception/
├── application/             ← casos de uso
│   ├── CouponService.java
│   ├── CreateCouponCommand.java
│   └── port/CouponRepository.java
├── infrastructure/          ← adaptadores de saída
│   ├── persistence/
│   │   ├── CouponEntity.java
│   │   ├── CouponJpaRepository.java
│   │   ├── CouponMapper.java
│   │   └── CouponRepositoryAdapter.java
│   └── config/
│       ├── ClockConfig.java
│       ├── CorsConfig.java
│       └── OpenApiConfig.java
└── api/                     ← adaptadores de entrada (HTTP)
    ├── CouponController.java
    ├── dto/
    └── exception/RestExceptionHandler.java
```

**Domínio ≠ entidade JPA**: `Coupon` (domínio) e `CouponEntity` (JPA) são classes distintas, traduzidas pelo `CouponMapper`.

**Imutabilidade**: `Coupon` é imutável; mudanças de estado retornam uma nova instância.

**Clock injetado**: `Coupon.create` e `Coupon.delete` recebem `Clock`, permitindo testes determinísticos com `Clock.fixed(...)`.

## Testes

```bash
./mvnw verify
```

- `domain/`: unitários puros sem Spring, cobrindo cada Value Object e o agregado.
- `application/`: serviço com Mockito.
- `infrastructure/persistence/`: `@DataJpaTest` em H2.
- `api/`: `@SpringBootTest` + MockMvc cobrindo todos os endpoints e os mapeamentos de erro.

**Resultado atual**: 90 testes, 0 falhas. Cobertura **100% por instrução · 100% por linha · 86% por branch** (gate JaCoCo de 80% por instrução). Classes excluídas: `CouponApiApplication` e o pacote `infrastructure/config` (apenas bean wiring).

Relatório: `target/site/jacoco/index.html`.

## Postman / Insomnia

Importe `postman/Coupon-API.postman_collection.json`. 18 cenários:

- Create — sucesso (sanitização do código), publicado e desconto mínimo (0,5)
- Create — 400: código curto, código longo após sanitização, código bruto > 60, descrição > 255, desconto < 0,5, data passada, campos faltando, body mal-formado
- List e Get by id (200 e 404)
- Delete (204, 409 já deletado, 404 não existe, 400 id inválido)

Todos os requests têm testes embarcados (Postman test scripts) que validam status e shape da resposta.

## Decisões de design

- **Saldo absoluto**: `discountValue` é `BigDecimal` puro, sem `Currency`/`Money`.
- **Soft delete dentro do agregado**: `Coupon.delete()` lança a exceção quando aplicável.
- **Sanitização do código**: tudo que não case `[A-Za-z0-9]` é removido. Acentuação conta como caractere especial.
- **`expirationDate` aceita hoje**: a regra é "nunca no passado", então hoje é válido.
- **Restauração do agregado**: `Coupon.restore(...)` reidrata sem reaplicar validações temporais — um cupom persistido continua acessível mesmo após a data de expiração.
- **Frontend de produção via Nginx**: o container `coupon-web` faz reverse proxy para a API, evitando CORS no navegador. O `CorsConfig` da API existe para permitir testes locais abrindo o HTML direto no browser.
