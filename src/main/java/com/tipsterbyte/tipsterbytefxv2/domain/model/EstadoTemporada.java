// ─────────────────────────────────────────────
// [QUÉ]: Enum que representa el estado de una temporada deportiva.
// [POR QUÉ]: Permite modelar el ciclo de vida de una temporada (planificada, activa, finalizada)
//            y facilita consultas como "temporada actual" en una liga.
// [ALTERNATIVAS]: Boolean activo/inactivo; se descarta porque no distingue planificada de finalizada.
// [RELACIONES]: Usado por Temporada entity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum EstadoTemporada {
    PLANIFICADA,
    ACTIVA,
    FINALIZADA
}
