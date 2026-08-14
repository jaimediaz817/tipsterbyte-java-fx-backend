// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de PronosticoRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del aggregate Pronostico, incluyendo
//            la restauración de selección, cuota y resultado final verificado, y el
//            filtro de publicados por partidos (CU-08).
// [RELACIONES]: CU-06, CU-07, CU-08. Cubre el puerto PronosticoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.SeleccionPronostico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PronosticoRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private PronosticoRepository pronosticoRepository;

    @Test
    void debe_guardar_y_recuperar_pronostico_con_resultado_final() {
        UUID tipsterId = UUID.randomUUID();
        UUID partidoId = UUID.randomUUID();
        Pronostico pronostico = Pronostico.reconstruir(
                UUID.randomUUID(), tipsterId, partidoId,
                new SeleccionPronostico(Mercado.UNO_X_DOS, "1"),
                new Cuota(new BigDecimal("1.85")), EstadoPronostico.PUBLICADO,
                new Resultado(2, 1));

        pronosticoRepository.guardar(pronostico);

        Pronostico recuperado = pronosticoRepository.buscarPorId(pronostico.id()).orElseThrow();
        assertEquals(tipsterId, recuperado.tipsterId());
        assertEquals(partidoId, recuperado.partidoId());
        assertEquals(Mercado.UNO_X_DOS, recuperado.seleccion().mercado());
        assertEquals("1", recuperado.seleccion().resultadoEsperado());
        assertEquals(EstadoPronostico.PUBLICADO, recuperado.estado());
        assertEquals(2, recuperado.resultadoFinal().golesLocal());
    }

    @Test
    void debe_buscar_solo_publicados_por_partidos() {
        UUID partidoId = UUID.randomUUID();
        Pronostico publicado = new Pronostico(
                UUID.randomUUID(), partidoId,
                new SeleccionPronostico(Mercado.UNO_X_DOS, "X"), new Cuota(new BigDecimal("3.20")));
        publicado.publicar(true, true);
        Pronostico borrador = new Pronostico(
                UUID.randomUUID(), partidoId,
                new SeleccionPronostico(Mercado.OVER_UNDER, "over"), new Cuota(new BigDecimal("1.90")));
        pronosticoRepository.guardar(publicado);
        pronosticoRepository.guardar(borrador);

        List<Pronostico> publicados = pronosticoRepository.buscarPublicadosPorPartidos(List.of(partidoId));
        assertEquals(1, publicados.size());
        assertEquals(publicado.id(), publicados.get(0).id());
        assertTrue(publicados.get(0).pullEventos().isEmpty());
    }
}