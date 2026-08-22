// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto ProveedorEquiposPorLiga que consume el endpoint real #6
//        (ext-soccerway-teams-by-league) del proyecto Python de extracción.
// [POR QUÉ]: Implementa el puerto de aplicación sin acoplar el dominio a HTTP. El JSON
//            real ({success, data.leagues[].teams[]}) se deserializa en records internos
//            y se mapea a EquipoFuente (nombre + logo_url). A diferencia de las fuentes
//            operativas (#2/#3/#4), NO usa path_to_scrape: se consume con country_name +
//            league_name, datos que ya viven en el aggregate Liga.
// [ALTERNATIVAS]: football-data.org / API-Football para equipos; se descartan porque la
//                 fuente real del proyecto es Soccerway (#6).
// [RELACIONES]: Implementa application.port.ProveedorEquiposPorLiga; envuelto por
//               ProveedorEquiposCacheable; consumido por CU-10 encadenado (HU-11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorEquiposPorLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.service.NormalizadorNombresEquipos;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component("soccerwayEquiposAdapter")
public class SoccerwayEquiposAdapter implements ProveedorEquiposPorLiga {

    private final RestClient restClient;

    public SoccerwayEquiposAdapter(RestClient restClientFuentes) {
        this.restClient = restClientFuentes;
    }

    @Override
    public List<EquipoFuente> obtenerEquipos(String countryName, String leagueName) {
        try {
            RespuestaEquipos respuesta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ext-soccerway-teams-by-league")
                            .queryParam("country_name", countryName)
                            .queryParam("league_name", leagueName)
                            .build())
                    .retrieve()
                    .body(RespuestaEquipos.class);
            if (respuesta == null || respuesta.data() == null || respuesta.data().leagues() == null) {
                return List.of();
            }
            // La respuesta puede traer varias ligas cuyo nombre coincida parcialmente:
            // matcheamos por nombre normalizado contra el solicitado.
            return respuesta.data().leagues().stream()
                    .filter(l -> NormalizadorNombresEquipos.normalizar(l.name())
                            .equals(NormalizadorNombresEquipos.normalizar(leagueName)))
                    .findFirst()
                    .map(l -> l.teams() == null ? List.<EquipoJson>of() : l.teams())
                    .orElse(List.of())
                    .stream()
                    .map(t -> new EquipoFuente(t.name(), t.logoUrl()))
                    .toList();
        } catch (RestClientException ex) {
            throw new InfraestructureException(
                    "Fuente de equipos por liga no disponible: " + ex.getMessage(), ex);
        }
    }

    // [QUÉ]: Wrapper del JSON real de #6 ({success, data.country_name, data.leagues[]}).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaEquipos(boolean success, DataJson data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DataJson(
            @JsonProperty("country_name") String countryName,
            @JsonProperty("leagues") List<LigaJson> leagues) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LigaJson(
            @JsonProperty("name") String name,
            @JsonProperty("total_teams") Integer totalTeams,
            @JsonProperty("teams") List<EquipoJson> teams) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EquipoJson(
            @JsonProperty("name") String name,
            @JsonProperty("logo_url") String logoUrl) {
    }
}
