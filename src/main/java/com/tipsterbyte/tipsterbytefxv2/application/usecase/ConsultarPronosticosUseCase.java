// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-08 (HU-08): consulta los pronósticos PUBLICADO de una liga y
//        fecha, pero solo de tipsters a los que el cliente está suscrito activamente.
// [POR QUÉ]: Aplica BR-006 (un cliente solo consume de tipsters con suscripción activa)
//            cruzando suscripciones vigentes con pronósticos publicados del día.
// [ALTERNATIVAS]: Devolver todos los pronósticos y filtrar en el cliente; se descarta
//                 porque expondría pronósticos de tipsters no suscritos.
// [RELACIONES]: HU-08 → CU-08 → PronosticoRepository + PartidoRepository + SuscripcionRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PronosticoPublicoDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ConsultarPronosticosUseCase {

    private final SuscripcionRepository suscripcionRepository;
    private final PartidoRepository partidoRepository;
    private final PronosticoRepository pronosticoRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public ConsultarPronosticosUseCase(SuscripcionRepository suscripcionRepository,
                                       PartidoRepository partidoRepository,
                                       PronosticoRepository pronosticoRepository) {
        this.suscripcionRepository = suscripcionRepository;
        this.partidoRepository = partidoRepository;
        this.pronosticoRepository = pronosticoRepository;
    }

    // [QUÉ]: Ejecuta CU-08: valida suscripciones activas del cliente (BR-006), busca los
    //        partidos de la liga en la fecha y devuelve los pronósticos publicados de los
    //        tipsters suscritos como DTOs públicos.
    public List<PronosticoPublicoDto> ejecutar(UUID clienteId, UUID ligaId, LocalDate fecha, LocalDateTime momento) {
        Set<UUID> tipstersSuscritos = suscripcionRepository.buscarActivasPorCliente(clienteId).stream()
                .filter(s -> s.estaActiva(momento)) // BR-006
                .map(Suscripcion::tipsterId)
                .collect(Collectors.toSet());

        List<Partido> partidos = partidoRepository.buscarPorLigaYFecha(ligaId, fecha);
        List<UUID> partidoIds = partidos.stream().map(Partido::id).toList();

        return pronosticoRepository.buscarPublicadosPorPartidos(partidoIds).stream()
                .filter(p -> tipstersSuscritos.contains(p.tipsterId()))
                .map(p -> toDto(p, partidos))
                .toList();
    }

    // [QUÉ]: Construye el DTO público a partir del pronóstico y su partido.
    private PronosticoPublicoDto toDto(Pronostico pronostico, List<Partido> partidos) {
        Partido partido = partidos.stream()
                .filter(p -> p.id().equals(pronostico.partidoId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Pronóstico sin partido asociado: " + pronostico.id()));
        return new PronosticoPublicoDto(
                pronostico.id(),
                pronostico.tipsterId(),
                partido.id(),
                partido.equipoLocal().nombre(),
                partido.equipoVisitante().nombre(),
                partido.fechaProgramada().fechaHora(),
                pronostico.seleccion().mercado(),
                pronostico.seleccion().resultadoEsperado(),
                pronostico.cuota().valor());
    }
}