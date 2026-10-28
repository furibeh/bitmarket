# BitMarket

Tienda en línea colombiana de tecnología y equipos de cómputo, con productos en varias variantes (capacidad, color, configuración), cada una con su propio precio y stock, y pago con tarjeta por Mercado Pago. Su foco es no vender nunca stock que no existe, ni siquiera con compras simultáneas. El administrador puede consultar stock y ventas desde un asistente de IA vía MCP.

Es un proyecto de portafolio y aprendizaje con datos ficticios, pensado para estar preparado para producción.

**Estado: 🟡 En desarrollo.** Existe el esqueleto del backend (Spring Boot + Flyway) y del frontend (Next.js); las funcionalidades del MVP siguen planeadas. La documentación base está en [`docs/`](docs/).

## Funcionalidades

Todas están **planeadas** para el MVP:

- [ ] Catálogo con categorías, búsqueda por nombre y productos con variantes (SKU, atributos, precio y stock por variante).
- [ ] Registro obligatorio de compradores y "Mis pedidos" con el estado de cada pedido.
- [ ] Carrito y checkout con pago con tarjeta por Mercado Pago (Checkout API, sandbox).
- [ ] Reserva de stock al crear el pedido, sin sobreventa ante compras concurrentes.
- [ ] Estados de pedido y pago actualizados por la respuesta de Mercado Pago, por webhooks verificados y por reconciliación.
- [ ] Panel web de administración: productos, variantes, stock, pedidos y estado de envío.
- [ ] Servidor MCP para el administrador: consultas de pedidos, inventario y ventas, y ajuste de stock con confirmación en dos pasos.
- [ ] Tests de integración con Testcontainers, CI en GitHub Actions y despliegue efímero en AWS.

## Stack

| Capa | Elección | Por qué |
| --- | --- | --- |
| Backend | Java 25 + Spring Boot 4.1 (Maven) | Ecosistema maduro para transacciones, seguridad y testing. |
| Modularidad | Spring Modulith | Los límites entre módulos se verifican en tests, y hay eventos de dominio sin necesidad de microservicios. |
| Persistencia | PostgreSQL 17 + Spring Data JPA + Flyway | Transacciones y bloqueos fiables para el inventario; soportado por Testcontainers. |
| Frontend | Next.js (BFF) + Better Auth | Renderizado en servidor para el SEO del catálogo; el token de acceso nunca llega al navegador. |
| Pagos | Mercado Pago Checkout API vía Orders API (sandbox) | El formulario queda dentro de la tienda; la tarjeta se tokeniza en el navegador y el backend solo ve el token. |
| Identidad | Amazon Cognito | Delega la autenticación, que es la parte de mayor riesgo; un solo proveedor para la web, la API y el MCP. |
| IA | Servidor MCP (Spring AI 2.0) | Expone tools tipadas; el lenguaje natural lo pone el cliente LLM. |
| Tests | JUnit 6, Testcontainers, WireMock, Vitest, Playwright | Integración contra PostgreSQL real; Mercado Pago simulado de forma determinista. |
| CI | GitHub Actions | Integrado con el repositorio y los PR; protege `main`. |
| Infraestructura | AWS con Terraform (ECS Fargate, RDS, CloudFront) en `us-east-1` | Aprender y demostrar AWS pagando solo las horas que el entorno está encendido. |

## Documentación

- [`docs/brief.md`](docs/brief.md): problema, alcance, objetivos y riesgos.
- [`docs/design.md`](docs/design.md): casos de uso, reglas de negocio (RN-xx), diagramas de estados, casos de borde y glosario.
- [`docs/architecture.md`](docs/architecture.md): módulos, contratos, modelo de datos, integraciones, seguridad y despliegue.
- [`docs/adr/`](docs/adr/): registros de decisiones de arquitectura.
- [`AGENTS.md`](AGENTS.md): reglas de trabajo para agentes de código.

## Cómo ejecutar

Requisitos: Java 25, Node 24 (ver `frontend/.nvmrc`), pnpm 12.11.2 (la versión de `packageManager` en `frontend/package.json`) y Docker con Compose v2.

1. Levanta PostgreSQL 17 desde la raíz del repositorio:

   ```sh
   docker compose up -d
   ```

   Escucha en `localhost:5432` con base, usuario y contraseña `bitmarket`. Si ya tienes otro PostgreSQL en el puerto 5432, detenlo antes.

2. Arranca el backend; Flyway aplica las migraciones al iniciar:

   ```sh
   cd backend && ./mvnw spring-boot:run
   ```

3. En otra terminal, arranca el frontend:

   ```sh
   cd frontend && pnpm install && pnpm dev
   ```

4. Abre `http://localhost:3000`.

Cognito, Mercado Pago y los datos semilla se documentarán en la guía de demo.

## Cómo probar

Cada línea se ejecuta desde la raíz del repositorio.

Backend:

```sh
cd backend && ./mvnw test     # unitarios (*Test), sin Docker
cd backend && ./mvnw verify   # *Test + *IT con Testcontainers, ApplicationModules.verify() y spotless:check; requiere Docker
```

Frontend:

```sh
cd frontend && pnpm lint
cd frontend && pnpm format:check
cd frontend && pnpm typecheck
cd frontend && pnpm test
```

El resto de comandos (formato, ejecutar un solo test) está en [`AGENTS.md`](AGENTS.md#comandos). Los tests E2E con Playwright todavía no existen.
