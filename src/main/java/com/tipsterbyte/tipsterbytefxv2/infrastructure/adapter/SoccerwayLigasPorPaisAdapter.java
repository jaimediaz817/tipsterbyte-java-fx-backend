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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class SoccerwayLigasPorPaisAdapter implements ProveedorLigasPorPais {

    private static final Logger log = LoggerFactory.getLogger(SoccerwayLigasPorPaisAdapter.class);

    private final RestClient restClient;

    private final ServicioCortesia cortesia;

    // [QUÉ]: Base URL inyectada solo para el log de diagnóstico (mostrar la URL completa).
    @org.springframework.beans.factory.annotation.Value("${app.fuentes.base-url:http://127.0.0.1:8001}")
    private String baseUrlLog;

    public SoccerwayLigasPorPaisAdapter(RestClient restClientFuentes) {
        this(restClientFuentes, ServicioCortesia.passthrough());
    }

    // [POR QUÉ]: Cortesía H-06 aplicada DENTRO del adapter (infraestructura): pausa +
    //            reintento/backoff alrededor de cada llamada real al scraper. El test
    //            unitario usa el constructor corto con cortesia passthrough.
    @org.springframework.beans.factory.annotation.Autowired
    public SoccerwayLigasPorPaisAdapter(RestClient restClientFuentes, ServicioCortesia cortesia) {
        this.restClient = restClientFuentes;
        this.cortesia = cortesia;
    }

    @Override
    public List<LigaFuente> obtenerLigasPorPais(String countryName, int limit) {
        // [QUÉ]: Log de diagnóstico HU-12: imprime la petición exacta a la fuente #5
        //        (base-url de app.fuentes.base-url + path + params) para poder auditar
        //        qué se le pide al scraper Python y con cuántos resultados responde.
        long inicioNs = System.nanoTime();
        log.info("[FUENTE #5] GET {}/ext-soccerway-leagues-by-country?country_name={}&limit={} (inicio petición)",
                baseUrlLog, countryName, limit);
        try {
            RespuestaLigas respuesta = cortesia.ejecutar(() -> restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ext-soccerway-leagues-by-country")
                            .queryParam("country_name", countryName)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(RespuestaLigas.class));
            if (respuesta == null || respuesta.data() == null) {
                log.warn("[FUENTE #5] respuesta vacía para '{}' (limit={}): body nulo o sin 'data'.", countryName, limit);
                return List.of();
            }
            List<LigaFuente> ligas = respuesta.data().stream()
                    .flatMap(pais -> pais.leagues().stream())
                    .map(l -> new LigaFuente(
                            l.name(), l.type(), l.logoUrl(), l.apiId(), l.urlSoccerway(), l.anio()))
                    .toList();
            log.info("[FUENTE #5] respuesta OK para '{}': {} ligas recibidas en {} ms.",
                    countryName, ligas.size(), (System.nanoTime() - inicioNs) / 1_000_000);
            return ligas;
        } catch (RestClientException ex) {
            log.error("[FUENTE #5] fallo HTTP para '{}': {} - causa raíz: {}",
                    countryName, ex.getMessage(),
                    ex.getCause() != null ? ex.getCause().getClass().getSimpleName() + ": " + ex.getCause().getMessage() : "n/a");
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