// ─────────────────────────────────────────────
// [QUÉ]: Test del adapter SoccerwayLigasPorPaisAdapter (fuente #5) con MockRestServiceServer.
// [POR QUÉ]: Verifica que el JSON real de #5 (agrupado por país) se deserializa,
//            aplana las ligas y mapea a LigaFuente, incluyendo query params.
// [RELACIONES]: CU-10. Implementa application.port.ProveedorLigasPorPais.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SoccerwayLigasPorPaisAdapterTest {

    private MockRestServiceServer server;
    private SoccerwayLigasPorPaisAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:8001");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new SoccerwayLigasPorPaisAdapter(builder.build());
    }

    @Test
    void debe_mapear_ligas_desde_json_real_de_fuente_5() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-soccerway-leagues-by-country?country_name=Espa%C3%B1a&limit=3"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": [
                            {
                              "country_name": "España",
                              "leagues": [
                                {
                                  "name": "LaLiga EA Sports",
                                  "type": "League",
                                  "logo_url": "",
                                  "api_id": null,
                                  "url_soccerway": "https://co.soccerway.com/espana/laliga-ea-sports/",
                                  "nombre_torneo": "LaLiga EA Sports",
                                  "semestre": "2026/2027",
                                  "anio": "2026/2027"
                                },
                                {
                                  "name": "LaLiga Hypermotion",
                                  "type": "League",
                                  "logo_url": "",
                                  "api_id": null,
                                  "url_soccerway": "https://co.soccerway.com/espana/laliga-hypermotion/",
                                  "nombre_torneo": "LaLiga Hypermotion",
                                  "semestre": "2026/2027",
                                  "anio": "2026/2027"
                                }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<LigaFuente> ligas = adapter.obtenerLigasPorPais("España", 3);

        assertEquals(2, ligas.size());
        LigaFuente primera = ligas.get(0);
        assertEquals("LaLiga EA Sports", primera.nombre());
        assertEquals("League", primera.type());
        assertEquals("", primera.logoUrl());
        assertNull(primera.apiId());
        assertEquals("https://co.soccerway.com/espana/laliga-ea-sports/", primera.urlSoccerway());
        assertEquals("2026/2027", primera.anio());
        server.verify();
    }

    @Test
    void debe_devolver_lista_vacia_cuando_no_hay_data() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-soccerway-leagues-by-country?country_name=Espa%C3%B1a&limit=0"))
                .andRespond(withSuccess("""
                        { "success": true, "data": [] }
                        """, MediaType.APPLICATION_JSON));

        List<LigaFuente> ligas = adapter.obtenerLigasPorPais("España", 0);

        assertTrue(ligas.isEmpty());
        server.verify();
    }
}