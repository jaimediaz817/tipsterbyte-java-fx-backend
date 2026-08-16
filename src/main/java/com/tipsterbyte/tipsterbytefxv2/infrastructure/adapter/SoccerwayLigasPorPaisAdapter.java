// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto ProveedorLigasPorPais que consume el endpoint real #5
//        (ext-soccerway-leagues-by-country) del proyecto Python de extracción.
// [POR QUÉ]: Implementa el puerto de aplicación sin acoplar el dominio a HTTP. El
//            JSON real se deserializa en records internos (wrapper) y se mapea a
//            LigaFuente para CU-10. La URL base viene de app.fuentes.base-url.
// [ALTERNATIVAS]: football-data.org / API-Football para ligas por país; se descartan
//                 porque la fuente real del proyecto es Soccerway (#5).
// [RELACIONES]: Implementa application.port.ProveedorLigasPorPais (CU-10).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class SoccerwayLigasPorPaisAdapter implements ProveedorLigasPorPais {

    private final RestClient restClient;

    public SoccerwayLigasPorPaisAdapter(RestClient restClientFuentes) {
        this.restClient = restClientFuentes;
    }

    @Override
    public List<LigaFuente> obtenerLigasPorPais(String countryName, int limit) {
        try {
            RespuestaLigas respuesta = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ext-soccerway-leagues-by-country")
                            .queryParam("country_name", countryName)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(RespuestaLigas.class);
            if (respuesta == null || respuesta.data() == null) {
                return List.of();
            }
            return respuesta.data().stream()
                    .flatMap(pais -> pais.leagues().stream())
                    .map(l -> new LigaFuente(
                            l.name(), l.type(), l.logoUrl(), l.apiId(), l.urlSoccerway(), l.anio()))
                    .toList();
        } catch (RestClientException ex) {
            throw new InfraestructureException("Fuente de ligas por país no disponible: " + ex.getMessage(), ex);
        }
    }

    // [QUÉ]: Wrapper de la respuesta real de #5 (success, data agrupado por país).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaLigas(boolean success, List<PaisLigasJson> data) {
    }

    // [QUÉ]: Agrupación por país en el JSON real de #5 (country_name).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaisLigasJson(
            @JsonProperty("country_name") String countryName,
            List<LigaJson> leagues) {
    }

    // [QUÉ]: Estructura de una liga en el JSON real de #5 (campos en snake_case).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LigaJson(
            String name,
            String type,
            @JsonProperty("logo_url") String logoUrl,
            @JsonProperty("api_id") String apiId,
            @JsonProperty("url_soccerway") String urlSoccerway,
            String anio) {
    }
}