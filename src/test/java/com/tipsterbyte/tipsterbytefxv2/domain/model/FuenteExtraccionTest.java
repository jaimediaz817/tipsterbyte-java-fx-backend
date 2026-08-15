package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuenteExtraccionTest {

    @Test
    void debe_crear_fuente_con_identidad_generada() {
        FuenteExtraccion fuente = new FuenteExtraccion("Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true);

        assertTrue(fuente.id() != null);
        assertEquals("Posiciones Flashscore", fuente.nombre());
        assertEquals(TipoFuenteExtraccion.STANDINGS, fuente.tipo());
        assertTrue(fuente.activa());
    }

    @Test
    void debe_reconstruir_fuente_con_identidad_provista() {
        java.util.UUID id = java.util.UUID.randomUUID();
        FuenteExtraccion fuente = new FuenteExtraccion(id, "Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, false);

        assertEquals(id, fuente.id());
        assertEquals(TipoFuenteExtraccion.ODDS_WPLAY, fuente.tipo());
    }

    @Test
    void debe_rechazar_nombre_vacio() {
        assertThrows(DomainException.class,
                () -> new FuenteExtraccion(" ", TipoFuenteExtraccion.CALENDAR, true));
    }

    @Test
    void debe_rechazar_tipo_nulo() {
        assertThrows(DomainException.class,
                () -> new FuenteExtraccion("Calendario", null, true));
    }

    @Test
    void debe_comparar_por_identidad() {
        java.util.UUID id = java.util.UUID.randomUUID();
        FuenteExtraccion a = new FuenteExtraccion(id, "A", TipoFuenteExtraccion.STANDINGS, true);
        FuenteExtraccion b = new FuenteExtraccion(id, "A", TipoFuenteExtraccion.STANDINGS, true);
        FuenteExtraccion c = new FuenteExtraccion("A", TipoFuenteExtraccion.STANDINGS, true);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
