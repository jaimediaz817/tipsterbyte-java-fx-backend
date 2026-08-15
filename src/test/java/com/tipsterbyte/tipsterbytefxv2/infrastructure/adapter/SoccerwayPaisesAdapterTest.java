// ─────────────────────────────────────────────
// [QUÉ]: Test del adapter SoccerwayPaisesAdapter (fuente #1) con MockRestServiceServer.
// [POR QUÉ]: Verifica que el JSON real de #1 (wrapper success/data) se deserializa y
//            mapea a PaisFuente, sin necesidad del proyecto Python de extracción.
// [RELACIONES]: CU-10. Implementa application.port.ProveedorPaises.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SoccerwayPaisesAdapterTest {

    private MockRestServiceServer server;
    private SoccerwayPaisesAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:8001");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new SoccerwayPaisesAdapter(builder.build());
    }

    @Test
    void debe_mapear_paises_desde_json_real_de_fuente_1() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-soccerway-countries"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "total": 176,
                          "data": [
                            {
                              "nombre": "Albania",
                              "href": "/albania/",
                              "code": "17",
                              "iso_alpha2": "AL",
                              "continente": "Europa",
                              "mapeado": true
                            },
                            {
                              "nombre": "Alemania",
                              "href": "/alemania/",
                              "code": "81",
                              "iso_alpha2": "DE",
                              "continente": "Europa",
                              "mapeado": true
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<PaisFuente> paises = adapter.obtenerPaises();

        assertEquals(2, paises.size());
        PaisFuente albania = paises.get(0);
        assertEquals("Albania", albania.nombre());
        assertEquals("AL", albania.isoAlpha2());
        assertEquals("Europa", albania.continente());
        assertEquals("17", albania.code());
        assertEquals("/albania/", albania.href());
        assertTrue(albania.mapeado());
        server.verify();
    }

    @Test
    void debe_devolver_lista_vacia_cuando_no_hay_data() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-soccerway-countries"))
                .andRespond(withSuccess("""
                        { "success": true, "total": 0, "data": [] }
                        """, MediaType.APPLICATION_JSON));

        List<PaisFuente> paises = adapter.obtenerPaises();

        assertTrue(paises.isEmpty());
        server.verify();
    }
}