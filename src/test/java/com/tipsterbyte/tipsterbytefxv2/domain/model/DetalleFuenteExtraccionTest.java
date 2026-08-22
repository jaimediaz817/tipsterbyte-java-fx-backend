// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del entity DetalleFuenteExtraccion (asociación temporada ↔
//        fuente ↔ URL, CU-04/CU-11).
// [POR QUÉ]: Verifica las invariantes: temporadaId y fuente obligatorios, URL no vacía,
//            tipo derivado de la fuente y el flag activa.
// [RELACIONES]: CU-04/CU-11 → DetalleFuenteExtraccion; resuelto por adapters de fuentes.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetalleFuenteExtraccionTest {

    private final FuenteExtraccion fuente = new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true);

    @Test
    void debe_crear_detalle_con_temporada_fuente_y_url() {
        UUID temporadaId = UUID.randomUUID();

        DetalleFuenteExtraccion detalle = new DetalleFuenteExtraccion(temporadaId, fuente, "https://flashscore.com/tabla", true);

        assertEquals(temporadaId, detalle.temporadaId());
        assertEquals(fuente, detalle.fuente());
        assertEquals(TipoFuenteExtraccion.STANDINGS, detalle.tipo());
        assertEquals("https://flashscore.com/tabla", detalle.url());
        assertTrue(detalle.activa());
    }

    @Test
    void debe_reconstruir_detalle_con_identidad() {
        UUID id = UUID.randomUUID();
        UUID temporadaId = UUID.randomUUID();

        DetalleFuenteExtraccion detalle = new DetalleFuenteExtraccion(
                id, temporadaId, fuente, "https://flashscore.com/tabla", false);

        assertEquals(id, detalle.id());
        assertEquals(temporadaId, detalle.temporadaId());
    }

    @Test
    void debe_rechazar_temporada_nula() {
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
