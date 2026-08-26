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

## Contrato HTTP: formato de respuestas (estandarizado en FASE 12 / FASE T1-b)

> Toda respuesta de la API, sea de éxito o de error, sigue un formato predecible que el frontend Angular (y cualquier otro cliente) puede consumir sin parseos ad-hoc.

### Respuestas de éxito

| Status | Cuándo usar | Body |
|--------|-------------|------|
| `200 OK` | Consulta exitosa (GET, PUT sin cambio de representación) | DTO de respuesta o lista de DTOs (`LigaResponse`, `PartidoResponse[]`, etc.) |
| `201 Created` | Recurso creado exitosamente (POST) | DTO del recurso creado (`AuthResponse`, `SuscripcionResponse`, `RecursoCreadoResponse`) + header `Location` cuando aplica |
| `204 No Content` | Operación exitosa sin representación que devolver (POST/PUT de transición) | Vacío |

**Reglas:**
- Los DTOs de respuesta viven obligatoriamente en `interfaces.rest.dto.response`. Nunca se expone un `application.dto` ni un aggregate de dominio directamente al HTTP.
- Las listas se devuelven como arrays JSON directos (sin wrapper `{ "data": [...] }`) para mantener el contrato ligero.

### Respuestas de error

Toda respuesta no 2xx usa el mismo schema `ApiError`:

```json
{
  "timestamp": "2026-08-15T20:30:00Z",
  "status": 422,
  "error": "Unprocessable Content",
  "mensaje": "Liga no activable: fuentes de datos no operativas (BR-001)",
  "path": "/api/v1/ligas/abc-123/activacion"
}
```

| Status | Cuándo ocurre | Origen |
|--------|---------------|--------|
| `400 Bad Request` | JSON malformado, tipo incorrecto, enum inválido, validación de `@Valid` fallida, o query param obligatorio ausente | `GlobalExceptionHandler` |
| `401 Unauthorized` | Sin token, token expirado, o firma inválida | `ApiErrorAuthenticationEntryPoint` (Spring Security) |
| `403 Forbidden` | Token válido pero rol no autorizado para el path | `ApiErrorAccessDeniedHandler` (Spring Security) |
| `422 Unprocessable Content` | Violación de regla de negocio (`DomainException`, BR-001..008) | `GlobalExceptionHandler` |
| `500 Internal Server Error` | Excepción no esperada (bug) | `GlobalExceptionHandler` |

**Reglas:**
- Spring Security NO devuelve HTML/texto plano en 401/403: ambos usan handlers custom que escriben `ApiError` en JSON.
- `DomainException` nunca se propaga cruda; siempre se traduce a 422.

---

## Decisiones de arquitectura (ADR — formalizados en FASE 4)

### ADR-001 — Clean Architecture + DDD
- **Concepto**: Organizar el sistema en 4 capas (domain, application, interfaces, infrastructure) con Dependency Rule hacia el dominio.
- **Artefacto**: paquetes raíz `domain`, `application`, `interfaces`, `infrastructure` + `package-info.java`.
- **Decisión**: Separación clara por capas; el dominio no conoce Spring/JPA/APIs. Defendible en entrevista.
- **Alternativas descartadas**: arquitectura por capas "tradicional" (controller-service-repository) sin dominio protegido.

### ADR-002 — Ports/adapters para las 4 APIs
- **Concepto**: Las 4 APIs (football-data.org, API-Football, The Odds API, SharpAPI) se consumen solo vía puertos.
- **Artefacto**: `ProveedorPosiciones`, `ProveedorCalendario`, `ProveedorCuotas` (application.port).
- **Decisión**: Cambiar de proveedor = ajustar un adapter, sin tocar dominio ni casos de uso.
- **Alternativas descartadas**: usar RestTemplate/WebClient directo en application (acopla la app a infraestructura).

### ADR-003 — Java + Spring Boot + Gradle
- **Concepto**: Stack base del proyecto.
- **Artefacto**: `build.gradle` (Gradle 9.5.1, Java 21, Spring Boot 4.1.0).
- **Decisión**: Estandar del proceso de selección + coherencia con el proyecto previo (`contexto_java/pronosticos-futbol`).
- **Alternativas descartadas**: Maven (correcto, pero el estándar evaluado es Gradle).

### ADR-004 — Núcleo funcional pequeño (9 casos de uso)
- **Concepto**: Solo las funcionalidades que demuestran arquitectura: ingesta, ligas/partidos, pronósticos, suscripciones.
- **Artefacto**: catálogo CU-01..09 (`docs/use-cases/casos-de-uso.md`).
- **Decisión**: Evita sobreingeniería; Security/Redis/RabbitMQ/WebFlux entran por fases cuando se necesitan.
- **Alternativas descartadas**: 30 funcionalidades el día uno (proyecto inflado, decisiones sin defender).

### ADR-005 — Excepciones de dominio unchecked
- **Concepto**: `DomainException` extiende `RuntimeException`.
- **Artefacto**: `domain/DomainException.java`.
- **Decisión**: No ensucia los casos de uso con try/catch; el handler global de interfaces las traduce a HTTP.
- **Alternativas descartadas**: checked exceptions (acoplan manejo obligatorio), excepciones de Spring en dominio (acoplamiento a framework).

### ADR-006 — Reconstrucción de aggregates sin emitir eventos (FASE 8)
- **Concepto**: Los aggregates exponen factory methods estáticos `reconstruir(...)` para hidratarse desde persistencia.
- **Artefacto**: `Liga.reconstruir`, `Partido.reconstruir`, `Pronostico.reconstruir`, `Suscripcion.reconstruir` (domain.model).
- **Decisión**: Reconstruir **no** es una transición de negocio: no aplica reglas como BR-002/BR-003 ni emite eventos (`PartidoProgramado`, `SuscripcionCreada`). Evita re-publicar eventos al leer de BD y permite restaurar el estado completo (cuotas, resultado, posiciones, equipos).
- **Alternativas descartadas**: hidratar vía métodos de negocio (re-emite eventos y valida reglas de transición al cargar); anotar el dominio con JPA (viola la Dependency Rule).

### ADR-007 — Mapeo JPA en infraestructura, separado del dominio (FASE 8)
- **Concepto**: 7 entidades JPA en `infrastructure.persistence.entity` (Liga, Equipo, PosicionTabla, Partido, Cuota, Pronostico, Suscripcion) + 4 repositorios Spring Data + 4 adapters con mappers entity↔dominio.
- **Decisión**: El dominio no conoce JPA. Los agregados se guardan como tablas y se reconstruyen con `reconstruir(...)` (ADR-006). VOs se mapean a columnas planas o tablas hijas (`equipos`, `posiciones_tabla`, `cuotas`) con cascade/orphanRemoval.
- **Equipos denormalizados en Partido**: el partido guarda `equipo_local_id/nombre` y `equipo_visitante_id/nombre` en lugar de `@ManyToOne` a EquipoEntity, respetando la regla de referencias por id entre agregados y evitando ciclos de asociación.
- **Alternativas descartadas**: `@ManyToOne` a EquipoEntity en Partido (ciclo Liga→Equipo→Partido, acopla agregados).

### ADR-008 — Gestión de esquema en dev con `ddl-auto=update` y enums (roles)
- **Concepto**: El esquema se genera en dev con `spring.jpa.hibernate.ddl-auto=update`. Hibernate crea una CHECK constraint por columna `@Enumerated(EnumType.STRING)` (ej. `usuarios_rol_check`), con los valores del enum **en el momento de crear la tabla**.
- **Artefacto**: `application.properties` (`ddl-auto=update`), entidades JPA con `@Enumerated` (UsuarioEntity.rol, etc.).
- **Decisión**: `ddl-auto=update` es **aditivo**: crea tablas/columnas faltantes pero **nunca modifica ni borra constraints existentes** al cambiar un enum. Consecuencia real (unificación de roles a `CLIENTE`/`TIPSTER`/`SUPERADMIN`): `usuarios.rol` conservó el CHECK viejo `('TIPSTER','CLIENTE','ADMIN')` y registrar un `SUPERADMIN` falló con 500 silencioso (`DataIntegrityViolationException`). Corrección manual en dev: `ALTER TABLE usuarios DROP CONSTRAINT usuarios_rol_check;` y recrearla con los valores nuevos. Regla práctica: **cada cambio de enum de dominio exige revisar/ajustar el CHECK en la BD dev**, hasta que lleguen las migrations formales con Flyway (FASE 19/20). El error no se ve en la respuesta HTTP (ApiError genérico, sin fugas) sino en los logs de `bootRun`: `GlobalExceptionHandler.manejarGeneral` registra la causa real con stack trace (mejora aplicada en esta misma fase).
- **Alternativas descartadas**: `ddl-auto=validate` hoy (bloquea el arranque hasta alinear el esquema; frena la iteración en dev); Flyway desde ahora (el plan lo difiere a FASE 19/20).
- **Relaciones**: `docs/PROYECTO-PLAN.md` (FASE 19/20 migrations), `application.properties`, `GlobalExceptionHandler`.
- **Actualización (post-Flyway, migración V6)**: el mismo síntoma reapareció tras adoptar Flyway (H-05) con `'EQUIPOS'` (fuente #6, CU-18 poblar-ligas). Causa: la BD dev se creó con `ddl-auto=update` ANTES de Flyway y `baseline-on-migrate=true` + `baseline-version=1` marcó V1 como aplicada **sin reconciliar** su contenido con el esquema existente → 3 CHECKs legacy sin `'EQUIPOS'` (`fuentes_extraccion_tipo_check`, `detalle_fuentes_extraccion_tipo_check`, `tareas_programadas_tipo_fuente_check`). Los tests pasaban en verde porque Testcontainers ejecuta las migraciones desde cero; solo dev divergía. Corregido con **V6__corregir_constraints_tipo_fuente.sql** (nunca ALTER manual fuera de Flyway). Lecciones permanentes: (1) una BD que heredó esquema pre-Flyway NO es confiable como referencia — la verdad son las migraciones + Testcontainers; (2) al añadir un valor de enum, auditar TODOS los CHECKs que lo referencien, no solo la tabla nueva; (3) diagnóstico de 500 silenciosos: stack trace en logs de `bootRun` primero. Runbook de verificación end-to-end: `docs/runbook-extraccion-fuentes.md`.