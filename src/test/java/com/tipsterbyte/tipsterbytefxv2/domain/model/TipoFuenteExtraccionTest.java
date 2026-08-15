// ─────────────────────────────────────────────
// [QUÉ]: Test del enum TipoFuenteExtraccion: los 3 tipos canónicos que alinean los
//        adapters de extracción con el catálogo de fuentes (FASE 8.5).
// [POR QUÉ]: Cualquier desalineación de nombres rompe la resolución de URL por liga
//            (CU-04/CU-11); verifica el contrato del conjunto cerrado.
// [RELACIONES]: TipoFuenteExtraccion → FuenteExtraccion, DetalleFuenteExtraccion,
//               WplayCuotasAdapter, FlashscorePosicionesAdapter, SoccerwayCalendarioAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoFuenteExtraccionTest {

    @Test
    void debe_contener_los_tres_tipos_de_fuente() {
        assertEquals(3, TipoFuenteExtraccion.values().length);
        assertTrue(java.util.Arrays.asList(TipoFuenteExtraccion.values())
                .containsAll(java.util.List.of(
                        TipoFuenteExtraccion.STANDINGS,
                        TipoFuenteExtraccion.ODDS_WPLAY,
                        TipoFuenteExtraccion.CALENDAR)));
    }

    @Test
    void debe_resolver_por_nombre_canonico() {
        assertEquals(TipoFuenteExtraccion.STANDINGS, TipoFuenteExtraccion.valueOf("STANDINGS"));
        assertEquals(TipoFuenteExtraccion.ODDS_WPLAY, TipoFuenteExtraccion.valueOf("ODDS_WPLAY"));
        assertEquals(TipoFuenteExtraccion.CALENDAR, TipoFuenteExtraccion.valueOf("CALENDAR"));
    }
}
