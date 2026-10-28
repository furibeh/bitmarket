# BitMarket — Reglas para agentes
Tienda en línea colombiana de tecnología: variantes con precio y stock propios, pago con tarjeta por Mercado Pago, sin sobreventa, y un MCP para el administrador. Brief: [docs/brief.md](docs/brief.md) · Qué: [docs/design.md](docs/design.md) · Cómo: [docs/architecture.md](docs/architecture.md) · Decisiones: [docs/adr/](docs/adr/)

## Forma de trabajo
- El backlog vive en GitHub Issues. Una tarea por sesión: `gh issue view <n> --comments`.
- Flujo por issue: plan (esperar aprobación) → tests en rojo (commit solo de tests, esperar aprobación) → implementación → autorrevisión → PR.
- Durante la implementación no modifiques tests. Si uno parece incorrecto, detente y explica.
- No toques lo listado en "Fuera de alcance" del issue. Bugs o mejoras ajenas: propón un issue.
- Si el plan supera ~400 líneas de producción, propón partir el issue.
- Commits pequeños con Conventional Commits.
- Nunca hagas push ni merge. Nunca hagas commit de secretos.
- El workflow `ci` (`.github/workflows/ci.yml`, jobs `backend` y `frontend`) es el *required check* de `main`: un PR solo se fusiona con ambos en verde.
- Si algo no está decidido en docs/, pregunta; no decidas en silencio.
- Cita las reglas de negocio por su ID (`RN-xx`) y los casos de borde (`B-xx`) de `docs/design.md` en planes, tests y PR.
- Una decisión de arquitectura nueva con alternativas reales requiere un ADR en `docs/adr/` (siguiente número, plantilla de los existentes).

## Comandos
Los marcados TODO todavía no existen.

| Acción | Comando |
| --- | --- |
| Instalar backend | `cd backend && ./mvnw -q dependency:go-offline` |
| Instalar frontend | `cd frontend && pnpm install` |
| Base de datos local | `docker compose up -d` |
| Levantar backend | `cd backend && ./mvnw spring-boot:run` |
| Levantar frontend | `cd frontend && pnpm dev` |
| Tests unitarios backend | `cd backend && ./mvnw test` (solo `*Test`, sin Docker) |
| Todos los tests backend | `cd backend && ./mvnw verify` (`*Test` + `*IT` con Testcontainers, `ApplicationModules.verify()` y `spotless:check`) |
| Un test unitario | `cd backend && ./mvnw test -Dtest=NombreTest#metodo` |
| Un test de integración | `cd backend && ./mvnw verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=NombreIT` |
| Tests frontend | `cd frontend && pnpm test` (Vitest) |
| Un test frontend | `cd frontend && pnpm vitest run ruta/al/archivo` |
| E2E | TODO `cd frontend && pnpm playwright test` (requiere Docker Compose) |
| Lint | frontend `cd frontend && pnpm lint` (ESLint, config de Next); backend sin linter aparte |
| Formato | backend `cd backend && ./mvnw spotless:apply` (google-java-format); frontend `cd frontend && pnpm format` (Prettier) |
| Comprobar formato | backend `cd backend && ./mvnw spotless:check`; frontend `cd frontend && pnpm format:check` (ambos en CI) |
| Typecheck frontend | `cd frontend && pnpm typecheck` (`next typegen` + `tsc --noEmit`) |
| Migraciones | Flyway al arrancar el backend; nuevas en `backend/src/main/resources/db/migration/` |
| Tipos API frontend | TODO regenerar desde OpenAPI con `openapi-typescript` |

## Arquitectura
1. Backend = monolito modular Spring Modulith con módulos `customers`, `catalog`, `inventory`, `cart`, `orders`, `payments`, `mcp` bajo `com.bitmarket`. API pública = tipos del paquete raíz del módulo; todo en `internal/` es privado. `ApplicationModules.verify()` debe pasar.
2. Dependencias permitidas (sin ciclos): `cart → customers, catalog, inventory`; `orders → customers, cart, catalog, inventory`; `payments → orders`; `mcp → orders, inventory, catalog`; `inventory` solo escucha `VariantCreated` de `catalog`. Cualquier otra exige actualizar `docs/architecture.md`.
3. Cada tabla pertenece a un solo módulo y solo él la lee o escribe. Entre módulos se referencia por ID (`sku`, `orderId`, `customerId`): sin FK ni relaciones JPA cruzadas.
4. Lo que debe ser atómico con el stock o el estado del pedido va por llamada síncrona en la misma transacción. Los eventos (outbox de Modulith) solo para reacciones eventuales, con listeners idempotentes; todo evento nuevo se añade a la tabla de `architecture.md` §5.2.
5. El stock solo cambia con `UPDATE` condicionados (`available >= :qty`, `available + :delta >= 0`) comprobando filas afectadas (ADR-0003). Prohibido leer-y-luego-escribir el stock.
6. Las transiciones de estado se escriben como `UPDATE ... WHERE status IN (orígenes válidos)` comprobando filas afectadas; nunca se sobrescribe un estado sin validar el origen.
7. El estado de un pago solo lo fija Mercado Pago (respuesta síncrona de `POST /v1/orders` o `GET /v1/orders/{id}` tras webhook con `x-signature` válida). Nunca el cuerpo del webhook ni el cliente.
8. Los tipos y el cliente HTTP de Mercado Pago viven solo en `payments/internal`.
9. El navegador solo habla con Next.js (BFF). El backend expone públicamente solo `/webhooks/*`, `/mcp` y `/.well-known/oauth-protected-resource/mcp` (metadatos del MCP, RFC 9728). Ningún componente cliente (`"use client"`) usa el access token ni el cliente de la API del backend.
10. Montos en COP como enteros (`long`/`bigint`); nunca `double`, `float` ni decimales.

## Tests
- **Backend:** JUnit 6 (Jupiter). Unitarios para reglas puras de dominio. Integración con **Testcontainers (PostgreSQL 17 real, la misma versión que RDS)** para repositorios, consultas nativas, concurrencia, jobs y flujos entre módulos; nunca simular la base de datos en esos casos.
- **Mercado Pago:** WireMock en integración y en E2E; nunca llamar a la API real desde tests.
- **Seguridad:** tests con JWT de prueba para 401/403/404 por rol y propiedad.
- **Modularidad:** test con `ApplicationModules.verify()`.
- **Obligatorios por su riesgo:** concurrencia de reserva (N hilos, stock 1), webhook (firma inválida, duplicado, estado desde la API), idempotencia del cobro y transiciones inválidas de pedido e intento.
- **Frontend:** Vitest para lógica y componentes; Playwright para E2E de humo sobre Docker Compose.
- **Nombres:** unitarios `*Test` (Surefire, sin Docker); integración `*IT` (Failsafe, Testcontainers/WireMock). Un test que use Docker nunca se llama `*Test`.
- Cada test que cubre una regla cita su ID en `@DisplayName` (p. ej. `RN-31: rechaza reserva sin stock suficiente`).

## Convenciones
- Errores HTTP en `application/problem+json`.
- `spring.jpa.open-in-view=false`.
- Logs JSON con `X-Request-Id`; nunca registrar token de tarjeta, dirección, teléfono ni correo.
- Secretos solo por variables de entorno (SSM en AWS, `.env` ignorado en local).
- Idioma: el código en inglés (identificadores, nombres de tests, comentarios, javadoc, nombres de migraciones); los `@DisplayName` de los tests en español citando `RN-xx`/`B-xx`; commits, issues, PR y `docs/` en español.
