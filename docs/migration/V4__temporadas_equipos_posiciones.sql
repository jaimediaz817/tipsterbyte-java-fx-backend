-- V4__temporadas_equipos_posiciones.sql
-- Fase dedicada de temporadas: los equipos y la tabla de posiciones pasan a depender de
-- TEMPORADAS (no de ligas); las ligas ganan FK real al país del catálogo.
--
-- [POR QUÉ] ddl-auto=update crea columnas/FK nuevas pero no suelta las legadas. Las
--           columnas NOT NULL viejas (equipos.liga_id, posiciones_tabla.liga_id) romperían
--           todo INSERT nuevo porque Hibernate ya no las mapea.
-- [CUÁNDO] Ejecutar UNA vez contra tipsterbytefxv2_dev (localhost:5434) con la app
--          detenida, ANTES de arrancar bootRun tras este cambio.
-- [ORDEN]  Este script crea las columnas nuevas él mismo (idempotente con IF NOT
--          EXISTS): puede correr antes o después de un bootRun intermedio.
-- [DATOS]  Backfill incluido: si había filas legadas (liga_id), se les asigna la
--          primera temporada existente de esa liga. Si no quieres conservarlas,
--          usa los TRUNCATE del final en su lugar.

BEGIN;

-- 1) Columnas nuevas (Hibernate las creará igual si arrancas primero; IF NOT EXISTS evita duplicar).
ALTER TABLE equipos ADD COLUMN IF NOT EXISTS temporada_id UUID REFERENCES temporadas(id);
ALTER TABLE posiciones_tabla ADD COLUMN IF NOT EXISTS temporada_id UUID REFERENCES temporadas(id);
ALTER TABLE ligas ADD COLUMN IF NOT EXISTS pais_id UUID REFERENCES paises(id);

-- 2) Backfill desde el modelo legado (primera temporada de la liga).
UPDATE equipos e
SET temporada_id = t.id
FROM temporadas t
WHERE t.liga_id = e.liga_id AND e.temporada_id IS NULL;

UPDATE posiciones_tabla p
SET temporada_id = t.id
FROM temporadas t
WHERE t.liga_id = p.liga_id AND p.temporada_id IS NULL;

-- 3) Backfill de la FK de país por nombre (case-insensitive).
UPDATE ligas l
SET pais_id = pa.id
FROM paises pa
WHERE lower(pa.nombre) = lower(l.pais) AND l.pais_id IS NULL;

-- 4) Obligatorias las columnas nuevas (fallará si quedó alguna fila sin backfill:
--    en ese caso usa los TRUNCATE del paso 6 y re-ejecuta desde aquí).
ALTER TABLE equipos ALTER COLUMN temporada_id SET NOT NULL;
ALTER TABLE posiciones_tabla ALTER COLUMN temporada_id SET NOT NULL;

-- 5) Soltar las columnas legadas (sus FKs caen con la columna).
ALTER TABLE equipos DROP COLUMN IF EXISTS liga_id;
ALTER TABLE posiciones_tabla DROP COLUMN IF EXISTS liga_id;

COMMIT;

-- 6) Opción "dev desechable" (alternativa al backfill). Descomentar y ejecutar ANTES
--    del BEGIN si prefieres reiniciar el dato deportivo en lugar de migrarlo:
-- TRUNCATE cuotas, partidos, posiciones_tabla, equipos, detalle_fuentes_extraccion,
--          temporadas RESTART IDENTITY CASCADE;

-- Nota: ligas.pais_id queda nullable a propósito (ligas manuales sin catálogo); CU-10
-- siempre lo llena para ligas de catálogo. Endurecer a NOT NULL es decisión futura.
