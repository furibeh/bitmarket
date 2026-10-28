# ADR-0001: Monolito modular con Spring Modulith y eventos en proceso
- Estado: Aceptado
- Fecha: 2026-10-27

## Contexto
El sistema tiene dominios con límites claros (catálogo, inventario, carrito, pedidos, pagos, clientes, MCP) y uno de los objetivos del brief es demostrar módulos desacoplados de forma verificable. Al mismo tiempo es un proyecto de una persona, con presupuesto de AWS mínimo (entorno efímero, sin broker gestionado) y donde la consistencia entre inventario, pedidos y pagos es crítica (no sobreventa).

## Decisión
Construimos un único backend Spring Boot organizado en módulos de Spring Modulith (`catalog`, `inventory`, `cart`, `orders`, `payments`, `customers`, `mcp`), con eventos de dominio en proceso persistidos en el *event publication registry* JDBC (outbox) y `ApplicationModules.verify()` en CI.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Monolito modular con Spring Modulith + outbox JDBC** (elegida) | Límites verificables en tests; transacciones locales para stock/pedido/pago; un solo despliegue; eventos con entrega garantizada sin infraestructura extra. | Escala como una unidad; la disciplina de límites depende de la verificación en CI; eventos solo dentro del proceso. |
| Microservicios | Despliegue y escalado independientes; límites físicos. | Transacciones distribuidas o sagas para reservar stock y cobrar; varias bases y servicios que pagar y operar; desproporcionado para el tamaño del proyecto. |
| Monolito sin módulos verificados (capas clásicas) | Lo más simple de arrancar. | Nada impide el acoplamiento entre dominios; no demuestra el objetivo de módulos desacoplados. |
| Monolito modular + broker externo (Kafka/RabbitMQ) | Eventos durables fuera del proceso; camino a microservicios. | Infraestructura y costo adicional (explícitamente fuera de alcance en el brief); complejidad operativa sin necesidad actual. |

## Consecuencias
- **Positivas:** consistencia transaccional local; un solo artefacto que desplegar; límites comprobables automáticamente.
- **Negativas:** sin escalado independiente por módulo; los listeners de eventos deben ser idempotentes (entrega al menos una vez); hay que vigilar ciclos entre módulos.
- **Verificación:** test `ApplicationModules.of(BitmarketApplication.class).verify()` en `./mvnw verify`, obligatorio en el *required check* de CI; un PR que use internos de otro módulo o cree un ciclo falla.
