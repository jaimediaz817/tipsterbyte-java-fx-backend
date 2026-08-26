-- ─────────────────────────────────────────────
-- [QUÉ]: Recrear los CHECK constraints de tipo de fuente para incluir 'EQUIPOS'
--        en fuentes_extraccion, detalle_fuentes_extraccion y tareas_programadas.
-- [POR QUÉ]: La BD dev fue creada por ddl-auto=update ANTES de adoptar Flyway
--            (FASE H-05); baseline-on-migrate=true marcó V1 como aplicada sin
--            reconciliar el esquema existente, dejando constraints viejas sin
--            'EQUIPOS'. Al poblar ligas por país (HU-12/CU-18), el registro de
--            la fuente #6 (ProveedorEquiposPorLiga/SoccerwayEquiposAdapter,
--            TipoFuente.EQUIPOS) violaba el CHECK y la extracción fallaba con
--            500 silencioso. Esta migración alinea la BD con V1 en cualquier
--            entorno que heredó el esquema legacy.
-- [ALTERNATIVAS]: Reset del volumen de Docker (docker compose down -v); se
--            descarta porque destruye datos de desarrollo (usuarios, catálogo).
-- [RELACIONES]: V1__baseline.sql (líneas 14/16/25), TipoFuenteEnum del dominio,
--               CU-11 (GestionarFuenteExtraccionUseCase), CU-18
--               (SincronizarLigasPorPaisUseCase), fuente #6 SoccerwayEquiposAdapter.
-- ─────────────────────────────────────────────

ALTER TABLE fuentes_extraccion DROP CONSTRAINT fuentes_extraccion_tipo_check;
ALTER TABLE fuentes_extraccion ADD CONSTRAINT fuentes_extraccion_tipo_check
    CHECK (tipo IN ('STANDINGS','ODDS_WPLAY','CALENDAR','EQUIPOS'));

ALTER TABLE detalle_fuentes_extraccion DROP CONSTRAINT detalle_fuentes_extraccion_tipo_check;
ALTER TABLE detalle_fuentes_extraccion ADD CONSTRAINT detalle_fuentes_extraccion_tipo_check
    CHECK (tipo IN ('STANDINGS','ODDS_WPLAY','CALENDAR','EQUIPOS'));

ALTER TABLE tareas_programadas DROP CONSTRAINT tareas_programadas_tipo_fuente_check;
ALTER TABLE tareas_programadas ADD CONSTRAINT tareas_programadas_tipo_fuente_check
    CHECK (tipo_fuente IN ('STANDINGS','ODDS_WPLAY','CALENDAR','EQUIPOS'));
