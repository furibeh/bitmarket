# ADR-0017: Roles de ECS en la capa permanente y usuario de despliegue sin escritura en IAM
- Estado: Aceptado
- Fecha: 2026-11-08

## Contexto
Los workflows `env-up` y `env-down` se autentican con llaves de larga duración de un usuario IAM, porque las SCP de la cuenta impiden OIDC (ADR-0016). La capa efímera necesita roles de ejecución y de tarea para ECS. ADR-0010 no decide dónde se crean. Si Terraform los crea desde CI, ese usuario necesita `iam:CreateRole` junto con `iam:PassRole`, y quien filtre sus llaves puede crear un rol con permisos de administrador y dárselo a una tarea.

## Decisión
Los roles de ejecución y de tarea de ECS se definen en la capa permanente, que se aplica en local. El usuario `bitmarket-ci-deploy` no tiene ningún permiso de escritura en IAM: solo recibe `iam:PassRole` sobre esos dos roles.

Sobre EC2 y RDS, el usuario se limita por acción y región, no por recurso, porque la cuenta solo aloja este proyecto y limitar por ARN o por etiqueta complica una política que hay que ensayar en cada cambio.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **Roles en la capa permanente** (elegida) | La llave de CI no puede crear ni ampliar permisos; los roles no tienen costo si quedan creados. | Los roles viven fuera de la capa que los usa; un cambio en sus permisos exige aplicar la capa permanente en local. |
| Roles en la capa efímera, limitados por prefijo y con *permissions boundary* | Cada capa contiene todo lo suyo. | La llave de CI conserva escritura en IAM; la política y el límite son más difíciles de escribir y de auditar. |
| Roles en la capa efímera, con IAM sin limitar | Lo más simple. | Una filtración de las llaves equivale a acceso de administrador a la cuenta. |

## Consecuencias
- **Positivas:** una filtración de las llaves de despliegue no permite escalar privilegios mediante IAM. Queda acotado el riesgo que ADR-0016 acepta.
- **Negativas:** los roles sobreviven a `env-down`, y `infra/ephemeral` debe referenciarlos por nombre. Los *service-linked roles* de ECS y del balanceador deben existir antes del primer `env-up` que los necesite, porque este usuario no puede crearlos. Una filtración de las llaves permite modificar o borrar cualquier VPC o instancia RDS de `us-east-2`; si la cuenta llega a alojar otros proyectos, hay que limitar esos permisos por recurso.
- **Verificación:** `aws iam simulate-principal-policy` sobre `bitmarket-ci-deploy` responde `implicitDeny` para `iam:CreateRole` e `iam:PutUserPolicy`.
