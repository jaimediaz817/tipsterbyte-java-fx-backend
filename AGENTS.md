# AGENTS.md — tipsterbyte-fx-v2

Proyecto de aprendizaje/portafolio Java para proceso de selección. Plataforma de pronósticos de fútbol que consume 4 APIs externas (football-data.org, API-Football, The Odds API, SharpAPI) con arquitectura limpia + DDD.

## Estado actual — CRÍTICO

- El repo está en **FASE 9 completada (Docker)**. FASE 8 (PostgreSQL + JPA): modelo de dominio completo en `domain.model` (aggregates `Liga/Partido/Pronostico/Suscripcion` con BR-001..008, + `Pais`, `FuenteExtraccion`, `DetalleFuenteExtraccion`), eventos en `domain.event`, factory methods `reconstruir(...)`. Capa application con **11 casos de uso (CU-01..11)** en `application.usecase`, puertos (`ProveedorPosiciones/Calendario/Cuotas` + 6 repositorios) en `application.port`, DTOs en `application.dto`. Capa interfaces con 6 controllers (`LigaController`, `PartidoController`, `PronosticoController`, `SuscripcionController`, `FuenteExtraccionController`) y `GlobalExceptionHandler` (DomainException → 422, validación → 400). Capa infrastructure con 10 entidades JPA, 6 repositorios Spring Data y 6 adapters JPA (`infrastructure.adapter`), **más 3 adapters HTTP de fuentes**: `FlashscorePosicionesAdapter` (#3 → ProveedorPosiciones), `SoccerwayCalendarioAdapter` (#4 → ProveedorCalendario), `WplayCuotasAdapter` (#2 → ProveedorCuotas). **173 tests en verde**. El wiring REST se activa con `app.api.rest.enabled=true` (por defecto): los 10 use cases se registran como beans en `UseCaseConfig` (son POJOs sin anotaciones Spring). FASE 9 dockeriza el PostgreSQL de dev (`docker-compose.yml`: `postgres:17` en `:5434`, base `tipsterbytefxv2_dev`, volumen + red + healthcheck) y el datasource por defecto apunta a ese contenedor. FASE 9 **sin commitear**.
- Avanzamos **una fase a la vez** (ver `docs/PROYECTO-PLAN.md`), con aprobación explícita del usuario entre fases. **NUNCA** ejecutar fases adelantadas ni "construir el proyecto completo".
- **Decisión FASE 9 (aislamiento de BDs)**: la BD dockerizada de este proyecto (`:5434`, `tipsterbytefxv2_dev`) es **exclusiva**; NO se usa ni modifica la BD compartida del proyecto Python (`db_pg_tipsterbyte_fx_dev` en `localhost:5433`). Testcontainers (tests) usa su propio contenedor, independiente de ambos.
- **Documento de referencia: `docs/architecture/fuentes-externas.md`** — registra las **5 fuentes reales** expuestas por el proyecto Python (`http://127.0.0.1:8001`): `#1 ext-soccerway-countries` (países), `#5 ext-soccerway-leagues-by-country` (ligas por país, params `country_name`/`limit`), `#3 ext-position-table-by-league-stable` (posiciones + últimos 5 resultados por equipo, param `path_to_scrape`), `#4 ext-calendar-league-by-league-v2` (calendario + estadísticas, param `path_to_scrape`) y `#2 ext-next-matches-wplay-by-league` (cuotas Wplay incl. doble oportunidad, param `path_to_scrape`). Orden de extracción: países (#1) → ligas por país (#5) → posiciones (#3)/calendario (#4)/cuotas (#2) por liga. Mapeo: #1/#5 → CU-10 (catálogo), #2 → ProveedorCuotas, #3 → ProveedorPosiciones, #4 → ProveedorCalendario. **Los 5 esquemas están confirmados** (JSON real en el doc).
- **Decisiones FASE 8.5**: catálogo de fuentes vía CU-11 `GestionarFuenteExtraccionUseCase` (registrar/listar fuentes, asociar URL por liga, listar detalles); CU-04 `ActivarLigaUseCase` reescrito con `ActivarLigaComando` (recibe `urlPosiciones`/`urlCalendario`/`urlCuotas` → crea `DetalleFuenteExtraccion` por tipo y activa con BR-001); `Cuota` ganó `mercado` (canónico `Cuota(Mercado, BigDecimal)`; `Cuota(BigDecimal)` → UNO_X_DOS); `PosicionTabla` ganó `ultimosResultados` (clave 1 = más reciente, G/E/P) persistido como VARCHAR "G,E,P,G,G"; equipos se matchean por **nombre exacto** (fuzzy difiere a FASE 17); `SoccerwayCalendarioAdapter` **solo crea partidos**; tablas JPA propias `fuentes_extraccion`/`detalle_fuentes_extraccion` (unique (liga_id, tipo)); **no se reutilizan** las tablas `fuente_extraccion`/`detalle_fuente_extraccion` del esquema Python.

## Configuración de OpenCode (FASE 1)

- `.opencode/rules/` — 5 reglas siempre activas (architecture, java, documentation, naming, testing), registradas en `opencode.json` vía `instructions`.
- `.opencode/skills/` — skills activas: java-engineering, spring-boot-engineering, clean-architecture, ddd-domain-modeling, java-documentation, architecture-review, diagram-design (estilo visual), diagramas-interactivos (capas de interacción, OBLIGATORIA en todo diagrama). Más adelante: docker-engineering, persistence-jpa, testing-java, spring-security, redis-engineering, messaging-rabbitmq, spring-webflux, observability, aws-engineering.
- Reglas y skills se cargan al reiniciar OpenCode; no hay hot-reload.

## Fuentes de verdad (leer antes de tocar nada)

- `docs/PROYECTO-PLAN.md` — las 22 fases y el orden de trabajo.
- `docs/project-definition.md` — nombre, problema, usuarios, funcionalidades.
- `docs/domain/modelo-dominio.md` — DDD: aggregates (`Liga`, `Partido`, `Pronostico`, `Suscripcion`), VOs, 8 reglas de negocio (BR-001..008), eventos.
- `docs/use-cases/casos-de-uso.md` — 11 casos de uso (CU-01..11) con flujos y puertos.
- `docs/use-cases/historias-de-usuario.md` — 9 HU con trazabilidad HU → CU → puertos.
- `docs/architecture/arquitectura-objetivo.md` — capas, Dependency Rule, ports/adapters, 4 ADRs.

## Convenciones obligatorias

- **Documentación en español** (docs, comentarios, commits).
- Todo artefacto codificado debe documentar: `[QUÉ]`, `[POR QUÉ]`, `[ALTERNATIVAS]` y `[RELACIONES]` (con qué casos de uso/puertos/adapters se conecta). Estándar definido en FASE 1 (`.opencode/rules/documentation.md`).
- **Ports/adapters desde el inicio**: las 4 APIs se modelan como adapters de puertos de dominio (`ProveedorPosiciones`, `ProveedorCalendario`, `ProveedorCuotas`), nunca se acoplan al dominio.
- Comandos en **Git Bash** (Windows), no cmd/PowerShell (regla heredada del proyecto previo).

## Convención de diagramas interactivos (OBLIGATORIA)

- **Todo diagrama que se genere** (modelo de dominio, arquitectura, ERD de FASE 8, flujos de casos de uso, etc.) debe permitir que **sus elementos principales sean clicables**: clases/artefactos y relaciones.
- Al hacer **click** en un elemento se abre un **dialog modal** que ilustra esa parte del diagrama: `QUÉ` hace, `RELACIÓN` (para conectores: el atributo exacto, ej: `Pronostico.partidoId → Partido.id`; para clases: con qué se relaciona y por qué atributo), `REGLAS` (BR-xx), `MÉTODOS` y `EVENTOS`.
- El hover **no oculta nada**: solo indica clicabilidad (cursor pointer, borde más grueso, subrayado de título). El detalle vive detrás del click, no en tooltips.
- El modal **no debe producir salto de scroll**: al bloquear el scroll de fondo, compensar el ancho de la barra con `padding-right` (medido antes de ocultarla) para que el lienzo no se desplace horizontalmente al abrir/cerrar.
- El lienzo permite **scroll horizontal** cuando el diagrama no cabe: el SVG nunca se comprime por debajo del ancho de su `viewBox` (ej: `min-width:1320px` en un `div.lienzo` con `overflow-x:auto`) para que los textos de las relaciones no se solapen.
- El lienzo muestra la vista estructural (densidad baja); el detalle se explica en el modal.
- Referencia de implementación: skill `.opencode/skills/diagramas-interactivos/SKILL.md` y ejemplo `diagrams/modelo-dominio.html`.
- Se usa junto al skill `diagram-design` (estilo visual) para los diagramas del proyecto.

## Entorno

- Directorio de trabajo: `ecosistema_java` (este repo). Proyecto Spring Boot compilando en la raíz (`build.gradle`, `src/`).
- Referencia de estudio (NO se modifica): `../contexto_java/pronosticos-futbol` (proyecto Java hexagonal previo con `.clinerules`) y `../../contextos_gpt/tipsterByte_fx` (backend Python original).
- Build: `./gradlew build` (Git Bash). `./gradlew bootRun` levanta la app en `localhost:8080`.