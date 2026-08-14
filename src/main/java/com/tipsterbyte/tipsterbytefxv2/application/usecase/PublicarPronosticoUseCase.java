// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-07 (HU-07): publica un pronóstico que está en BORRADOR,
//        validando que el partido siga jugable y la cuota vigente (BR-004).
// [POR QUÉ]: Recupera el pronóstico y su partido, calcula la jugabilidad y la vigencia
//            de la cuota, y delega la transición BORRADOR→PUBLICADO al aggregate (BR-005).
// [ALTERNATIVAS]: Publicar sin revalidar el partido; se descarta porque el partido puede
//                 haberse finalizado entre la creación y la publicación.
// [RELACIONES]: HU-07 → CU-07 → PronosticoRepository + PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;

import java.util.List;
import java.util.UUID;

public final class PublicarPronosticoUseCase {

    private final PronosticoRepository pronosticoRepository;
    private final PartidoRepository partidoRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public PublicarPronosticoUseCase(PronosticoRepository pronosticoRepository, PartidoRepository partidoRepository) {
        this.pronosticoRepository = pronosticoRepository;
        this.partidoRepository = partidoRepository;
    }

    // [QUÉ]: Ejecuta CU-07: valida partido jugable y cuota vigente, publica y persiste.
    //        Devuelve el evento PronosticoPublicado.
    public List<DomainEvent> ejecutar(UUID pronosticoId) {
        Pronostico pronostico = pronosticoRepository.buscarPorId(pronosticoId)
                .orElseThrow(() -> new DomainException("Pronóstico no encontrado: " + pronosticoId));
        Partido partido = partidoRepository.buscarPorId(pronostico.partidoId())
                .orElseThrow(() -> new DomainException("Partido no encontrado: " + pronostico.partidoId()));

        boolean partidoJugable = partido.estado() == EstadoPartido.PROGRAMADO
                || partido.estado() == EstadoPartido.EN_VIVO;
        boolean cuotaVigente = partido.cuotas().stream()
                .anyMatch(c -> c.valor().equals(pronostico.cuota().valor()));

        pronostico.publicar(partidoJugable, cuotaVigente); // BR-004 y BR-005
        pronosticoRepository.guardar(pronostico);
        return pronostico.pullEventos();
    }
}