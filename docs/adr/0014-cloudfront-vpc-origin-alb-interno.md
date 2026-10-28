# ADR-0014: CloudFront con el ALB interno como VPC origin
- Estado: Aceptado
- Fecha: 2026-10-28

## Contexto
CloudFront es la única entrada pública del sistema (ADR-0010). Con un ALB público restringido a la lista de prefijos administrada de CloudFront, cualquier distribución de CloudFront, incluida una de otra cuenta, puede llegar al ALB. Además, el ALB tiene IP pública y se descubre desde internet. Sin dominio propio no hay certificado para el ALB, así que el tramo CloudFront → ALB va por HTTP. Desde noviembre de 2024 CloudFront admite *VPC origins*: un ALB en subredes privadas al que CloudFront llega por una ENI gestionada, sin costo adicional, también en `us-east-1` (salvo la zona `use1-az3`).

## Decisión
El ALB de la capa efímera es interno, en subredes privadas, y CloudFront llega a él como *VPC origin*. El recurso VPC origin se crea en la capa efímera junto al ALB. La capa permanente recibe su `vpc_origin_id` como origen de la distribución, y el security group del ALB solo acepta tráfico del grupo gestionado `CloudFront-VPCOrigins-Service-SG`. Las tareas Fargate siguen en subredes públicas con IP pública, porque no hay NAT.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **ALB interno como VPC origin** (elegida) | ALB sin IP pública ni superficie en internet; solo las distribuciones de la cuenta llegan a él; tráfico privado dentro de la red de AWS; sin costo adicional. | El VPC origin tarda hasta 15 min en desplegarse en cada `env-up`; el VPC necesita un internet gateway; hay que evitar `use1-az3`; sin Lambda@Edge de origen (no se usa). |
| ALB público + lista de prefijos de CloudFront (anterior) | Lo más simple; despliegue rápido. | Cualquier distribución de CloudFront llega al ALB; ALB descubrible desde internet. |
| ALB público + prefijos + cabecera secreta de origen | Solo nuestra distribución pasa la regla del ALB. | Secreto que rotar y guardar en SSM; el ALB sigue expuesto; la cabecera viaja por HTTP. |
| Dominio propio con certificado ACM en el ALB | HTTPS de punta a punta. | Costo y gestión de un dominio, fuera de lo decidido (URLs `*.cloudfront.net`). |

## Consecuencias
- **Positivas:** el ALB deja de ser alcanzable desde internet; ya no hace falta aceptar todos los prefijos de CloudFront; menos superficie que proteger.
- **Negativas:** `env-up` espera a que el VPC origin pase a *Deployed* (hasta 15 min); `env-down` debe desasociarlo de la distribución (origen *placeholder*) antes de destruirlo; el tramo CloudFront → ALB sigue en HTTP, aunque por la red privada de AWS.
- **Verificación:** tras `env-up`, el DNS del ALB no resuelve a IPs públicas y una petición directa desde internet no llega; la compra sandbox de punta a punta funciona por la URL de CloudFront; `env-down` destruye el VPC origin sin errores de dependencia.
