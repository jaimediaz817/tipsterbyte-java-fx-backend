---
name: spring-boot-engineering
description: Use when working with Spring Boot in this repository: configuration, dependency management, dependency injection, profiles, Actuator, and application wiring. Refer to the phase roadmap before adding dependencies.
---

# Spring Boot Engineering — tipsterbyte-fx-v2

## Cuándo usar

Al configurar Spring Boot, agregar dependencias, definir beans, profiles o Actuator. Complementa `.opencode/rules/architecture.md`.

## Reglas clave

- **Dependency injection por constructor** (no field injection). Facilita tests y respeta la inmutabilidad.
- **Versión Java/Spring Boot se congela en FASE 2** (`docs/PROYECTO-PLAN.md`). No agregar dependencias fuera de su fase.
- Dependencias por fase:
  - FASE 2: Web, Validation, Actuator, Testing (mínimas).
  - FASE 8: JPA + PostgreSQL.
  - FASE 11: Security + JWT.
  - FASE 12: Redis.
  - FASE 13: RabbitMQ.
  - FASE 14: WebFlux (R2DBC si procede).
  - FASE 16: Micrometer + Prometheus.
- Cada dependencia nueva debe documentarse con `[POR QUÉ]` y la alternativa descartada (rule `documentation.md`).

## Configuración

- `application.yml` base + perfiles `dev`/`prod`. Secrets vía variables de entorno, nunca en el repo.
- Actuator expone `/actuator/health`; se amplía en FASE 16 (metrics/traces).
- Beans de infraestructura (adapters, mappers) se definen como `@Component`/`@Configuration`; los casos de uso como beans de application.

## Cheat-sheet de entrevista (preparación FASE 22)

- **Por qué constructor injection**: testabilidad, inmutabilidad, evita estados parciales.
- **Por qué perfiles**: separar config dev/prod sin tocar código.
- **Por qué Actuator desde el inicio**: observabilidad temprana, base para FASE 16.