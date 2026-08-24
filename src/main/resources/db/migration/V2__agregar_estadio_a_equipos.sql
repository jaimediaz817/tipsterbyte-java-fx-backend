-- ─────────────────────────────────────────────
-- [QUÉ]: Agregar columna estadio (nullable) a la tabla equipos.
-- [POR QUÉ]: Verificación bidireccional del flujo de migraciones del skill migrations-ddl-flyway-auto.
-- [RELACIONES]: 


ALTER TABLE equipos ADD COLUMN estadio VARCHAR(120);
