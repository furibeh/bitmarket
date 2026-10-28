# ADR-0012: Persistencia con Spring Data JPA y consultas nativas para concurrencia
- Estado: Aceptado
- Fecha: 2026-10-27

## Contexto
El backend persiste en PostgreSQL agregados de varios módulos. Las operaciones críticas (reserva atómica, `FOR UPDATE SKIP LOCKED`, transiciones guardadas) requieren SQL específico, y los límites de Spring Modulith prohíben acoplar entidades entre módulos. El proyecto busca además empleabilidad con tecnologías extendidas.

## Decisión
Usamos Spring Data JPA (Hibernate) para el acceso a datos; las sentencias de concurrencia se escriben como `@Query(nativeQuery = true)` (con `@Modifying` cuando modifican) en los repositorios del módulo dueño, y las entidades no tienen relaciones JPA hacia entidades de otro módulo.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Spring Data JPA + consultas nativas** (elegida) | Muy extendido en el mercado; CRUD con poco código; buen soporte en Spring Boot y Testcontainers. | *Lazy loading* y *dirty checking* pueden sorprender; tentación de relacionar entidades entre módulos; el contexto de persistencia puede quedar desfasado tras un `UPDATE` nativo. |
| Spring Data JDBC + `JdbcClient` | Agregados simples sin magia; encaja con los límites de módulo; SQL explícito natural. | Menos conocido; más código para mapeos complejos. |
| jOOQ | SQL tipado generado del esquema. | *Codegen* contra la base en el build; más configuración. |

## Consecuencias
- **Positivas:** productividad en CRUD (catálogo, panel); tecnología reconocible.
- **Negativas:** tras un `@Modifying` nativo hay que limpiar o no reutilizar entidades cargadas (`clearAutomatically = true` o recargar); `spring.jpa.open-in-view=false` para evitar consultas fuera de transacción.
- **Verificación:** las consultas nativas se prueban contra PostgreSQL real con Testcontainers; `ApplicationModules.verify()` falla si una entidad referencia un tipo interno de otro módulo; `open-in-view` desactivado en la configuración.
