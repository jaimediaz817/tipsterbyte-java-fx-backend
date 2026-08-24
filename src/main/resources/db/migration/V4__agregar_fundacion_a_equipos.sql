-- ─────────────────────────────────────────────
-- [QUÉ]: Agregar columna fundacion (año de fundación del club, nullable) a equipos.
-- [POR QUÉ]: Ejercicio 1 del tutorial — campo nuevo sobre tabla con registros
--            (patrón expandir-contrato: nullable primero).
-- [RELACIONES]: domain.model.Equipo.fundacion() / EquipoEntity.getFundacion().
-- ─────────────────────────────────────────────
ALTER TABLE equipos ADD COLUMN fundacion INTEGER;
