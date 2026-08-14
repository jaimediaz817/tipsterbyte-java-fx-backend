# Plan Maestro — tipsterbyte-fx-v2

> Secuencia completa de 22 fases. Avanzamos **una fase a la vez**, con aprobación explícita del usuario entre fases. No programamos por adelantado.

---

## Mapa de fases

```
FASE 0  Definición del proyecto
FASE 1  OpenCode / Cline: Rules + Skills
FASE 2  Spring Initializr: bootstrap del proyecto
FASE 3  Git + estructura base
FASE 4  Clean Architecture
FASE 5  Domain + DDD
FASE 6  Application + Use Cases
FASE 7  REST API
FASE 8  PostgreSQL + JPA
FASE 8.5  Adapters de fuentes externas (4 endpoints reales)
FASE 9  Docker + Docker Compose
FASE 10 Testing
FASE 11 Spring Security + JWT
FASE 12 Redis
FASE 13 RabbitMQ
FASE 14 Spring WebFlux
FASE 15 Scheduling / Async
FASE 16 Observabilidad
FASE 17 Integración completa
FASE 18 Dockerización de aplicación
FASE 19 CI/CD
FASE 20 AWS / Deployment
FASE 21 Architecture Review
FASE 22 Preparación para entrevista
```

---

## Detalle por fase

### FASE 0 — Definición del proyecto ✅
- Nombre, problema, usuarios, funcionalidades, dominio (DDD), casos de uso.
- **Resultado**: `README.md`, `docs/project-definition.md`, `docs/domain/`, `docs/use-cases/`, `docs/architecture/`.
- **Estado**: COMPLETADA.

### FASE 1 — OpenCode / Cline: Rules + Skills ✅
- Crear `.opencode/rules/` (architecture, java, documentation, naming, testing).
- Crear `.opencode/skills/` iniciales: java-engineering, spring-boot-engineering, clean-architecture, ddd-domain-modeling, java-documentation, architecture-review.
- `opencode.json` registra las rules y skills. `AGENTS.md` creado como memoria de sesión.
- **Estado**: COMPLETADA. No se escribe código de la app todavía.

### FASE 2 — Spring Initializr ✅
- Java + Gradle + Spring Boot.
- Dependencias mínimas: Web (webmvc), Validation, Actuator, Testing. (No Security/Redis/RabbitMQ/JPA el primer día.)
- **Configuración cerrada**: Gradle 9.5.1 (wrapper), Java 21, Spring Boot 4.1.0, paquete `com.tipsterbyte.tipsterbytefxv2`.
- **Estado**: COMPLETADA — `./gradlew build` OK, `bootRun` OK, Actuator `/actuator/health` responde UP.

### FASE 3 — Git + estructura base ✅
- `.gitignore` (del inicializador, verificado), `README.md`, primer commit, package base `com.tipsterbyte.tipsterbytefxv2`.
- Rama `main`, autor `JDiaz`.
- **Estado**: COMPLETADA — commit inicial `83bad74`.

### FASE 4 — Clean Architecture ✅
- Capas: domain / application / infrastructure / interfaces con `package-info.java` documentado.
- Dependency Rule. `DomainException` (ADR-005), primer VO `Cuota` (patrón), puertos `ProveedorPosiciones/Calendario/Cuotas`.
- ADRs formalizados en `docs/architecture/arquitectura-objetivo.md` (ADR-001..005).
- **Estado**: COMPLETADA — `./gradlew build` OK.

### FASE 5 — Domain + DDD ✅
- Implementar el modelo de `docs/domain/modelo-dominio.md`.
- Entities, VOs, Aggregates, reglas de negocio, domain events.
- **Resultado**: enums (`EstadoLiga`, `EstadoPartido`, `EstadoPronostico`, `EstadoSuscripcion`, `Mercado`, `Rol`), VOs (`Temporada`, `Resultado`, `Cuota`, `PosicionTabla`, `SeleccionPronostico`, `Email`, `Plan`, `FechaProgramada`), entities (`Equipo`, `Tipster`, `Cliente`), aggregates (`Liga`, `Partido`, `Pronostico`, `Suscripcion`) con BR-001..008, eventos (`LigaActivada`, `PartidoProgramado`, `CuotaActualizada`, `PronosticoPublicado`, `SuscripcionCreada`) + interfaz `DomainEvent`.
- Referencias por id entre aggregates; eventos recolectados con `pullEventos()` para publicarse en FASE 13.
- **Estado**: COMPLETADA — 52 tests unitarios en verde, `./gradlew build` OK.

### FASE 6 — Application + Use Cases ✅
- Implementar casos de uso de `docs/use-cases/casos-de-uso.md`.
- Ports (interfaces) + use cases.
- **Resultado**: 9 casos de uso en `application.usecase` (CU-01..09), puertos completados `ProveedorPosiciones/Calendario/Cuotas` + puertos nuevos `LigaRepository`, `PartidoRepository`, `PronosticoRepository`, `SuscripcionRepository`, y DTOs en `application.dto` (`PosicionFuente`, `PartidoFuente`, `CuotaFuente`, `DisponibilidadFuentes`, `CrearPronosticoComando`, `PronosticoPublicoDto`).
- **Estado**: COMPLETADA — 80 tests en verde (28 nuevos de casos de uso con Mockito), `./gradlew build` OK.

### FASE 7 — REST API ✅
- Controllers, DTOs, validación, status codes, manejo global de errores.
- **Resultado**: 4 controllers en `interfaces.rest.controller` (`LigaController`, `PartidoController`, `PronosticoController`, `SuscripcionController`) con los 9 endpoints de CU-01..09, request DTOs con Bean Validation en `interfaces.rest.dto.request`, response DTOs en `interfaces.rest.dto.response`, y `GlobalExceptionHandler` (`@RestControllerAdvice`: DomainException → 422, validación/params/JSON → 400, resto → 500).
- Los beans de los controllers se registran solo si `app.api.rest.enabled=true` (FASE 8 habilita el wiring); hoy se ejercitan con MockMvc standalone.
- **Estado**: COMPLETADA — 96 tests en verde (16 nuevos de controllers), `./gradlew build` OK, `bootRun` OK.

### FASE 8 — PostgreSQL + JPA
- Repository Ports → Adapters JPA. Entities mapping, relaciones, constraints, transacciones.

### FASE 8.5 — Adapters de fuentes externas (4 endpoints reales)
- **Documentar primero** las 4 fuentes de extracción reales del usuario (cómo funcionan, qué devuelve cada endpoint) y **decidir el mapeo** a los puertos del modelo (`ProveedorPosiciones/Calendario/Cuotas`), aún sin cerrar si conviven con las APIs originales o las reemplazan. El usuario entrega la respuesta real (JSON) de cada endpoint cuando se necesita; **no asumir formatos**.
- Definir DTOs de fuente en `application.dto` a partir de las respuestas reales.
- Extender dominio si procede: modelar "últimos 5 resultados por equipo" (clave para predicción) y nuevo caso de uso CU-10 para poblar catálogo de países y ligas (fuente #1).
- Implementar 4 adapters en `infrastructure.adapter` contra los endpoints reales, con tests usando las respuestas reales como fixtures.
- Actualizar ADRs y `docs/architecture/arquitectura-objetivo.md` según el mapeo decidido.

### FASE 9 — Docker
- PostgreSQL en docker-compose. Volumes, networks, healthcheck.

### FASE 10 — Testing
- Unit (domain, use cases), Integration (app, infra), Controller tests, Testcontainers.

### FASE 11 — Spring Security + JWT
- Login, autenticación, JWT, roles/permisos, hashing de contraseñas, filters.

### FASE 12 — Redis
- Cache-aside para lecturas de alta frecuencia. TTL, invalidación.

### FASE 13 — RabbitMQ
- Event-driven: domain events → exchange → queue → consumer (notificaciones).

### FASE 14 — Spring WebFlux
- Mono/Flux, reactive streams, backpressure. Comparativa MVC vs WebFlux.

### FASE 15 — Scheduling / Async
- `@Scheduled`, `@Async`, executors, thread pools. Conexión con RabbitMQ/Redis/WebFlux.

### FASE 16 — Observabilidad
- Actuator, Micrometer, Prometheus, Grafana, correlation ID, structured logging.

### FASE 17 — Integración completa
- Unir REST + UseCase + Domain + PostgreSQL + Redis + RabbitMQ + Consumer.

### FASE 18 — Dockerizar la aplicación
- Dockerfile multi-stage + docker-compose completo (app + postgres + redis + rabbitmq).

### FASE 19 — CI/CD
- GitHub Actions: build, tests, imagen Docker, deploy.

### FASE 20 — AWS / Deployment
- Evaluar arquitectura de deployment (solo cuando todo funcione localmente).

### FASE 21 — Architecture Review
- Auditoría completa usando el skill `architecture-review`.
- Salidas: CRITICAL / WARNING / OBSERVATION / GOOD / RECOMMENDATION.

### FASE 22 — Preparación para entrevista
- Por cada concepto: qué es, por qué, qué problema resuelve, alternativas, ventajas, desventajas, cuándo NO usarlo, cómo lo implementamos, cómo defenderlo en entrevista.

---

## Orden de trabajo (regla de oro)

1. Contexto maestro entregado.
2. Rules configuradas.
3. Skills creados.
4. Se pide: "Ejecuta FASE N. No programes todavía."
5. Se revisa la propuesta.
6. Se aprueba.
7. Se ejecuta la siguiente fase.

> **Nunca** se pide "construye el proyecto completo".