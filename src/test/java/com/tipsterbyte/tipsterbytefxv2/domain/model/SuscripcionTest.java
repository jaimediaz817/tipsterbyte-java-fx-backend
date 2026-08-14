package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuscripcionTest {

    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final UUID TIPSTER_ID = UUID.randomUUID();
    private static final Plan PLAN = new Plan("Premium", new BigDecimal("9.99"), 30);
    private static final LocalDateTime HOY = LocalDateTime.now();

    @Test
    void debe_estar_activa_dentro_del_periodo_br006() {
        Suscripcion suscripcion = new Suscripcion(CLIENTE_ID, TIPSTER_ID, PLAN, HOY);
        assertTrue(suscripcion.estaActiva(HOY.plusDays(15)));
    }

    @Test
    void debe_no_estar_activa_tras_la_fecha_fin() {
        Suscripcion suscripcion = new Suscripcion(CLIENTE_ID, TIPSTER_ID, PLAN, HOY);
        assertFalse(suscripcion.estaActiva(HOY.plusDays(31)));
    }

    @Test
    void debe_no_estar_activa_al_cancelarse() {
        Suscripcion suscripcion = new Suscripcion(CLIENTE_ID, TIPSTER_ID, PLAN, HOY);
        suscripcion.cancelar();
        assertFalse(suscripcion.estaActiva(HOY.plusDays(1)));
    }

    @Test
    void debe_rechazar_cancelar_suscripcion_no_activa() {
        Suscripcion suscripcion = new Suscripcion(CLIENTE_ID, TIPSTER_ID, PLAN, HOY);
        suscripcion.cancelar();
        assertThrows(DomainException.class, suscripcion::cancelar);
    }

    @Test
    void debe_expirar_al_llegar_la_fecha_fin() {
        Suscripcion suscripcion = new Suscripcion(CLIENTE_ID, TIPSTER_ID, PLAN, HOY);
        suscripcion.expirar(HOY.plusDays(31));
        assertEquals(EstadoSuscripcion.EXPIRADA, suscripcion.estado());
    }
}