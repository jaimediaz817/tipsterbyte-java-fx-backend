// ─────────────────────────────────────────────
// [QUÉ]: Test del enum Mercado: conjunto cerrado y descripciones legibles.
// [POR QUÉ]: Los mercados son el catálogo fijo que consumen SeleccionPronostico y
//            los adapters de ProveedorCuotas; verifica que el contrato no se rompa.
// [RELACIONES]: Mercado → Cuota, SeleccionPronostico (domain.model).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoTest {

    @Test
    void debe_contener_los_tres_mercados_soportados() {
        assertEquals(3, Mercado.values().length);
        assertTrue(java.util.Arrays.asList(Mercado.values())
                .containsAll(java.util.List.of(Mercado.UNO_X_DOS, Mercado.DOBLE_OPORTUNIDAD, Mercado.OVER_UNDER)));
    }

    @Test
    void debe_exponer_descripcion_legible_de_cada_mercado() {
        assertEquals("1X2", Mercado.UNO_X_DOS.descripcion());
        assertEquals("Doble oportunidad", Mercado.DOBLE_OPORTUNIDAD.descripcion());
        assertEquals("Over/Under", Mercado.OVER_UNDER.descripcion());
    }
}
