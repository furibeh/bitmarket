# BitMarket

Tienda en línea colombiana de tecnología y equipos de cómputo, con productos en varias variantes (capacidad, color, configuración), cada una con su propio precio y stock, y pago con tarjeta por Mercado Pago. Su foco es no vender nunca stock que no existe, ni siquiera con compras simultáneas. El administrador puede consultar stock y ventas desde un asistente de IA vía MCP.

Es un proyecto de portafolio y aprendizaje con datos ficticios, pensado para estar preparado para producción.

**Estado: 🟡 En desarrollo.** Existen el esqueleto del backend (Spring Boot + Flyway) y del frontend (Next.js) y el CI en GitHub Actions; el resto de funcionalidades del MVP sigue planeado. La documentación base está en [`docs/`](docs/).

## Funcionalidades

Las marcadas están hechas; el resto está **planeado** para el MVP:

- [ ] Catálogo con categorías, búsqueda por nombre y productos con variantes (SKU, atributos, precio y stock por variante).
- [ ] Registro obligatorio de compradores y "Mis pedidos" con el estado de cada pedido.
- [ ] Carrito y checkout con pago con tarjeta por Mercado Pago (Checkout API, sandbox).
- [ ] Reserva de stock al crear el pedido, sin sobreventa ante compras concurrentes.
- [ ] Estados de pedido y pago actualizados por la respuesta de Mercado Pago, por webhooks verificados y por reconciliación.
- [ ] Panel web de administración: productos, variantes, stock, pedidos y estado de envío.
- [ ] Servidor MCP para el administrador: consultas de pedidos, inventario y ventas, y ajuste de stock con confirmación en dos pasos.
- [x] CI en GitHub Actions: el workflow `ci` ejecuta los tests y las comprobaciones del backend, del frontend y de la infraestructura en cada PR y push a `main`, y es *required check* para fusionar en `main` (el E2E de humo con Playwright llegará con el MVP).
- [ ] Tests de integración con Testcontainers y despliegue efímero en AWS.

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
| Infraestructura | AWS con Terraform (ECS Fargate, RDS, CloudFront) en `us-east-2` | Aprender y demostrar AWS pagando solo las horas que el entorno está encendido. |

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

   Escucha en `localhost:5432` con base, usuario y contraseña `bitmarket`; para usar otros valores, ver [Configurar la base de datos local](#configurar-la-base-de-datos-local). Si ya tienes otro PostgreSQL en el puerto 5432, detenlo antes.

2. Arranca el backend; Flyway aplica las migraciones al iniciar:

   ```sh
   cd backend && ./mvnw spring-boot:run
   ```

3. En otra terminal, arranca el frontend:

   ```sh
   cd frontend && pnpm install && pnpm dev
   ```

4. Abre `http://localhost:3000`.

### Configurar la base de datos local

Compose y el backend leen estas variables de entorno:

| Variable | Valor por defecto | La leen |
| --- | --- | --- |
| `DB_NAME` | `bitmarket` | Compose y backend |
| `DB_USER` | `bitmarket` | Compose y backend |
| `DB_PASSWORD` | `bitmarket` | Compose y backend |
| `DB_URL` | `jdbc:postgresql://localhost:5432/${DB_NAME}` | Solo el backend |

`DB_URL` toma `DB_NAME`, así que solo hace falta definirla para conectar el backend a otro PostgreSQL (otro host o puerto); el contenedor de `compose.yaml` publica siempre el puerto 5432.

Para usar otros valores, expórtalos **en la terminal de `docker compose` y en la del backend**:

```sh
export DB_NAME=mi_base DB_USER=mi_usuario DB_PASSWORD=mi_contraseña
```

Un archivo `.env` en la raíz lo lee Compose, pero no el backend: si defines las variables solo ahí, el backend se conecta con los valores por defecto y falla la autenticación.

La imagen de PostgreSQL solo aplica `DB_NAME`, `DB_USER` y `DB_PASSWORD` al inicializar un volumen vacío. Si ya levantaste la base con otros valores, recrea el volumen; **esto borra los datos locales**:

```sh
docker compose down -v && docker compose up -d
```

Mercado Pago y los datos semilla se documentarán en la guía de demo.

## Capa permanente de AWS

Crea el User Pool de Cognito, su Lambda *Post Confirmation* y el presupuesto de AWS Budgets. Se aplica en local.

Requisitos: Terraform 1.10 o superior, AWS CLI y credenciales de la cuenta de AWS con permisos para crear estos recursos. Los recursos van en `us-east-2` ([ADR-0016](docs/adr/0016-region-us-east-2-y-llaves-iam-en-ci.md)).

1. Crea el bucket del estado de Terraform. Solo hace falta una vez por cuenta:

   ```sh
   BUCKET="bitmarket-tfstate-$(aws sts get-caller-identity --query Account --output text)"
   aws s3api create-bucket --bucket "$BUCKET" --region us-east-2 --create-bucket-configuration LocationConstraint=us-east-2
   aws s3api put-bucket-versioning --bucket "$BUCKET" --versioning-configuration Status=Enabled
   aws s3api put-bucket-encryption --bucket "$BUCKET" --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
   aws s3api put-public-access-block --bucket "$BUCKET" --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

2. Define las variables. `terraform.tfvars` está ignorado por Git:

   ```sh
   cd infra/permanent
   cp terraform.tfvars.example terraform.tfvars
   ```

   Pon en `alert_email` el correo que recibirá las alertas de gasto, y en `cognito_domain_prefix` un prefijo que esté libre en la región.

3. Inicializa y aplica:

   ```sh
   terraform init -backend-config="bucket=$BUCKET"
   terraform apply
   ```

4. Consulta los datos que necesita la tienda:

   ```sh
   terraform output
   terraform output -raw cognito_local_client_secret
   ```

El User Pool tiene la protección contra borrado activa: `terraform destroy` falla mientras no la desactives.

### Asignar un grupo a mano

El grupo `ADMIN` se asigna a mano. Desde `infra/permanent`:

```sh
aws cognito-idp admin-add-user-to-group \
  --region us-east-2 \
  --user-pool-id "$(terraform output -raw cognito_user_pool_id)" \
  --username correo@example.com \
  --group-name ADMIN
```

El grupo aparece en el token a partir del siguiente inicio de sesión. El mismo comando con `--group-name CUSTOMER` sirve para un usuario que haya quedado sin ese grupo: uno creado con `admin-create-user`, o uno cuyo registro falló en la Lambda *Post Confirmation*. En ese caso queda confirmado y sin grupo, y repetir el código no lo arregla.

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

Infraestructura:

```sh
cd infra/permanent && terraform fmt -check -recursive
cd infra/permanent && terraform init -backend=false && terraform validate
cd infra/permanent/lambda/post-confirmation && node --test
```

El resto de comandos (formato, ejecutar un solo test) está en [`AGENTS.md`](AGENTS.md#comandos). Los tests E2E con Playwright todavía no existen.
