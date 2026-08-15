// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de PaisRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del catálogo de países y la
//            consulta por ISO alfa-2 (clave natural de la fuente #1).
// [RELACIONES]: CU-10. Cubre el puerto PaisRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaisRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private PaisJpaRepository jpaRepository;

    // [POR QUÉ]: Los tests JPA comparten el contenedor Testcontainers; se limpia la
    //            tabla antes de cada test para que el conteo de "buscarTodos" sea estable.
    @BeforeEach
    void limpiarTabla() {
        jpaRepository.deleteAll();
    }

    @Test
    void debe_guardar_y_recuperar_pais_por_iso_alpha2() {
        Pais pais = new Pais("España", "ES", "Europa", "81", "/espana/", true);

        paisRepository.guardar(pais);

        Pais recuperado = paisRepository.buscarPorIsoAlpha2("ES").orElseThrow();
        assertEquals("España", recuperado.nombre());
        assertEquals("Europa", recuperado.continente());
        assertEquals("81", recuperado.code());
        assertTrue(recuperado.mapeado());
    }

    @Test
    void debe_recuperar_todos_los_paises() {
        paisRepository.guardar(new Pais("España", "ES", "Europa", "81", "/espana/", true));
        paisRepository.guardar(new Pais("Francia", "FR", "Europa", "80", "/francia/", true));

        List<Pais> todos = paisRepository.buscarTodos();

        assertEquals(2, todos.size());
    }

    @Test
    void debe_devolver_vacio_si_iso_no_existe() {
        assertTrue(paisRepository.buscarPorIsoAlpha2("XX").isEmpty());
    }

    @Test
    void debe_recuperar_por_id() {
        Pais pais = new Pais("Colombia", "CO", "America", "82", "/colombia/", true);
        paisRepository.guardar(pais);

        Pais recuperado = paisRepository.buscarPorId(UUID.randomUUID()).orElse(null);
        Pais recuperadoCorrecto = paisRepository.buscarPorId(pais.id()).orElseThrow();
        assertEquals("Colombia", recuperadoCorrecto.nombre());
        assertTrue(recuperado == null);
    }
}