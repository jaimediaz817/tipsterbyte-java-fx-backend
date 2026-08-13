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

### FASE 3 — Git + estructura base
- `.gitignore`, `README.md`, primer commit, package base.

### FASE 4 — Clean Architecture
- Capas: domain / application / infrastructure / interfaces.
- Dependency Rule. Formalizar ADRs.

### FASE 5 — Domain + DDD
- Implementar el modelo de `docs/domain/modelo-dominio.md`.
- Entities, VOs, Aggregates, reglas de negocio, domain events.

### FASE 6 — Application + Use Cases
- Implementar casos de uso de `docs/use-cases/casos-de-uso.md`.
- Ports (interfaces) + use cases.

### FASE 7 — REST API
- Controllers, DTOs, validación, status codes, manejo global de errores.

### FASE 8 — PostgreSQL + JPA
- Repository Ports → Adapters JPA. Entities mapping, relaciones, constraints, transacciones.

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