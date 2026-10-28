# ADR-0006: Un pedido tiene N intentos de pago
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-28

## Contexto
Un rechazo de tarjeta es frecuente (fondos, antifraude, datos erróneos). Si cada rechazo obligara a rehacer el carrito y el pedido, se liberaría y volvería a reservar stock, con riesgo de perder la unidad. Además hay que evitar dos cobros aprobados para el mismo pedido.

## Decisión
Modelamos el pago como N intentos por pedido, cada uno con su clave de idempotencia; solo se crea un intento si el pedido está `PENDING_PAYMENT`, con reserva vigente y sin otro intento en curso (`CREATED`/`PENDING`), así que solo un intento puede llevar el pedido a `PAID`.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **N intentos, uno en curso a la vez** (elegida) | Reintentar con otra tarjeta sin perder la reserva; historial completo de intentos; doble cobro prácticamente imposible. | Entidad adicional y reglas de "en curso"; un huérfano cerrado como `REJECTED` puede resultar cobrado (alerta, B-29). |
| Un pago por pedido (rechazo = nuevo pedido) | Modelo mínimo. | Mala experiencia; libera y re-reserva stock; más pedidos basura. |
| N intentos en paralelo permitidos | Sin bloqueo del comprador. | Riesgo real de doble cobro; más alertas y reembolsos. |

## Consecuencias
- **Positivas:** mejor conversión ante rechazos; trazabilidad de cada intento.
- **Negativas:** un intento huérfano en `CREATED` bloquea nuevos intentos hasta que la reconciliación lo resuelve (ADR-0005); el comprador debe esperar a que un `PENDING` se resuelva.
- **Verificación:** tests: rechazo seguido de nuevo intento aprobado → `PAID`; nuevo intento con otro en curso → rechazado; `processed` sobre un intento `REJECTED` → pedido sin cambios y alerta `CHARGED_AFTER_REJECTION`.
