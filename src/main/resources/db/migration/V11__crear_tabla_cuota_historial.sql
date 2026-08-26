-- ─────────────────────────────────────────────
-- [QUÉ]: Crea tabla cuota_historial para el registro append-only de cuotas (HU-14 AC4.5).
-- [POR QUÉ]: Cada sincronización horaria registra TODAS las cuotas observadas (sin
--            deduplicación) para que HU-15 pueda calcular volatilidad y mostrar
--            series temporales. Escritura incondicional: si la cuota no cambió, se
--            registra igual (permite detectar rebotes). Volumen acotado (~1.440 filas/
--            día por liga con ciclo horario).
-- [RELACIONES]: HU-14 AC4.5 → CuotaHistorialRepository (puerto) +
--               SincronizarCuotasUseCase (escritura); HU-15 (lectura).
-- ─────────────────────────────────────────────

CREATE TABLE cuota_historial (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partido_id      UUID NOT NULL,
    mercado         VARCHAR(20) NOT NULL,
    seleccion       VARCHAR(100) NULL,
    valor           NUMERIC(10,2) NOT NULL,
    fuente          VARCHAR(20) NOT NULL DEFAULT 'ODDS_WPLAY',
    capturada_en    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_cuota_historial_partido
        FOREIGN KEY (partido_id) REFERENCES partidos(id)
);

CREATE INDEX idx_cuota_historial_partido_fecha
    ON cuota_historial (partido_id, capturada_en);
