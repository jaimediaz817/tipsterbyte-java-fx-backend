-- ─────────────────────────────────────────────
-- [QUÉ]: Eliminar columna estadio de equipos (campo temporal de verificación del skill).
-- [POR QUÉ]: Segunda dirección de la verificación bidireccional: quitar campo implica DROP COLUMN + revertir Entity/dominio/mapper en el mismo commit.
-- [POR QUÉ]: 
-- [RELACIONES]: 


ALTER TABLE equipos DROP COLUMN estadio;
