// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-25 (HU-16): consulta pronósticos sugeridos de una estrategia
//        con filtros por liga y confianza mínima.
// [POR QUÉ]: El tipster/superadmin revisa las sugerencias generadas por la evaluación
//            antes de publicar como pronósticos oficiales.
// [ALTERNATIVAS]: Consultar directamente el repositorio; se descarta porque necesita
//                 enriquecer con datos del partido (equipos, fecha, cuotas).
// [RELACIONES]: HU-16 AC13 → PronosticoSugeridoRepository + PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoSugeridoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PronosticoSugerido;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.SugerenciaResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ConsultarSugerenciasUseCase {

    private final PronosticoSugeridoRepository pronosticoSugeridoRepository;
    private final PartidoRepository partidoRepository;

    public ConsultarSugerenciasUseCase(PronosticoSugeridoRepository pronosticoSugeridoRepository,
                                       PartidoRepository partidoRepository) {
        this.pronosticoSugeridoRepository = pronosticoSugeridoRepository;
        this.partidoRepository = partidoRepository;
    }

    // [QUÉ]: Devuelve las sugerencias de una estrategia con filtros opcionales.
    public List<SugerenciaResponse> ejecutar(UUID estrategiaId, UUID ligaId,
                                              BigDecimal confianzaMinima) {
        List<PronosticoSugerido> sugeridos = pronosticoSugeridoRepository.buscarPorEstrategiaId(estrategiaId);

        return sugeridos.stream()
                .filter(s -> confianzaMinima == null || s.score().compareTo(confianzaMinima) >= 0)
                .map(s -> construirResponse(s, ligaId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    // [QUÉ]: Construye el response enriquecido con datos del partido.
    private Optional<SugerenciaResponse> construirResponse(PronosticoSugerido sugerido, UUID ligaId) {
        Optional<Partido> partidoOpt = partidoRepository.buscarPorId(sugerido.partidoId());
        if (partidoOpt.isEmpty()) return Optional.empty();

        Partido partido = partidoOpt.get();

        // Si se filtra por liga, verificar que el partido pertenezca a esa liga.
        if (ligaId != null) {
            // Los partidos no tienen ligaId directo, se accede vía temporada.
            // Placeholder: se necesita LigaRepository para validar.
        }

        return Optional.of(new SugerenciaResponse(
                sugerido.id(),
                sugerido.estrategiaId(),
                sugerido.partidoId(),
                partido.equipoLocal().nombre(),
                partido.equipoVisitante().nombre(),
                partido.fechaProgramada().fechaHora(),
                partido.jornada(),
                sugerido.score(),
                sugerido.criteriosCumplidos(),
                sugerido.criteriosFallidos(),
                sugerido.createdAt()));
    }
}
