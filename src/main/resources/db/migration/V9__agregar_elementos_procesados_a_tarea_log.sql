-- ─────────────────────────────────────────────
-- [QUÉ]: Agrega columna elementos_procesados a tarea_log (HU-14 AC7).
-- [POR QUÉ]: Permite al frontend mostrar el conteo explícito de elementos procesados
--            (ej: "sin datos aún" cuando elementosProcesados=0) sin parsear el texto
--            del campo mensaje. Nullable para compatibilidad con registros existentes.
-- [RELACIONES]: HU-14 → TareaLog (domain) + TareaLogEntity (JPA) +
--               CatalogoScheduler (persiste el conteo).
-- ─────────────────────────────────────────────

ALTER TABLE tarea_log
    ADD COLUMN elementos_procesados INTEGER NULL;
