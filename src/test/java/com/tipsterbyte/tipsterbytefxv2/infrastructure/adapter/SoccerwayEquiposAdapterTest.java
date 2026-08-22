// ─────────────────────────────────────────────
// [QUÉ]: Test del adapter SoccerwayEquiposAdapter (fuente #6) con MockRestServiceServer.
// [POR QUÉ]: Verifica que el JSON real de #6 ({success, data.leagues[].teams[]} con
//            campos snake_case) se deserializa, matchea la liga pedida por nombre
//            normalizado y mapea los equipos a EquipoFuente (nombre + logo_url).
// [RELACIONES]: CU-10 encadenado (HU-11). Implementa application.port.ProveedorEquiposPorLiga.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;

class SoccerwayEquiposAdapterTest {

    private MockRestServiceServer server;
    private SoccerwayEquiposAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:8001");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new SoccerwayEquiposAdapter(builder.build());
    }

    @Test
    void debe_mapear_equipos_con_escudo_desde_json_real_de_fuente_6() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-soccerway-teams-by-league?country_name=Argentina&league_name=Liga%20Profesional"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "country_name": "Argentina",
                            "leagues": [
                              {
                                "name": "Liga Profesional",
                                "total_teams": 30,
                                "teams": [
                                  { "name": "Instituto", "logo_url": "https://static.flashscore.com/res/image/data/IuZoU3iT.png" },
                                  { "name": "Vélez Sarsfield", "logo_url": "https://static.flashscore.com/res/image/data/SzosWlSq.png" }
                                ]
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<EquipoFuente> equipos = adapter.obtenerEquipos("Argentina", "Liga Profesional");

        assertEquals(2, equipos.size());
        assertEquals("Instituto", equipos.get(0).nombre());
        assertEquals("https://static.flashscore.com/res/image/data/IuZoU3iT.png", equipos.get(0).logoUrl());
        assertEquals("Vélez Sarsfield", equipos.get(1).nombre());
    }

    @Test
    void debe_matchear_la_liga_por_nombre_normalizado() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-soccerway-teams-by-league?country_name=Argentina&league_name=liga%20profesional"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "country_name": "Argentina",
                            "leagues": [
                              { "name": "Otra Liga", "teams": [{ "name": "X", "logo_url": "" }] },
                              { "name": "Liga Profesional", "teams": [{ "name": "Racing", "logo_url": "" }] }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<EquipoFuente> equipos = adapter.obtenerEquipos("Argentina", "liga profesional");

        assertEquals(1, equipos.size());
        assertEquals("Racing", equipos.get(0).nombre());
    }

    @Test
    void debe_devolver_vacio_si_no_hay_datos() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-soccerway-teams-by-league?country_name=X&league_name=Y"))
                .andRespond(withSuccess("{\"success\": true, \"data\": null}", MediaType.APPLICATION_JSON));

        assertTrue(adapter.obtenerEquipos("X", "Y").isEmpty());
    }
}
