// ─────────────────────────────────────────────
// [QUÉ]: Test de la excepción base del dominio DomainException (mensaje y causa).
// [POR QUÉ]: Es el contrato de error que el GlobalExceptionHandler traduce a HTTP 422;
//            verifica que ambos constructores preservan la información de la regla violada.
// [RELACIONES]: DomainException → lanzada por domain.model, capturada por interfaces.rest.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainExceptionTest {

    @Test
    void debe_conservar_el_mensaje() {
        DomainException ex = new DomainException("BR-001: liga sin fuentes activas");
        assertEquals("BR-001: liga sin fuentes activas", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void debe_conservar_mensaje_y_causa() {
        IllegalStateException causa = new IllegalStateException("origen");
        DomainException ex = new DomainException("fallo de mapeo", causa);
        assertEquals("fallo de mapeo", ex.getMessage());
        assertSame(causa, ex.getCause());
    }
}
