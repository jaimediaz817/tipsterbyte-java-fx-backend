-- ─────────────────────────────────────────────
-- [QUÉ]: Ampliar iso_alpha2 de varchar(2) a varchar(10) en paises y paises_interes.
-- [POR QUÉ]: La fuente #1 devuelve GB-ENG/GB-SCT/GB-WLS/GB-NIR (6 chars) para
--            Inglaterra/Escocia/Gales/Irlanda del Norte; varchar(2) revienta con
--            "value too long for type character varying(2)" al poblar países
--            (SincronizarPaisesUseCase → insert into paises). Ampliar a 10 cubre
--            el formato extendido sin romper los ISO de 2 letras estándar.
-- [RELACIONES]: PaisEntity.isoAlpha2 (length 10) / PaisInteresEntity.isoAlpha2 (10).
-- ─────────────────────────────────────────────
ALTER TABLE paises ALTER COLUMN iso_alpha2 TYPE VARCHAR(10);
ALTER TABLE paises_interes ALTER COLUMN iso_alpha2 TYPE VARCHAR(10);
