-- ─────────────────────────────────────────────
-- [QUÉ]: Crea tabla equipos_alias para el diccionario de alias de equipos (HU-14 AC4.2).
-- [POR QUÉ]: Wplay usa nombres distintos a los de la plantilla ("Fluminense RJ" vs
--            "Fluminense"). El resolutor multi-fuente usa esta tabla como tercera
--            estrategia de matching (después de exacto y difuso). Los alias se
--            auto-aprenden tras cada match difuso exitoso y admiten override manual
--            del SUPERADMIN.
-- [RELACIONES]: HU-14 AC4.2 → ResolutorEquipoExtraccion + EquipoAliasRepository.
-- ─────────────────────────────────────────────

CREATE TABLE equipos_alias (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fuente_tipo     VARCHAR(20) NOT NULL,
    nombre_externo  VARCHAR(200) NOT NULL,
    equipo_id       UUID NOT NULL,
    temporada_id    UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_equipos_alias_equipo
        FOREIGN KEY (equipo_id) REFERENCES equipos(id),
    CONSTRAINT fk_equipos_alias_temporada
        FOREIGN KEY (temporada_id) REFERENCES temporadas(id),
    CONSTRAINT uk_equipos_alias_fuente_nombre_temporada
        UNIQUE (fuente_tipo, nombre_externo, temporada_id)
);

CREATE INDEX idx_equipos_alias_temporada ON equipos_alias (temporada_id);
