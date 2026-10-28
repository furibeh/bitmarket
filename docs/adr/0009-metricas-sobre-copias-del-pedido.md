# ADR-0009: Métricas de ventas sobre las copias guardadas en los pedidos
- Estado: Aceptado
- Fecha: 2026-10-27

## Contexto
El MCP debe responder preguntas como "¿cuánto se vendió esta semana por categoría?". El catálogo cambia (nombres, categorías, precios) y las métricas históricas no deben cambiar con él. El volumen de datos es pequeño y no se quiere infraestructura adicional.

## Decisión
Calculamos las métricas de ventas con consultas sobre `orders`/`order_line`, usando las copias de nombre, categoría y precio guardadas al crear el pedido, sin un módulo `reporting` separado.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Consultas sobre las copias del pedido** (elegida) | Sin módulo ni tablas extra; histórico estable; reutiliza datos que ya se guardan por RN-21. | Las consultas analíticas comparten base con la operación; si crecen, puede hacer falta optimizar. |
| Módulo `reporting` alimentado por eventos | Modelo de lectura propio y desacoplado; escalable. | Más código, eventos y tablas sin necesidad actual; consistencia eventual. |
| Unir pedidos con el catálogo actual | Sin duplicar datos. | Métricas que cambian al renombrar o recategorizar productos; incorrecto para histórico. |

## Consecuencias
- **Positivas:** simple y correcto para el MVP.
- **Negativas:** las consultas de métricas viven en `orders` (`OrderQueries`) y deben indexarse por `paid_at`/estado.
- **Verificación:** test que crea ventas, cambia nombre y categoría del producto y comprueba que `sales_by_category` no cambia; test de la definición de venta y semana (RN-75).
