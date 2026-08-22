// ─────────────────────────────────────────────
// [QUÉ]: Test del adapter WplayCuotasAdapter (fuente #2) con MockRestServiceServer.
// [POR QUÉ]: Verifica que el JSON real de #2 (success: 200, matches_wplay con
//            date_match + double_chance) se deserializa, filtra por equipos/fecha y
//            mapea a las 6 CuotaFuente (3 de UNO_X_DOS + 3 de DOBLE_OPORTUNIDAD).
// [RELACIONES]: CU-03. Implementa application.port.ProveedorCuotas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CuotaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
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

class WplayCuotasAdapterTest {

    private MockRestServiceServer server;
    private WplayCuotasAdapter adapter;
    private Partido partido;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:8001");
        server = MockRestServiceServer.bindTo(builder).build();

        partido = new Partido(UUID.randomUUID(), UUID.randomUUID(),
                new Equipo("Fluminense RJ"), new Equipo("Palmeiras SP"),
                new FechaProgramada(LocalDateTime.of(2026, 8, 15, 14, 30)),
                EstadoPartido.PROGRAMADO);

        PartidoRepository partidoRepository = mock(PartidoRepository.class);
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        DetalleFuenteExtraccionRepository detalleRepository = mock(DetalleFuenteExtraccionRepository.class);
        FuenteExtraccion fuente = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
        DetalleFuenteExtraccion detalle = new DetalleFuenteExtraccion(
                UUID.randomUUID(), partido.temporadaId(), fuente, "https://www.wplay.co/es/apuestas-deportivas/futbol/colombia", true);
        when(detalleRepository.buscarPorTemporadaYTipo(partido.temporadaId(), TipoFuenteExtraccion.ODDS_WPLAY))
                .thenReturn(Optional.of(detalle));

        adapter = new WplayCuotasAdapter(builder.build(), partidoRepository, detalleRepository);
    }

    @Test
    void debe_mapear_seis_cuotas_desde_json_real_de_fuente_2() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-next-matches-wplay-by-league?path_to_scrape=https://www.wplay.co/es/apuestas-deportivas/futbol/colombia"))
                .andRespond(withSuccess("""
                        {
                          "success": 200,
                          "matches_wplay": [
                            {
                              "time_match": "14:30",
                              "date_match": "15 Ago 2026",
                              "date_match_raw": "15 Ago",
                              "team_local": "Fluminense RJ",
                              "quota_team_local": "2.72",
                              "quota_tie": "3.15",
                              "team_visiting": "Palmeiras SP",
                              "quota_team_visiting": "2.65",
                              "double_chance": [
                                { "name": "Fluminense RJ/Empate", "name_quota": "1x", "quota": "1.45" },
                                { "name": "Fluminense RJ/Palmeiras SP", "name_quota": "12", "quota": "1.30" },
                                { "name": "Palmeiras SP/Empate", "name_quota": "2x", "quota": "1.444" }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<CuotaFuente> cuotas = adapter.obtenerCuotas(partido.id());

        assertEquals(6, cuotas.size());
        assertEquals(Mercado.UNO_X_DOS, cuotas.get(0).mercado());
        assertEquals(new BigDecimal("2.72"), cuotas.get(0).valor());
        assertEquals(Mercado.UNO_X_DOS, cuotas.get(1).mercado());
        assertEquals(new BigDecimal("3.15"), cuotas.get(1).valor());
        assertEquals(Mercado.UNO_X_DOS, cuotas.get(2).mercado());
        assertEquals(new BigDecimal("2.65"), cuotas.get(2).valor());
        assertEquals(Mercado.DOBLE_OPORTUNIDAD, cuotas.get(3).mercado());
        assertEquals(new BigDecimal("1.45"), cuotas.get(3).valor());
        assertEquals(Mercado.DOBLE_OPORTUNIDAD, cuotas.get(4).mercado());
        assertEquals(new BigDecimal("1.30"), cuotas.get(4).valor());
        assertEquals(Mercado.DOBLE_OPORTUNIDAD, cuotas.get(5).mercado());
        assertEquals(new BigDecimal("1.444"), cuotas.get(5).valor());
        server.verify();
    }

    @Test
    void debe_devolver_vacio_cuando_no_hay_partidos() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-next-matches-wplay-by-league?path_to_scrape=https://www.wplay.co/es/apuestas-deportivas/futbol/colombia"))
                .andRespond(withSuccess("""
                        { "success": 200, "matches_wplay": [] }
                        """, MediaType.APPLICATION_JSON));

        assertTrue(adapter.obtenerCuotas(partido.id()).isEmpty());
        server.verify();
    }
}
