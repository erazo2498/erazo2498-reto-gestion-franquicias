# Reto Gestión de Franquicias

API REST **reactiva** para gestionar una red de franquicias, sus sucursales y sus productos, construida con arquitectura hexagonal
(scaffold de Clean Architecture de Bancolombia).

## Estado del proyecto

| Funcionalidad | Estado |
|---|---|
| Crear franquicia (`POST /api/v1/franchises`) | Hecho |
| Sucursales, productos, stock, producto con más stock por sucursal, renombrar | Pendiente |
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

Al arrancar, Liquibase crea la tabla `franchise` (migración `001`) y el log termina con `Netty started on port 8080`.

### 3. Probar

- **Swagger UI:** <http://localhost:8080/swagger-ui.html> (solo con los perfiles `local`, `dev` o `staging`).
- **Health:** <http://localhost:8080/actuator/health>
- **Archivo `http/franchises.http`:** ejemplos listos para el cliente HTTP de IntelliJ. Elige el entorno `local` en el selector del editor.
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

### Errores

Todos los errores usan el mismo formato: `{ "code": "...", "message": "...", "timestamp": "..." }`.

| HTTP | `code` | Cuándo |
|---|---|---|
| `400` | `INVALID_DATA` | Nombre ausente, nulo, en blanco o de más de 120 caracteres; JSON mal formado; nombre que no es texto; sin cuerpo |
| `409` | `DUPLICATE_NAME` | Ya existe una franquicia con ese nombre (ignorando mayúsculas) |
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
