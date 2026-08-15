package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetalleFuenteExtraccionTest {

    private final FuenteExtraccion fuente = new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true);

    @Test
    void debe_crear_detalle_con_liga_fuente_y_url() {
        UUID ligaId = UUID.randomUUID();

        DetalleFuenteExtraccion detalle = new DetalleFuenteExtraccion(ligaId, fuente, "https://flashscore.com/tabla", true);

        assertEquals(ligaId, detalle.ligaId());
        assertEquals(fuente, detalle.fuente());
        assertEquals(TipoFuenteExtraccion.STANDINGS, detalle.tipo());
        assertEquals("https://flashscore.com/tabla", detalle.url());
        assertEquals(true, detalle.activa());
    }

    @Test
    void debe_rechazar_liga_nula() {
        assertThrows(DomainException.class,
                () -> new DetalleFuenteExtraccion(null, fuente, "https://flashscore.com/tabla", true));
    }

    @Test
    void debe_rechazar_fuente_nula() {
        assertThrows(DomainException.class,
                () -> new DetalleFuenteExtraccion(UUID.randomUUID(), null, "https://flashscore.com/tabla", true));
    }

    @Test
    void debe_rechazar_url_vacia() {
        assertThrows(DomainException.class,
                () -> new DetalleFuenteExtraccion(UUID.randomUUID(), fuente, "   ", true));
    }
}
