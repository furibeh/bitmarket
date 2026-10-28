# ADR-0010: Infraestructura AWS en dos capas con Terraform y ECS Fargate, entorno efímero
- Estado: Aceptado
- Fecha: 2026-10-27
- Revisado: 2026-10-28

## Contexto
El brief exige desplegar en AWS y demostrarlo, pero pagando solo por horas encendidas; el mayor riesgo de costo es olvidar el entorno encendido. Las URLs de callback de Cognito y de webhook de Mercado Pago deben ser estables entre despliegues, sin dominio propio. App Runner está cerrado a clientes nuevos desde el 30-04-2026.

## Decisión
Definimos con Terraform una capa permanente (Cognito con su Lambda *Post Confirmation*, ECR, S3, CloudFront, SSM, Budgets, estado de Terraform, rol OIDC) y una capa efímera (VPC sin NAT, ALB interno, ECS Fargate para backend y Next.js, RDS `db.t4g.micro`) en `us-east-1`; la efímera se crea y destruye con workflows manuales de GitHub Actions autenticados por OIDC. `env-up` vuelve a aplicar la capa permanente con el nuevo ALB como origen de CloudFront (VPC origin, ADR-0014), y un workflow programado cada hora destruye la capa efímera si lleva más de 4 horas.

## Alternativas consideradas
| Alternativa | Ventajas | Desventajas |
| --- | --- | --- |
| **ECS Fargate definido a mano + capa efímera** (elegida) | Estándar del mercado; enseña cada pieza (VPC, ALB, ECS, RDS); costo ~0,10–0,12 USD/h solo encendido. | Más Terraform que escribir; arranque de RDS lento en cada `apply`; datos no persistentes. |
| ECS Express Mode | Menos configuración. | Oculta las piezas que el proyecto quiere aprender y demostrar. |
| App Runner | Muy simple. | Cerrado a clientes nuevos desde el 30-04-2026. |
| EC2 + Docker Compose | Barato y familiar. | Gestión manual del servidor; menos representativo de producción; HTTPS y escalado a mano. |
| Entorno permanente | Siempre disponible. | Costo continuo (RDS, ALB); explícitamente fuera de alcance. |
| Apagado con EventBridge + Lambda (escalar a 0) | No depende de GitHub. | Deja el estado de Terraform desalineado; RDS detenido se reinicia solo a los 7 días; más infraestructura. |

## Consecuencias
- **Positivas:** costo acotado (máximo ~5 h por olvido); URLs estables gracias a CloudFront; sin llaves de AWS en GitHub.
- **Negativas:** cada `apply` empieza con datos semilla (Flyway); las tareas Fargate van en subredes públicas con IP pública (sin NAT); cada `env-up`/`env-down` también modifica la capa permanente (solo el origen de CloudFront) y espera la propagación de la distribución (minutos).
- **Verificación:** `terraform apply` + compra sandbox de punta a punta en la URL de CloudFront + `terraform destroy` sin recursos con costo restantes; el workflow `env-reaper` destruye un entorno con `bitmarket:created-at` de más de 4 h; alertas de AWS Budgets configuradas.
