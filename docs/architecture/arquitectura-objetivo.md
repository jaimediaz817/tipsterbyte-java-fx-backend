# Arquitectura Objetivo — tipsterbyte-fx-v2

> Visión de arquitectura que la FASE 4 implementará en estructura de paquetes y que la FASE 22 defenderá en entrevista. Se basa en **Clean Architecture** + **DDD** + **ports/adapters**.

---

## Capas y Dependency Rule

```
              ┌─────────────────────────────────────────┐
              │         INTERFACES (REST / Web)          │  ← controllers, DTOs request/response, exception handlers
              └──────────────────┬──────────────────────┘
                                 │  (depende de)
              ┌──────────────────▼──────────────────────┐
              │         APPLICATION (casos de uso)      │  ← use cases, ports (interfaces), application services
              └──────────────────┬──────────────────────┘
                                 │  (depende de)
              ┌──────────────────▼──────────────────────┐
              │         DOMAIN (núcleo)                 │  ← entities, value objects, aggregates, domain events, business rules
              └──────────────────┬──────────────────────┘
                                 │  (implementa)
              ┌──────────────────▼──────────────────────┐
              │         INFRASTRUCTURE (adaptadores)    │  ← JPA, PostgreSQL, APIs externas, Redis, RabbitMQ
              └─────────────────────────────────────────┘
```

**Dependency Rule**: las dependencias siempre apuntan hacia adentro (el dominio no sabe nada del mundo exterior). El dominio NO conoce JPA, Spring, ni las APIs externas.

---

## Flujo de una petición (ej: CU-08 Consultar pronósticos)

```
HTTP
 ↓
Controller (interfaces)  → valida request DTO (FASE 7)
 ↓
UseCase (application)    → orquesta reglas (FASE 6)
 ↓
Domain (dominio)         → aplica reglas de negocio BR-006 (FASE 5)
 ↓
Port (application)       → interface PronosticoRepository / SuscripcionRepository
 ↓
Adapter JPA (infra)      → PostgreSQL (FASE 8)
 ↓
Response DTO → HTTP
```

---

## Ports / Adapters con las 4 APIs

### Puertos de persistencia (definidos en application)

| Puerto | Adapter (infra) | Tecnología |
| --- | --- | --- |
| `LigaRepository` | `LigaRepositoryJpaAdapter` | Spring Data JPA |
| `PartidoRepository` | `PartidoRepositoryJpaAdapter` | Spring Data JPA |
| `PronosticoRepository` | `PronosticoRepositoryJpaAdapter` | Spring Data JPA |
| `SuscripcionRepository` | `SuscripcionRepositoryJpaAdapter` | Spring Data JPA |

### Puertos de proveedores externos (adapters a las 4 APIs)

```
ProveedorPosiciones (port)
 ├── FootballDataPosicionesAdapter   → football-data.org
 └── ApiFootballPosicionesAdapter    → API-Football

ProveedorCalendario (port)
 ├── ApiFootballCalendarioAdapter    → API-Football
 └── FootballDataCalendarioAdapter   → football-data.org

ProveedorCuotas (port)
 ├── ApiFootballCuotasAdapter        → API-Football   (principal)
 ├── TheOddsApiCuotasAdapter         → The Odds API   (backup)
 └── SharpApiCuotasAdapter           → SharpAPI       (backup 2)
```

> **Por qué ports/adapters**: cambiar de proveedor (o agregar uno) no toca dominio ni casos de uso. Esto se defenderá en entrevista: "si mañana football-data.org sube precios, solo cambia un adapter".

---

## Mapeo de responsabilidad por capa

| Capa | Paquete conceptual | Contenido | Fase |
| --- | --- | --- | --- |
| Domain | `domain.model`, `domain.service`, `domain.event` | Entities, VOs, aggregates, reglas, eventos | 5 |
| Application | `application.usecase`, `application.port`, `application.dto` | Casos de uso, ports, DTOs | 6 |
| Interfaces | `interfaces.rest` | Controllers, request/response, exception handlers | 7 |
| Infrastructure | `infrastructure.persistence`, `infrastructure.adapter`, `infrastructure.config` | JPA, adapters APIs, config | 8+ |

---

## Mapa de tecnologías por fase (visión final)

```
FASE 2   Spring Boot + Gradle + Java          (bootstrap)
FASE 7   REST API (Spring Web)                 (interfaces)
FASE 8   PostgreSQL + JPA                      (persistencia)
FASE 9   Docker Compose (postgres/redis/rabbitmq)
FASE 10  Testing (unit + integration + testcontainers)
FASE 11  Spring Security + JWT                 (auth)
FASE 12  Redis                                 (cache)
FASE 13  RabbitMQ                              (eventos/notificaciones)
FASE 14  Spring WebFlux (Mono/Flux, comparativa MVC)
FASE 15  Scheduling / Async
FASE 16  Observabilidad (Actuator, Micrometer, Prometheus, Grafana)
FASE 18  Dockerizar la aplicación
FASE 19  CI/CD (GitHub Actions)
FASE 20  AWS / Deployment
```

---

## Decisiones de arquitectura (ADR — se formaliza en FASE 4)

| # | Decisión | Justificación corta |
| --- | --- | --- |
| ADR-001 | Clean Architecture + DDD | Separación clara por capas, dominio protegido, defendible en entrevista |
| ADR-002 | Ports/adapters para las 4 APIs | Abstracción de proveedores externos, cambio sin impacto al núcleo |
| ADR-003 | Java + Spring Boot + Gradle | Estandar del proceso de selección + coherencia con proyecto previo |
| ADR-004 | Núcleo funcional pequeño (9 casos de uso) | Evita sobreingeniería; las demás features entran por fases |