# ADR-0011: Next.js como BFF entre el navegador y el backend
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-28

## Contexto
La web inicia sesión con Cognito por OIDC y el backend valida JWT. Hay que decidir si el navegador llama directamente a la API del backend (con el token en JavaScript) o si todo pasa por el servidor de Next.js. Importan la exposición del token, CORS, el enrutado en CloudFront/ALB y el SEO del catálogo renderizado en servidor. También hay que elegir la librería de sesión: Auth.js (antes NextAuth.js) pasó en 2025 al equipo de Better Auth, solo recibe parches de seguridad y su v5 sigue en beta.

## Decisión
El navegador solo habla con Next.js; las páginas, *server actions* y *route handlers* llaman al backend por REST con el access token guardado en la sesión del servidor, y el backend solo expone públicamente `/webhooks/*`, `/mcp` y `/.well-known/oauth-protected-resource/mcp`. La sesión la gestiona Better Auth, con sesiones y tokens de Cognito en un esquema `bff` de PostgreSQL propiedad exclusiva de Next.js.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Next.js como BFF** (elegida) | El token nunca llega al JavaScript del navegador; sin CORS; un solo origen público para la web; el renderizado en servidor ya llama al backend de todas formas. | Un salto de red más por petición; Next.js debe propagar errores y `X-Request-Id`. |
| Sesión con Auth.js v5 | Muy conocido; integración directa con Cognito. | En modo mantenimiento; v5 todavía en beta. |
| Better Auth sin base de datos (cookie cifrada) | Sin tablas propias. | Los JWT de Cognito son grandes (cookies troceadas); hay reportes de fallos de `getAccessToken` con Cognito en este modo. |
| Navegador → backend directo (`/api/*` por CloudFront) | Menos código en Next.js; menor latencia. | Token accesible desde JavaScript (riesgo XSS); configurar CORS y rutas; dos superficies públicas que proteger. |

## Consecuencias
- **Positivas:** menor superficie de ataque; contratos tipados desde OpenAPI en un solo lugar (`frontend/src/lib/api`).
- **Negativas:** Next.js es dependencia de todas las operaciones de la tienda; más lógica en el servidor del frontend; Next.js necesita acceso a PostgreSQL para su esquema `bff` (rol `bitmarket_bff` limitado a ese esquema, creado por Flyway; tablas migradas por Better Auth al arrancar) y las sesiones se pierden con cada entorno efímero; el cierre de sesión debe pasar también por el `/logout` de Cognito.
- **Verificación:** las reglas del ALB solo enrutan `/webhooks/*`, `/mcp` y `/.well-known/oauth-protected-resource/mcp` al backend; ningún código cliente (`"use client"`) importa el cliente de API del backend ni lee el access token (revisión y regla de lint).
