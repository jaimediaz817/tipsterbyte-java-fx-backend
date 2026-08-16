// ─────────────────────────────────────────────
// [QUÉ]: Test del adapter SoccerwayCalendarioAdapter (fuente #4) con MockRestServiceServer.
// [POR QUÉ]: Verifica que el JSON real de #4 (partidos_por_jornada con fecha_iso + hora +
//            jornada) se deserializa y mapea a PartidoFuente (equipos + fechaHora + jornada).
// [RELACIONES]: CU-02. Implementa application.port.ProveedorCalendario.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SoccerwayCalendarioAdapterTest {

    private MockRestServiceServer server;
    private SoccerwayCalendarioAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:8001");
        server = MockRestServiceServer.bindTo(builder).build();

        DetalleFuenteExtraccionRepository detalleRepository = mock(DetalleFuenteExtraccionRepository.class);
        FuenteExtraccion fuente = new FuenteExtraccion("Calendario", TipoFuenteExtraccion.CALENDAR, true);
        DetalleFuenteExtraccion detalle = new DetalleFuenteExtraccion(
                UUID.randomUUID(), UUID.randomUUID(), fuente, "https://co.soccerway.com/colombia/", true);
        when(detalleRepository.buscarPorLigaYTipo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(TipoFuenteExtraccion.CALENDAR)))
                .thenReturn(Optional.of(detalle));

        adapter = new SoccerwayCalendarioAdapter(builder.build(), detalleRepository);
    }

    @Test
    void debe_mapear_partidos_con_fecha_hora_desde_json_real_de_fuente_4() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-calendar-league-by-league-v2?path_to_scrape=https://co.soccerway.com/colombia/"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "url": "...",
                          "total_partidos": 2,
                          "total_partidos_procesados": 2,
                          "total_partidos_con_error": 0,
                          "partidos_por_jornada": [
                            [
                              {
                                "jornada": "Jornada 4",
                                "partido_jugado": true,
                                "fecha": "12/08/2026",
                                "fecha_original": "11.08.",
                                "fecha_iso": "2026-08-12",
                                "hora": "19:00",
                                "equipo_local": "Atletico Nacional",
                                "equipo_visitante": "Millonarios",
                                "goles_local": 0,
                                "goles_visitante": 3,
                                "url_partido": "...",
                                "url_estadisticas": "...",
                                "estadisticas": { },
                                "partido_procesado_status": true
                              }
                            ],
                            [
                              {
                                "jornada": "Jornada 4",
                                "partido_jugado": false,
                                "fecha_iso": "2026-08-16",
                                "hora": "17:00",
                                "equipo_local": "America de Cali",
                                "equipo_visitante": "Junior",
                                "partido_procesado_status": true
                              }
                            ]
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<PartidoFuente> partidos = adapter.obtenerCalendario(UUID.randomUUID());

        assertEquals(2, partidos.size());
        PartidoFuente primero = partidos.get(0);
        assertEquals("Atletico Nacional", primero.equipoLocalNombre());
        assertEquals("Millonarios", primero.equipoVisitanteNombre());
        assertEquals(LocalDateTime.of(2026, 8, 12, 19, 0), primero.fechaHora());
        assertEquals(4, primero.jornada());
        PartidoFuente segundo = partidos.get(1);
        assertEquals(LocalDateTime.of(2026, 8, 16, 17, 0), segundo.fechaHora());
        assertEquals(4, segundo.jornada());
        server.verify();
    }

    @Test
    void debe_usar_indice_de_jornada_cuando_el_label_no_trae_numero() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-calendar-league-by-league-v2?path_to_scrape=https://co.soccerway.com/colombia/"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "partidos_por_jornada": [
                            [
                              {
                                "jornada": "Fecha",
                                "fecha_iso": "2026-08-12",
                                "hora": "19:00",
                                "equipo_local": "A",
                                "equipo_visitante": "B"
                              }
                            ],
                            [
                              {
                                "fecha_iso": "2026-08-16",
                                "hora": "17:00",
                                "equipo_local": "C",
                                "equipo_visitante": "D"
                              }
                            ]
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<PartidoFuente> partidos = adapter.obtenerCalendario(UUID.randomUUID());

        assertEquals(2, partidos.size());
        assertEquals(1, partidos.get(0).jornada());
        assertEquals(2, partidos.get(1).jornada());
        server.verify();
    }

    @Test
    void debe_devolver_lista_vacia_cuando_no_hay_partidos() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-calendar-league-by-league-v2?path_to_scrape=https://co.soccerway.com/colombia/"))
                .andRespond(withSuccess("""
                        { "success": true, "total_partidos": 0, "partidos_por_jornada": [] }
                        """, MediaType.APPLICATION_JSON));

        List<PartidoFuente> partidos = adapter.obtenerCalendario(UUID.randomUUID());

        assertTrue(partidos.isEmpty());
        server.verify();
    }
}
