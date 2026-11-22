# Reto Gestión de Franquicias

API REST **reactiva** para gestionar una red de franquicias, sus sucursales y sus productos, construida con arquitectura hexagonal
(scaffold de Clean Architecture de Bancolombia).

## Estado del proyecto

| Funcionalidad | Estado |
|---|---|
| Crear franquicia (`POST /api/v1/franchises`) | Hecho |
| Agregar sucursal (`POST /api/v1/franchises/{franchiseId}/branches`) | Hecho |
| Agregar producto (`POST /api/v1/branches/{branchId}/products`) | Hecho |
| Eliminar producto (`DELETE /api/v1/branches/{branchId}/products/{productId}`) | Hecho |
| Modificar stock, producto con más stock por sucursal, renombrar | Pendiente |
| Resiliencia (timeout, reintentos, circuit breaker) | Pendiente |
| Imagen Docker, despliegue en AWS con Terraform | Pendiente |

## Stack

Java 17 · Spring Boot 4 + WebFlux (Reactor) · PostgreSQL 16 con R2DBC · Liquibase (migraciones en SQL) · Lombok · springdoc (OpenAPI)
· Gradle · JUnit 5, Mockito, `StepVerifier`, ArchUnit, Testcontainers, JaCoCo.

## Prerrequisitos

- **Java 17** (JDK).
- **Docker Desktop** en ejecución: levanta la base de datos local y las pruebas de integración la usan (Testcontainers).
- Git. No hace falta instalar Gradle: el proyecto trae el wrapper (`gradlew`).

## Puesta en marcha

### 1. Base de datos local

```powershell
Copy-Item .env.example .env        # en Linux/macOS: cp .env.example .env
```

Abre `.env` y cambia `DB_PASSWORD` por una contraseña tuya. El archivo `.env` está ignorado por Git: nunca se sube.

```powershell
docker compose up -d
```

Crea el contenedor `franquicias-postgres` (PostgreSQL 16) y publica su puerto en **`localhost:5433`**
(el 5433 evita chocar con otro PostgreSQL que ya use el 5432).

> **Importante:** `docker compose` es lo único que lee el `.env`. La aplicación **no** lo lee: recibe las variables del entorno (paso siguiente).
> Si cambias la contraseña en `.env` después de haber creado el contenedor, PostgreSQL conserva la antigua en su volumen:
> recrea la base con `docker compose down -v` y `docker compose up -d` (esto borra los datos locales).

### 2. Arrancar la API

La aplicación necesita estas variables de entorno. No tienen valores por defecto a propósito: sin ellas no arranca.

| Variable | Valor local |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5433` |
| `DB_NAME` | `franquicias` |
| `DB_USERNAME` | `franquicias` |
| `DB_PASSWORD` | la que pusiste en `.env` |

Además, activa el perfil `local` para que Swagger esté disponible.

**Desde IntelliJ:** *Run → Edit Configurations →* `MainApplication`; en *Modify options* activa *Environment variables* y *Active profiles*:

```
DB_HOST=localhost;DB_PORT=5433;DB_NAME=franquicias;DB_USERNAME=franquicias;DB_PASSWORD=tu_contraseña
```

Perfil activo: `local`.

**Desde la terminal (PowerShell):** carga las variables de `.env` en esa terminal y arranca con Gradle.

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#\s][^=]*=' } | ForEach-Object { $k,$v = $_ -split '=',2; Set-Item "env:$($k.Trim())" $v.Trim() }
./gradlew :app-service:bootRun --args='--spring.profiles.active=local'
```

**Desde la terminal (Linux/macOS):**

```bash
set -a; source .env; set +a
./gradlew :app-service:bootRun --args='--spring.profiles.active=local'
```

Al arrancar, Liquibase crea las tablas `franchise`, `branch` y `product` (migraciones `001`, `002` y `003`) y el log termina con `Netty started on port 8080`.

### 3. Probar

- **Swagger UI:** <http://localhost:8080/swagger-ui.html> (solo con los perfiles `local`, `dev` o `staging`).
- **Health:** <http://localhost:8080/actuator/health>
- **Archivos `http/franchises.http`, `http/branches.http` y `http/products.http`:** ejemplos listos para el cliente HTTP de IntelliJ. Elige el
  entorno `local` en el selector del editor. En `branches.http` ejecuta primero la petición 1 y en `products.http` las peticiones 1 y 2: crean la
  franquicia y la sucursal que usan las demás.
- **`curl`:**

```bash
curl -i -X POST http://localhost:8080/api/v1/franchises \
  -H "Content-Type: application/json" \
  -d '{"name": "Burger Co"}'
```

## API

### `POST /api/v1/franchises`: crear una franquicia

Petición:

```json
{ "name": "Burger Co" }
```

- El nombre se recorta (se eliminan los espacios de los extremos) y debe tener **entre 1 y 120 caracteres**.
- Debe ser **único sin distinguir mayúsculas ni minúsculas** (`Burger Co` y `BURGER CO` son el mismo nombre; también con `Ñ` y tildes).
- Debe ser texto: un número como `123` se rechaza.

Respuesta `201`:

```json
{ "id": "3f2b8c1e-7a4d-4e59-9c0a-1d6e5b8f2a47", "name": "Burger Co" }
```

### `POST /api/v1/franchises/{franchiseId}/branches`: agregar una sucursal a una franquicia

Petición (`franchiseId` es el id de una franquicia existente):

```json
{ "name": "Centro" }
```

- El nombre sigue las mismas reglas que el de la franquicia: se recorta, de **1 a 120 caracteres** y debe ser texto.
- Es **único dentro de su franquicia, sin distinguir mayúsculas**: dos franquicias distintas pueden tener cada una una sucursal `Centro`.
- Si `franchiseId` no es un UUID responde `400`; si es un UUID que no existe, `404`.
- Una franquicia con sucursales no se puede borrar (la base de datos lo impide); hoy no hay endpoint para borrar franquicias.

Respuesta `201`:

```json
{ "id": "9a1c4e72-5b3d-4f08-8e61-2c7d0a9b3f15", "franchiseId": "3f2b8c1e-7a4d-4e59-9c0a-1d6e5b8f2a47", "name": "Centro" }
```

### `POST /api/v1/branches/{branchId}/products`: agregar un producto a una sucursal

Petición (`branchId` es el id de una sucursal existente):

```json
{ "name": "Hamburguesa", "stock": 10 }
```

- El nombre sigue las mismas reglas que los anteriores: se recorta, de **1 a 120 caracteres** y debe ser texto.
- Es **único dentro de su sucursal, sin distinguir mayúsculas**: dos sucursales distintas pueden tener cada una un producto `Hamburguesa`.
- El `stock` es **obligatorio** y debe ser un **número entero mayor o igual que 0**. Se rechazan (con `400`) un valor ausente o `null`, los
  negativos, los decimales (`1.7`), el texto (`"5"`) y los números mayores que el máximo de un entero; nada se convierte ni se corta en silencio.
- Si `branchId` no es un UUID responde `400`; si es un UUID que no existe, `404`.
- Una sucursal con productos no se puede borrar (la base de datos lo impide); hoy no hay endpoint para borrar sucursales.

Respuesta `201`:

```json
{ "id": "5d7e2a91-3c4b-4a6f-9b18-0e2f7c6d4a53", "branchId": "9a1c4e72-5b3d-4f08-8e61-2c7d0a9b3f15", "name": "Hamburguesa", "stock": 10 }
```

### `DELETE /api/v1/branches/{branchId}/products/{productId}`: eliminar un producto de una sucursal

Sin cuerpo en la petición. Respuesta `204` sin cuerpo.

- El borrado es **idempotente**: responde `204` tanto si el producto existía como si no, y repetirlo deja el mismo estado y la misma respuesta.
  Por eso este endpoint **no tiene `404`**.
- Solo se borra si el producto pertenece a la sucursal de la ruta: un producto de **otra** sucursal no se toca (y también se responde `204`).
- Si `branchId` o `productId` no son un UUID responde `400`.
- El nombre del producto borrado queda libre: se puede crear otro con el mismo nombre en esa sucursal.

### Errores

Todos los errores usan el mismo formato: `{ "code": "...", "message": "...", "timestamp": "..." }`.

| HTTP | `code` | Cuándo |
|---|---|---|
| `400` | `INVALID_DATA` | Nombre ausente, nulo, en blanco o de más de 120 caracteres; stock ausente, negativo, decimal o que no es un entero; JSON mal formado; nombre que no es texto; sin cuerpo; `franchiseId`, `branchId` o `productId` que no son un UUID |
| `404` | `FRANCHISE_NOT_FOUND` | La franquicia a la que se quiere agregar la sucursal no existe |
| `404` | `BRANCH_NOT_FOUND` | La sucursal a la que se quiere agregar el producto no existe |
| `409` | `DUPLICATE_NAME` | Ya existe una franquicia con ese nombre, una sucursal con ese nombre en esa franquicia o un producto con ese nombre en esa sucursal (ignorando mayúsculas) |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | El `Content-Type` no es `application/json` |
| `500` | `INTERNAL_ERROR` | Error inesperado; el detalle solo se escribe en el log del servidor |

## Pruebas y cobertura

```powershell
./gradlew build
```

Compila, ejecuta todas las pruebas (incluidas las de integración con PostgreSQL en Testcontainers, por eso Docker debe estar en marcha) y verifica
las reglas de arquitectura y de código. El mismo comando lo ejecuta el CI en cada pull request.

- **Cobertura:** el build falla si la cobertura de líneas del conjunto de módulos baja del **70 %**. El reporte HTML queda en
  `build/reports/jacoco/jacocoAggregatedReport/html/index.html`. Para probar otro mínimo: `./gradlew jacocoCoverageCheck -PcoverageMinimum=0.9`.
- **Reglas automáticas** (`applications/app-service/src/test/java/com/pragma/franquicias/rules`): el dominio no depende de Spring ni de la
  infraestructura, no hay controllers (solo `RouterFunction` y handlers), y el código fuente no usa `.block()`, `java.util.stream`,
  `try/catch` en flujos, `if` en flujos ni `javax.*`, ni contiene secretos.
- **Caché de configuración de Gradle:** si generas un módulo nuevo con el plugin y `./gradlew build` no lo toma, ejecútalo una vez con
  `--no-configuration-cache`.

## Arquitectura

Dependencias hacia adentro: la infraestructura implementa los puertos que define el dominio, nunca al revés.

| Módulo | Contenido |
|---|---|
| `domain/model` | Entidades (Lombok `@Builder`, inmutables y que se validan al construirse) y puertos (`SaveFranchiseGateway`) |
| `domain/usecase` | Casos de uso: Java puro + Reactor (`CreateFranchiseUseCase`) |
| `infrastructure/driven-adapters/r2dbc-postgresql` | Adaptador de persistencia (`FranchiseAdapter`) y pool de conexiones |
| `infrastructure/entry-points/reactive-web` | `RouterFunction`, handlers, manejo de errores y documentación OpenAPI |
| `applications/app-service` | Arranque, configuración y migraciones Liquibase (`src/main/resources/db/changelog`) |
| `deployment/` | `Dockerfile` (plantilla del plugin, aún sin probar) |

Decisiones que conviene conocer:

- **La unicidad del nombre la garantiza la base de datos** (índice único sobre `lower(name)`), no una consulta previa: así dos peticiones
  simultáneas con el mismo nombre no pueden crear dos franquicias; una gana y la otra recibe `409`.
- **Liquibase usa JDBC y corre una sola vez al arrancar**, antes de atender peticiones. La API en sí es 100 % reactiva (R2DBC).
- **Las migraciones no se editan una vez aplicadas:** cada cambio de esquema es un archivo nuevo en `db/changelog/changes/`.
- **El actuator solo expone `health`.** Swagger y `/v3/api-docs` están apagados por defecto y en `prod`.

Para generar piezas nuevas se usan las tareas del plugin: `./gradlew gm` (modelo), `guc` (caso de uso), `gda` (adaptador de salida) y
`gep` (entry point).

## Flujo de trabajo

Ramas `main`, `develop` y `feature/NNN-nombre` (una por funcionalidad, desde `develop`, integrada con un pull request).
Commits atómicos y convencionales (`feat:`, `test:`, `docs:`, `build:`, `chore:`…). Cada funcionalidad se desarrolla con pruebas primero
(`StepVerifier`), viéndolas fallar antes de escribir el código.
