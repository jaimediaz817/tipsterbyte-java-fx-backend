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

### FASE 8 — PostgreSQL + JPA ✅
- Repository Ports → Adapters JPA. Entities mapping, relaciones, constraints, transacciones.
- **Resultado**: dependencias `spring-boot-starter-data-jpa` + `org.postgresql:postgresql` + Testcontainers 2.0.5 (`testcontainers-postgresql`, `testcontainers-junit-jupiter`, `spring-boot-testcontainers`). 7 entidades JPA en `infrastructure.persistence.entity` (`LigaEntity`, `EquipoEntity`, `PosicionTablaEntity`, `PartidoEntity`, `CuotaEntity`, `PronosticoEntity`, `SuscripcionEntity`). 4 repositorios Spring Data en `infrastructure.persistence.repository` (`LigaJpaRepository`, `PartidoJpaRepository`, `PronosticoJpaRepository`, `SuscripcionJpaRepository`). 4 adapters JPA en `infrastructure.adapter` (`LigaRepositoryJpaAdapter`, `PartidoRepositoryJpaAdapter`, `PronosticoRepositoryJpaAdapter`, `SuscripcionRepositoryJpaAdapter`) con mappers entity↔dominio y transacciones.
- Factory methods `reconstruir(...)` en `Liga`, `Partido`, `Pronostico`, `Suscripcion`: restauran el aggregate completo desde persistencia **sin emitir eventos** (reconstrucción ≠ transición, patrón DDD).
- `application.properties`: datasource con env vars con fallback al contenedor local (`localhost:5433/tipsterbyte_fx_db`), `ddl-auto=update` en dev. Equipos del partido denormalizados (id + nombre) para respetar referencias por id entre agregados.
- **Nota wiring**: los controllers NO se activan todavía (`app.api.rest.enabled=false`). CU-01..04 requieren los proveedores de FASE 8.5; el wiring REST completo se difiere a FASE 8.5/17.
- **Estado**: COMPLETADA — 114 tests en verde (8 nuevos de dominio para `reconstruir`, 11 de integración con Testcontainers), `./gradlew build` OK.

### FASE 8.5 — Adapters de fuentes externas (5 endpoints reales) ✅
- **Documentado primero**: `docs/architecture/fuentes-externas.md` registra las **5 fuentes reales** expuestas por el proyecto Python (host `http://127.0.0.1:8001`): `#1 ext-soccerway-countries` (países), `#5 ext-soccerway-leagues-by-country` (ligas por país), `#3 ext-position-table-by-league-stable` (posiciones + últimos 5 resultados por equipo), `#4 ext-calendar-league-by-league-v2` (calendario + estadísticas) y `#2 ext-next-matches-wplay-by-league` (cuotas Wplay, incl. doble oportunidad). Orden de extracción: países → ligas por país → posiciones/calendario/cuotas por liga.
- **Esquemas confirmados** ✅: `#1` (países: nombre, href, code, iso_alpha2, continente, mapeado), `#5` (ligas por país: name, type, logo_url, api_id, url_soccerway, nombre_torneo, semestre, anio), `#2` (cuotas Wplay: time_match, date_match "15 Ago 2026", cuotas 1X2 + double_chance `1x/12/2x`), `#3` (posiciones + resultados_ultimos_5_jugados `{1..5: -1|0|1}`), `#4` (calendario: fecha_iso + hora, equipos, goles, estadísticas). **Los 5 esquemas están confirmados.**
- **Diagnóstico (CU-10)**: `#1`+`#5` alcanzan para catálogo de países y ligas. Brechas del modelo: no existe entidad `Pais` (hoy `Liga.pais` es String), `Liga` no guarda `urlSoccerway`/`apiId`, `semestre` es inconsistente → usar `anio` para `Temporada`. `url_soccerway` es candidato a `path_to_scrape` de calendario (`#4`).
- **Decidir el mapeo** a los puertos del modelo (`ProveedorPosiciones/Calendario/Cuotas`), aún sin cerrar si conviven con las APIs originales o las reemplazan. El usuario entrega la respuesta real (JSON) de cada endpoint cuando se necesita; **no asumir formatos**.
- Definir DTOs de fuente en `application.dto` a partir de las respuestas reales.
- Extender dominio si procede: modelar "últimos 5 resultados por equipo" (clave para predicción, fuente `#3`) y nuevo caso de uso CU-10 para poblar catálogo de países y ligas (fuentes `#1` y `#5`).
- Implementar 5 adapters en `infrastructure.adapter` contra los endpoints reales, con tests usando las respuestas reales como fixtures.
- Actualizar ADRs y `docs/architecture/arquitectura-objetivo.md` según el mapeo decidido.

#### Subplan propuesto (orden de trabajo dentro de 8.5)
1. **✅ CU-10 + catálogo (`#1` y `#5`)** — COMPLETADO: entity `Pais` + `PaisRepository`; `Liga` extendida con `urlSoccerway`/`apiId`; DTOs `PaisFuente`/`LigaFuente`; puertos `ProveedorPaises`/`ProveedorLigasPorPais`; `SincronizarCatalogoUseCase`; adapters `SoccerwayPaisesAdapter` y `SoccerwayLigasPorPaisAdapter` (RestClient); config `app.fuentes.base-url`; `PaisEntity` + `PaisJpaRepository` + `PaisRepositoryJpaAdapter`; tests unitarios/integración/HTTP. Diagrama de dominio actualizado con `Pais`.
2. **✅ Adapters de datos deportivos (`#2`, `#3`, `#4`)** — COMPLETADO: DTOs según esquema real; dominio extendido: `Cuota`+`mercado` (canónico `Cuota(Mercado, BigDecimal)`; overload `Cuota(BigDecimal)`→`UNO_X_DOS`), `PosicionTabla`+`ultimosResultados` (clave 1 = más reciente, `1`=G/`0`=E/`-1`=P) persistidos en `PosicionTablaEntity` (`ultimos_resultados` VARCHAR "G,E,P,G,G"), `ResultadoReciente` (G/E/P), catálogo de fuentes `FuenteExtraccion`/`DetalleFuenteExtraccion` (tablas `fuentes_extraccion`, `detalle_fuentes_extraccion` con unique (liga_id, tipo)); puertos `FuenteExtraccionRepository`/`DetalleFuenteExtraccionRepository`; CU-11 `GestionarFuenteExtraccionUseCase`; CU-04 reescrito con `ActivarLigaComando` (3 URLs → crea detalles y activa, BR-001); adapters HTTP `FlashscorePosicionesAdapter`, `SoccerwayCalendarioAdapter` (solo crea partidos), `WplayCuotasAdapter` (6 `CuotaFuente`: 3 UNO_X_DOS + 3 DOBLE_OPORTUNIDAD); tests con fixtures reales.
3. **✅ Wiring REST** — COMPLETADO: `app.api.rest.enabled=${APP_API_REST_ENABLED:true}`; `UseCaseConfig` registra los 10 use cases como beans (los use cases son POJOs sin anotaciones Spring); endpoints de catálogo de fuentes (`POST/GET /api/v1/fuentes`, `PUT/GET /api/v1/ligas/{ligaId}/fuentes`) + `LigaController` con `ActivarLigaRequest` (3 URLs). 173 tests en verde.

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