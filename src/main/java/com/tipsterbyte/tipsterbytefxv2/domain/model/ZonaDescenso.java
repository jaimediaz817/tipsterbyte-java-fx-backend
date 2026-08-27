// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa la zona de descenso configurada para una temporada.
// [POR QUÉ]: HU-16 AC4 — la posición que determina descenso varía por liga/temporada.
//            Se configura por temporada (no genérica por liga) porque puede cambiar
//            entre ediciones del mismo torneo.
// [ALTERNATIVAS]: VU por liga genérica; se descarta porque no permite variación por edición.
// [RELACIONES]: HU-16 AC5/AC6 — `PosicionTabla.posicion >= zonaDescenso.posicionDescenso`.
//               Consultado por CU-24 en evaluación de criterios ZONA_DESCENSO/REACCION_DESCENSO.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.UUID;

public final class ZonaDescenso {

    private final UUID id;
    private final UUID temporadaId;
    private final Integer posicionDescenso;
    private final String descripcion;

    public ZonaDescenso(UUID temporadaId, Integer posicionDescenso, String descripcion) {
        this(UUID.randomUUID(), temporadaId, posicionDescenso, descripcion);
    }

    public ZonaDescenso(UUID id, UUID temporadaId, Integer posicionDescenso, String descripcion) {
        if (id == null) throw new DomainException("ZonaDescenso requiere id");
        if (temporadaId == null) throw new DomainException("ZonaDescenso requiere temporadaId");
        if (posicionDescenso == null || posicionDescenso < 1) {
            throw new DomainException("posicionDescenso debe ser >= 1");
        }
        this.id = id;
        this.temporadaId = temporadaId;
        this.posicionDescenso = posicionDescenso;
        this.descripcion = descripcion;
    }

    // [QUÉ]: Indica si una posición está en zona de descenso.
    // [POR QUÉ]: HU-16 AC6 — un equipo está "en zona" si su posición >= posicionDescenso.
    public boolean enZonaDescenso(int posicion) {
        return posicion >= posicionDescenso;
    }

    public UUID id() { return id; }
    public UUID temporadaId() { return temporadaId; }
    public Integer posicionDescenso() { return posicionDescenso; }
    public String descripcion() { return descripcion; }
}
