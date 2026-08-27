-- ─────────────────────────────────────────────
-- [QUÉ]: Migración V12 — tablas para HU-16 (Estrategias de pronóstico).
-- [POR QUÉ]: Crea las tablas de estrategias, criterios embebidos (JSONB),
--            zona de descenso por temporada, señales pre-computadas y
--            pronósticos sugeridos.
-- [RELACIONES]: HU-16 → CU-23/24/25 → EstrategiaRepository, ZonaDescensoRepository,
--               SenalPartidoRepository.
-- ─────────────────────────────────────────────

-- Estrategias: aggregate root
CREATE TABLE estrategias (
    id UUID PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    tipster_id UUID NOT NULL,
    mercado VARCHAR(30) NOT NULL,
    max_partidos INTEGER,
    confianza_minima DECIMAL(3,2),
    activa BOOLEAN NOT NULL DEFAULT true,
    liga_ids UUID[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT estrategias_tipster_id_fk FOREIGN KEY (tipster_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_estrategias_tipster_id ON estrategias(tipster_id);
CREATE INDEX idx_estrategias_activa ON estrategias(activa);

-- Criterios: embebidos como JSONB dentro de cada estrategia
-- (no tabla separada; el dominio los modela como lista en Estrategia)
-- Se almacenan en columna JSONB para flexibilidad de esquema.
ALTER TABLE estrategias ADD COLUMN criterios JSONB NOT NULL DEFAULT '[]';

-- Zona de descenso por temporada
CREATE TABLE zona_descenso (
    id UUID PRIMARY KEY,
    temporada_id UUID NOT NULL,
    posicion_descenso INTEGER NOT NULL CHECK (posicion_descenso >= 1),
    descripcion VARCHAR(255),
    CONSTRAINT zona_descenso_temporada_id_fk FOREIGN KEY (temporada_id) REFERENCES temporadas(id),
    CONSTRAINT zona_descenso_temporada_uk UNIQUE (temporada_id)
);

-- Señales pre-computadas (AC9, opcional fase 2)
CREATE TABLE senal_partido (
    id UUID PRIMARY KEY,
    partido_id UUID NOT NULL,
    fuente VARCHAR(30) NOT NULL,
    campo VARCHAR(50) NOT NULL,
    valor VARCHAR(255) NOT NULL,
    calculada_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT senal_partido_partido_id_fk FOREIGN KEY (partido_id) REFERENCES partidos(id)
);

CREATE INDEX idx_senal_partido_partido_id ON senal_partido(partido_id);
CREATE INDEX idx_senal_partido_fuente_campo ON senal_partido(fuente, campo);

-- Pronósticos sugeridos
CREATE TABLE pronostico_sugerido (
    id UUID PRIMARY KEY,
    estrategia_id UUID NOT NULL,
    partido_id UUID NOT NULL,
    score DECIMAL(4,3) NOT NULL CHECK (score >= 0 AND score <= 1),
    criterios_cumplidos INTEGER NOT NULL DEFAULT 0,
    criterios_fallidos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pronostico_sugerido_estrategia_id_fk FOREIGN KEY (estrategia_id) REFERENCES estrategias(id) ON DELETE CASCADE,
    CONSTRAINT pronostico_sugerido_partido_id_fk FOREIGN KEY (partido_id) REFERENCES partidos(id)
);

CREATE INDEX idx_pronostico_sugerido_estrategia_id ON pronostico_sugerido(estrategia_id);
CREATE INDEX idx_pronostico_sugerido_partido_id ON pronostico_sugerido(partido_id);
