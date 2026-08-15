// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de FuenteExtraccionRepositoryJpaAdapter y
//        DetalleFuenteExtraccionRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del catálogo de fuentes y de la
//            asociación liga↔fuente↔URL (unicidad por liga+tipo, resolución de URL).
// [RELACIONES]: CU-04/CU-11. Cubre los puertos FuenteExtraccionRepository y
//               DetalleFuenteExtraccionRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.DetalleFuenteExtraccionJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.FuenteExtraccionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuenteExtraccionRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private FuenteExtraccionRepository fuenteRepository;
    @Autowired
    private DetalleFuenteExtraccionRepository detalleRepository;
    @Autowired
    private FuenteExtraccionJpaRepository fuenteJpaRepository;
    @Autowired
    private DetalleFuenteExtraccionJpaRepository detalleJpaRepository;

    @BeforeEach
    void limpiarTablas() {
        detalleJpaRepository.deleteAll();
        fuenteJpaRepository.deleteAll();
    }

    @Test
    void debe_guardar_y_recuperar_fuente_por_tipo() {
        FuenteExtraccion fuente = new FuenteExtraccion("Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true);

        fuenteRepository.guardar(fuente);

        FuenteExtraccion recuperada = fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.STANDINGS).orElseThrow();
        assertEquals(fuente.id(), recuperada.id());
        assertEquals("Posiciones Flashscore", recuperada.nombre());
        assertTrue(recuperada.activa());
    }

    @Test
    void debe_devolver_vacio_si_tipo_no_existe() {
        assertTrue(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.ODDS_WPLAY).isEmpty());
    }

    @Test
    void debe_recuperar_todas_las_fuentes() {
        fuenteRepository.guardar(new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true));
        fuenteRepository.guardar(new FuenteExtraccion("Calendario", TipoFuenteExtraccion.CALENDAR, true));
        fuenteRepository.guardar(new FuenteExtraccion("Cuotas", TipoFuenteExtraccion.ODDS_WPLAY, true));

        List<FuenteExtraccion> todas = fuenteRepository.buscarTodas();

        assertEquals(3, todas.size());
    }

    @Test
    void debe_guardar_y_resolver_url_de_fuente_por_liga_y_tipo() {
        UUID ligaId = UUID.randomUUID();
        FuenteExtraccion fuente = new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true);
        fuenteRepository.guardar(fuente);

        detalleRepository.guardar(new DetalleFuenteExtraccion(ligaId, fuente, "https://flashscore.com/tabla", true));

        Optional<DetalleFuenteExtraccion> detalle =
                detalleRepository.buscarPorLigaYTipo(ligaId, TipoFuenteExtraccion.STANDINGS);
        assertTrue(detalle.isPresent());
        assertEquals("https://flashscore.com/tabla", detalle.get().url());
        assertEquals("Posiciones", detalle.get().fuente().nombre());
    }

    @Test
    void debe_devolver_vacio_si_liga_sin_fuente_del_tipo() {
        assertTrue(detalleRepository.buscarPorLigaYTipo(UUID.randomUUID(), TipoFuenteExtraccion.STANDINGS).isEmpty());
    }

    @Test
    void debe_recuperar_todos_los_detalles_de_una_liga() {
        UUID ligaId = UUID.randomUUID();
        FuenteExtraccion standings = new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true);
        FuenteExtraccion calendar = new FuenteExtraccion("Calendario", TipoFuenteExtraccion.CALENDAR, true);
        fuenteRepository.guardar(standings);
        fuenteRepository.guardar(calendar);
        detalleRepository.guardar(new DetalleFuenteExtraccion(ligaId, standings, "https://flashscore.com/tabla", true));
        detalleRepository.guardar(new DetalleFuenteExtraccion(ligaId, calendar, "https://soccerway.com/cal", true));

        List<DetalleFuenteExtraccion> detalles = detalleRepository.buscarPorLiga(ligaId);

        assertEquals(2, detalles.size());
    }
}
