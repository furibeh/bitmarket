# ADR-0008: Servidor MCP remoto con OAuth dentro del monolito
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-28

## Contexto
El administrador quiere consultar stock y ventas desde un asistente de IA y ejecutar una acción acotada (ajustar stock) con permiso y confirmación explícita. El MCP debe respetar los límites de módulos y la lógica de concurrencia del inventario, y demostrar autorización real.

## Decisión
Implementamos el servidor MCP como módulo `mcp` del monolito (Spring AI 2.0 MCP Server, transporte Streamable HTTP), protegido con OAuth de Cognito para el grupo `ADMIN` y tokens con `aud` del MCP (*resource indicators*), que usa solo las APIs públicas de `orders`, `inventory` y `catalog`; la única tool de escritura (ajustar stock) exige vista previa + token de un solo uso de 5 minutos.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Módulo MCP remoto en el monolito, OAuth** (elegida) | Reutiliza la lógica y la transaccionalidad del inventario; autorización real; un solo despliegue. | Compatibilidad OAuth MCP–Cognito por validar; el MCP comparte proceso con la tienda. |
| Servidor MCP local por stdio | Sin autenticación de red; muy simple. | Requiere acceso directo a la base o credenciales en el equipo; no demuestra autorización; no es remoto. |
| Servicio MCP aparte que llama a la API REST | Aislamiento de proceso. | Otro servicio que desplegar y pagar; duplica autenticación; la confirmación en dos pasos cruza servicios. |

## Consecuencias
- **Positivas:** la acción administrativa reutiliza `Inventory.adjust` con las mismas garantías (RN-74); permisos centralizados en Cognito.
- **Negativas:** riesgo técnico en OAuth: Cognito solo admite clientes registrados de antemano, sin documentos de metadatos de cliente ni registro dinámico (especificación MCP 2026-07-28) → spike temprano con MCP Inspector y plan B (proxy de autorización); la revisión 2026-07-28 también cambia el transporte (sin sesión ni `initialize`, cabeceras `Mcp-Method`/`Mcp-Name`) y Spring AI 2.0 es anterior a ella, así que la compatibilidad de versión con el cliente se valida en el mismo spike.
- **Verificación:** tests: llamada sin token o sin `ADMIN` → rechazo; confirmación sin vista previa, con token caducado, reutilizado o con stock cambiado → sin efecto; demo con cliente MCP real (criterio de éxito del brief).
