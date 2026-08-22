-- V3__bridge_fix_temporadas.sql
-- Bridge Fix Torneos/Temporadas: alinea la BD dev con el modelo nuevo
-- (temporadas como tabla propia; partidos y detalle_fuentes_extraccion referencian
-- temporada_id en lugar de liga_id).
--
-- [POR QUÉ] ddl-auto=update crea las tablas/columnas nuevas (temporadas,
--           partidos.temporada_id, detalle_fuentes_extraccion.temporada_id) pero NO
--           elimina columnas antiguas ni constraints viejas. Las columnas legadas con
--           NOT NULL (ligas.temporada_anio_inicio, partidos.liga_id,
--           detalle_fuentes_extraccion.liga_id) romperían todo INSERT nuevo porque
--           Hibernate ya no las mapea.
-- [CUÁNDO] Ejecutar UNA vez contra tipsterbytefxv2_dev (localhost:5434) antes de
--          arrancar bootRun tras este cambio. Testcontainers NO lo necesita (esquema
--          fresco por test). Cuando llegue Flyway (FASE 19/20) se formaliza como V3.
-- [DATOS]  Opción A (dev desechable): TRUNCATE de partidos/detalles/temporadas/ligas y
--          re-poblar con CU-10 + activar ligas de nuevo.
--          Opción B (conservar datos): backfill manual creando filas en temporadas por
--          cada liga a partir de sus columnas legadas antes de soltarlas.

-- 1) Ligas: soltar las columnas planas de la temporada única (modelo anterior).
ALTER TABLE ligas DROP CONSTRAINT IF EXISTS ligas_temporada_anio_inicio_temporada_anio_fin_key;
ALTER TABLE ligas DROP COLUMN IF EXISTS temporada_anio_inicio;
ALTER TABLE ligas DROP COLUMN IF EXISTS temporada_anio_fin;

-- 2) Partidos: la FK pasa a ser temporada_id → temporadas.id (creada por Hibernate).
--    La columna legada liga_id es NOT NULL y ya no se mapea: se suelta.
ALTER TABLE partidos DROP CONSTRAINT IF EXISTS fk_partidos_liga;
ALTER TABLE partidos DROP COLUMN IF EXISTS liga_id;

-- 3) Detalle de fuentes: la asociación es (temporada_id, tipo); se suelta la UK vieja
--    por (liga_id, tipo) y la columna legada liga_id.
ALTER TABLE detalle_fuentes_extraccion DROP CONSTRAINT IF EXISTS uk_detalle_fuente_liga_tipo;
ALTER TABLE detalle_fuentes_extraccion DROP CONSTRAINT IF EXISTS fk_detalle_fuentes_extraccion_liga;
ALTER TABLE detalle_fuentes_extraccion DROP COLUMN IF EXISTS liga_id;

-- 4) Limpieza opcional de datos (Opción A, dev desechable). Descomentar si se prefiere
--    reiniciar el catálogo en lugar de hacer backfill:
-- TRUNCATE cuotas, partidos, posiciones_tabla, equipos, detalle_fuentes_extraccion,
--          temporadas, ligas RESTART IDENTITY CASCADE;

-- Nota: paises_interes.max_ligas_por_pais la crea ddl-auto=update automáticamente
-- (campo maxLigasPorPais del entity PaisInteres, CU-14). No requiere acción aquí.
