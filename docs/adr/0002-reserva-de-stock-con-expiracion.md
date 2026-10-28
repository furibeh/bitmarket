# ADR-0002: Reserva de stock al crear el pedido, con expiración
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-28

## Contexto
Productos muy demandados se agotan con varios compradores pagando a la vez. Vender lo que no se tiene obliga a reembolsar y daña la confianza (brief §1). Los pagos con tarjeta pueden quedar pendientes (3DS, antifraude, procesamiento asíncrono) y resolverse minutos después, y un webhook puede llegar tarde. Hay que decidir **cuándo** se compromete el stock y qué pasa con un cobro aprobado cuando ya no hay unidades.

## Decisión
Reservamos el stock de todas las líneas (todo o nada) al crear el pedido; la reserva vence a los 15 minutos salvo que haya un intento de pago en curso (`CREATED`/`PENDING`), sin tope de tiempo. Como la reserva no expira con un intento en curso y solo se crean intentos con la reserva vigente (RN-65), todo pago aprobado encuentra su stock reservado: no hay re-reserva ni estado `PAID_OUT_OF_STOCK`. El único cobro inesperado posible es un intento ya `REJECTED` que Mercado Pago reporta cobrado (B-29), que genera una alerta y reembolso manual.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Reservar al crear el pedido, con expiración** (elegida) | Quien llega a pagar tiene la unidad; sobreventa imposible en el flujo normal; el abandono libera stock solo. | Stock retenido hasta 15 min por pedidos abandonados; hace falta un job de expiración y una política para pagos tardíos. |
| Descontar stock al aprobarse el pago | Sin reservas ni jobs; el stock solo se compromete con dinero. | Dos compradores pueden pagar la misma última unidad → reembolsos frecuentes justo en los productos más demandados. |
| Reservar al añadir al carrito | El comprador "asegura" antes. | Carritos abandonados bloquean stock mucho tiempo; abuso trivial; complica el carrito (que decidimos sin reserva). |
| Sin expiración mientras haya pago pendiente, pero con tope de tiempo | Libera stock de pagos colgados. | Una aprobación tras el tope obligaría a re-reservar y, sin stock, a reembolsar; se descartó el tope (RN-33). |
| Re-reservar ante una aprobación tardía, con `PAID_OUT_OF_STOCK` si falta stock (versión anterior de esta decisión) | Cubre aprobaciones tras la expiración. | Sin tope no hay camino que la active: estado, contrato de inventario y tests para un caso imposible; se retiró el 2026-10-28 (RN-35, B-09, B-10). |

## Consecuencias
- **Positivas:** RN-30 se cumple incluso con concurrencia; ningún cobro aprobado se pierde en silencio (alerta en el panel).
- **Negativas:** un intento `PENDING` indefinido retiene stock (mitigado por la reconciliación); un huérfano cerrado como `REJECTED` que resulta cobrado exige reembolso manual (B-29).
- **Verificación:** test de concurrencia (N compradores, stock 1 → 1 pedido, N−1 rechazos, stock ≥ 0); tests del job de expiración (vence sin intentos en curso, no vence con `CREATED` ni `PENDING`, B-28); test de cancelación rechazada con intento en curso (B-25); test de B-29 (cobro sobre un intento `REJECTED` → alerta, pedido sin cambios).
