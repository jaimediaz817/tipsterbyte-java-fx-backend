-- ─────────────────────────────────────────────
-- [QUÉ]: Agrega la columna url_base_fuente al catálogo de fuentes (fuentes_extraccion)
--        y hace backfill con la base del scraper Python para las filas existentes.
-- [POR QUÉ]: El SUPERADMIN necesita, desde el formulario de activación de liga, un
--            ENLACE a la fuente base de cada tipo para construir manualmente la URL
--            específica (país/liga) sin recurrir a apuntes externos. Hoy el catálogo
--            solo guarda id/activa/nombre/tipo. Backfill: todas las fuentes actuales
--            (#2/#3/#4/#6) viven en el mismo scraper Python, así que esa base es el
--            valor razonable de partida (editable después vía PUT /fuentes/{tipo}).
-- [RELACIONES]: CU-11 (GestionarFuenteExtraccionUseCase) + FuenteExtraccion (dominio) +
--               FuenteExtraccionEntity (JPA) + FuenteExtraccionResponse (REST).
--               Patrón expandir-contrato: nullable primero; endurecer NOT NULL solo si
--               el producto lo exige más adelante.
-- ─────────────────────────────────────────────

ALTER TABLE fuentes_extraccion ADD COLUMN url_base_fuente VARCHAR(500);

UPDATE fuentes_extraccion SET url_base_fuente = 'http://127.0.0.1:8001' WHERE url_base_fuente IS NULL;
