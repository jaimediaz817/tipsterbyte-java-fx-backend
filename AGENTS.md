# AGENTS.md — tipsterbyte-fx-v2

Proyecto de aprendizaje/portafolio Java para proceso de selección. Plataforma de pronósticos de fútbol que consume 4 APIs externas (football-data.org, API-Football, The Odds API, SharpAPI) con arquitectura limpia + DDD.

## Estado actual — CRÍTICO

- El repo está en **FASE 6 completada (Application + Use Cases)**. Modelo de dominio completo en `domain.model` (enums, VOs, entities, aggregates `Liga/Partido/Pronostico/Suscripcion` con BR-001..008), eventos en `domain.event` + interfaz `DomainEvent`. Capa application con los 9 casos de uso (CU-01..09) en `application.usecase`, puertos `ProveedorPosiciones/Calendario/Cuotas` + 4 repositorios en `application.port`, DTOs en `application.dto`. 80 tests en verde. FASE 4, 5 y 6 **sin commitear**.
- Avanzamos **una fase a la vez** (ver `docs/PROYECTO-PLAN.md`), con aprobación explícita del usuario entre fases. **NUNCA** ejecutar fases adelantadas ni "construir el proyecto completo".
- Próxima: FASE 7 (REST API).

## Configuración de OpenCode (FASE 1)

- `.opencode/rules/` — 5 reglas siempre activas (architecture, java, documentation, naming, testing), registradas en `opencode.json` vía `instructions`.
- `.opencode/skills/` — skills activas: java-engineering, spring-boot-engineering, clean-architecture, ddd-domain-modeling, java-documentation, architecture-review, diagram-design (estilo visual), diagramas-interactivos (capas de interacción, OBLIGATORIA en todo diagrama). Más adelante: docker-engineering, persistence-jpa, testing-java, spring-security, redis-engineering, messaging-rabbitmq, spring-webflux, observability, aws-engineering.
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