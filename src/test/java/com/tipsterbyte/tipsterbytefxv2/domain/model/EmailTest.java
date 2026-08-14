package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void debe_aceptar_email_valido() {
        assertDoesNotThrow(() -> new Email("tipster@example.com"));
    }

    @Test
    void debe_rechazar_email_sin_arroba() {
        assertThrows(DomainException.class, () -> new Email("tipster.example.com"));
    }

    @Test
    void debe_rechazar_email_sin_dominio() {
        assertThrows(DomainException.class, () -> new Email("tipster@"));
    }

    @Test
    void debe_rechazar_email_nulo() {
        assertThrows(DomainException.class, () -> new Email(null));
    }
}