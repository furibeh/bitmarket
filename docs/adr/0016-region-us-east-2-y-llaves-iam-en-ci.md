# ADR-0016: Región `us-east-2` y llaves de usuarios IAM en CI por las restricciones de la cuenta de AWS
- Estado: Aceptado
- Fecha: 2026-11-06

## Contexto
ADR-0010 fijó `us-east-1` y autenticación de GitHub Actions por OIDC, sin llaves de AWS en GitHub. Al aplicar la capa permanente (#28) apareció una restricción que esa decisión no contemplaba: la cuenta se creó con la [nueva experiencia de AWS](https://docs.aws.amazon.com/accounts/latest/reference/sign-up-for-aws.html) (Builder ID y proyectos), en la que AWS administra las SCP de la organización y el titular no puede editarlas.

- La región la asigna AWS según el país de la dirección de contacto y no se puede cambiar; a Colombia le corresponde `us-east-2`. La creación de un bucket en `us-east-1` falló con un *deny* explícito de la SCP y `cognito-idp` solo responde en `us-east-2`.
- Las [SCP administradas](https://docs.aws.amazon.com/accounts/latest/reference/scps-and-rcps-for-projects.html) deniegan `iam:*Provider*` en Free Tier y en el plan de pago, así que no se puede crear el proveedor OIDC de GitHub. `aws iam list-open-id-connect-providers` responde `AccessDenied` en la cuenta.

## Decisión
Mantenemos la cuenta actual. Los recursos regionales se crean en `us-east-2`, y GitHub Actions se autentica en AWS con llaves de usuarios IAM de alcance limitado, guardadas como secretos de GitHub. Esta decisión sustituye en ADR-0010 la región y la autenticación por OIDC; el resto de ADR-0010 sigue vigente.

Las llaves se crean fuera de Terraform para que no queden en su estado, tienen solo los permisos de su workflow y se rotan a mano. Los workflows que las usan corren únicamente con `push` a `main`, `workflow_dispatch` o `schedule`: los PR no las reciben.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **`us-east-2` con llaves IAM en la cuenta actual** (elegida) | No exige otra cuenta ni plan de pago; es reversible; conserva el crédito de Free Tier. | Llaves de larga duración que proteger y rotar; cambia una decisión tomada; el plan de pago será obligatorio a los 6 meses o al agotar el crédito. |
| Activar las funciones avanzadas en esta cuenta | Permite editar las SCP, usar OIDC y `us-east-1` sin cambiar el diseño. | [Irreversible y exige plan de pago](https://docs.aws.amazon.com/accounts/latest/reference/activate-advanced-features.html); elimina el límite de gasto; la cuenta de proyecto nunca podrá salir de la organización. |
| Cuenta nueva con *Sign up for AWS (advanced)* | Conserva OIDC y `us-east-1` sin cambios en el repositorio. | Crear y administrar otra cuenta, más complicado para el titular en este momento. |

## Consecuencias
- **Positivas:** el proyecto puede seguir sin nueva cuenta y sin gasto inicial. Las subredes del ALB ya no deben evitar `use1-az3`: [`us-east-2` admite VPC origins](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-vpc-origins.html) sin zonas excluidas.
- **Negativas:** una filtración de las llaves da acceso hasta que se revoquen, lo que no ocurría con OIDC. Las funciones de CloudFront que crean recursos fuera de la región, como AWS WAF, [no están disponibles](https://docs.aws.amazon.com/accounts/latest/reference/project-regions.html). AWS puede cambiar las SCP administradas sin que podamos ajustarlas.
- **Reversión:** si más adelante se activan las funciones avanzadas o se cambia de cuenta, se puede volver a OIDC y retirar las llaves.
- **Verificación:** `terraform apply` de la capa permanente crea los recursos en `us-east-2`; el `env-up` de #32 y la distribución de #34 confirman que las SCP permiten la capa efímera y el VPC origin; los secretos de las llaves no aparecen en los logs de los workflows.
