// ─────────────────────────────────────────────
// [QUÉ]: Test del VO FechaProgramada: validación de nulidad y semántica de pasada.
// [POR QUÉ]: Cubre las invariantes que usa el aggregate Partido (CU-02) al programar.
// [RELACIONES]: FechaProgramada → Partido (domain.model).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FechaProgramadaTest {

    private final LocalDateTime ahora = LocalDateTime.of(2026, 8, 15, 10, 0);

    @Test
    void debe_aceptar_fecha_valida() {
        FechaProgramada fecha = new FechaProgramada(ahora);
        assertEquals(ahora, fecha.fechaHora());
    }

    @Test
    void debe_rechazar_fecha_nula() {
        assertThrows(DomainException.class, () -> new FechaProgramada(null));
    }

    @Test
    void debe_decir_pasada_cuando_el_momento_coincide() {
        FechaProgramada fecha = new FechaProgramada(ahora);
        assertTrue(fecha.esPasada(ahora));
    }

    @Test
    void debe_decir_pasada_cuando_el_momento_es_posterior() {
        FechaProgramada fecha = new FechaProgramada(ahora);
        assertTrue(fecha.esPasada(ahora.plusHours(1)));
    }

    @Test
    void debe_decir_no_pasada_cuando_el_momento_es_anterior() {
        FechaProgramada fecha = new FechaProgramada(ahora);
        assertFalse(fecha.esPasada(ahora.minusHours(1)));
    }

    @Test
    void debe_decir_no_pasada_si_el_momento_es_nulo() {
        FechaProgramada fecha = new FechaProgramada(ahora);
        assertFalse(fecha.esPasada(null));
    }
}
