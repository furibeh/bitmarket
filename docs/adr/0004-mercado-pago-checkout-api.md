# ADR-0004: Mercado Pago Checkout API vía Orders API con tokenización en el navegador
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-29

## Contexto
La tienda cobra en Colombia con tarjeta (único medio del MVP), en sandbox. Queremos el formulario de pago dentro de la tienda para controlar el flujo (reintentos con otra tarjeta durante la reserva) y a la vez minimizar la superficie PCI: el backend no debe ver datos de tarjeta. Mercado Pago ofrece Checkout API sobre dos APIs: Payments (`/v1/payments`), que su referencia marca como *legacy*, y Orders (`/v1/orders`), recomendada para integraciones nuevas y disponible en Colombia.

## Decisión
Usamos Mercado Pago Checkout API vía Orders API: el navegador tokeniza la tarjeta con MercadoPago.js / Card Payment Brick y el backend crea una orden por intento con `POST /v1/orders` (`processing_mode = automatic`) y una `X-Idempotency-Key` por intento.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Checkout API vía Orders API + Card Payment Brick** (elegida) | Formulario en la tienda; control de reintentos e intentos por pedido; el backend solo ve el token; idempotencia explícita. | Más integración (crear pago, estados, webhooks); 3DS y estados intermedios a manejar en la UI. |
| Checkout API vía Payments API (`/v1/payments`) | Más documentación y ejemplos; modelo de estados conocido (`approved`, `pending`…). | Marcada como *legacy*; las novedades (búsqueda de órdenes, nuevos medios) llegan a Orders; obligaría a migrar después. |
| Checkout Pro (redirección) | Integración mínima; Mercado Pago gestiona todo el formulario y medios. | El comprador sale de la tienda; menos control sobre intentos y reintentos; explícitamente fuera de alcance en el brief. |
| Checkout API con datos de tarjeta enviados al backend | Control total. | Alcance PCI DSS completo; inaceptable incluso para un proyecto "preparado para producción". |

## Consecuencias
- **Positivas:** superficie PCI baja (SAQ A-EP como referencia); flujo de reintento propio (ADR-0006).
- **Negativas:** el backend gestiona estados intermedios de la orden (`created`, `processing`, `action_required`) y webhooks (ADR-0005); menos ejemplos que Payments; dependencia del SDK de Mercado Pago en el frontend.
- **Verificación:** test que crea la orden dos veces con la misma `X-Idempotency-Key` y obtiene una sola orden; revisión de que ningún endpoint ni log recibe PAN/CVV (solo `token`).
