// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto ProveedorPosiciones que consume el endpoint real #3
//        (ext-position-table-by-league-stable) del proyecto Python de extracción
//        (Flashscore), incluyendo los últimos 5 resultados por equipo.
// [POR QUÉ]: Implementa el puerto de aplicación sin acoplar el dominio a HTTP. Resuelve
//            la URL del endpoint (path_to_scrape) consultando el DetalleFuenteExtraccion
//            de tipo STANDINGS de la liga (asociada en CU-04/CU-11).
// [ALTERNATIVAS]: football-data.org / API-Football para posiciones; se descartan porque
//                 la fuente real del proyecto es Flashscore (#3).
// [RELACIONES]: Implementa application.port.ProveedorPosiciones (CU-01); consulta
//               DetalleFuenteExtraccionRepository para resolver la URL por liga.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PosicionFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPosiciones;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FlashscorePosicionesAdapter implements ProveedorPosiciones {

    private final RestClient restClient;
    private final DetalleFuenteExtraccionRepository detalleRepository;

    public FlashscorePosicionesAdapter(RestClient restClientFuentes,
                                       DetalleFuenteExtraccionRepository detalleRepository) {
        this.restClient = restClientFuentes;
        this.detalleRepository = detalleRepository;
    }

    @Override
    public List<PosicionFuente> obtenerPosiciones(UUID ligaId) {
        String url = resolverUrl(ligaId);
        RespuestaPosiciones respuesta = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ext-position-table-by-league-stable")
                        .queryParam("path_to_scrape", url)
                        .build())
                .retrieve()
                .body(RespuestaPosiciones.class);
        if (respuesta == null || respuesta.tablaPosiciones() == null) {
            return List.of();
        }
        return respuesta.tablaPosiciones().stream()
                .map(this::toPosicionFuente)
                .toList();
    }

    // [QUÉ]: Resuelve la URL (path_to_scrape) de la fuente de posiciones de la liga.
    // [POR QUÉ]: La URL real se asocia por liga en CU-04/CU-11; sin ella no hay endpoint.
    private String resolverUrl(UUID ligaId) {
        return detalleRepository.buscarPorLigaYTipo(ligaId, TipoFuenteExtraccion.STANDINGS)
                .filter(d -> d.activa())
                .map(d -> d.url())
                .orElseThrow(() -> new DomainException(
                        "Liga sin URL de posiciones (STANDINGS) asociada y activa: " + ligaId));
    }

    // [QUÉ]: Mapea una fila JSON a PosicionFuente parseando strings numéricos y la racha.
    private PosicionFuente toPosicionFuente(FilaJson fila) {
        return new PosicionFuente(
                fila.nombreEquipoFull(),
                parseInt(fila.posicion()),
                parseInt(fila.partidosJugados()),
                parseInt(fila.partidosGanados()),
                parseInt(fila.partidosEmpatados()),
                parseInt(fila.partidosPerdidos()),
                parseInt(fila.golesAFavor()),
                parseInt(fila.golesEnContra()),
                parseInt(fila.puntos()),
                mapearRacha(fila.resultadosUltimos5()));
    }

    // [QUÉ]: Convierte las claves 1..5 (1 = más reciente) en lista ordenada índice 0 = más reciente.
    // [POR QUÉ]: La fuente entrega la racha como diccionario {clave: 1|0|-1}; el dominio
    //            quiere una lista con el partido más reciente primero (decisión FASE 8.5).
    private List<ResultadoReciente> mapearRacha(Map<Integer, Integer> racha) {
        if (racha == null || racha.isEmpty()) {
            return List.of();
        }
        List<ResultadoReciente> lista = new ArrayList<>();
        for (int clave = 1; clave <= 5; clave++) {
            if (racha.containsKey(clave)) {
                lista.add(ResultadoReciente.desdeCodigo(racha.get(clave)));
            }
        }
        return List.copyOf(lista);
    }

    private int parseInt(String valor) {
        return Integer.parseInt(valor.trim());
    }

    // [QUÉ]: Wrapper de la respuesta real de #3 (status_code, tabla_posiciones).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaPosiciones(
            @JsonProperty("status_code") Integer statusCode,
            @JsonProperty("tabla_posiciones") List<FilaJson> tablaPosiciones) {
    }

    // [QUÉ]: Estructura de una fila de la tabla en el JSON real de #3.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FilaJson(
            @JsonProperty("nombre_equipo_full") String nombreEquipoFull,
            @JsonProperty("posicion") String posicion,
            @JsonProperty("partidos_jugados") String partidosJugados,
            @JsonProperty("partidos_ganados") String partidosGanados,
            @JsonProperty("partidos_empatados") String partidosEmpatados,
            @JsonProperty("partidos_perdidos") String partidosPerdidos,
            @JsonProperty("goles_a_favor") String golesAFavor,
            @JsonProperty("goles_en_contra") String golesEnContra,
            @JsonProperty("puntos") String puntos,
            @JsonProperty("resultados_ultimos_5_jugados") Map<Integer, Integer> resultadosUltimos5) {
    }
}
