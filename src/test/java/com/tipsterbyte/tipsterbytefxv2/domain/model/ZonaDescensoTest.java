// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios de ZonaDescenso (HU-16 AC4/AC6).
// [POR QUÉ]: Valida construcción y lógica de determinación de zona de descenso.
// [RELACIONES]: ZonaDescenso (domain.model), CU-24 (evaluación).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ZonaDescensoTest {

    @Test
    void debe_crear_zona_descenso_valida() {
        UUID temporadaId = UUID.randomUUID();
        ZonaDescenso zona = new ZonaDescenso(temporadaId, 17, "Desciende el último");

        assertEquals(17, zona.posicionDescenso());
        assertEquals("Desciende el último", zona.descripcion());
    }

    @Test
    void debe_detectar_equipo_en_zona_de_descenso() {
        ZonaDescenso zona = new ZonaDescenso(UUID.randomUUID(), 17, null);

        assertTrue(zona.enZonaDescenso(17));
        assertTrue(zona.enZonaDescenso(18));
        assertTrue(zona.enZonaDescenso(20));
    }

    @Test
    void debe_detectar_equipo_fuera_de_zona() {
        ZonaDescenso zona = new ZonaDescenso(UUID.randomUUID(), 17, null);

        assertFalse(zona.enZonaDescenso(16));
        assertFalse(zona.enZonaDescenso(1));
    }

    @Test
    void debe_rechazar_posicion_invalida() {
        assertThrows(DomainException.class, () ->
                new ZonaDescenso(UUID.randomUUID(), 0, null));
    }

    @Test
    void debe_rechazar_temporada_nula() {
        assertThrows(DomainException.class, () ->
                new ZonaDescenso(null, 17, null));
    }
}
