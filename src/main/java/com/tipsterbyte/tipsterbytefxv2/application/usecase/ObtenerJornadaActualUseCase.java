// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso que calcula la jornada actual de una liga (CU-02) a partir de sus
//        partidos: la jornada del próximo partido por jugarse (mínima fecha >= ahora);
//        si todos los partidos ya se jugaron, la jornada del último jugado.
// [POR QUÉ]: El frontend muestra el indicador "Jornada X de la temporada Y" por liga.
//            Se calcula en el backend porque es la única fuente de verdad: deriva el
//            estado del calendario persistido y evita depender del reloj del cliente.
// [ALTERNATIVAS]: Calcular en el frontend comparando fechas; se descarta porque cada
//                 cliente tendría su propio cómputo (timezone/drift). Guardar la
//                 jornada actual en la liga; se descarta porque se puede derivar del
//                 calendario y quedaría obsoleta sin recalcularla.
// [RELACIONES]: CU-02 → PartidoRepository (buscarPorLiga) → LigaController
//               (GET /api/v1/ligas/{id}/jornada-actual).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.JornadaActualDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ObtenerJornadaActualUseCase {

    private final PartidoRepository partidoRepository;

    // [QUÉ]: Construye el caso de uso con su puerto (inyección por constructor).
    public ObtenerJornadaActualUseCase(PartidoRepository partidoRepository) {
        this.partidoRepository = partidoRepository;
    }

    // [QUÉ]: Devuelve la jornada actual y la siguiente de la liga, o ambas null si la
    //        liga no tiene partidos con jornada.
    // [POR QUÉ]: El calendario se ordena por fecha (el repositorio no garantiza orden)
    //            y la jornada actual es la del primer partido con fecha >= ahora; si
    //            todos ya se jugaron, la jornada del último partido del calendario.
    public JornadaActualDto ejecutar(UUID ligaId) {
        List<Partido> conJornada = partidoRepository.buscarPorLiga(ligaId).stream()
                .filter(p -> p.jornada() != null)
                .sorted(Comparator.comparing(p -> p.fechaProgramada().fechaHora()))
                .toList();
        if (conJornada.isEmpty()) {
            return new JornadaActualDto(null, null);
        }

        LocalDateTime ahora = LocalDateTime.now();
        Optional<Partido> proximo = conJornada.stream()
                .filter(p -> !p.fechaProgramada().fechaHora().isBefore(ahora))
                .findFirst();
        int jornadaActual = proximo.map(Partido::jornada)
                .orElseGet(() -> conJornada.get(conJornada.size() - 1).jornada());
        return new JornadaActualDto(jornadaActual, jornadaActual + 1);
    }
}