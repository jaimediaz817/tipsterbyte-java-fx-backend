# tipsterbyte-fx-v2

Plataforma de pronósticos deportivos de fútbol construida sobre el ecosistema Java (Spring Boot) para demostrar arquitectura limpia, DDD y buenas prácticas end-to-end.

> **Objetivo de aprendizaje**: no construir un CRUD al azar, sino un proyecto que progresivamente obligue a tocar POO, SOLID, Clean Architecture, DDD, REST, PostgreSQL, Docker, testing, Spring Security, Redis, RabbitMQ, WebFlux, concurrencia y observabilidad. Cada decisión queda documentada y defendible.

## Contexto del negocio (tipsterbyte)

La plataforma consume datos deportivos de fútbol desde **4 APIs externas** (scrapers) y los estructura para generar pronósticos. Las 3 fuentes de extracción de datos del proyecto son:

1. **Tabla de posiciones** de todas las ligas del mundo.
2. **Calendario completo** de partidos jugados y pendientes por jugar de cada liga.
3. **Cuotas** de los partidos próximos a jugar (tipo página de apuestas como Wplay).

## APIs disponibles para consumir

| API | Rol | Uso |
| --- | --- | --- |
| football-data.org | Fuente secundaria | Ligas y posiciones (top europeas) |
| API-Football (api-sports.io) | Fuente principal | Ligas, fixtures, odds |
| The Odds API | Backup odds | Cuotas |
| SharpAPI | Backup odds #2 | Cuotas |

## Plan maestro

Ver `docs/PROYECTO-PLAN.md` para el plan completo de 22 fases. Avanzamos una fase a la vez, con aprobación explícita entre fases.

## Documentación

- `docs/project-definition.md` — Definición del proyecto (FASE 0)
- `docs/domain/` — Modelo de dominio (DDD)
- `docs/use-cases/` — Casos de uso
- `docs/architecture/` — Arquitectura objetivo (ports/adapters)

## Estado actual

- [x] FASE 0 — Definición del proyecto
- [ ] FASE 1 — OpenCode / Cline: Rules + Skills
- [ ] FASE 2 — Spring Initializr: bootstrap
- [ ] FASE 3 — Git + estructura base
- [ ] ... (resto de fases del plan)
