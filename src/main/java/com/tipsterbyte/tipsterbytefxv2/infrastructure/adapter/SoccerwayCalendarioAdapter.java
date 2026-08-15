// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto ProveedorCalendario que consume el endpoint real #4
//        (ext-calendar-league-by-league-v2) del proyecto Python de extracción
//        (Soccerway).
// [POR QUÉ]: Implementa el puerto de aplicación sin acoplar el dominio a HTTP. Resuelve
//            la URL del endpoint (path_to_scrape) consultando el DetalleFuenteExtraccion
//            de tipo CALENDAR de la liga (asociada en CU-04/CU-11).
// [ALTERNATIVAS]: API-Football / football-data.org para calendario; se descartan porque
//                 la fuente real del proyecto es Soccerway (#4).
// [RELACIONES]: Implementa application.port.ProveedorCalendario (CU-02); consulta
//               DetalleFuenteExtraccionRepository para resolver la URL por liga.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCalendario;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SoccerwayCalendarioAdapter implements ProveedorCalendario {

    private final RestClient restClient;
    private final DetalleFuenteExtraccionRepository detalleRepository;

    public SoccerwayCalendarioAdapter(RestClient restClientFuentes,
                                      DetalleFuenteExtraccionRepository detalleRepository) {
        this.restClient = restClientFuentes;
        this.detalleRepository = detalleRepository;
    }

    @Override
    public List<PartidoFuente> obtenerCalendario(UUID ligaId) {
        String url = resolverUrl(ligaId);
        RespuestaCalendario respuesta = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ext-calendar-league-by-league-v2")
                        .queryParam("path_to_scrape", url)
                        .build())
                .retrieve()
                .body(RespuestaCalendario.class);
        if (respuesta == null || respuesta.partidosPorJornada() == null) {
            return List.of();
        }

        // [POR QUÉ]: FASE 8.5 solo crea partidos (equipos + fechaHora). Los goles,
        //            estado FINALIZADO y estadísticas quedan para un futuro CU de
        //            resultados (decisión del usuario, documentada en fuentes-externas.md).
        List<PartidoFuente> partidos = new ArrayList<>();
        for (List<PartidoJson> jornada : respuesta.partidosPorJornada()) {
            if (jornada == null) {
                continue;
            }
            for (PartidoJson partido : jornada) {
                partidos.add(new PartidoFuente(
                        partido.equipoLocal(), partido.equipoVisitante(),
                        combinarFechaHora(partido.fechaIso(), partido.hora())));
            }
        }
        return partidos;
    }

    // [QUÉ]: Resuelve la URL (path_to_scrape) de la fuente de calendario de la liga.
    // [POR QUÉ]: La URL real se asocia por liga en CU-04/CU-11; sin ella no hay endpoint.
    private String resolverUrl(UUID ligaId) {
        return detalleRepository.buscarPorLigaYTipo(ligaId, TipoFuenteExtraccion.CALENDAR)
                .filter(d -> d.activa())
                .map(d -> d.url())
                .orElseThrow(() -> new DomainException(
                        "Liga sin URL de calendario (CALENDAR) asociada y activa: " + ligaId));
    }

    // [QUÉ]: Combina fecha_iso ("2026-08-12") y hora ("19:00") en LocalDateTime.
    // [POR QUÉ]: El dominio espera FechaProgramada(LocalDateTime); la fuente entrega
    //            fecha y hora por separado sin timezone (hora local asumida).
    private LocalDateTime combinarFechaHora(String fechaIso, String hora) {
        LocalDate fecha = LocalDate.parse(fechaIso.trim());
        if (hora == null || hora.isBlank()) {
            return fecha.atStartOfDay();
        }
        return fecha.atTime(LocalTime.parse(hora.trim()));
    }

    // [QUÉ]: Wrapper de la respuesta real de #4 (success, partidos_por_jornada).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaCalendario(
            Boolean success,
            @JsonProperty("partidos_por_jornada") List<List<PartidoJson>> partidosPorJornada) {
    }

    // [QUÉ]: Estructura de un partido en el JSON real de #4 (solo campos usados en 8.5).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PartidoJson(
            @JsonProperty("fecha_iso") String fechaIso,
            String hora,
            @JsonProperty("equipo_local") String equipoLocal,
            @JsonProperty("equipo_visitante") String equipoVisitante) {
    }
}
