# ADR-0013: Asignación del grupo `CUSTOMER` con el disparador Post Confirmation de Cognito
- Estado: Aceptado
- Fecha: 2026-10-28
- Revisado: 2026-11-07, corrige el efecto de un fallo del Lambda tras probarlo en #28

## Contexto
Los roles son grupos de Cognito (RN-44) y las rutas de comprador exigen el grupo `CUSTOMER`. El auto-registro de Cognito no añade al usuario a ningún grupo, así que hay que decidir cómo un comprador recién registrado obtiene su rol sin intervención manual y sin que el backend deduzca roles por ausencia de grupo.

## Decisión
Un Lambda mínimo, asociado al disparador *Post Confirmation* del User Pool y definido en `infra/permanent`, añade al usuario al grupo `CUSTOMER` al confirmar su registro. El grupo `ADMIN` se asigna a mano.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Lambda Post Confirmation** (elegida) | RN-44 se cumple tal cual; el grupo viaja en `cognito:groups` desde el primer login; el backend solo mira grupos explícitos. | Un Lambda y su rol IAM más en la capa permanente; si falla, el usuario queda confirmado y sin grupo, y debe asignarse a mano. |
| Backend trata "autenticado sin `ADMIN`" como comprador | Sin infraestructura extra. | Cambia RN-44; un usuario creado a mano o por error obtiene acceso de comprador por omisión. |
| Backend añade el grupo en el primer acceso (`AdminAddUserToGroup`) | Sin Lambda. | El token vigente no trae el grupo hasta renovarse; el backend necesita permisos de administración sobre el User Pool. |

## Consecuencias
- **Positivas:** autorización basada solo en grupos explícitos; sin permisos de administración de Cognito en el backend.
- **Negativas:** una pieza más que desplegar y probar en la capa permanente; si el Lambda falla, Managed Login muestra el error, pero el usuario ya queda `CONFIRMED` y sin grupo; reintentar el código no lo arregla y las rutas de comprador le responden 403 hasta asignarle `CUSTOMER` a mano. Los usuarios creados por un administrador tampoco reciben el grupo.
- **Verificación:** tras registrar y confirmar un usuario de prueba en el User Pool, su access token incluye `cognito:groups = ["CUSTOMER"]`; test de seguridad del backend: JWT sin grupo en una ruta de comprador → 403.
