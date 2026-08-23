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
FASE 12.5 Scheduling / Async (completada)
FASE 13 RabbitMQ
FASE 14 Spring WebFlux
FASE 15 Scheduling avanzado / Pipelines async
FASE 16 Observabilidad
FASE 17 Integración completa
FASE 18 Dockerización de aplicación
FASE 19 CI/CD
FASE 20 AWS / Deployment
FASE 21 Architecture Review
FASE 22 Preparación para entrevista
FASE T1 Frontend Angular 22 (transversal, proyecto hermano)
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

### FASE 9 — Docker ✅
- **`docker-compose.yml`** en la raíz con el servicio `postgres:17` **aislado**: puerto `5434:5432`, base `tipsterbytefxv2_dev`, usuario `postgres`, volumen nombrado `pg_tipsterbytefxv2_data`, red `tipsterbytefxv2-net` y `healthcheck` (`pg_isready`).
- **Decisión de aislamiento**: NO se usa ni modifica la BD compartida del proyecto Python (`db_pg_tipsterbyte_fx_dev` en `localhost:5433`); cada proyecto corre sobre su propia BD.
- `application.properties`: defaults del datasource actualizados a `DB_PORT:5434` / `DB_NAME:tipsterbytefxv2_dev` (siguen siendo env-overridable).
- **Verificado**: `docker compose up -d` → contenedor healthy → `./gradlew build` (173 tests, Testcontainers intacto) → `bootRun` conecta a `jdbc:postgresql://localhost:5434/tipsterbytefxv2_dev` → Actuator UP y `GET /api/v1/fuentes` → 200.
- **Estado**: COMPLETADA.

### FASE 10 — Testing ✅
- Unit (domain, use cases), Integration (app, infra), Controller tests, Testcontainers.
- **Cobertura**: 195 tests en verde. Se cerraron los huecos de FASE 8/8.5:
  - **Domain**: `FechaProgramadaTest`, `MercadoTest`, `TipoFuenteExtraccionTest`, `DomainExceptionTest` (eventos ya cubiertos vía aggregates/use cases).
  - **Interfaces**: `GlobalExceptionHandlerTest` (6 casos: DomainException→422, validación→400, JSON malformado→400, query param faltante→400, genérico→500, estructura ApiError).
  - **Infrastructure**: `UseCaseConfigTest` (los 10 use cases como beans con contexto Spring real).
  - **Integration (app+infra)**: `SincronizarCatalogoUseCaseIntegrationTest` — CU-10 con adapters JPA reales contra PostgreSQL (Testcontainers), proveedores #1/#5 mockeados; valida persistencia e idempotencia.
  - Spring 7: `@MockBean` (eliminado en Spring Boot 4) se reemplaza por `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
- **Estado**: COMPLETADA.

### FASE 11 — Spring Security + JWT ✅
- **Dominio**: nueva entity `Usuario` (id, nombre, `Email` VO, passwordHash, `Rol`, activo). Decisión: el password/credenciales NO se añaden a `Tipster`/`Cliente` (perfiles de negocio sin credenciales); se usa una entidad de auth independiente.
- **Application**: puertos `UsuarioRepository`, `PasswordHasher`, `TokenEmisor` (sin acoplar dominio a BCrypt/JJWT); DTOs `RegistrarUsuarioComando`, `AutenticarUsuarioComando`, `AutenticacionResultado`; nuevos casos de uso CU-12 `RegistrarUsuarioUseCase` y CU-13 `AutenticarUsuarioUseCase` (total **13 use cases**, registrados en `UseCaseConfig`).
- **Infrastructure**: `UsuarioEntity` + `UsuarioJpaRepository` + `UsuarioRepositoryJpaAdapter` (tabla `usuarios`, email UNIQUE + CHECK rol); `BcryptPasswordHasher`; `JwtTokenEmisor` (jjwt 0.12.6, HS256, claims subject/email/rol); `JwtAuthenticationFilter`; `SecurityConfig` (stateless, csrf off, `AuthenticationEntryPoint` → 401).
- **Interfaces**: `AuthController` — `POST /api/v1/auth/registro` (201) y `POST /api/v1/auth/login` (200 → `AuthResponse{token, rol, email}`; credenciales inválidas → 422).
- **Política de autorización** (`authorizeHttpRequests`): públicos `/api/v1/auth/**` y `/actuator/health`; `GET /api/v1/fuentes` → 3 roles; `/api/v1/ligas/**` y `/api/v1/partidos/**` → SUPERADMIN+TIPSTER; `/api/v1/pronosticos/**` → 3 roles; `/api/v1/suscripciones/**` → CLIENTE; resto autenticado. 401 sin/inválido, 403 rol insuficiente.
- **Configuración**: `app.jwt.secret` (dev ≥256 bits) y `app.jwt.expiration-ms` (dev 86400000), env-overridable.
- **Dependencias**: `spring-boot-starter-security`, `jjwt-api/impl/jackson:0.12.6`.
- **Diagrama interactivo**: `diagrams/flujo-jwt-security.html` (clicable, mismo patrón que `modelo-dominio.html`).
- **Cobertura**: 224 tests en verde (+29): `UsuarioTest`, `RegistrarUsuarioUseCaseTest`, `AutenticarUsuarioUseCaseTest`, `JwtTokenEmisorTest`, `AuthControllerTest`, `UsuarioRepositoryJpaAdapterTest` (Testcontainers), `SecurityFlowIntegrationTest` (registro→login→recurso protegido, 401, token inválido, 422).
- **Estado**: COMPLETADA.

### FASE 12 — Redis ✅
- **Decisión (acordada con el usuario)**: Redis puro, sin Caffeine en FASE 12. Caffeine (L1 local) queda como opcional para un futuro `CompositeCacheAdapter` (FASE 17); como el cache vive detrás del puerto `CacheLecturas`, subir de nivel solo implica un adapter nuevo sin tocar application.
- **Patrón**: cache-aside sobre los **proveedores** (no sobre el front): los decoradores `ProveedorPosicionesCacheable` / `ProveedorCalendarioCacheable` / `ProveedorCuotasCacheable` envuelven los adapters reales (`FlashscorePosicionesAdapter` #3, `SoccerwayCalendarioAdapter` #4, `WplayCuotasAdapter` #2) y protegen al scraper Python; la **invalidación al sincronizar** en CU-01/02/03 elimina la clave antes de consultar → la sincronización trae datos frescos y re-puebla el cache.
- **Application**: puerto `CacheLecturas` (`obtener/guardar/eliminar`) + `CacheClaves` (claves canónicas `posiciones:{ligaId}`, `calendario:{ligaId}`, `cuotas:{partidoId}`). Los 3 `Sincronizar*UseCase` inyectan `CacheLecturas` y hacen `eliminar(CacheClaves.*)` al inicio.
- **Infrastructure**: `RedisCacheAdapter` (StringRedisTemplate + TTL, `@ConditionalOnProperty(app.cache.enabled=true)`) y `NoOpCacheLecturas` (registrada solo con `app.cache.enabled=false`, e.g. tests). Decoradores `@Primary` + `@ConditionalOnProperty` que serializan/deserializan los DTOs de fuente con Jackson 3 (`ObjectMapper` bean de Spring; `TypeReference` de `tools.jackson.core.type`). Wiring en `UseCaseConfig` pasa `CacheLecturas` a los 3 use cases de sync.
- **Configuración**: `app.cache.enabled` (env `APP_CACHE_ENABLED`, default true), `app.cache.ttl-posiciones-seg` (300), `app.cache.ttl-calendario-seg` (300), `app.cache.ttl-cuotas-seg` (120), `spring.data.redis.host/port` (default `localhost:6380`).
- **Docker**: servicio `redis:7-alpine` en `docker-compose.yml` (`tipsterbytefxv2-redis`, `6380:6379`, volumen `redis_tipsterbytefxv2_data`, red `tipsterbytefxv2-net`, healthcheck `redis-cli ping`) — aislado como la BD (decisión FASE 9).
- **Tests**: `src/test/resources/application.properties` replica el principal con `app.cache.enabled=false` (los tests de contexto Spring no requieren Redis). Nuevos: `RedisCacheAdapterTest` (Testcontainers Redis con `GenericContainer redis:7-alpine` vía `RedisTestConfiguration`; NO existe `testcontainers-redis` en la línea 2.0.5), `RedisCacheAdapterTest` valida get/put/evict/TTL, `ProveedorPosiciones/Calendario/CuotasCacheableTest` (hit/miss/serialización), `CacheClavesTest`, `NoOpCacheLecturasTest`, + invalidación en los 3 `Sincronizar*UseCaseTest` y bean NoOp en `UseCaseConfigTest`.
- **Diagrama interactivo**: `diagrams/cache-aside-redis.html` (clicable, mismo patrón que `modelo-dominio.html`).
- **Cobertura**: 246 tests en verde (+22). `./gradlew test` OK.
- **Estado**: COMPLETADA.

### FASE 12.5 — Scheduling / Async ✅ (COMPLETADA)
> **Nota de cierre**: implementada y verificada en código con tests — entity `TareaProgramada` + `TareaLog` (dominio), CU-15 `GestionarTareasProgramasUseCase`, `TareaProgramadaRepositoryJpaAdapter`, `CatalogoScheduler` (dispatcher `@Scheduled` con anti-solapamiento por tarea), endpoints `/api/v1/tareas-programadas/**` y tests (dominio, use case, adapter). El detalle original del plan se conserva abajo como referencia histórica.
- **Contexto / [POR QUÉ]**: necesidad operativa real de ejecución programada de las extracciones — cuotas Wplay **cada hora** (ver variación), calendario **cada 8 días** (actualizar resultados), posiciones diario, catálogo semanal. Se adelanta antes de RabbitMQ/WebFlux porque **no depende de ellos**: los use cases de sincronización ya existen y ya invalidan el cache Redis al correr (CU-01/02/03). El scheduler solo debe invocarlos con cron expressions.
- **Modelo (validado con el usuario, un job por fuente de extracción)**: cada `DetalleFuenteExtraccion` (liga + tipo `CALENDAR`/`ODDS_WPLAY`/`STANDINGS`) puede tener una **tarea programada** asociada; existe además una **tarea global de catálogo** (CU-10, no ligada a un detalle). Dos maneras de crear tareas desde Angular: (1) clicando sobre cada fuente de extracción en Geografía ("Programar"), (2) desde el ítem de agrupación **"Tareas programadas"** en Automatización.
- **Domain**: nueva entity `TareaProgramada` (id UUID, nombre, `detalleFuenteExtraccionId` **nullable** = tarea global de catálogo, `cronExpression` validada con `CronExpression` de Spring — 6 segmentos, `activa` boolean, `fechaCreacion`). Reglas: una tarea por detalle (unique `detalle_fuente_extraccion_id`), **una sola tarea global** (unique sobre columna nullable o validación en use case), cron inválido → `DomainException` (422), no se puede programar un detalle inexistente/inactivo.
- **Application**: puerto `TareaProgramadaRepository` (guardar/buscarPorId/listarActivas/eliminar) + **CU-15 `GestionarTareaProgramadaUseCase`** (crear/listar/actualizar cron y estado/eliminar, con validaciones de negocio).
- **Infrastructure**: `TareaProgramadaEntity` (tabla `tareas_programadas`, FK nullable a `detalle_fuentes_extraccion`) + `TareaProgramadaJpaRepository` + `TareaProgramadaRepositoryJpaAdapter`. **`CatalogoScheduler`**: `@EnableScheduling` + dispatcher `@Scheduled` fijo (intervalo `app.scheduling.dispatcher-ms`, default 60000) que evalúa el `CronExpression` de cada tarea activa y dispara el use case correcto según el tipo de la fuente asociada:
  - `CALENDAR` → `SincronizarCalendarioUseCase.ejecutar(ligaId)`
  - `ODDS_WPLAY` → `SincronizarCuotasUseCase.ejecutar(ligaId)`
  - `STANDINGS` → `SincronizarPosicionesUseCase.ejecutar(ligaId)`
  - Sin detalle (global) → `SincronizarCatalogoUseCase.ejecutar()`
  - Ejecución con `@Async` + thread pool (`app.scheduling.pool-size`) y **guard anti-solapamiento por tarea** (AtomicBoolean) para no apalear al scraper Python con corridas concurrentes.
- **Configuración**: `app.scheduling.enabled` (default true; **false en tests** para no disparar jobs en contextos Spring), `app.scheduling.dispatcher-ms`, `app.scheduling.pool-size`. Los crons viven **en BD** (no en properties) → habilita el panel "Programación/Tareas programadas" del frontend.
- **Interfaces** (roles `SUPERADMIN`/`TIPSTER`, validación Bean Validation):
  | Método | Ruta | Descripción |
  |---|---|---|
  | `GET` | `/api/v1/tareas-programadas` | Lista de tareas (id, nombre, liga+tipo, cron, activa, fechaCreacion, próxima ejecución derivada) |
  | `POST` | `/api/v1/tareas-programadas` | Crea tarea; body `{nombre, detalleFuenteExtraccionId?, cron, activa}` → 201; 422 si cron inválido, duplicado o detalle inexistente/inactivo |
  | `PUT` | `/api/v1/tareas-programadas/{id}` | Actualiza `{nombre?, cron?, activa?}` → 204 |
  | `DELETE` | `/api/v1/tareas-programadas/{id}` | Elimina tarea → 204 |
  | `GET` | `/api/v1/tareas-programadas/disponibles` | Fuentes candidatas (detalles activos) + opción global "Catálogo" |
- **Alternativas descartadas**: `@Scheduled` con crons estáticos en properties (no soporta jobs dinámicos por BD); `SchedulingConfigurer` re-registrando tareas en cada cambio (más Spring-native pero frágil ante CRUD frecuente); ShedLock diferido al escalado multi-nodo (FASE 17/20, se anota como decisión futura).
- **¿Más potente que Python (APScheduler)?** Sí, para este stack: integración con el ciclo de vida y la configuración (properties/env, sin cron daemon), observabilidad con Micrometer (FASE 16), testabilidad con `TaskScheduler` mockeado y un único runtime (el job corre donde corre la API). Matiz: `@Scheduled` asume un solo nodo → ShedLock cuando haya N instancias.
- **Tests esperados**: dominio (`TareaProgramadaTest`), use case CU-15, adapter JPA, controller, y scheduler unitario (evaluación de `CronExpression` + disparo al use case correcto). Sin nuevas dependencias (usa `spring-context`).
- **Comunicado frontend**: ver sección "Comunicado Tareas programadas" abajo.
- **Estado**: ✅ COMPLETADA.

#### 📣 Comunicado Tareas programadas (para el equipo frontend)

**Modelo de negocio:** cada **detalle de fuente de extracción** (liga + tipo `CALENDAR`/`ODDS_WPLAY`/`STANDINGS`) puede tener una **tarea programada** asociada; existe además una **tarea global de catálogo** (CU-10) que no va ligada a ningún detalle. Un job = fuente asociada + frecuencia (cron) + estado activo/pausado.

**Dos maneras de crear tareas desde Angular:**
1. **Desde cada fuente (Geografía):** en el detalle de una liga, cada fuente tiene la acción **"Programar"** → modal (nombre + frecuencia + toggle activa) → `POST /tareas-programadas` con `detalleFuenteExtraccionId`.
2. **Desde "Tareas programadas" (Automatización):** pantalla central que lista **todas** las tareas (ligas, tipos, cron, estado, última/próxima ejecución) con crear/editar/pausar/eliminar.

**Pantalla propuesta:**
```
Automatización
└── Tareas programadas
    ├── Lista (tabla/cards):
    │     Nombre | Fuente (liga + tipo / Catálogo global) | Frecuencia (cron + humanizado)
    │     | Estado (activa/pausada) | Próxima ejecución | Acciones (editar, pausar, eliminar)
    └── Crear/Editar (modal):
          Nombre
          Fuente asociada (selector desde GET /tareas-programadas/disponibles)
          Frecuencia: selector rápido (Cada hora / Cada 8 días / Diario / Semanal / Custom cron)
          Activa desde el inicio (toggle)
```

**Endpoints (roles `SUPERADMIN`/`TIPSTER`):**
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/tareas-programadas` | `List<TareaProgramadaResponse>`: id, nombre, liga?, tipo, cron, activa, fechaCreacion, próxima ejecución |
| `POST` | `/api/v1/tareas-programadas` | body `{nombre, detalleFuenteExtraccionId?, cron, activa}` → `201`; `422` si cron inválido, duplicado o detalle inexistente/inactivo |
| `PUT` | `/api/v1/tareas-programadas/{id}` | body `{nombre?, cron?, activa?}` → `204` |
| `DELETE` | `/api/v1/tareas-programadas/{id}` | → `204` |
| `GET` | `/api/v1/tareas-programadas/disponibles` | fuentes candidatas (detalles activos) + opción "Catálogo global" |

**Ejemplos de cron (6 segmentos Spring):**
- Cada hora: `0 0 * * * *`
- Cada 8 días a las 02:00: `0 0 2 */8 * *`
- Diario a las 03:00: `0 0 3 * * *`
- Semanal (lunes 04:00): `0 0 4 * * 1`

**Notas UX:**
- El humanizado de la frecuencia es **responsabilidad del frontend** (el backend guarda el cron crudo y valida con `CronExpression` → 422 si no es válido).
- La "próxima ejecución" la puede calcular el backend (derivada) o el frontend con una librería de cron; se define en la implementación.
- Pausar = `PUT` con `activa:false` (el job no se dispara pero queda configurado); no borra la tarea.

### FASE 13 — RabbitMQ
- Event-driven: domain events → exchange → queue → consumer (notificaciones).

### FASE 14 — Spring WebFlux
- Mono/Flux, reactive streams, backpressure. Comparativa MVC vs WebFlux.

### FASE 15 — Scheduling avanzado / Pipelines async
- El **scheduling básico de las extracciones** (jobs por fuente + crons en BD) vive ahora en **FASE 12.5**. Esta fase queda para el nivel avanzado: `@Async`/executors/thread pools profundizados, **ShedLock** (bloqueo distribuido para multi-nodo), pipelines async con RabbitMQ/Redis/WebFlux (FASE 17), y reconciliación de tareas programadas con el estado de fuentes.

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

### FASE T1 — Frontend Angular 22 (transversal, proyecto hermano) ✅ (plan)
- **Contexto**: el frontend **no vive en `ecosistema_java`**; es un **proyecto hermano** en el mismo ecosistema (`LABORATORIO_retos_Laborales`), por ejemplo `ecosistema_angular`. Consume la REST API expuesta por este backend (`/api/v1/**`, 13 CU + auth JWT + cache). Esta fase es **transversal**: se planifica junto con las fases backend, no en secuencia, y su documentación vive aquí como contexto (arquitectura, endpoints consumidos, convenciones).
- **Stack (a confirmar al iniciar)**: Angular 22 (CLI `@angular/cli@22.1.4`), TypeScript, Angular Router, consumo HTTP con interceptores JWT. Node.js ≥ 22.22.3.
- **Comandos Git Bash para crear el proyecto Angular 22** (ejecutar desde `LABORATORIO_retos_Laborales`):
  ```bash
  # 1. Verificar Node (requerido ≥ 22.22.3)
  node --version
  npm --version

  # 2. Instalar Angular CLI 22 (una vez por máquina o vía npx)
  npm install -g @angular/cli@22

  # 3. Crear el workspace/app (sin prompts: CSS, sin SSR, con routing)
  ng new ecosistema_angular --style=css --ssr=false --routing --skip-git

  # 4. Entrar y levantar dev server (http://localhost:4200)
  cd ecosistema_angular
  ng serve --open
  ```
- **Alternativas descartadas**: React+Vite y Vue (por elección del usuario se va a Angular 22); Thymeleaf (server-side, contaminaría la separación de capas del backend).
- **Relaciones**: consume CU-01..13 vía controllers (`LigaController`, `PartidoController`, `PronosticoController`, `SuscripcionController`, `FuenteExtraccionController`, `AuthController`); usa `app.jwt` para auth y `app.cache` de forma transparente.
- **Estado**: PLANIFICADA (se abre al completar o en paralelo con FASE 13+; requiere aprobación del usuario y configuración previa de skills de Angular).

### FASE T1-b — API de consulta para el frontend (transversal) ✅
- **Contexto**: el backend expone hoy los flujos de **escritura/sincronización/auth**, pero Angular necesita una capa de **lectura (GETs)** para mostrar datos. Esta fase transversal entrega esa superficie SIN tocar el dominio: solo controllers, DTOs de respuesta y (si hace falta) métodos de consulta en repositorios que ya existen.
- **Catálogo de endpoints GET a exponer** (todos `app.api.rest.enabled=true`, JWT según política actual):
  | Endpoint | Fuente de datos (ya existe) | Qué devuelve |
  | --- | --- | --- |
  | `GET /api/v1/ligas` | `LigaRepository.buscarActivas()` (LigaRepository:23) + buscarPorId | Lista de ligas con `id`, `nombre`, `pais`, `estado` |
  | `GET /api/v1/ligas/{ligaId}` | `LigaRepository.buscarPorId` + `Liga.posiciones()` | Detalle de liga + tabla de posiciones |
  | `GET /api/v1/ligas/{ligaId}/posiciones` | `Liga.posiciones()` → `PosicionTabla` | Tabla (equipo, posición, J/G/E/P, GF/GC, pts, racha últimos 5) |
  | `GET /api/v1/partidos?ligaId=` | `PartidoRepository.buscarPorLiga()` (PartidoRepository:23) | Partidos de la liga con equipos y estado |
  | `GET /api/v1/partidos?ligaId=&fecha=` | `PartidoRepository.buscarPorLigaYFecha()` (PartidoRepository:31) | Partidos de la liga en una fecha |
  | `GET /api/v1/partidos?ligaId=&proximos=true` | `PartidoRepository.buscarProximosPorLiga()` (PartidoRepository:27) | Próximos partidos (para cuotas) |
  | `GET /api/v1/partidos/{partidoId}/cuotas` | `Partido.cuotas()` → `List<Cuota>` | Cuotas del partido por mercado |
  | `GET /api/v1/ligas/{ligaId}/fuentes` | **ya existe** (CU-11, FuenteExtraccionController:81) | Detalles de fuentes de la liga |
  | `GET /api/v1/fuentes` | **ya existe** (CU-11, FuenteExtraccionController:60) | Catálogo de fuentes |
  | `GET /api/v1/pronosticos?clienteId=&ligaId=&fecha=` | **ya existe** (CU-08, PronosticoController:85) | Pronósticos publicados |
  | `GET /api/v1/suscripciones?clienteId=` | `SuscripcionRepository.buscarActivasPorCliente()` (SuscripcionRepository:20) | Suscripciones activas del cliente |
- **Nuevos DTOs de respuesta** (en `interfaces/rest/dto/response/`): `LigaResponse`, `PosicionTablaResponse` (equipo + stats + `ultimosResultados` como lista de `ResultadoReciente`), `PartidoResponse` (equipos, fecha, estado, resultado), `CuotaResponse` (mercado, valor). Reutilizar `FuenteExtraccionResponse`, `PronosticoPublicoDto` y `AuthResponse`.
- **CORS** (imprescindible para Angular en `:4200`): configurar `CorsConfigurationSource` en `SecurityConfig` (orígenes permitidos `http://localhost:4200`, métodos GET/POST/PUT, headers `Authorization` + `Content-Type`) o `WebMvcConfigurer.addCorsMappings`. Hoy **no existe** ninguna configuración CORS.
- **Flujo del administrador (explorar fuentes → activar liga)**: el frontend usa `GET /ligas` (ver ligas), `GET /ligas/{id}/fuentes` + `PUT /ligas/{id}/fuentes/{tipo}` (asociar URL por tipo, guardando una a una como decide el usuario) y `POST /ligas/{id}/activacion` con las 3 URLs (activación **manual** a conciencia, decisión del usuario; NO auto-activación). El sistema deja la puerta abierta a 4ª/5ª/6ª fuente sin romper BR-001.
- **Implementado**:
  - Nuevos DTOs de respuesta: `LigaResponse`, `LigaDetalleResponse`, `PosicionTablaResponse`, `PartidoResponse`, `CuotaResponse`, `PronosticoResponse`, `RecursoCreadoResponse`.
  - CORS configurado en `SecurityConfig` (`CorsConfigurationSource`) para `http://localhost:4200`.
  - Endpoints GET: `LigaController` (listar, detalle, posiciones), `PartidoController` (por liga, fecha, próximos, cuotas), `SuscripcionController` (activas por cliente con autorización de propiedad), `PronosticoController` (consulta pública con mapeo a `PronosticoResponse`).
  - Estandarización de respuestas de error: `ApiErrorAuthenticationEntryPoint` (401) y `ApiErrorAccessDeniedHandler` (403) devuelven `ApiError` en JSON, alineados con `GlobalExceptionHandler`.
  - Estandarización de respuestas de éxito: documentado en `docs/architecture/arquitectura-objetivo.md` (contrato HTTP con status 200/201/204 y DTOs de respuesta).
- **No se toca**: dominio, agregados, BR-001..008, casos de uso de escritura.
- **Tests**: 266 tests en verde (+20 unitarios/integración de controllers, CORS, seguridad y consultas end-to-end).
- **Relaciones**: habilita FASE T1 (Angular 22); consume puertos de repositorio existentes y `app.api.rest.enabled`.
- **Estado**: COMPLETADA.

### FASE T2 — Poblamiento de equipos por liga (fuente #6) ✅ (COMPLETADA)

> **Contexto / [POR QUÉ]**: completar el poblamiento geográfico con la plantilla oficial de equipos de cada liga. Hoy los equipos entran como subproducto de las fuentes operativas (#3/#4) con matching por nombre exacto y sin escudo. Con la fuente `#6 ext-soccerway-teams-by-league` (params `country_name` + `league_name`, datos que ya viven en el aggregate), cada temporada nace con su plantilla canónica (nombre + `logo_url`) durante el propio poblamiento — paso 2 de la secuencia: países (#1) → ligas/torneos (#5) → **equipos (#6)** → luego activar y sincronizar lo operativo. HU-11; esquema confirmado en `docs/architecture/fuentes-externas.md`.

**Dominio (cambios mínimos)**:
- `Equipo` gana atributo opcional `logoUrl` (String nullable; columna nueva `equipos.logo_url` creada por ddl-auto).
- Normalizador de nombres en `domain.service` (`NormalizadorNombresEquipos`): sin tildes + trim + case-insensitive. Usado por el matching de `resolverEquipo` en CU-10/CU-01/CU-02 (los nombres se guardan tal cual llegan para display; solo la COMPARACIÓN normaliza).

**Application**:
- Puerto nuevo: `ProveedorEquiposPorLiga.obtenerEquipos(countryName, leagueName)` → DTO `EquipoFuente(nombre, logoUrl)`.
- `CacheClaves.equipos(countryName, leagueName)` + decorador cache-aside `ProveedorEquiposCacheable` (`@Primary`, mismo patrón Jackson/TypeReference que los otros 3) con TTL largo (plantilla estable). El caso de uso invalida la clave antes de consultar (consistencia del patrón).

**CU-10 encadenado (SincronizarCatalogoUseCase)**:
- Tras persistir liga+temporada de catálogo de un **país de interés**, consumir `#6` y poblar la plantilla de la **temporada vigente** (activa o primera registrada): match normalizado contra existentes (reuso id + update `logo_url`) o alta nueva. Las ligas de países SIN preferencia se catalogan sin equipos (como hasta hoy).
- **Tolerancia a fallos por liga**: un fallo de `#6` no aborta el poblamiento (log WARN con contexto país+liga vía MDC y continuar) — mismo patrón que temporada inválida.
- Iteración trazable: log INFO por país y por liga (país → liga → N equipos registrados/reutilizados).
- Idempotencia total: re-ejecutar no duplica nada.
- Sin cambios en BR-001 (siguen siendo 3 URLs operativas en CU-04) ni en `DetalleFuenteExtraccion` (#6 no usa path_to_scrape).

**Infrastructure**:
- Adapter nuevo `SoccerwayEquiposAdapter implements ProveedorEquiposPorLiga` (RestClient a la base Python; parseo del envoltorio `{success, data.leagues[].teams[]}`, matcheando `leagues[].name == league_name`).
- Migración manual dev: ninguna requerida más allá de arrancar bootRun (ddl-auto crea `equipos.logo_url`). Flyway formaliza en FASE 19/20.

**Tests esperados**: dominio (`Equipo.logoUrl`, normalizador: tildes/case/trim), CU-10 encadenado (alta, reuso por nombre normalizado, fallo de #6 tolerado, cache invalidada), adapter (JSON real mockeado), integración JPA (`equipos.logo_url` persistida). Suite completa en verde como señal de éxito.

**Decisiones cerradas con el usuario**:
1. **Alcance**: poblar equipos SOLO de las ligas pertenecientes a **países de interés** (el resto del mundo se cataloga sin equipos, como hasta hoy). Controla el volumen de llamadas al scraper.
2. **Sin flag de configuración**: no se añade `app.poblamiento.equipos-enabled`; el alcance limitado a países de interés ya controla el costo. Se evaluará un interruptor solo si surge la necesidad operativa.
3. **#6 nunca elimina**: los equipos presentes en la plantilla que NO aparezcan en la respuesta de #6 se conservan tal cual (agregar/actualizar únicamente). Eliminar quedará para cuando exista fuzzy matching (FASE 17), porque un nombre escrito distinto podría provocar borrados injustificados.

**Tests esperados**: dominio (`Equipo.logoUrl`, normalizador: tildes/case/trim), CU-10 encadenado (alta, reuso por nombre normalizado, fallo de #6 tolerado, cache invalidada, equipos SOLO para países de interés), adapter (JSON real mockeado), integración JPA (`equipos.logo_url` persistida). Suite completa en verde como señal de éxito.

- **Relaciones**: HU-11 → CU-10 → `ProveedorEquiposPorLiga`; consume el modelo de temporadas (plantilla por temporada vigente); habilita matching más estable para FASE 17.
- **Estado**: ✅ COMPLETADA — implementada con suite en verde (426 tests). Nota: los equipos se poblan solo para ligas de países de interés; #6 nunca elimina; matching normalizado centralizado en `NormalizadorNombresEquipos` (también adoptado por CU-01/CU-02).

### FASE T3 — Poblamiento asíncrono con progreso y trazabilidad (H-02) ✅ (COMPLETADA)

> **Contexto / [POR QUÉ]**: `POST /api/v1/catalogo/activar` hoy ejecuta CU-10 de forma síncrona en el hilo HTTP. Con 176 países × ligas × #6 puede tardar 10-30 min: timeout del navegador, spinner infinito, sin feedback y sin rastro. El scheduler CU-15 ya resuelve el mismo problema para tareas programadas (hilos virtuales + anti-solapamiento + `tarea_log` con `executionId`/`duracionMs`/`estado`). H-02 propone reutilizar ese mecanismo para la vía manual, sin duplicar infraestructura. Ver `docs/architecture/hallazgos-arquitectura.md` (H-02).

**Domain (sin nueva Entity)**:
- Reutiliza `TareaLog` (id, tareaProgramadaId nullable, executionId, fechaEjecucion, estado SUCCESS/ERROR/RUNNING, duracionMs, tipoError, mensaje) + `TareaProgramada` como origen opcional.
- Para ejecuciones manuales, `tareaProgramadaId` es `null` (no hay tarea programada asociada) y `executionId` es el correlador visible para el frontend. Alternativa considerada y descartada: nueva Entity `PoblamientoEjecucion` — se descarta porque duplica `TareaLog` y el scheduler ya aporta el modelo.

**Application**:
- Wrapper asíncrono de CU-10: `SincronizarCatalogoAsyncUseCase` o extensión de `SincronizarCatalogoUseCase` con método `ejecutarAsync() → UUID executionId` que: genera `executionId`, persiste `TareaLog` en RUNNING, lanza CU-10 en `ExecutorService` de hilos virtuales, al terminar actualiza a SUCCESS/ERROR con `duracionMs`.
- Puerto `EstadoPoblamiento` (opcional, fachada de lectura): `Optional<TareaLog> buscarPorExecutionId(executionId)` + `Set<UUID> ejecucionesEnCurso()` (reutiliza `EstadoEjecucionTareas` del scheduler).
- Regla: una sola ejecución manual en curso a la vez (anti-solapamiento por clave global, mismo `ConcurrentHashMap` del scheduler). Segundo `POST` mientras hay RUNNING → `409 Conflict` con `executionId` en curso.
- DTO `PoblamientoEstadoDto(executionId, estado, paisActual, ligasProcesadas, totalLigasEstimado, equiposCreados, mensaje)`: progreso derivado de contadores en memoria + `TareaLog`.

**Infrastructure**:
- `ExecutorService` de hilos virtuales (ya existe en `CatalogoScheduler`; extraer a bean compartido `poblamientoExecutor` o reutilizar el del scheduler).
- `CatalogoScheduler` aporta el patrón; no se modifica su tick. El wrapper manual usa el mismo `MDCTaskContext` para correlacionar logs JSON por `executionId`.
- `TareaLogRepositoryJpaAdapter` ya persiste el historial.

**Interfaces (roles SUPERADMIN)**:
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/catalogo/activar` | **Cambia a `202 Accepted`**: `{executionId}`. Antes `200` síncrono. Mantener compatibilidad una versión vía `?sync=true` opcional para tests. |
| `GET` | `/api/v1/catalogo/activar/{executionId}` | Estado/progreso de esa ejecución: `{estado: RUNNING\|SUCCESS\|ERROR, paisActual, ligasProcesadas, totalLigasEstimado, duracionMs, mensaje}`. `404` si no existe. |
| `GET` | `/api/v1/catalogo/estado` | Sin cambios (VACIO/POBLADO + conteos). Útil para badge inicial antes de disparar. |

**Alternativas descartadas**:
- Mantener síncrono con timeout largo; se descarta por UX y riesgo de doble ejecución.
- WebSocket/SSE para push de progreso en v1; se descarta por complejidad — polling del `GET /{executionId}` cada 5-10 s es suficiente; push queda para FASE 16 (observabilidad).
- Nueva tabla `poblamiento_ejecuciones`; se descarta por duplicar `tarea_log`.

**Tests esperados**: use case async (genera executionId, persiste RUNNING, actualiza a SUCCESS/ERROR, anti-solapamiento → 409), controller (`202` + `Location`, `GET` progreso, `404`), scheduler sin regresión, integración con `TareaLog` real. Sin nuevas dependencias (usa `spring-context` + hilos virtuales).

**Comunicado frontend**: actualizar `docs/frontend/comunicado-poblamiento-geografico.md` (sección 2/3): botón deshabilitado + polling del `GET /{executionId}` con barra de progreso por país, toast final con conteos, y manejo de `409` ("ya hay un poblamiento en curso").

- **Relaciones**: H-02 → CU-10 + `TareaLog`/`EstadoEjecucionTareas`; reutiliza `CatalogoScheduler` (FASE 12.5). No toca BR-001, fuentes #1/#5/#6 ni `Temporada`.
- **Estado**: ✅ COMPLETADA — implementada con suite en verde (433 tests). Implementado tal al diseño: `SincronizarCatalogoAsyncUseCase` (hilos virtuales + RUNNING/SUCCESS/ERROR en `tarea_log` + anti-solapamiento), `POST /catalogo/activar` → `202` (`409` si en curso vía `PoblamientoEnCursoException`), `GET /catalogo/activar/{executionId}` con progreso por país (`ProgresoPoblamientoEnMemoria`). Nota infra: `tarea_log.tarea_programada_id` ahora nullable (ejecuciones manuales sin tarea asociada); parche aplicado a dev BD.
> Nota de cierre: ver comunicado actualizado en `docs/frontend/comunicado-poblamiento-geografico.md` (secciones 3/4).

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

> **Hallazgos cerrados post-T2/T3**: H-02 (poblamiento async FASE T3), H-03 (`GET /ligas/{id}/equipos`), H-04 (detección de duplicados), H-06 (cortesía scraper) y **H-05 (Flyway)**: esquema versionado con `V1__baseline.sql`, `ddl-auto=validate`, tests validando migraciones reales. Diagnóstico completo en `docs/architecture/hallazgos-arquitectura.md`; notificaciones hexagonales como excusa de aprendizaje pendiente de aprobación.

> **FASE T1** es transversal: puede ejecutarse en paralelo a las fases backend. La creación del repo Angular y sus skills viven en el proyecto hermano; aquí solo se mantiene el contexto de consumo de API. **FASE T1-b** (GETs de consulta + CORS) es la habilitadora para que el frontend tenga datos que mostrar.