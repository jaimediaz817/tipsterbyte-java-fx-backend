// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del entity PaisInteres (país de interés del catálogo, CU-14).
// [POR QUÉ]: Verifica las invariantes del VO/entity: iso_alpha2 y nombre obligatorios,
//            prioridad positiva, y la normalización a MAYÚSCULAS del iso_alpha2 para
//            comparar de forma estable con la fuente #1.
// [RELACIONES]: CU-14 → PaisInteres → PaisInteresRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaisInteresTest {

    @Test
    void debe_crear_pais_de_interes_valido() {
        PaisInteres interes = new PaisInteres("CO", "Colombia", 1);

        assertEquals("CO", interes.isoAlpha2());
        assertEquals("Colombia", interes.nombre());
        assertEquals(1, interes.prioridad());
    }

    @Test
    void debe_normalizar_iso_alpha2_a_mayusculas() {
        PaisInteres interes = new PaisInteres("es", "España", 2);

        assertEquals("ES", interes.isoAlpha2());
    }

    @Test
    void debe_rechazar_iso_alpha2_vacio() {
        assertThrows(DomainException.class, () -> new PaisInteres(" ", "España", 1));
    }

    @Test
    void debe_rechazar_nombre_vacio() {
        assertThrows(DomainException.class, () -> new PaisInteres("ES", "  ", 1));
    }

    @Test
    void debe_rechazar_prioridad_no_positiva() {
        assertThrows(DomainException.class, () -> new PaisInteres("ES", "España", 0));
        assertThrows(DomainException.class, () -> new PaisInteres("ES", "España", -3));
    }
}