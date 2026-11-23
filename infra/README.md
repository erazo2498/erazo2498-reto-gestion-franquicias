# Infraestructura en AWS

Todo lo necesario para correr la API en la nube con Terraform: red, base de datos, imagen, balanceador y servicio.
Un solo código sirve para tres ambientes (`dev`, `staging`, `prod`); lo único que cambia es un archivo de valores por ambiente.

> Estado: **`dev` está desplegado y probado**. `staging` y `prod` están definidos y validados con `terraform validate`, pero todavía no se han desplegado.

## Qué se despliega

```
Internet ──HTTP:80──► ALB ──8080──► ECS Fargate (la API) ──5432, TLS──► RDS PostgreSQL 16
                       │                  │                                  (subredes privadas)
                       │                  └── lee sus credenciales de Secrets Manager
                       └── health check: /actuator/health
```

| Pieza | Detalle |
|---|---|
| Red | VPC con 2 subredes públicas (ALB y tareas) y 2 privadas (base de datos). Sin NAT, para no pagarlo. |
| ECR | Un repositorio por ambiente, con etiquetas **inmutables**: cada despliegue usa una etiqueta nueva (`v` + commit). |
| RDS | PostgreSQL 16, cifrada, no pública. La contraseña la genera RDS y vive solo en Secrets Manager. |
| Secrets Manager | `DB_USERNAME` y `DB_PASSWORD` (secreto de RDS) y `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SSL` (secreto de conexión). Llegan al contenedor como `secrets`, nunca en texto plano. |
| ECS Fargate | Servicio detrás del ALB con **rollback automático** si el despliegue no se vuelve sano y **Auto Scaling** por CPU (objetivo 60 %). |
| IAM | Rol de ejecución (baja la imagen, escribe logs y lee **solo** los dos secretos de su ambiente) y rol de tarea (sin permisos). Uno por ambiente. |

## Estructura

```
infra/
  main.tf  variables.tf  outputs.tf  versions.tf   código único para todos los ambientes
  envs/        dev.tfvars  staging.tfvars  prod.tfvars   lo que cambia por ambiente
  backends/    dev.hcl     staging.hcl     prod.hcl      dónde se guarda el estado de cada ambiente
  modules/     networking  ecr  database  alb  ecs       un módulo por pieza
  bootstrap/   bucket de estado, proveedor OIDC y roles de GitHub (se aplica una sola vez)
```

| | `dev` | `staging` | `prod` |
|---|---|---|---|
| Base de datos | `db.t4g.micro`, 1 zona | igual que `dev` | `db.t4g.small`, **Multi-AZ**, con protección contra borrado |
| Tareas (mín–máx) | 1–2 | 1–2 | 2–4 |
| Se puede destruir sin trabas | sí | sí | no, a propósito |
| Swagger | encendido | encendido | apagado |

El estado de Terraform vive en S3 (un archivo por ambiente, con bloqueo por archivo `use_lockfile`, sin DynamoDB).

## Requisitos

- Cuenta de AWS y credenciales en la CLI (`aws sts get-caller-identity` debe responder). Región `us-east-2`.
- Terraform 1.10 o superior, AWS CLI v2 y Docker en ejecución.
- Comandos en **PowerShell**.

## 1. Una sola vez: bucket de estado y roles de GitHub

Sigue [`bootstrap/README.md`](bootstrap/README.md). Al terminar tendrás el bucket `reto-franquicias-tfstate-<cuenta>` y un rol por ambiente
(`reto-franquicias-<ambiente>-deploy`).

Después, en GitHub (*Settings → Environments*), crea tres ambientes con la variable `AWS_ROLE_ARN` (en *Variables*, no en *Secrets*):

| Environment | Variable `AWS_ROLE_ARN` | Ramas permitidas | Revisores |
|---|---|---|---|
| `dev` | ARN de `reto-franquicias-dev-deploy` | `develop` | no |
| `staging` | ARN de `reto-franquicias-staging-deploy` | `main` | no |
| `prod` | ARN de `reto-franquicias-prod-deploy` | `main` | sí |

`terraform output deploy_role_arns` (en `bootstrap/`) imprime los tres ARN.

## 2. Desplegar un ambiente a mano

Ejemplo con `dev`. Para otro ambiente, cambia `dev` por `staging` o `prod` en los tres lugares donde aparece.

```powershell
cd infra
$ACCOUNT = aws sts get-caller-identity --query Account --output text
$TAG = "v" + (git rev-parse --short HEAD)

# a) Conectar con el estado remoto de ese ambiente
terraform init -reconfigure "-backend-config=backends/dev.hcl" "-backend-config=bucket=reto-franquicias-tfstate-$ACCOUNT"

# b) Primero solo el repositorio de imágenes (la imagen debe existir antes que el servicio)
terraform apply "-target=module.ecr" "-var-file=envs/dev.tfvars" "-var=image_tag=$TAG"

# c) Construir y subir la imagen (tarda unos 2 minutos)
$REPO = terraform output -raw ecr_repository_url
$PW = aws ecr get-login-password --region us-east-2
docker login --username AWS --password $PW $REPO.Split("/")[0]
docker build -f ..\deployment\Dockerfile -t "${REPO}:$TAG" ..
docker push "${REPO}:$TAG"

# d) Todo lo demás (la base de datos tarda unos 10 minutos)
terraform plan  "-var-file=envs/dev.tfvars" "-var=image_tag=$TAG"
terraform apply "-var-file=envs/dev.tfvars" "-var=image_tag=$TAG"
```

El `plan` de un ambiente nuevo muestra 32 recursos por crear. Al terminar, `terraform output api_url` da la dirección pública.

## 3. Desplegar automáticamente (GitHub Actions)

| Evento | Workflow | Qué hace |
|---|---|---|
| Pull request que toca `infra/`, `deployment/` o `.github/` | `infra-checks.yml` | `terraform fmt`, `validate` y construye el `Dockerfile`. No usa AWS. |
| Merge a `develop` | `deploy.yml` | Construye la imagen y despliega `dev`. |
| Merge a `main` | — | No despliega nada: entregar el código no crea ni cambia ambientes. |
| Ejecución manual (*Actions → Deploy → Run workflow*, eligiendo `staging` o `prod`) | `deploy.yml` | Construye la imagen y despliega ese ambiente. `prod` espera la aprobación de un revisor. |

Cada despliegue asume el rol de su ambiente por OIDC (sin claves guardadas), sube la imagen con la etiqueta del commit, aplica Terraform y
termina comprobando que `/actuator/health` responda 200. Una **misma imagen** se construye una vez por commit y viaja a todos los ambientes.

## 4. Usar la API desplegada

```powershell
$URL = terraform output -raw api_url

# Salud
Invoke-RestMethod "$URL/actuator/health"

# Crear una franquicia (201). Repetir el nombre, aunque cambien las mayúsculas, da 409.
Invoke-RestMethod -Method Post -Uri "$URL/api/v1/franchises" -ContentType "application/json" -Body '{"name":"Mi franquicia"}'
```

Con `curl` desde Git Bash o Linux funciona igual: `curl -X POST "$URL/api/v1/franchises" -H "Content-Type: application/json" -d '{"name":"Mi franquicia"}'`.

Los demás endpoints (sucursales, productos, stock, producto con más stock, renombrar) están descritos con ejemplos en el
[README principal](../README.md). Solo cambia la dirección base: en la nube es `$URL` en lugar de `http://localhost:8080`.

En `dev` y `staging` la documentación interactiva está en `$URL/swagger-ui.html` y el JSON en `$URL/v3/api-docs`. En `prod` está apagada.
De todo el actuator, solo `health` es público: `/actuator/prometheus` y `/actuator/env` responden 404.

## 5. Comprobar la seguridad

```powershell
# Las DB_* deben aparecer solo en "secrets" de la task definition, nunca en "environment"
aws ecs describe-task-definition --task-definition reto-franquicias-dev --region us-east-2 `
  --query "taskDefinition.containerDefinitions[0].[environment,secrets[].name]"

# El rol de tarea no debe tener políticas propias (en dev, solo ecs-exec)
aws iam list-role-policies --role-name reto-franquicias-dev-task

# El rol de ejecución solo debe leer los secretos de su ambiente
aws iam get-role-policy --role-name reto-franquicias-dev-execution --policy-name read-db-secret
```

## 6. Costos y cómo apagar

Mientras un ambiente exista cobran RDS, el ALB y las tareas de Fargate. Para apagar `dev` o `staging`:

```powershell
terraform destroy "-var-file=envs/dev.tfvars" "-var=image_tag=$TAG"
```

Terraform solo borra lo que creó. Después revisa que no queden recursos sueltos (RDS, balanceadores, IPs elásticas, VPC) y, al día siguiente,
*Billing → Bills*. El bucket de estado y los roles de `bootstrap/` no cuestan y se conservan.

`prod` tiene protección contra borrado a propósito. Para destruirlo hay que apagarla primero con un `apply` que pase
`-var=db_deletion_protection=false` (después de `-var-file`) y luego hacer el `destroy`. Este camino no se ha probado todavía.

## Problemas frecuentes

| Síntoma | Causa |
|---|---|
| PowerShell parte `-target=module.ecr` | Escríbelo entre comillas, como arriba. |
| `docker login` a ECR da 400 | No uses una tubería en PowerShell: guarda el token en `$PW`. |
| `terraform apply` falla al subir una etiqueta ya usada | Las etiquetas de ECR son inmutables: usa un commit nuevo. |
| El ALB responde 502 o 503 los primeros 1–2 minutos | La tarea aún arranca (Spring + migración). Espera; si no se arregla, mira el log group `/ecs/reto-franquicias-<ambiente>`. |
| `terraform init` no encuentra el bucket | Pasa siempre `-backend-config="bucket=..."` y comprueba la cuenta con `aws sts get-caller-identity`. |
| Un job de GitHub dice `AccessDenied` | El rol del ambiente es de mínimo privilegio y puede faltarle un permiso: se ajusta en `bootstrap/main.tf`. |
| La app no conecta con RDS | PostgreSQL 16 exige TLS: la tarea necesita `DB_SSL=true` (viene del secreto de conexión). |

## Límites conocidos

- El servicio se publica solo por **HTTP**. HTTPS necesitaría un dominio y un certificado de ACM.
- La conexión a RDS va cifrada, pero **no verifica el certificado del servidor** (`NonValidatingFactory`).
- El usuario que ejecuta Terraform a mano es administrador de laboratorio. Los roles de GitHub sí son de mínimo privilegio, acotados por
  servicio: lo que AWS no deja limitar por recurso (por ejemplo `ec2:*`) queda abierto en la cuenta.
- El estado del `bootstrap/` vive solo en la máquina de quien lo aplicó.
- `staging` y `prod` no se han desplegado aún.
