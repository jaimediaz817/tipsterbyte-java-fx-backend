// ─────────────────────────────────────────────
// [QUÉ]: Test del adapter FlashscorePosicionesAdapter (fuente #3) con MockRestServiceServer.
// [POR QUÉ]: Verifica que el JSON real de #3 (status_code + tabla_posiciones, strings
//            numéricas y resultados_ultimos_5_jugados) se deserializa y mapea a
//            PosicionFuente con la racha ordenada (índice 0 = más reciente).
// [RELACIONES]: CU-01. Implementa application.port.ProveedorPosiciones.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PosicionFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FlashscorePosicionesAdapterTest {

    private MockRestServiceServer server;
    private FlashscorePosicionesAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:8001");
        server = MockRestServiceServer.bindTo(builder).build();

        DetalleFuenteExtraccionRepository detalleRepository = mock(DetalleFuenteExtraccionRepository.class);
        FuenteExtraccion fuente = new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true);
        DetalleFuenteExtraccion detalle = new DetalleFuenteExtraccion(
                UUID.randomUUID(), UUID.randomUUID(), fuente, "https://flashscore.com/colombia", true);
        when(detalleRepository.buscarPorLigaYTipo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(TipoFuenteExtraccion.STANDINGS)))
                .thenReturn(Optional.of(detalle));

        adapter = new FlashscorePosicionesAdapter(builder.build(), detalleRepository);
    }

    @Test
    void debe_mapear_posiciones_y_racha_desde_json_real_de_fuente_3() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-position-table-by-league-stable?path_to_scrape=https://flashscore.com/colombia"))
                .andRespond(withSuccess("""
                        {
                          "status_code": 200,
                          "tabla_posiciones": [
                            {
                              "nombre_equipo_full": "Palmeiras",
                              "url_logo_equipo": "https://logo.png",
                              "posicion": "1",
                              "partidos_jugados": "22",
                              "partidos_ganados": "14",
                              "partidos_empatados": "6",
                              "partidos_perdidos": "2",
                              "goles_a_favor": "38",
                              "goles_en_contra": "16",
                              "goles_diferencia": "22",
                              "puntos": "48",
                              "resultados_ultimos_5_jugados": { "1": 0, "2": 1, "3": -1, "4": 1, "5": 1 }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<PosicionFuente> posiciones = adapter.obtenerPosiciones(UUID.randomUUID());

        assertEquals(1, posiciones.size());
        PosicionFuente fila = posiciones.get(0);
        assertEquals("Palmeiras", fila.equipoNombre());
        assertEquals(1, fila.posicion());
        assertEquals(22, fila.jugados());
        assertEquals(14, fila.ganados());
        assertEquals(6, fila.empatados());
        assertEquals(2, fila.perdidos());
        assertEquals(38, fila.golesFavor());
        assertEquals(16, fila.golesContra());
        assertEquals(48, fila.puntos());
        // Clave 1 = más reciente → índice 0. Orden esperado: E,G,P,G,G.
        assertEquals(List.of(ResultadoReciente.EMPATE, ResultadoReciente.GANADO,
                ResultadoReciente.PERDIDO, ResultadoReciente.GANADO, ResultadoReciente.GANADO),
                fila.ultimosResultados());
        server.verify();
    }

    @Test
    void debe_devolver_lista_vacia_cuando_no_hay_tabla() {
        server.expect(requestTo("http://127.0.0.1:8001/ext-position-table-by-league-stable?path_to_scrape=https://flashscore.com/colombia"))
                .andRespond(withSuccess("""
                        { "status_code": 200, "tabla_posiciones": [] }
                        """, MediaType.APPLICATION_JSON));

        List<PosicionFuente> posiciones = adapter.obtenerPosiciones(UUID.randomUUID());

        assertTrue(posiciones.isEmpty());
        server.verify();
    }
}
