// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de LigaRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del aggregate Liga completo, incluyendo
//            la restauración de equipos y posiciones, y el filtro de ligas activas.
// [RELACIONES]: CU-01, CU-02, CU-04. Cubre el puerto LigaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LigaRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private LigaRepository ligaRepository;

    @Test
    void debe_guardar_y_recuperar_liga_con_equipos_y_posiciones() {
        Equipo equipo = new Equipo("Equipo A");
        PosicionTabla posicion = new PosicionTabla(equipo, 1, 5, 3, 1, 1, 10, 4, 10);
        Liga liga = Liga.reconstruir(
                UUID.randomUUID(), "La Liga", "España", new Temporada(2025, 2026),
                EstadoLiga.ACTIVA, List.of(equipo), List.of(posicion));

        ligaRepository.guardar(liga);

        Liga recuperada = ligaRepository.buscarPorId(liga.id()).orElseThrow();
        assertEquals("La Liga", recuperada.nombre());
        assertEquals(EstadoLiga.ACTIVA, recuperada.estado());
        assertEquals(1, recuperada.equipos().size());
        assertEquals(1, recuperada.posiciones().size());
        assertEquals(equipo.id(), recuperada.posiciones().get(0).equipo().id());
        assertEquals(10, recuperada.posiciones().get(0).puntos());
        assertTrue(recuperada.pullEventos().isEmpty(), "reconstrucción no debe emitir eventos");
    }

    @Test
    void debe_buscar_solo_ligas_activas() {
        Liga borrador = new Liga("Liga X", "Colombia", new Temporada(2025, 2026));
        Liga activa = new Liga("Liga Y", "Argentina", new Temporada(2025, 2026));
        activa.activar(true, true, true);
        ligaRepository.guardar(borrador);
        ligaRepository.guardar(activa);

        List<Liga> activas = ligaRepository.buscarActivas();
        assertEquals(1, activas.size());
        assertEquals(activa.id(), activas.get(0).id());
    }

    @Test
    void debe_actualizar_posiciones_de_liga_existente() {
        Liga liga = new Liga("Liga Z", "México", new Temporada(2025, 2026));
        liga.activar(true, true, true);
        Equipo equipo = new Equipo("Equipo B");
        liga.agregarEquipo(equipo);
        ligaRepository.guardar(liga);

        liga.actualizarPosiciones(List.of(new PosicionTabla(equipo, 1, 3, 2, 1, 0, 6, 2, 7)));
        ligaRepository.guardar(liga);

        Liga recuperada = ligaRepository.buscarPorId(liga.id()).orElseThrow();
        assertEquals(1, recuperada.posiciones().size());
        assertEquals(7, recuperada.posiciones().get(0).puntos());
    }
}