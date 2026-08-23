// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de ServicioCortesia (H-06): pausa, reintento con backoff
//        exponencial y passthrough cuando está deshabilitado.
// [POR QUÉ]: Verifica la política de cortesía con un Sleeper falso: determinista,
//            sin sleeps reales, registrando cada espera para aserción exacta.
// [RELACIONES]: Usado por los decoradores Proveedor*ConCortesia (#1/#5/#6).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioCortesiaTest {

    // Sleeper falso: registra cada espera sin dormir de verdad.
    private record SleeperFalso(List<Long> esperas) implements Sleeper {
        @Override
        public void dormir(long millis) {
            esperas.add(millis);
        }
    }

    @Test
    void debe_ejecutar_la_llamada_aplicando_pausa_previa() {
        List<Long> esperas = new ArrayList<>();
        ServicioCortesia cortesia = new ServicioCortesia(new SleeperFalso(esperas), true, 250, 2, 1500);

        String resultado = cortesia.ejecutar(() -> "ok");

        assertEquals("ok", resultado);
        assertEquals(List.of(250L), esperas, "una sola pausa si la llamada funciona al primer intento");
    }

    @Test
    void debe_reintentar_con_backoff_exponencial_y_terminar_en_exito() {
        List<Long> esperas = new ArrayList<>();
        ServicioCortesia cortesia = new ServicioCortesia(new SleeperFalso(esperas), true, 250, 2, 1500);

        var intentos = new int[]{0};
        String resultado = cortesia.ejecutar(() -> {
            intentos[0]++;
            if (intentos[0] < 3) {
                throw new RuntimeException("fallo transitorio");
            }
            return "ok en el 3er intento";
        });

        assertEquals("ok en el 3er intento", resultado);
        // Pausa antes de cada intento + backoff exponencial entre reintentos.
        assertEquals(List.of(250L, 1500L, 250L, 3000L, 250L), esperas);
        assertEquals(3, intentos[0]);
    }

    @Test
    void debe_relanzar_el_ultimo_error_tras_agotar_reintentos() {
        List<Long> esperas = new ArrayList<>();
        ServicioCortesia cortesia = new ServicioCortesia(new SleeperFalso(esperas), true, 250, 1, 1000);

        var intentos = new int[]{0};
        RuntimeException lanzada = assertThrows(RuntimeException.class, () ->
                cortesia.ejecutar(() -> {
                    intentos[0]++;
                    throw new RuntimeException("scraper caído");
                }));

        assertEquals("scraper caído", lanzada.getMessage());
        assertEquals(2, intentos[0], "intento inicial + 1 reintento");
        assertEquals(List.of(250L, 1000L, 250L), esperas);
    }

    @Test
    void deshabilitado_es_passthrough_sin_esperas() {
        List<Long> esperas = new ArrayList<>();
        ServicioCortesia cortesia = new ServicioCortesia(new SleeperFalso(esperas), false, 250, 5, 1500);

        RuntimeException lanzada = assertThrows(RuntimeException.class, () ->
                cortesia.ejecutar(() -> {
                    throw new RuntimeException("ni siquiera reintenta");
                }));

        assertEquals("ni siquiera reintenta", lanzada.getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(esperas.isEmpty(), "deshabilitado no duerme nunca");
    }

}
