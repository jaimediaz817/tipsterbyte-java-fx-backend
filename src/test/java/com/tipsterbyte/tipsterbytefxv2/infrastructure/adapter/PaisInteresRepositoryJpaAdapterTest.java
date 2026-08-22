// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de PaisInteresRepositoryJpaAdapter contra PostgreSQL
//        (Testcontainers): guardar/recuperar por iso_alpha2, listado por prioridad,
//        eliminación y la unicidad de iso_alpha2.
// [POR QUÉ]: Cierra la cadena application → port → adapter JPA → PostgreSQL con la
//            tabla nueva paises_interes (regla testing.md: adapters con Testcontainers).
// [RELACIONES]: CU-14 → PaisInteresRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisInteresJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaisInteresRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private PaisInteresRepository paisInteresRepository;

    @Autowired
    private PaisInteresJpaRepository paisInteresJpaRepository;

    @BeforeEach
    void limpiar() {
        paisInteresJpaRepository.deleteAll();
    }

    @Test
    void debe_guardar_y_recuperar_por_iso() {
        paisInteresRepository.guardar(new PaisInteres("CO", "Colombia", 1, null));

        PaisInteres recuperado = paisInteresRepository.buscarPorIsoAlpha2("CO").orElseThrow();

        assertEquals("CO", recuperado.isoAlpha2());
        assertEquals("Colombia", recuperado.nombre());
        assertEquals(1, recuperado.prioridad());
    }

    @Test
    void debe_guardar_y_recuperar_max_ligas_por_pais() {
        paisInteresRepository.guardar(new PaisInteres("CO", "Colombia", 1, 5));

        PaisInteres recuperado = paisInteresRepository.buscarPorIsoAlpha2("CO").orElseThrow();

        assertEquals(5, recuperado.maxLigasPorPais());
    }

    @Test
    void debe_listar_ordenado_por_prioridad() {
        paisInteresRepository.guardar(new PaisInteres("ES", "España", 2, null));
        paisInteresRepository.guardar(new PaisInteres("CO", "Colombia", 1, null));

        List<PaisInteres> lista = paisInteresRepository.listarPorPrioridad();

        assertEquals(2, lista.size());
        assertEquals("CO", lista.get(0).isoAlpha2());
        assertEquals("ES", lista.get(1).isoAlpha2());
    }

    @Test
    void debe_eliminar_por_iso() {
        paisInteresRepository.guardar(new PaisInteres("CO", "Colombia", 1, null));

        paisInteresRepository.eliminar("CO");

        assertTrue(paisInteresRepository.buscarPorIsoAlpha2("CO").isEmpty());
    }

    @Test
    void debe_rechazar_iso_duplicado() {
        paisInteresRepository.guardar(new PaisInteres("CO", "Colombia", 1, null));

        assertThrows(DataIntegrityViolationException.class,
                () -> paisInteresRepository.guardar(new PaisInteres("CO", "Colombia", 2, null)));
    }
}