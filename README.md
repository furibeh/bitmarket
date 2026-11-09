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

   Flyway también crea el esquema `bff` y el rol `bitmarket_bff`, así que el backend debe arrancar antes que el frontend.

3. En otra terminal, crea `frontend/.env` como se explica en [Configurar la sesión](#configurar-la-sesión), migra las tablas de sesión y arranca el frontend:

   ```sh
   cd frontend && pnpm install && pnpm auth:migrate && pnpm dev
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
| `BFF_DB_PASSWORD` | `bitmarket_bff` | Solo el backend |

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

`BFF_DB_PASSWORD` es la contraseña del rol `bitmarket_bff`, y Flyway la escribe literalmente en el SQL, así que no puede contener comillas simples. Solo se aplica la primera vez que corre la migración: para cambiarla después, recrea el volumen.

### Configurar la sesión

Next.js lee estas variables de `frontend/.env`, ignorado por Git. `pnpm auth:migrate` solo lee ese archivo, no `.env.local`. Antes necesitas la [capa permanente de AWS](#capa-permanente-de-aws) aplicada.

| Variable | Valor |
| --- | --- |
| `BFF_DATABASE_URL` | `postgres://bitmarket_bff:<BFF_DB_PASSWORD>@localhost:5432/<DB_NAME>` |
| `BETTER_AUTH_SECRET` | Una cadena aleatoria, por ejemplo la salida de `openssl rand -base64 32` |
| `BETTER_AUTH_URL` | `http://localhost:3000` |
| `COGNITO_CLIENT_ID` | `terraform output -raw cognito_local_client_id` |
| `COGNITO_CLIENT_SECRET` | `terraform output -raw cognito_local_client_secret` |
| `COGNITO_DOMAIN` | `terraform output -raw cognito_domain_url` |
| `COGNITO_REGION` | `us-east-2` |
| `COGNITO_USER_POOL_ID` | `terraform output -raw cognito_user_pool_id` |

`pnpm auth:migrate` crea las tablas de Better Auth en el esquema `bff` y se puede repetir sin problema.

Mercado Pago y los datos semilla se documentarán en la guía de demo.

## Capa permanente de AWS

Crea el User Pool de Cognito, su Lambda *Post Confirmation*, el presupuesto de AWS Budgets, los repositorios ECR, el usuario IAM que publica en ellos y el usuario IAM que despliega la capa efímera. Se aplica en local.

Requisitos: Terraform 1.11 o superior, AWS CLI y credenciales de la cuenta de AWS con permisos para crear estos recursos. Los recursos van en `us-east-2` ([ADR-0016](docs/adr/0016-region-us-east-2-y-llaves-iam-en-ci.md)).

1. Crea el bucket del estado de Terraform. Solo hace falta una vez por cuenta:

   ```sh
   export AWS_REGION=us-east-2
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
   BUCKET="bitmarket-tfstate-$(aws sts get-caller-identity --query Account --output text)"
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

### Llaves para publicar imágenes

El workflow `images` publica en ECR con las llaves del usuario `bitmarket-ci-images`, guardadas en el Environment `aws-images`, que solo admite despliegues desde `main` ([ADR-0016](docs/adr/0016-region-us-east-2-y-llaves-iam-en-ci.md)). Hazlo después de aplicar la capa permanente y antes de fusionar el primer cambio que deba publicarse.

1. Crea el Environment y limítalo a `main`. Solo hace falta una vez:

   ```sh
   gh api -X PUT repos/furibeh/bitmarket/environments/aws-images --input - <<'EOF'
   {"deployment_branch_policy":{"protected_branches":false,"custom_branch_policies":true}}
   EOF
   gh api -X POST repos/furibeh/bitmarket/environments/aws-images/deployment-branch-policies -f name=main -f type=branch
   ```

2. Crea las llaves y guárdalas sin mostrarlas en pantalla:

   ```sh
   read -r KEY_ID SECRET_KEY <<<"$(aws iam create-access-key --user-name bitmarket-ci-images --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text)"
   gh secret set AWS_ACCESS_KEY_ID --env aws-images --body "$KEY_ID"
   gh secret set AWS_SECRET_ACCESS_KEY --env aws-images --body "$SECRET_KEY"
   unset KEY_ID SECRET_KEY
   ```

Para rotarlas, repite el paso 2 y borra la llave anterior con `aws iam delete-access-key --user-name bitmarket-ci-images --access-key-id <id>`. Un usuario IAM admite dos llaves a la vez. Si ya tiene dos, borra primero la que no esté en uso.

## Capa efímera de AWS

Los workflows `env-up` y `env-down` crean y destruyen la red y la base de datos del entorno. Cada `env-up` necesita su `env-down`: todavía no hay nada que destruya el entorno automáticamente, y RDS cobra por hora.

### Preparar el usuario de despliegue

Hazlo una vez, desde la raíz del repositorio, con tus credenciales de administrador.

1. Aplica la capa permanente. El plan debe añadir solo el usuario `bitmarket-ci-deploy`, su política y la asociación entre ambos:

   ```sh
   terraform -chdir=infra/permanent plan
   terraform -chdir=infra/permanent apply
   ```

2. Comprueba sus permisos. `rds:CreateDBInstance` debe salir `allowed`; `iam:CreateRole` e `iam:PutUserPolicy`, `implicitDeny`:

   ```sh
   aws iam simulate-principal-policy \
     --policy-source-arn "$(aws iam get-user --user-name bitmarket-ci-deploy --query User.Arn --output text)" \
     --action-names rds:CreateDBInstance iam:CreateRole iam:PutUserPolicy \
     --context-entries ContextKeyName=aws:RequestedRegion,ContextKeyValues=us-east-2,ContextKeyType=string \
     --query 'EvaluationResults[].[EvalActionName,EvalDecision]' --output table --no-cli-pager
   ```

3. Crea el Environment y limítalo a `main`. El último comando debe mostrar solo `main`:

   ```sh
   gh api -X PUT repos/furibeh/bitmarket/environments/aws-deploy --input - <<'EOF'
   {"deployment_branch_policy":{"protected_branches":false,"custom_branch_policies":true}}
   EOF
   gh api -X POST repos/furibeh/bitmarket/environments/aws-deploy/deployment-branch-policies -f name=main -f type=branch
   gh api repos/furibeh/bitmarket/environments/aws-deploy/deployment-branch-policies --jq '.branch_policies[].name'
   ```

4. Crea las llaves y guárdalas sin mostrarlas en pantalla. Si vas a hacer el ensayo, no cierres esta terminal, porque usa `KEY_ID` y `SECRET_KEY`. Si no, ejecuta después `unset KEY_ID SECRET_KEY`:

   ```sh
   read -r KEY_ID SECRET_KEY <<<"$(aws iam create-access-key --user-name bitmarket-ci-deploy --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text)"
   gh secret set AWS_ACCESS_KEY_ID --env aws-deploy --body "$KEY_ID"
   gh secret set AWS_SECRET_ACCESS_KEY --env aws-deploy --body "$SECRET_KEY"
   gh secret list --env aws-deploy
   ```

Para rotarlas, sigue el procedimiento de `bitmarket-ci-images` con este usuario y este Environment.

### Ensayar en local

La capa efímera se crea y destruye desde CI. La excepción es este ensayo, que comprueba los permisos con las llaves de despliegue antes de abrir un PR que cambie la capa, porque un workflow nuevo no se puede lanzar hasta que está en `main`. Termina siempre con `terraform destroy`.

1. Desde la terminal del paso 4, abre una sola vez un shell con las llaves de despliegue: dentro de él `KEY_ID` y `SECRET_KEY` ya no existen. Espera unos segundos a que la llave nueva esté activa. El último comando debe mostrar `user/bitmarket-ci-deploy`:

   ```sh
   env -u AWS_PROFILE -u AWS_SESSION_TOKEN AWS_ACCESS_KEY_ID="$KEY_ID" AWS_SECRET_ACCESS_KEY="$SECRET_KEY" AWS_REGION=us-east-2 AWS_PAGER= "$SHELL"
   aws sts get-caller-identity --query Arn --output text
   ```

2. Crea el entorno. RDS tarda varios minutos:

   ```sh
   cd infra/ephemeral
   terraform init -backend-config="bucket=bitmarket-tfstate-$(aws sts get-caller-identity --query Account --output text)"
   outputs=$(terraform output -json)
   created_at=$(jq -r '.created_at.value // empty' <<<"$outputs")
   if [ -z "$created_at" ]; then
     created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
   fi
   export TF_VAR_created_at="$created_at"
   echo "$TF_VAR_created_at"
   terraform apply
   ```

3. Comprueba la base de datos: PostgreSQL 17.x, `db.t4g.micro`, `Public` en `false` y un `Secret` en estado `active`. Después, las subredes (`4`) y los NAT Gateway (`0`):

   ```sh
   aws rds describe-db-instances --db-instance-identifier bitmarket --query 'DBInstances[0].{Version:EngineVersion,Class:DBInstanceClass,Public:PubliclyAccessible,Secret:MasterUserSecret.SecretStatus}'
   aws ec2 describe-subnets --filters Name=vpc-id,Values="$(terraform output -raw vpc_id)" --query 'length(Subnets)'
   aws ec2 describe-nat-gateways --filter Name=vpc-id,Values="$(terraform output -raw vpc_id)" --query 'length(NatGateways)'
   ```

4. Comprueba los recursos etiquetados. El primer comando debe contar como mínimo 1 `db`, 1 `internet-gateway`, 2 `route-table`, 1 `security-group`, 1 `subgrp`, 4 `subnet` y 1 `vpc`; puede aparecer además un `secret`. Después, repite la resolución de la fecha: debe mostrar la misma del paso 2, y el `plan` debe responder `No changes`:

   ```sh
   aws resourcegroupstaggingapi get-resources --tag-filters Key=bitmarket:created-at --query 'ResourceTagMappingList[].ResourceARN' --output text | tr '\t' '\n' | sed -E 's/^arn:aws:[^:]+:[^:]*:[^:]*:([^:/]+).*/\1/' | sort | uniq -c
   outputs=$(terraform output -json)
   created_at=$(jq -r '.created_at.value // empty' <<<"$outputs")
   if [ -z "$created_at" ]; then
     created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
   fi
   export TF_VAR_created_at="$created_at"
   echo "$TF_VAR_created_at"
   terraform plan
   ```

5. Destruye el entorno:

   ```sh
   terraform destroy
   exit
   ```

6. De vuelta en tu terminal, con tus credenciales, borra las variables y comprueba que no quedó ningún snapshot (`0`):

   ```sh
   unset KEY_ID SECRET_KEY
   aws rds describe-db-snapshots --db-instance-identifier bitmarket --region us-east-2 --query 'length(DBSnapshots)' --no-cli-pager
   ```

Si un permiso falla, añádelo a `infra/permanent/iam_ci_deploy.tf`, aplica la capa permanente y repite el paso que falló.

### Levantar y destruir el entorno

Los workflows solo se pueden lanzar cuando ya están en `main`:

```sh
gh workflow run env-up --ref main
gh workflow run env-down --ref main
gh run list --workflow env-up --limit 1
```

Si un workflow se interrumpe (tiempo agotado o cancelación manual), el estado puede quedar bloqueado y el siguiente `env-up` o `env-down` fallará con `Error acquiring the state lock`. Comprueba que no haya otra ejecución en curso y libera el bloqueo con el `ID` que muestra ese error, desde `infra/ephemeral` y con el backend inicializado como en el ensayo:

```sh
terraform force-unlock <ID>
```

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
cd infra/ephemeral && terraform fmt -check -recursive
cd infra/ephemeral && terraform init -backend=false && terraform validate
cd infra/permanent/lambda/post-confirmation && node --test
```

El resto de comandos (formato, ejecutar un solo test) está en [`AGENTS.md`](AGENTS.md#comandos). Los tests E2E con Playwright todavía no existen.
