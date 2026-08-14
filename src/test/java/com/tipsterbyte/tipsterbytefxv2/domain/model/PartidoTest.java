package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartidoTest {

    private static final UUID LIGA_ID = UUID.randomUUID();
    private static final Equipo LOCAL = new Equipo("Equipo A");
    private static final Equipo VISITANTE = new Equipo("Equipo B");
    private static final FechaProgramada FECHA = new FechaProgramada(LocalDateTime.now().plusDays(3));

    private static Partido partidoProgramado() {
        return new Partido(LIGA_ID, LOCAL, VISITANTE, FECHA);
    }

    @Test
    void debe_emitir_evento_partido_programado_al_crearse() {
        Partido partido = partidoProgramado();
        assertEquals(1, partido.pullEventos().size());
    }

    @Test
    void debe_rechazar_equipos_iguales() {
        assertThrows(DomainException.class, () -> new Partido(LIGA_ID, LOCAL, LOCAL, FECHA));
    }

    @Test
    void debe_actualizar_cuotas_desde_fuente_br007() {
        Partido partido = partidoProgramado();
        List<Cuota> cuotas = List.of(new Cuota(new BigDecimal("1.85")), new Cuota(new BigDecimal("3.20")));
        partido.actualizarCuotas(cuotas);
        assertEquals(2, partido.cuotas().size());
    }

    @Test
    void debe_rechazar_actualizacion_de_cuotas_vacia() {
        Partido partido = partidoProgramado();
        assertThrows(DomainException.class, () -> partido.actualizarCuotas(List.of()));
    }

    @Test
    void debe_iniciar_y_finalizar_partido() {
        Partido partido = partidoProgramado();
        partido.iniciar();
        assertEquals(EstadoPartido.EN_VIVO, partido.estado());
        partido.finalizar();
        assertEquals(EstadoPartido.FINALIZADO, partido.estado());
    }

    @Test
    void debe_asignar_resultado_solo_al_finalizar_br003() {
        Partido partido = partidoProgramado();
        assertThrows(DomainException.class, () -> partido.asignarResultado(new Resultado(2, 1)));
        partido.finalizar();
        partido.asignarResultado(new Resultado(2, 1));
        assertEquals(2, partido.resultado().golesLocal());
    }
}