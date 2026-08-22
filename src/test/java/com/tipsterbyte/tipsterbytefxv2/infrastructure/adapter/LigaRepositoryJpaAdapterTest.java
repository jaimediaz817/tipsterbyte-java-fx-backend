// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de LigaRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del aggregate Liga completo, incluyendo
//            la restauración de temporadas (Bridge Fix Torneos/Temporadas), equipos y
//            posiciones, y el filtro de ligas activas.
// [RELACIONES]: CU-01, CU-02, CU-04. Cubre el puerto LigaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.DetalleFuenteExtraccionJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.LigaJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.TemporadaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LigaRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private LigaRepository ligaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpiarTablas() {
        // [POR QUÉ]: Borrado SQL directo hijos→padres en lugar de deleteAll():
        // la cascada de Hibernate al eliminar una LigaEntity disocia primero las
        // posiciones de sus equipos (UPDATE posiciones_tabla SET equipo_id = NULL),
        // lo que viola el NOT NULL de equipo_id. El DELETE directo evita esa ruta.
        jdbcTemplate.update("delete from cuotas");
        jdbcTemplate.update("delete from partidos");
        jdbcTemplate.update("delete from posiciones_tabla");
        jdbcTemplate.update("delete from equipos");
        jdbcTemplate.update("delete from detalle_fuentes_extraccion");
        jdbcTemplate.update("delete from temporadas");
        jdbcTemplate.update("delete from ligas");
    }

    private static Liga ligaConTemporada(String nombre, String pais) {
        Liga liga = new Liga(nombre, pais);
        liga.addTemporada(new Temporada(liga.id(), "2025/2026", null, 2025, 2026,
                EstadoTemporada.PLANIFICADA));
        return liga;
    }

    @Test
    void debe_guardar_y_recuperar_liga_con_temporadas_equipos_y_posiciones() {
        Equipo equipo = new Equipo("Equipo A", "https://escudos/equipo-a.png");
        PosicionTabla posicion = new PosicionTabla(equipo, 1, 5, 3, 1, 1, 10, 4, 10);
        UUID ligaId = UUID.randomUUID();
        Set<Temporada> temporadas = Set.of(new Temporada(
                UUID.randomUUID(), ligaId, "2025/2026", null, 2025, 2026,
                EstadoTemporada.PLANIFICADA, List.of(equipo), List.of(posicion)));
        Liga liga = Liga.reconstruir(
                ligaId, "La Liga", "España", null, EstadoLiga.ACTIVA, temporadas);

        ligaRepository.guardar(liga);

        Liga recuperada = ligaRepository.buscarPorId(liga.id()).orElseThrow();
        assertEquals("La Liga", recuperada.nombre());
        assertEquals(EstadoLiga.ACTIVA, recuperada.estado());
        assertEquals(1, recuperada.getTemporadas().size());
        Temporada temporada = recuperada.getTemporadas().iterator().next();
        assertEquals("2025/2026", temporada.nombre());
        assertEquals(2025, temporada.anioInicio());
        assertEquals(2026, temporada.anioFin());
        // La plantilla y la tabla viven en la TEMPORADA (persistidas con su FK).
        assertEquals(1, temporada.equipos().size());
        // El escudo (logo_url de la fuente #6) persiste y se reconstruye.
        assertEquals("https://escudos/equipo-a.png", temporada.equipos().get(0).logoUrl());
        assertEquals(equipo.id(), temporada.equipos().get(0).id());
        assertEquals(1, temporada.posiciones().size());
        assertEquals(10, temporada.posiciones().get(0).puntos());
        assertTrue(recuperada.pullEventos().isEmpty(), "reconstrucción no debe emitir eventos");
    }

    @Test
    void debe_buscar_solo_ligas_activas() {
        Liga borrador = ligaConTemporada("Liga X", "Colombia");
        Liga activa = ligaConTemporada("Liga Y", "Argentina");
        activa.activar(true, true, true);
        ligaRepository.guardar(borrador);
        ligaRepository.guardar(activa);

        List<Liga> activas = ligaRepository.buscarActivas();
        assertEquals(1, activas.size());
        assertEquals(activa.id(), activas.get(0).id());
    }

    @Test
    void debe_actualizar_posiciones_de_liga_existente() {
        Liga liga = ligaConTemporada("Liga Z", "México");
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

    @Test
    void debe_guardar_y_recuperar_liga_de_catalogo_con_datos_de_fuente() {
        String url = "https://co.soccerway.com/espana/laliga-ea-sports/";
        Liga liga = new Liga("LaLiga EA Sports", "España", url, "1530");
        liga.addTemporada(new Temporada(liga.id(), "2026/2027", null, 2026, 2027,
                EstadoTemporada.PLANIFICADA));

        ligaRepository.guardar(liga);

        Liga recuperada = ligaRepository.buscarPorUrlSoccerway(url).orElseThrow();
        assertEquals("LaLiga EA Sports", recuperada.nombre());
        assertEquals("España", recuperada.pais());
        assertEquals(url, recuperada.urlSoccerway());
        assertEquals("1530", recuperada.apiId());
        assertEquals(EstadoLiga.BORRADOR, recuperada.estado());
        assertEquals(1, recuperada.getTemporadas().size());
    }

    @Test
    void debe_devolver_vacio_cuando_url_soccerway_no_existe() {
        assertTrue(ligaRepository.buscarPorUrlSoccerway("https://co.soccerway.com/xyz/").isEmpty());
    }
}
