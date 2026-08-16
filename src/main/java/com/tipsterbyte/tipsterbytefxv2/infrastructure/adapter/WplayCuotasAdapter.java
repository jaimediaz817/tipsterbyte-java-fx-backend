// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto ProveedorCuotas que consume el endpoint real #2
//        (ext-next-matches-wplay-by-league) del proyecto Python de extracción (Wplay),
//        devolviendo las 6 cuotas de un partido (3 de 1X2 + 3 de doble oportunidad).
// [POR QUÉ]: Implementa el puerto de aplicación sin acoplar el dominio a HTTP. El puerto
//            recibe partidoId (no ligaId), así que el adapter resuelve el partido → liga,
//            obtiene la URL del endpoint (DetalleFuenteExtraccion ODDS_WPLAY) y filtra los
//            partidos de Wplay por equipos y fecha para quedarse con el partido buscado.
// [ALTERNATIVAS]: API-Football / The Odds API / SharpAPI para cuotas; se descartan porque
//                 la fuente real del proyecto es Wplay (#2) con doble oportunidad.
// [RELACIONES]: Implementa application.port.ProveedorCuotas (CU-03); consulta
//               PartidoRepository y DetalleFuenteExtraccionRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tipsterbyte.tipsterbytefxv2.application.dto.CuotaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class WplayCuotasAdapter implements ProveedorCuotas {

    private static final Map<String, Integer> MESES_ES = Map.ofEntries(
            Map.entry("ene", 1), Map.entry("feb", 2), Map.entry("mar", 3),
            Map.entry("abr", 4), Map.entry("may", 5), Map.entry("jun", 6),
            Map.entry("jul", 7), Map.entry("ago", 8), Map.entry("sep", 9),
            Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dic", 12));

    private final RestClient restClient;
    private final PartidoRepository partidoRepository;
    private final DetalleFuenteExtraccionRepository detalleRepository;

    public WplayCuotasAdapter(RestClient restClientFuentes,
                              PartidoRepository partidoRepository,
                              DetalleFuenteExtraccionRepository detalleRepository) {
        this.restClient = restClientFuentes;
        this.partidoRepository = partidoRepository;
        this.detalleRepository = detalleRepository;
    }

    @Override
    public List<CuotaFuente> obtenerCuotas(UUID partidoId) {
        Partido partido = partidoRepository.buscarPorId(partidoId)
                .orElseThrow(() -> new DomainException("Partido no encontrado: " + partidoId));
        String url = resolverUrl(partido.ligaId());

        try {
            RespuestaCuotas respuesta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ext-next-matches-wplay-by-league")
                            .queryParam("path_to_scrape", url)
                            .build())
                    .retrieve()
                    .body(RespuestaCuotas.class);
            if (respuesta == null || respuesta.matchesWplay() == null || respuesta.matchesWplay().isEmpty()) {
                return List.of();
            }

            // [POR QUÉ]: Wplay devuelve los próximos partidos de la liga; el adapter filtra
            //            por equipos y fecha para entregar solo las cuotas del partido pedido.
            return respuesta.matchesWplay().stream()
                    .filter(m -> coincidePartido(m, partido))
                    .findFirst()
                    .map(this::toCuotasFuente)
                    .orElseThrow(() -> new DomainException(
                            "El partido no está entre los próximos de Wplay: " + partidoId));
        } catch (RestClientException ex) {
            throw new InfraestructureException("Fuente de cuotas no disponible: " + ex.getMessage(), ex);
        }
    }

    // [QUÉ]: Resuelve la URL (path_to_scrape) de la fuente de cuotas Wplay de la liga.
    private String resolverUrl(UUID ligaId) {
        return detalleRepository.buscarPorLigaYTipo(ligaId, TipoFuenteExtraccion.ODDS_WPLAY)
                .filter(d -> d.activa())
                .map(d -> d.url())
                .orElseThrow(() -> new DomainException(
                        "Liga sin URL de cuotas (ODDS_WPLAY) asociada y activa: " + ligaId));
    }

    // [QUÉ]: Compara un partido de Wplay con el del dominio por nombre de equipos y fecha.
    // [POR QUÉ]: Decisión FASE 8.5: coincidencia por nombre exacto (CU-01 posiciones es la
    //            fuente canónica); el matching fuzzy se difiere a FASE 17.
    private boolean coincidePartido(MatchWplayJson match, Partido partido) {
        boolean mismosEquipos = partido.equipoLocal().nombre().equals(match.teamLocal())
                && partido.equipoVisitante().nombre().equals(match.teamVisiting());
        if (!mismosEquipos) {
            return false;
        }
        LocalDateTime fechaWplay = combinarFechaHora(match.dateMatch(), match.timeMatch());
        LocalDateTime fechaPartido = partido.fechaProgramada().fechaHora();
        return fechaWplay.toLocalDate().equals(fechaPartido.toLocalDate());
    }

    // [QUÉ]: Mapea un partido de Wplay a las 6 CuotaFuente (3 de 1X2 + 3 de doble oportunidad).
    private List<CuotaFuente> toCuotasFuente(MatchWplayJson match) {
        List<CuotaFuente> cuotas = new ArrayList<>();
        cuotas.add(new CuotaFuente(Mercado.UNO_X_DOS, parsearCuota(match.quotaTeamLocal())));
        cuotas.add(new CuotaFuente(Mercado.UNO_X_DOS, parsearCuota(match.quotaTie())));
        cuotas.add(new CuotaFuente(Mercado.UNO_X_DOS, parsearCuota(match.quotaTeamVisiting())));
        if (match.doubleChance() != null) {
            for (DoubleChanceJson dc : match.doubleChance()) {
                cuotas.add(new CuotaFuente(Mercado.DOBLE_OPORTUNIDAD, parsearCuota(dc.quota())));
            }
        }
        return cuotas;
    }

    private BigDecimal parsearCuota(String valor) {
        return new BigDecimal(valor.trim());
    }

    // [QUÉ]: Combina date_match ("15 Ago 2026") y time_match ("14:30") en LocalDateTime.
    // [POR QUÉ]: La fuente usa mes abreviado en español; se mapea manualmente porque
    //            el default locale del proceso no es garantía de parseo.
    private LocalDateTime combinarFechaHora(String dateMatch, String timeMatch) {
        String[] partes = dateMatch.trim().split(" ");
        int dia = Integer.parseInt(partes[0]);
        int mes = MESES_ES.getOrDefault(partes[1].toLowerCase(Locale.ROOT), -1);
        int anio = Integer.parseInt(partes[2]);
        if (mes == -1) {
            throw new DomainException("Mes inválido en fecha de Wplay: " + dateMatch);
        }
        if (timeMatch == null || timeMatch.isBlank()) {
            return LocalDate.of(anio, mes, dia).atStartOfDay();
        }
        return LocalDate.of(anio, mes, dia).atTime(LocalTime.parse(timeMatch.trim()));
    }

    // [QUÉ]: Wrapper de la respuesta real de #2 (success: 200, matches_wplay).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaCuotas(
            Integer success,
            @JsonProperty("matches_wplay") List<MatchWplayJson> matchesWplay) {
    }

    // [QUÉ]: Estructura de un partido próximo en el JSON real de #2.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MatchWplayJson(
            @JsonProperty("time_match") String timeMatch,
            @JsonProperty("date_match") String dateMatch,
            @JsonProperty("team_local") String teamLocal,
            @JsonProperty("quota_team_local") String quotaTeamLocal,
            @JsonProperty("quota_tie") String quotaTie,
            @JsonProperty("team_visiting") String teamVisiting,
            @JsonProperty("quota_team_visiting") String quotaTeamVisiting,
            @JsonProperty("double_chance") List<DoubleChanceJson> doubleChance) {
    }

    // [QUÉ]: Estructura de una cuota de doble oportunidad (1x/12/2x).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DoubleChanceJson(
            String name,
            @JsonProperty("name_quota") String nameQuota,
            String quota) {
    }
}
