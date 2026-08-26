-- ─────────────────────────────────────────────
-- [QUÉ]: Agrega columna primer_disparo a tareas_programadas (HU-14 AC3).
-- [POR QUÉ]: Permite que la primera ejecución de una tarea programada sea
--            postergada: mientras now() < primer_disparo, la tarea no corre
--            aunque su cron indique lo contrario. Útil para que el SUPERADMIN
--            defina una tarea y la ejecute en un horario específico (ej: "a partir
--            de las 18:00 de hoy"). Null = sin postergación (ejecuta según cron).
-- [RELACIONES]: HU-14 → TareaProgramada (domain) + CatalogoScheduler
--               (evalúa primer_disparo antes de despachar).
-- ─────────────────────────────────────────────

ALTER TABLE tareas_programadas
    ADD COLUMN primer_disparo TIMESTAMPTZ NULL;
