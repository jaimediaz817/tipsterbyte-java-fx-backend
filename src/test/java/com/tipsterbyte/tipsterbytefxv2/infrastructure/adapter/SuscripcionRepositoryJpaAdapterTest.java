// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de SuscripcionRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del aggregate Suscripcion y el
//            filtro de suscripciones activas por cliente (CU-08 valida BR-006).
// [RELACIONES]: CU-08, CU-09. Cubre el puerto SuscripcionRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuscripcionRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Test
    void debe_guardar_y_recuperar_suscripcion_activa() {
        UUID clienteId = UUID.randomUUID();
        UUID tipsterId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.now();
        Suscripcion suscripcion = Suscripcion.reconstruir(
                UUID.randomUUID(), clienteId, tipsterId,
                new Plan("Premium", new BigDecimal("9.99"), 30),
                inicio, inicio.plusDays(30), EstadoSuscripcion.ACTIVA);

        suscripcionRepository.guardar(suscripcion);

        List<Suscripcion> activas = suscripcionRepository.buscarActivasPorCliente(clienteId);
        assertEquals(1, activas.size());
        Suscripcion recuperada = activas.get(0);
        assertEquals(tipsterId, recuperada.tipsterId());
        assertEquals("Premium", recuperada.plan().nombre());
        assertTrue(recuperada.estaActiva(inicio.plusDays(15)));
    }

    @Test
    void debe_excluir_suscripciones_no_activas_de_un_cliente() {
        UUID clienteId = UUID.randomUUID();
        UUID tipsterId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.now();
        Suscripcion activa = Suscripcion.reconstruir(
                UUID.randomUUID(), clienteId, tipsterId,
                new Plan("Basic", new BigDecimal("4.99"), 30),
                inicio, inicio.plusDays(30), EstadoSuscripcion.ACTIVA);
        Suscripcion cancelada = Suscripcion.reconstruir(
                UUID.randomUUID(), clienteId, tipsterId,
                new Plan("Premium", new BigDecimal("9.99"), 30),
                inicio, inicio.plusDays(30), EstadoSuscripcion.CANCELADA);
        suscripcionRepository.guardar(activa);
        suscripcionRepository.guardar(cancelada);

        List<Suscripcion> activas = suscripcionRepository.buscarActivasPorCliente(clienteId);
        assertEquals(1, activas.size());
        assertEquals(activa.id(), activas.get(0).id());
    }
}