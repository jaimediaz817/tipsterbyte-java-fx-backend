package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LigaTest {

    private static final Temporada TEMPORADA = new Temporada(2025, 2026);
    private static final Equipo EQUIPO = new Equipo("Equipo A");

    @Test
    void debe_activarse_cuando_fuentes_operativas_br001() {
        Liga liga = new Liga("La Liga", "España", TEMPORADA);
        liga.activar(true, true, true);
        assertEquals(EstadoLiga.ACTIVA, liga.estado());
    }

    @Test
    void debe_rechazar_activacion_sin_fuentes_operativas_br001() {
        Liga liga = new Liga("La Liga", "España", TEMPORADA);
        assertThrows(DomainException.class, () -> liga.activar(true, true, false));
        assertEquals(EstadoLiga.BORRADOR, liga.estado());
    }

    @Test
    void debe_emitir_evento_liga_activada_al_activar() {
        Liga liga = new Liga("La Liga", "España", TEMPORADA);
        liga.activar(true, true, true);
        List<DomainEvent> eventos = liga.pullEventos();
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("LigaActivada")));
    }

    @Test
    void debe_rechazar_extraccion_de_posiciones_en_liga_inactiva_br002() {
        Liga liga = new Liga("La Liga", "España", TEMPORADA);
        PosicionTabla posicion = new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 10);
        assertThrows(DomainException.class, () -> liga.actualizarPosiciones(List.of(posicion)));
    }

    @Test
    void debe_actualizar_posiciones_en_liga_activa() {
        Liga liga = new Liga("La Liga", "España", TEMPORADA);
        liga.activar(true, true, true);
        PosicionTabla posicion = new PosicionTabla(EQUIPO, 1, 5, 3, 1, 1, 10, 4, 10);
        liga.actualizarPosiciones(List.of(posicion));
        assertEquals(1, liga.posiciones().size());
    }

    @Test
    void debe_rechazar_agregar_equipo_duplicado() {
        Liga liga = new Liga("La Liga", "España", TEMPORADA);
        liga.agregarEquipo(EQUIPO);
        assertThrows(DomainException.class, () -> liga.agregarEquipo(EQUIPO));
    }
}