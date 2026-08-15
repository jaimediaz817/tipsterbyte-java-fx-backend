// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del entity Pais (dominio).
// [POR QUÉ]: Verifica las invariantes del catálogo: id, nombre e isoAlpha2 son
//            obligatorios (la fuente #1 siempre los entrega).
// [RELACIONES]: CU-10. Entity de dominio.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaisTest {

    @Test
    void debe_crear_pais_con_identidad_generada() {
        Pais pais = new Pais("España", "ES", "Europa", "81", "/espana/", true);

        assertTrue(pais.id() != null);
        assertEquals("España", pais.nombre());
        assertEquals("ES", pais.isoAlpha2());
        assertEquals("Europa", pais.continente());
        assertEquals("81", pais.code());
        assertEquals("/espana/", pais.href());
        assertTrue(pais.mapeado());
    }

    @Test
    void debe_rechazar_pais_sin_nombre() {
        assertThrows(DomainException.class,
                () -> new Pais(" ", "ES", "Europa", "81", "/espana/", true));
    }

    @Test
    void debe_rechazar_pais_sin_iso_alpha2() {
        assertThrows(DomainException.class,
                () -> new Pais("España", null, "Europa", "81", "/espana/", true));
    }

    @Test
    void debe_rechazar_pais_sin_id_al_reconstruir() {
        assertThrows(DomainException.class,
                () -> new Pais(null, "España", "ES", "Europa", "81", "/espana/", true));
    }

    @Test
    void debe_igualar_por_id() {
        UUID id = UUID.randomUUID();
        Pais a = new Pais(id, "España", "ES", "Europa", "81", "/espana/", true);
        Pais b = new Pais(id, "España", "ES", "Europa", "81", "/espana/", false);

        assertTrue(a.equals(b));
        assertFalse(a.equals(new Pais(UUID.randomUUID(), "Francia", "FR", "Europa", "80", "/francia/", true)));
    }
}