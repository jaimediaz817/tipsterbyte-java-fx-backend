// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto ProveedorPaises que consume el endpoint real #1
//        (ext-soccerway-countries) del proyecto Python de extracción.
// [POR QUÉ]: Implementa el puerto de aplicación sin acoplar el dominio a HTTP. El
//            formato JSON real se deserializa en records internos (wrapper) y se
//            mapea a PaisFuente para CU-10. La URL base viene de app.fuentes.base-url.
// [ALTERNATIVAS]: football-data.org / API-Football para países; se descartan porque la
//                 fuente real del proyecto es Soccerway (#1).
// [RELACIONES]: Implementa application.port.ProveedorPaises (CU-10).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class SoccerwayPaisesAdapter implements ProveedorPaises {

    private final RestClient restClient;

    private final ServicioCortesia cortesia;

    public SoccerwayPaisesAdapter(RestClient restClientFuentes) {
        this(restClientFuentes, ServicioCortesia.passthrough());
    }

    // [POR QUÉ]: Cortesía H-06 aplicada DENTRO del adapter (infraestructura): pausa +
    //            reintento/backoff alrededor de cada llamada real al scraper. El test
    //            unitario usa el constructor corto con cortesia passthrough.
    @org.springframework.beans.factory.annotation.Autowired
    public SoccerwayPaisesAdapter(RestClient restClientFuentes, ServicioCortesia cortesia) {
        this.restClient = restClientFuentes;
        this.cortesia = cortesia;
    }

    @Override
    public List<PaisFuente> obtenerPaises() {
        try {
            RespuestaPaises respuesta = cortesia.ejecutar(() -> restClient.get()
                    .uri("/ext-soccerway-countries")
                    .retrieve()
                    .body(RespuestaPaises.class));
            if (respuesta == null || respuesta.data() == null) {
                return List.of();
            }
            return respuesta.data().stream()
                    .map(p -> new PaisFuente(
                            p.nombre(), p.href(), p.code(), p.isoAlpha2(), p.continente(), p.mapeado()))
                    .toList();
        } catch (RestClientException ex) {
            throw new InfraestructureException("Fuente de países no disponible: " + ex.getMessage(), ex);
        }
    }

    // [QUÉ]: Wrapper de la respuesta real de #1 (success, total, data).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaPaises(boolean success, Integer total, List<PaisJson> data) {
    }

    // [QUÉ]: Estructura de un país en el JSON real de #1 (iso_alpha2 en snake_case).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaisJson(
            String nombre,
            String href,
            String code,
            @JsonProperty("iso_alpha2") String isoAlpha2,
            String continente,
            boolean mapeado) {
    }
}