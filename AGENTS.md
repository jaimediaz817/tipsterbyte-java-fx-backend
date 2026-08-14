# AGENTS.md — tipsterbyte-fx-v2

Proyecto de aprendizaje/portafolio Java para proceso de selección. Plataforma de pronósticos de fútbol que consume 4 APIs externas (football-data.org, API-Football, The Odds API, SharpAPI) con arquitectura limpia + DDD.

## Estado actual — CRÍTICO

- El repo está en **FASE 5 completada (Domain + DDD)**. Modelo de dominio completo en `domain.model` (enums, VOs, entities, aggregates `Liga/Partido/Pronostico/Suscripcion` con BR-001..008), eventos en `domain.event` + interfaz `DomainEvent`. 52 tests unitarios en verde. FASE 4 (Clean Architecture) y FASE 5 aún **sin commitear**.
- Avanzamos **una fase a la vez** (ver `docs/PROYECTO-PLAN.md`), con aprobación explícita del usuario entre fases. **NUNCA** ejecutar fases adelantadas ni "construir el proyecto completo".
- Próxima: FASE 6 (Application + Use Cases).

## Configuración de OpenCode (FASE 1)

- `.opencode/rules/` — 5 reglas siempre activas (architecture, java, documentation, naming, testing), registradas en `opencode.json` vía `instructions`.
- `.opencode/skills/` — 6 skills iniciales: java-engineering, spring-boot-engineering, clean-architecture, ddd-domain-modeling, java-documentation, architecture-review. Más adelante: docker-engineering, persistence-jpa, testing-java, spring-security, redis-engineering, messaging-rabbitmq, spring-webflux, observability, aws-engineering.
- Reglas y skills se cargan al reiniciar OpenCode; no hay hot-reload.

## Fuentes de verdad (leer antes de tocar nada)

- `docs/PROYECTO-PLAN.md` — las 22 fases y el orden de trabajo.
- `docs/project-definition.md` — nombre, problema, usuarios, funcionalidades.
- `docs/domain/modelo-dominio.md` — DDD: aggregates (`Liga`, `Partido`, `Pronostico`, `Suscripcion`), VOs, 8 reglas de negocio (BR-001..008), eventos.
- `docs/use-cases/casos-de-uso.md` — 9 casos de uso (CU-01..09) con flujos y puertos.
- `docs/use-cases/historias-de-usuario.md` — 9 HU con trazabilidad HU → CU → puertos.
- `docs/architecture/arquitectura-objetivo.md` — capas, Dependency Rule, ports/adapters, 4 ADRs.

## Convenciones obligatorias

- **Documentación en español** (docs, comentarios, commits).
- Todo artefacto codificado debe documentar: `[QUÉ]`, `[POR QUÉ]`, `[ALTERNATIVAS]` y `[RELACIONES]` (con qué casos de uso/puertos/adapters se conecta). Estándar definido en FASE 1 (`.opencode/rules/documentation.md`).
- **Ports/adapters desde el inicio**: las 4 APIs se modelan como adapters de puertos de dominio (`ProveedorPosiciones`, `ProveedorCalendario`, `ProveedorCuotas`), nunca se acoplan al dominio.
- Comandos en **Git Bash** (Windows), no cmd/PowerShell (regla heredada del proyecto previo).

## Entorno

- Directorio de trabajo: `ecosistema_java` (este repo). Proyecto Spring Boot compilando en la raíz (`build.gradle`, `src/`).
- Referencia de estudio (NO se modifica): `../contexto_java/pronosticos-futbol` (proyecto Java hexagonal previo con `.clinerules`) y `../../contextos_gpt/tipsterByte_fx` (backend Python original).
- Build: `./gradlew build` (Git Bash). `./gradlew bootRun` levanta la app en `localhost:8080`.