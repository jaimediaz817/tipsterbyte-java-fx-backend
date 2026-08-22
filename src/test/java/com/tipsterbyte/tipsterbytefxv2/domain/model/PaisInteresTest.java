// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del entity PaisInteres (país de interés del catálogo, CU-14).
// [POR QUÉ]: Verifica las invariantes del VO/entity: iso_alpha2 y nombre obligatorios,
//            prioridad positiva, maxLigasPorPais opcional (null = sin límite), y la
//            normalización a MAYÚSCULAS del iso_alpha2 para comparar de forma estable
//            con la fuente #1.
// [RELACIONES]: CU-14 → PaisInteres → PaisInteresRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaisInteresTest {

    @Test
    void debe_crear_pais_de_interes_valido() {
        PaisInteres interes = new PaisInteres("CO", "Colombia", 1, null);

        assertEquals("CO", interes.isoAlpha2());
        assertEquals("Colombia", interes.nombre());
        assertEquals(1, interes.prioridad());
        assertNull(interes.maxLigasPorPais());
    }

    @Test
    void debe_crear_pais_de_interes_con_limite_de_ligas() {
        PaisInteres interes = new PaisInteres("CO", "Colombia", 1, 5);

        assertEquals(5, interes.maxLigasPorPais());
    }

    @Test
    void debe_reconstruir_pais_de_interes_con_identidad_y_limite() {
        UUID id = UUID.randomUUID();

        PaisInteres interes = new PaisInteres(id, "CO", "Colombia", 1, 3);

        assertEquals(id, interes.id());
        assertEquals(3, interes.maxLigasPorPais());
    }

    @Test
    void debe_normalizar_iso_alpha2_a_mayusculas() {
        PaisInteres interes = new PaisInteres("es", "España", 2, null);

        assertEquals("ES", interes.isoAlpha2());
    }

    @Test
    void debe_rechazar_iso_alpha2_vacio() {
        assertThrows(DomainException.class, () -> new PaisInteres(" ", "España", 1, null));
    }

    @Test
    void debe_rechazar_nombre_vacio() {
        assertThrows(DomainException.class, () -> new PaisInteres("ES", "  ", 1, null));
    }

    @Test
    void debe_rechazar_prioridad_no_positiva() {
        assertThrows(DomainException.class, () -> new PaisInteres("ES", "España", 0, null));
        assertThrows(DomainException.class, () -> new PaisInteres("ES", "España", -3, null));
    }
}
