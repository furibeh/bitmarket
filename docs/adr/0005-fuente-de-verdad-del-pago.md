# ADR-0005: Fuente de verdad del pago: respuesta síncrona, webhook verificado + consulta a la API y reconciliación
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-28

## Contexto
El estado del pago decide si un pedido pasa a `PAID`. Los webhooks pueden perderse, duplicarse, llegar fuera de orden o ser falsificados; `POST /v1/orders` puede agotar el tiempo de espera. El cliente (navegador) nunca es confiable para afirmar que pagó.

## Decisión
El estado de un intento lo fija solo Mercado Pago: la respuesta síncrona de `POST /v1/orders`, o un webhook con `x-signature` válida seguido siempre de `GET /v1/orders/{id}`. Los timeouts se reintentan hasta 3 veces con la misma clave de idempotencia y el mismo cuerpo, guardada antes de llamar. Un job de reconciliación consulta los intentos `PENDING` antiguos y busca por `external_reference` (`GET /v1/orders`) los `CREATED` huérfanos: si la orden no existe en Mercado Pago, el intento pasa a `REJECTED`.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Síncrona + webhook verificado con consulta + reconciliación** (elegida) | Resistente a pérdida, duplicado, desorden y falsificación; idempotente por transiciones guardadas. | Más piezas: verificación de firma, cliente HTTP, job, estado `CREATED`. |
| Confiar en el cuerpo del webhook | Simple. | Falsificable si falla la firma; el cuerpo puede estar desactualizado o fuera de orden. |
| Solo respuesta síncrona | Sin webhooks. | Las órdenes `processing`/`action_required` nunca se resolverían. |
| Solo *polling* periódico | Sin endpoint público. | Latencia alta para confirmar; muchas llamadas a la API. |

## Consecuencias
- **Positivas:** estados coherentes aunque falle la red; duplicados sin efecto (RN-66).
- **Negativas:** endpoint público que proteger; job adicional; un intento huérfano bloquea nuevos intentos hasta la siguiente reconciliación (≈ 2–7 min); si un huérfano marcado `REJECTED` resulta cobrado después, no se recupera: genera una alerta y reembolso manual (RN-66, B-29).
- **Verificación:** tests con WireMock: firma inválida → 4xx sin efectos; misma notificación dos veces → una sola transición; el estado aplicado es el de `GET /v1/orders/{id}` aunque el cuerpo diga otra cosa; la reconciliación resuelve un `PENDING` sin webhook; un `CREATED` huérfano se resuelve con el estado real si la orden existe y pasa a `REJECTED` si no existe; un `processed` sobre un intento `REJECTED` no cambia el intento y genera una alerta `CHARGED_AFTER_REJECTION`.
