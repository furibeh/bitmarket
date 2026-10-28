# ADR-0007: Amazon Cognito como proveedor de identidad
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-28

## Contexto
El registro es obligatorio, hay dos roles (`CUSTOMER`, `ADMIN`) y tres consumidores de identidad: la web Next.js, la API Spring y el servidor MCP (OAuth). La autenticación es la parte de mayor riesgo y el proyecto no debe almacenar credenciales ni más datos personales de los necesarios. El despliegue es en AWS con un entorno efímero, así que el proveedor de identidad debe vivir en la capa permanente.

## Decisión
Usamos Amazon Cognito (plan Essentials, con Managed Login) como único proveedor de identidad, con roles como grupos de Cognito; Next.js se integra con Better Auth por OIDC (ADR-0011) y Spring actúa como *resource server* que valida JWT y grupos. Essentials es necesario para los *resource indicators* (RFC 8707) que fijan el `aud` de los tokens del MCP.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Amazon Cognito** (elegida) | Gestionado, nivel gratuito amplio (10.000 MAU en Lite y Essentials); envía correos de verificación y recuperación; vive en la capa permanente; OIDC/OAuth estándar; *resource indicators* para el MCP. | Personalización limitada; para MCP solo admite clientes registrados de antemano (ni documentos de metadatos de cliente ni registro dinámico; spike pendiente); dependencia de AWS. |
| Keycloak autoalojado | Muy completo; soporta registro dinámico de clientes y está documentado para MCP. | Hay que operarlo y pagarlo; no sobrevive al entorno efímero sin base de datos propia permanente. |
| Autenticación propia con Spring Security | Control total. | Mayor riesgo (contraseñas, recuperación, verificación de correo); trabajo que no aporta al objetivo del proyecto. |

## Consecuencias
- **Positivas:** sin credenciales en nuestra base; un solo emisor de tokens para web, API y MCP; URLs de callback estables gracias a CloudFront.
- **Negativas:** la autorización MCP con Cognito necesita validarse (spike; plan B proxy de autorización); Better Auth guarda correo y nombre en el esquema `bff` del BFF (excepción documentada en RN-40); acoplamiento a claims de Cognito (`cognito:groups`).
- **Verificación:** tests de seguridad del backend con JWT de prueba: sin token → 401; `CUSTOMER` en `/api/admin/**` → 403; pedido ajeno → 404.
