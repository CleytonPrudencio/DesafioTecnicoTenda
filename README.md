# Coupon API — Desafio Técnico Tenda

API REST de gerenciamento de cupons em Java 21 + Spring Boot 3.5 com arquitetura em camadas (Domínio · Aplicação · Infraestrutura · API), regras de negócio encapsuladas em objetos de domínio puros, banco H2 em memória, documentação Swagger, **frontend web estilizado** e tudo orquestrado por Docker Compose.

## Stack

- Java 21 · Spring Boot 3.5 (Web · Data JPA · Validation · Actuator)
- H2 (banco em memória)
- SpringDoc OpenAPI · Swagger UI
- JUnit 5 · Mockito · AssertJ
- JaCoCo (gate de cobertura **≥ 80%**)
- Docker · Docker Compose
- Frontend: HTML + Tailwind CSS + JavaScript vanilla servido por Nginx (com reverse proxy)

## Subindo o ambiente completo

```bash
docker compose up --build
```

- API:        http://localhost:8080
- Frontend:   http://localhost:3000
- Swagger UI: http://localhost:8080/swagger-ui.html (ou http://localhost:3000/swagger via proxy)
- H2 Console: http://localhost:8080/h2-console

O frontend faz as chamadas via `/api/*` que o Nginx encaminha para a API, evitando CORS.

## Rodando localmente (sem Docker)

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O frontend (em `frontend/index.html`) pode ser aberto direto no navegador — a CORS já está configurada na API.

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
- `code`: alfanumérico, exatamente 6 caracteres após sanitização. Caracteres especiais são removidos.
- `discountValue`: saldo absoluto, mínimo 0,5, sem máximo.
- `expirationDate`: nunca pode estar no passado.
- `published`: opcional (default `false`). Permite criar o cupom já publicado.

### Delete

- Soft delete (marca `deleted=true` e registra `deletedAt`).
- Cupom já deletado → `409 Conflict`.
- Cupom inexistente → `404 Not Found`.

## Postman / Insomnia

Importe a collection em `postman/Coupon-API.postman_collection.json`. Contém os cenários:

- Create — sucesso (com sanitização do código)
- Create — publicado
- Create — 400 código inválido
- Create — 400 desconto abaixo do mínimo
- Create — 400 data no passado
- Create — 400 campos obrigatórios faltando
- List
- Get by id (sucesso e 404)
- Delete (sucesso, 409 quando já deletado, 404 quando não existe)

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

JaCoCo configurado em `pom.xml` falha o build se a cobertura por instrução cair abaixo de **80%**. Classes excluídas: `CouponApiApplication` e o pacote `infrastructure/config` (apenas bean wiring).

Relatório: `target/site/jacoco/index.html`.

## Frontend

Página única em `frontend/` (HTML + Tailwind via CDN + JS vanilla), com:

- Formulário de criação com validação client-side e preview da sanitização do código.
- Listagem em tempo real com busca por código/descrição.
- Modal de confirmação para deletar.
- Toasts de sucesso / aviso / erro.
- Badges visuais para publicado/rascunho/deletado.
- Indicador de status da API.

Em produção, é servido pelo Nginx do `coupon-web` na porta 3000; o Nginx atua como reverse proxy para `/api/*`.

## Decisões de design

- **Saldo absoluto**: `discountValue` é `BigDecimal` puro, sem `Currency`/`Money`.
- **Soft delete dentro do agregado**: `Coupon.delete()` lança a exceção quando aplicável.
- **Sanitização do código**: tudo que não case `[A-Za-z0-9]` é removido. Acentuação conta como caractere especial.
- **`expirationDate` aceita hoje**: a regra é "nunca no passado", então hoje é válido.
- **Restauração do agregado**: `Coupon.restore(...)` reidrata sem reaplicar validações temporais — um cupom persistido continua acessível mesmo após a data de expiração.
