// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-04 (HU-04): activa una liga solo cuando sus fuentes de datos
//        están operativas.
// [POR QUÉ]: Aplica BR-001 delegando en el aggregate Liga. El caso de uso traslada la
//            disponibilidad de fuentes (conocida por infraestructura/config) al dominio.
// [ALTERNATIVAS]: Preguntar la disponibilidad al proveedor; se descarta porque la
//                 disponibilidad es config de infraestructura, no del dominio.
// [RELACIONES]: HU-04 → CU-04 → LigaRepository + DTO DisponibilidadFuentes.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.DisponibilidadFuentes;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;

import java.util.List;
import java.util.UUID;

public final class ActivarLigaUseCase {

    private final LigaRepository ligaRepository;

    // [QUÉ]: Construye el caso de uso con su puerto (inyección por constructor).
    public ActivarLigaUseCase(LigaRepository ligaRepository) {
        this.ligaRepository = ligaRepository;
    }

    // [QUÉ]: Ejecuta CU-04: activa la liga y persiste. Devuelve el evento LigaActivada.
    public List<DomainEvent> ejecutar(UUID ligaId, DisponibilidadFuentes disponibilidad) {
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));

        liga.activar(disponibilidad.posiciones(), disponibilidad.calendario(), disponibilidad.cuotas());
        ligaRepository.guardar(liga);
        return liga.pullEventos();
    }
}