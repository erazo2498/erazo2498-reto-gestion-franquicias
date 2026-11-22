# Reto Gestión de Franquicias — API reactiva

API REST **reactiva** para gestionar franquicias → sucursales → productos, con arquitectura hexagonal (scaffold de Bancolombia).
Despliegue en AWS con Terraform en fases posteriores. Todo el código debe poder explicarse línea por línea.

## Stack
Java 17, Spring Boot + WebFlux (Reactor), Lombok, Gradle multi-módulo (plugin `co.com.bancolombia.cleanArchitecture`),
PostgreSQL con R2DBC y Liquibase. Pruebas: JUnit 5, Mockito, `StepVerifier`, ArchUnit, JaCoCo.

## Estructura
| Módulo | Contenido |
|---|---|
| `domain/model` | Entidades (Lombok `@Builder`) y puertos (gateways) |
| `domain/usecase` | Casos de uso: Java puro + Reactor |
| `infrastructure/driven-adapters` | Adaptadores de salida (r2dbc) |
| `infrastructure/entry-points` | `reactive-web`: `RouterFunction` + `Handler` |
| `applications/app-service` | Arranque, configuración y cableado de beans |
| `deployment/` | Dockerfile |

Las piezas nuevas se generan con las tareas del plugin (`./gradlew gep`, `gda`, `guc`), no a mano.

## Cómo trabajar
- Responder **en español**. El código y sus comentarios van en **inglés**. Sin comentarios obvios.
- **Un feature = una rama `feature/NNN-nombre` desde `develop` = un PR pequeño.** Commits atómicos y convencionales
  (`tipo(alcance): descripción`). Nunca commit directo en `main` ni `develop`.
- **Ir paso a paso con la persona en cada feature:** modelo (diagrama) → criterios de aceptación → test en rojo → mínimo código en verde → PR.
- Lo que no está definido se pregunta; no se inventa.
- **TDD** en dominio y casos de uso con `StepVerifier`. Nunca se debilita un test para que pase.
- Verificar con `./gradlew build` antes de cerrar un feature. El CI ejecuta lo mismo.
- `terraform apply` y `destroy` los ejecuta una persona.

## Convenciones del código (las comprueban los tests de `rules/`)
- Rutas con `RouterFunction` + `Handler`; nunca controllers.
- Errores con `onErrorResume` / `onErrorMap`; condicionales con `filter` / `switchIfEmpty`; `flatMap` si la función devuelve un `Publisher`, `map` si no.
- Sin `.block()`, sin `java.util.stream`, sin `try/catch` en flujos, `jakarta.*` y no `javax.*`.
- El dominio no depende de Spring ni de infraestructura; los puertos viven en el dominio, los adaptadores en infraestructura.
- Entidades con `@Builder`; DTOs como `record`.

## Secretos
Nunca en el código ni en variables de entorno en claro. El `.env` real no se lee ni se edita; las variables nuevas se documentan en `.env.example`.
