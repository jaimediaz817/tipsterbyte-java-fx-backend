// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa un alias de equipo para matching multi-fuente.
//        Asocia un nombre externo (ej: "Fluminense RJ" de Wplay) con el equipo
//        canónico de la plantilla, para una fuente y temporada concretas.
// [POR QUÉ]: HU-14 AC4.2 — el diccionario de alias permite que el resolutor
//            encuentre equipos cuyos nombres difieren entre fuentes. Los alias se
//            auto-aprenden tras cada match difuso exitoso y admiten override manual.
// [ALTERNATIVAS]: Guardar alias en memoria; se descarta porque se pierden al reiniciar.
// [RELACIONES]: ResolutorEquipoExtraccion (lee) + EquipoAliasRepository (persiste).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.util.UUID;

public record EquiposAlias(
        UUID id,
        TipoFuenteExtraccion fuenteTipo,
        String nombreExterno,
        UUID equipoId,
        UUID temporadaId) {

    public EquiposAlias(TipoFuenteExtraccion fuenteTipo, String nombreExterno,
                         UUID equipoId, UUID temporadaId) {
        this(UUID.randomUUID(), fuenteTipo, nombreExterno, equipoId, temporadaId);
    }
}
