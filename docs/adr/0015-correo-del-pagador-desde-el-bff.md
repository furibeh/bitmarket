# ADR-0015: El BFF envía el correo del comprador para el `payer` de Mercado Pago
- Estado: Aceptado
- Fecha: 2026-10-29

## Contexto
El spike #27 comprobó en sandbox que `POST /v1/orders` exige en `payer` al menos `email`, `customer_id` o `id`: sin `payer`, o solo con el nombre, responde 400 `required_properties`, aunque la referencia lo marca como opcional para tarjeta. En sandbox se acepta cualquier correo con formato válido. El backend no guarda el correo del comprador (RN-40) y el access token de Cognito no lo incluye; Better Auth sí lo guarda en el esquema `bff` de Next.js.

## Decisión
El BFF envía el correo de la sesión de Better Auth en el cuerpo de `POST /api/orders/{id}/payments`, y el backend lo reenvía como `payer.email` a Mercado Pago sin guardarlo en ninguna tabla ni registrarlo en logs. RN-40 se aclara: el backend no *guarda* el correo, pero puede recibirlo de paso para el cobro.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **El BFF envía el correo de la sesión** (elegida) | Sin piezas nuevas; correo real, útil para el antifraude en producción; Better Auth ya lo tiene. | Un dato personal pasa por el backend: hay que aclarar RN-40 y vigilar los logs. |
| El backend lo pide a Cognito (`/oauth2/userInfo`) | Fuente autoritativa; el BFF no interviene. | Una llamada de red más por pago y un punto de fallo nuevo; exige el scope `email` en el access token. |
| Correo sintético por cliente (`<customerId>@…`) | Ningún dato personal llega al backend ni a Mercado Pago. | Perjudica el antifraude y el contacto con el comprador en producción; contradice "preparado para producción". |
| Cliente de Mercado Pago (`payer.customer_id`) | No repite el correo en cada pago. | Crear el cliente también exige el correo; más integración y un ID externo que guardar. |

## Consecuencias
- **Positivas:** el cobro funciona sin almacenar datos de Cognito en el backend; ninguna dependencia nueva.
- **Negativas:** el backend confía en el correo que le pasa el BFF (no en el navegador); como el estado del pago lo fija Mercado Pago (RN-63), un correo erróneo no altera ningún estado. El correo no debe aparecer en logs ni en el registro del intento. Mercado Pago rechaza con 400 `property_value` los correos de dominios no permitidos o con palabras no permitidas: el intento pasa a `REJECTED` como cualquier error de validación, y cambiar de tarjeta no lo resuelve.
- **Verificación:** IT con WireMock: la petición a `POST /v1/orders` lleva `payer.email` con el valor recibido; ninguna tabla del backend contiene el correo; test de que los logs del cobro no incluyen el correo.
