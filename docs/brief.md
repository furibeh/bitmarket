# BitMarket — Brief

> Tienda en línea colombiana de tecnología y equipos de cómputo.
> El detalle técnico vive en `docs/design.md` y `docs/architecture.md`.

## 1. Problema

Un comercio de tecnología en Colombia necesita vender en línea productos que se ofrecen en varias versiones (capacidad, color, configuración), cada una con su propio precio y stock, y cobrar con medios de pago locales. Al vender las últimas unidades de productos muy demandados, varios compradores pueden pagar a la vez la misma unidad; vender lo que no se tiene obliga a reembolsar y daña la confianza. Además, quien administra la tienda necesita respuestas rápidas sobre stock y ventas sin construir reportes a mano.

**Propósito:** proyecto de portafolio y aprendizaje con datos ficticios, preparado para producción (secretos fuera del código, datos personales mínimos) sin asumir ahora sus cargas legales y operativas.

## 2. Usuarios

| Usuario | Qué necesita |
| --- | --- |
| Comprador | Encontrar un producto, elegir su variante, pagar con tarjeta y saber el estado de su pedido. |
| Administrador | Mantener catálogo e inventario, seguir pedidos y consultar stock y ventas, también desde un asistente de IA vía MCP. |

## 3. Alcance del MVP

1. **Catálogo con variantes:** productos con variantes (SKU, atributos clave-valor, precio y stock por variante).
2. **Carrito y checkout:** compra de una o varias variantes con pago con tarjeta por Mercado Pago (Checkout API, sandbox).
3. **Pedidos y pagos con estados:** máquina de estados del pedido y del pago, actualizada por la respuesta de Mercado Pago, por sus webhooks y por reconciliación.
4. **Inventario sin sobreventa:** el stock nunca se vende por encima de lo disponible, incluso con compras concurrentes.
5. **Servidor MCP:** consultas de pedidos, inventario y ventas (p. ej. "¿qué productos están por agotarse?", "¿cuánto se vendió esta semana por categoría?") y una acción administrativa acotada con permisos y confirmación.
6. **Calidad y entrega:** tests de integración con Testcontainers, CI y despliegue en AWS.

- **Cuentas de comprador:** registro obligatorio (Amazon Cognito), con historial de pedidos en "Mis pedidos".
- **Administración:** panel web de administración en Next.js (productos, variantes, stock, pedidos, estado de envío y alertas de cobro).
- **Alcance del MCP:** solo para el administrador, por HTTP remoto con OAuth (Cognito); la acción administrativa del MVP es ajustar el stock de una variante, con confirmación en dos pasos.
- **Envío:** tarifa fija nacional; el pedido separa subtotal, envío y total.

## 4. Fuera de alcance

- Medios de pago distintos a tarjeta (PSE, Nequi, efectivo) y Checkout Pro.
- Reembolsos y contracargos (`refunded`, `charged_back`): se registran, no se gestionan.
- Facturación electrónica DIAN, integración con transportadoras, devoluciones, multimoneda.
- Buscador facetado por atributos de variante.
- Broker de mensajería externo (Kafka, RabbitMQ): los eventos entre módulos son en proceso.
- Infraestructura AWS de alta disponibilidad (multi-AZ, autoescalado) y entorno encendido de forma permanente.
- NAT Gateway, Redis y correos transaccionales (el comprador ve el estado en "Mis pedidos").

## 5. Objetivos y cómo se demuestran

| Objetivo | Entregable verificable que lo demuestra |
| --- | --- |
| Evitar sobreventa | Test de integración: N compradores concurrentes y stock 1 → exactamente 1 pedido confirmado, N−1 rechazados, stock nunca negativo. |
| Webhooks seguros e idempotentes | Tests: firma `x-signature` inválida → 4xx sin efectos; misma notificación 2 veces → una sola transición; el estado se toma de `GET /v1/orders/{id}`, no del cuerpo. |
| Cobro sin duplicados | Test: crear el pago dos veces con la misma `X-Idempotency-Key` → un solo pago. |
| Estados del pedido y del pago coherentes | Diagrama de estados en `design.md` y tests que rechazan transiciones inválidas. |
| Módulos desacoplados | `ApplicationModules.verify()` en CI falla si un módulo usa internos de otro. |
| CI que protege `main` | Un PR con un test roto no puede fusionarse. |
| Desplegado en AWS | CI publica las imágenes en ECR; `terraform apply` levanta el entorno efímero, la URL de CloudFront permite una compra sandbox de punta a punta con cuentas de prueba de Mercado Pago, y `terraform destroy` lo elimina sin dejar recursos con costo. |
| MCP útil | Demo con un cliente MCP que responde las dos preguntas de ejemplo con datos reales y ejecuta la acción administrativa solo tras confirmación. |

## 6. Stack

| Capa | Elección | Por qué |
| --- | --- | --- |
| Backend | Java 25 + Spring Boot 4.1 | Ecosistema maduro para transacciones, seguridad y testing. |
| Modularidad | Spring Modulith | Límites entre módulos verificables en tests y eventos de dominio sin desplegar microservicios. |
| Persistencia | PostgreSQL 17 + Spring Data JPA + Flyway | Transacciones y bloqueos fiables para el inventario; soportado por Testcontainers. |
| Frontend | Next.js (BFF) + Better Auth | SEO del catálogo (renderizado en servidor); el token de acceso nunca llega al navegador; experiencia/empleabilidad. |
| Pagos | Mercado Pago, Checkout API vía Orders API (sandbox) | Formulario dentro de la tienda; la tarjeta se tokeniza en el navegador (MercadoPago.js / Card Payment Brick), el backend solo ve el token. |
| Identidad | Amazon Cognito (plan Essentials, Managed Login) | Delega la autenticación, la parte de mayor riesgo; un solo proveedor para la web, la API y el MCP. |
| Tests | JUnit 6, Testcontainers, WireMock, Vitest, Playwright | Integración contra PostgreSQL real, no simulada; Mercado Pago simulado. |
| CI | GitHub Actions | Integrado con el repositorio y los PRs. |
| Despliegue | AWS con Terraform: capa permanente (Cognito y su Lambda de registro, ECR, S3, CloudFront, SSM, Budgets) y capa efímera (VPC, ECS Fargate, ALB interno como VPC origin de CloudFront, RDS), región `us-east-1` | Aprender y demostrar AWS pagando solo por horas encendidas; CloudFront da HTTPS y URLs estables sin dominio. |
| IA | Servidor MCP (Spring AI 2.0) | Expone tools tipadas; el lenguaje natural lo pone el cliente LLM. |

## 7. Reglas de negocio ya decididas

- No se vende stock que no existe, ni siquiera con compras simultáneas.
- Precios en COP, enteros, con IVA incluido.
- Precio y stock pertenecen a la variante, no al producto.
- El estado final del pago lo determina Mercado Pago (respuesta síncrona, webhook verificado seguido de consulta a su API, o reconciliación); nunca el cliente.
- Estados de pago del MVP (orden de Mercado Pago): `processed` (aprobado), `failed`/`canceled` (rechazado), `created`/`processing`/`action_required` (pendiente); `refunded` y `charged_back` solo se registran.
- Toda acción administrativa por MCP requiere permiso y confirmación explícita.
- **Reserva de stock:** se reserva al crear el pedido y expira a los 15 minutos, salvo que haya un intento de pago en curso; así, todo pago aprobado encuentra su stock reservado. Un cobro inesperado (un intento ya rechazado que Mercado Pago reporta cobrado) genera una alerta para reembolso manual.

## 8. Riesgos

| Riesgo | Mitigación |
| --- | --- |
| Webhook perdido, duplicado o fuera de orden | Procesamiento idempotente, consulta del estado real a la API y reconciliación de intentos `PENDING` antiguos y de `CREATED` huérfanos. |
| Pagos pendientes (`processing`/`action_required`: 3DS, antifraude) que tardan en resolverse | La máquina de estados acepta llegada por respuesta síncrona, webhook o reconciliación; la reserva no expira mientras haya un intento en curso (ADR-0002). |
| Sobreventa por condiciones de carrera | Decremento atómico condicionado en base de datos y test de concurrencia en CI. |
| Costo de AWS por olvidar el entorno encendido | Entorno efímero que se destruye solo al pasar de 4 horas (como máximo ~5 h), sin NAT Gateway y alertas de AWS Budgets. |
| MCP ejecuta acciones no deseadas | Permisos, confirmación explícita y una sola acción de escritura en el MVP. |
| Diferencias entre sandbox y producción de Mercado Pago | Uso de cuentas de prueba oficiales; aislar la integración en el módulo de pagos. |

## 9. Criterios de éxito

- Con el entorno levantado, un comprador elige una variante, paga con tarjeta de prueba en la URL pública y ve su pedido confirmado.
- El test de concurrencia de inventario y los tests de webhooks e idempotencia pasan en CI.
- Un PR con tests rotos no se fusiona, y el entorno AWS se crea y destruye completo con Terraform.
- Un cliente MCP responde las dos preguntas de ejemplo y ejecuta la acción administrativa con confirmación.

<!-- brief revisado: 2026-10-28 -->
