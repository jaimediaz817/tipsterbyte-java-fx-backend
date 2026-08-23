// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso H-04: detecta pares de equipos sospechosos de ser el mismo club
//        duplicado en la temporada vigente de una liga (solo DETECCIÓN, sin fusión).
// [POR QUÉ]: Puente de diagnóstico hasta el fuzzy matching de FASE 17. El admin revisa
//            los pares reportados y decide; el sistema nunca elimina ni fusiona
//            automáticamente (un falso positivo borraría un club real).
// [ALTERNATIVAS]: Fusión manual vía endpoint (merge dos equipos conservando
//                 referencias); se difiere a FASE 17 por su complejidad referencial.
// [RELACIONES]: H-04 → LigaRepository (lectura) + DetectorDuplicadosEquipos (dominio);
//               expuesto por GET /ligas/{id}/equipos/discrepancias.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.domain.service.DetectorDuplicadosEquipos;

import java.util.List;
import java.util.UUID;

public final class DetectarDiscrepanciasEquiposUseCase {

    // [QUÉ]: Resultado del diagnóstico para una liga.
    public record DiscrepanciasLiga(
            UUID ligaId,
            UUID temporadaId,
            String temporadaNombre,
            int totalPares,
            List<DetectorDuplicadosEquipos.ParSospechoso> pares) {
    }

    private final LigaRepository ligaRepository;

    public DetectarDiscrepanciasEquiposUseCase(LigaRepository ligaRepository) {
        this.ligaRepository = ligaRepository;
    }

    // [QUÉ]: Ejecuta el detector sobre la plantilla de la temporada vigente.
    public DiscrepanciasLiga ejecutar(UUID ligaId) {
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new DomainException("Liga no encontrada: " + ligaId));
        Temporada temporada = liga.getTemporadaActual()
                .or(() -> liga.getTemporadas().stream().findFirst())
                .orElseThrow(() -> new DomainException(
                        "La liga no tiene temporadas registradas: " + ligaId));

        List<DetectorDuplicadosEquipos.ParSospechoso> pares =
                DetectorDuplicadosEquipos.detectar(temporada.equipos());
        return new DiscrepanciasLiga(liga.id(), temporada.id(), temporada.nombre(),
                pares.size(), pares);
    }
}
