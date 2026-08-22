# Hallazgos de arquitectura — diagnóstico post-FASE T2

> [QUÉ]: Registro de hallazgos estructurales detectados tras el poblamiento geográfico con fuente #6 (equipos por liga, 426 tests en verde). Priorizados por criticidad con impacto, causa y recomendación. Base para planificar las próximas FASEs.
> [POR QUÉ]: Tras convertir `Temporada` en el centro del modelo y encadenar #6 en CU-10, el flujo de poblamiento escala a cientos de ligas; varios supuestos del diseño inicial (activación manual por URL, poblamiento síncrono, ddl-auto) dejan de ser sostenibles y deben abordarse antes de FASE 17/19.
> [RELACIONES]: Afecta CU-04, CU-10, CU-15, `Liga`/`Temporada`/`Equipo`, `LigaRepositoryJpaAdapter`, `CatalogoScheduler`, `SecurityConfig`, `docs/PROYECTO-PLAN.md` (FASE 15/17/19).
> Estado: 📝 DIAGNÓSTICO — pendiente priorización y aprobación de próximas FASEs.

---

## Contexto

Al cierre de FASE T2 el sistema dispone de:

- Poblamiento geográfico completo: `#1` países (cache) → `#5` ligas por país (limit=`maxLigasPorPais`) → temporada PLANIFICADA por liga con `pais_id` FK → **#6 equipos con escudos** (solo países de interés, cache, matching normalizado).
- Scheduler CU-15 operativo (tarea global = poblamiento; tareas por liga+tipo para #2/#3/#4).
- Modelo deportivo centrado en `Temporada` (equipos y posiciones cuelgan de ella).

---

## 🔴 CRÍTICO — H-01: Activación manual de ligas y transición de temporada

- [QUÉ]: `ActivarLigaUseCase.ejecutar(ligaId, comando{urlPosiciones, urlCalendario, urlCuotas})` rechaza (BR-001) si falta alguna de las 3 URLs. **Decisión confirmada: la asignación es 100% manual** — el admin consulta las 3 fuentes operativas y pega cada URL en el flujo; no habrá derivación automática para evitar inconsistencias. Además, ninguna operación cambiaba `Temporada.estado` de PLANIFICADA a ACTIVA: `Liga.getTemporadaActual()` siempre vacío y toda delegación caía al fallback "primera registrada".
- [POR QUÉ]: La temporada en ejecución debe ser ACTIVA para que `actualizarPosiciones()` y la delegación distingan historial (un descendido en 2024 no debe aparecer en 2025). Sin transición, no existe "temporada en ejecución" en runtime.
- [ESTADO]: ✅ Parcialmente resuelto — `Liga.activar()` ahora transita `PLANIFICADA → ACTIVA` en su temporada vigente (`Temporada.activar()` idempotente, `Temporada.finalizar()` para `ACTIVA → FINALIZADA`). La asignación de URLs permanece manual por decisión explícita.
- [RECOMENDACIÓN pendiente]: Exponer transición explícita si se requiere control fino por temporada: `POST /temporadas/{id}/activar` y `POST /temporadas/{id}/finalizar` (hoy solo ocurre implícitamente al activar la liga).
- [ALTERNATIVAS descartadas]: Derivar URLs automáticamente desde `urlSoccerway`; se descarta por decisión de negocio (riesgo de incoherencias).
- [AFECTA]: `ActivarLigaUseCase`, `Liga`, `Temporada`, `docs/REFactor-Torneos-Temporadas.md`.

---

## 🔴 CRÍTICO — H-02: Poblamiento manual síncrono sin progreso ni trazabilidad

- [QUÉ]: `POST /api/v1/catalogo/activar` ejecuta CU-10 de forma síncrona en el hilo HTTP. Con 176 países × ligas × #6 puede tardar 10-30 min: timeout del navegador, spinner infinito, sin feedback. El scheduler sí registra `tarea_log` + anti-solapamiento; la vía manual no deja rastro.
- [POR QUÉ]: Un admin dispara y se queda a ciegas; un segundo clic duplica la carga sin saber si el primero sigue corriendo.
- [IMPACTO]: Experiencia de usuario rota en el flujo principal; imposible monitorear/reintentar.
- [RECOMENDACIÓN]: Volverlo asíncrono (`202 Accepted` + `executionId`/`tareaLog`) con endpoint de progreso/estado, o al menos registrar `TareaLog` también para ejecuciones manuales. El mecanismo del scheduler es reutilizable.
- [ALTERNATIVAS]: Mantener síncrono con timeout largo; se descarta por UX y por riesgo de doble ejecución.
- [AFECTA]: `CatalogoController`, CU-10, `TareaLog`, observabilidad (FASE 16).

---

## 🟠 ALTA — H-03: Sin endpoint para pintar la plantilla poblada

- [QUÉ]: No existe `GET /ligas/{id}/equipos` (ni `/temporadas/{id}/equipos`). El frontend no puede mostrar los equipos/escudos que ya persisten tras #6.
- [POR QUÉ]: Cierra el ciclo visible de HU-11. El badge `totalEquipos` añadido a `LigaResponse` solo da el conteo; la plantilla completa requiere el listado.
- [RECOMENDACIÓN]: `GET /api/v1/ligas/{id}/equipos` (o por temporada) — lectura paginada opcional, roles SUPERADMIN/TIPSTER/CLIENTE según pantalla.
- [AFECTA]: `LigaController`, `TemporadaRepository`, nuevo DTO `EquipoResponse`.

---

## 🟠 ALTA — H-04: Discrepancia de nombres entre fuentes (hasta FASE 17)

- [QUÉ]: `#3` Flashscore y `#6` Soccerway escriben el mismo club distinto ("Gimnasia Mendoza" vs "Gimnasia y Esgrima Mendoza"). El normalizador centralizado resuelve tildes/case/espacios, pero no abreviaturas. La regla actual es exacta-normalizada; #6 nunca elimina, así que no se pierde dato, pero no hay señal de discrepancia.
- [POR QUÉ]: Plantillas incompletas o posiciones huérfanas pasan inadvertidas hasta que un usuario las reporta.
- [RECOMENDACIÓN]: Añadir reporte de discrepancias (liga con 30 equipos en #6 pero 0 posiciones sincronizadas; equipo en posiciones sin match en plantilla) como log WARN estructurado o endpoint de diagnóstico, como puente hasta FASE 17 (fuzzy matching).
- [AFECTA]: `NormalizadorNombresEquipos`, CU-01/CU-02/CU-10, futuro `domain.service` fuzzy.

---

## 🟡 MEDIA — H-05: `ddl-auto=update` en lugar de Flyway

- [QUÉ]: Ya van 4 migraciones manuales (`V3` bridge fix, `V4` equipos/posiciones→temporada, `V4` equipos #6) aplicadas a mano en dev. Una instancia nueva que arranque sin esos `.sql` queda con columnas legadas `liga_id` NOT NULL y falla silenciosamente.
- [POR QUÉ]: No auditable ni reproducible; riesgo de drift entre dev/Testcontainers/prod.
- [RECOMENDACIÓN]: Implementar FASE 19/20 (Flyway) antes de escalar el número de nodos o el equipo.
- [AFECTA]: `docs/migration/`, `application.properties` (`ddl-auto`), pipeline CI.

---

## 🟡 MEDIA — H-06: Cortesía con el scraper Python

- [QUÉ]: El poblamiento hace cientos de llamadas secuenciales a `http://127.0.0.1:8001` sin pausa, backoff ni circuit breaker. Existe tolerancia por liga (log & continue) pero no reintento ni límite de concurrencia.
- [POR QUÉ]: Primera corrida masiva con 429/baneo deja el catálogo a medias y obliga a re-ejecutar completo (idempotente + cache ayudan, pero el tiempo se duplica).
- [RECOMENDACIÓN]: Añadir pausa breve entre países, reintento con backoff exponencial en los adapters (#5/#6), y evaluar paralelismo controlado con hilos virtuales (como ya hace el scheduler) para fases futuras.
- [AFECTA]: `SoccerwayLigasPorPaisAdapter`, `SoccerwayEquiposAdapter`, CU-10.

---

## 🟡 MEDIA — H-07: Cache de equipos y su refresco programado

- [QUÉ]: `equipos` tiene TTL largo (30 días) e invalidación al inicio de cada poblamiento de esa liga. No existe tarea programable tipo EQUIPOS por liga (hoy solo entra vía tarea global o botón manual `POST /ligas/{id}/equipos/sincronizar`).
- [POR QUÉ]: Si el roster cambia a mitad de temporada, solo se refresca al re-poblar todo el mundo o al pulsar el botón manual por liga — no hay automatización por liga como sí existe para posiciones/calendario/cuotas.
- [RECOMENDACIÓN]: Evaluar si se quiere un tipo de tarea `EQUIPOS` por liga (mismo patrón que STANDINGS/CALENDAR/ODDS_WPLAY). Cambio pequeño en backend si se decide.
- [AFECTA]: `CacheClaves.equipos`, `ProveedorEquiposCacheable`, CU-15, `CatalogoScheduler`.

---

## Orden de ataque recomendado

1. **H-01 + H-02 juntos** — hacen operable el volumen ya habilitado.
2. **H-03** — cierra el ciclo visible de HU-11 (trivial, alto valor UX).
3. **H-05** — antes de escalar equipo/nodos.
4. **H-04 / H-06 / H-07** — como refuerzos antes de FASE 17.

---

*Registrado tras FASE T2 (426 tests en verde). Próximos pasos requieren aprobación explícita por fase.*
