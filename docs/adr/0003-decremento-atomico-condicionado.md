# ADR-0003: Concurrencia de inventario con decremento atómico condicionado
- Estado: Aceptado
- Fecha: 2026-10-27

## Contexto
La reserva (ADR-0002) debe ser correcta con N compradores simultáneos y con varias réplicas del backend, sin dejar el stock negativo. Es el objetivo central del brief ("evitar sobreventa") y debe probarse en CI con PostgreSQL real.

## Decisión
Reservamos con `UPDATE stock SET available = available - :qty WHERE sku = :sku AND available >= :qty` y comprobamos las filas afectadas (0 = sin stock), sin bloqueos explícitos; la misma técnica se usa para ajustes de stock, con `CHECK (available >= 0)` como red de seguridad.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Decremento atómico condicionado** (elegida) | Una sola sentencia atómica; sin lecturas previas ni reintentos; seguro con N réplicas; la base de datos serializa por fila. | SQL nativo fuera del modelo JPA; el error "sin stock" se deduce de filas afectadas. |
| Bloqueo optimista (`@Version`) | Idiomático en JPA. | Bajo alta contención (última unidad) muchos conflictos y reintentos; lógica de reintento en la aplicación. |
| Bloqueo pesimista (`SELECT ... FOR UPDATE`) | Fácil de razonar. | Mantiene el bloqueo durante la lógica de aplicación; más contención y riesgo de *deadlocks* al reservar varias líneas. |
| Contador en Redis | Muy rápido. | Infraestructura fuera de alcance (`brief.md` §4); dos fuentes de verdad para el stock. |

## Consecuencias
- **Positivas:** corrección bajo concurrencia sin coordinación adicional; código pequeño.
- **Negativas:** consultas nativas (`@Modifying @Query(nativeQuery = true)`) que deben probarse contra PostgreSQL real; con varias líneas, el orden de actualización debe ser determinista (por SKU) para minimizar esperas.
- **Verificación:** test de integración con Testcontainers que lanza N hilos contra stock 1 y comprueba exactamente 1 éxito y stock final 0; test que intenta un ajuste que dejaría negativo y verifica el rechazo.
