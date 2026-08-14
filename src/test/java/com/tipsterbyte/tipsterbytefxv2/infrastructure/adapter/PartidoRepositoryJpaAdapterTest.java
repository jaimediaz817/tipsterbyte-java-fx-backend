// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de PartidoRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del aggregate Partido, incluyendo
//            cuotas, resultado final y los filtros por liga, próximos y por fecha.
// [RELACIONES]: CU-02, CU-03, CU-05, CU-06, CU-07. Cubre el puerto PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartidoRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private PartidoRepository partidoRepository;

    @Test
    void debe_guardar_y_recuperar_partido_con_cuotas_y_resultado() {
        UUID ligaId = UUID.randomUUID();
        Equipo local = new Equipo("Real Madrid");
        Equipo visitante = new Equipo("FC Barcelona");
        List<Cuota> cuotas = List.of(new Cuota(new BigDecimal("1.85")), new Cuota(new BigDecimal("3.20")));
        Partido partido = Partido.reconstruir(
                UUID.randomUUID(), ligaId, local, visitante,
                new FechaProgramada(LocalDateTime.of(2026, 3, 1, 20, 0)),
                EstadoPartido.FINALIZADO, cuotas, new Resultado(2, 1));

        partidoRepository.guardar(partido);

        Partido recuperado = partidoRepository.buscarPorId(partido.id()).orElseThrow();
        assertEquals(ligaId, recuperado.ligaId());
        assertEquals("Real Madrid", recuperado.equipoLocal().nombre());
        assertEquals(2, recuperado.cuotas().size());
        assertEquals(EstadoPartido.FINALIZADO, recuperado.estado());
        assertEquals(2, recuperado.resultado().golesLocal());
        assertTrue(recuperado.pullEventos().isEmpty(), "reconstrucción no debe emitir eventos");
    }

    @Test
    void debe_buscar_proximos_por_liga() {
        UUID ligaId = UUID.randomUUID();
        Equipo a = new Equipo("Equipo A");
        Equipo b = new Equipo("Equipo B");
        Equipo c = new Equipo("Equipo C");
        Equipo d = new Equipo("Equipo D");
        FechaProgramada fecha = new FechaProgramada(LocalDateTime.now().plusDays(3));
        partidoRepository.guardar(new Partido(ligaId, a, b, fecha));
        partidoRepository.guardar(Partido.reconstruir(
                UUID.randomUUID(), ligaId, c, d, fecha, EstadoPartido.FINALIZADO,
                List.of(new Cuota(new BigDecimal("2.10"))), new Resultado(1, 0)));
        partidoRepository.guardar(new Partido(ligaId, a, c, fecha));

        List<Partido> proximos = partidoRepository.buscarProximosPorLiga(ligaId);
        assertEquals(2, proximos.size());
    }

    @Test
    void debe_buscar_por_liga_y_fecha() {
        UUID ligaId = UUID.randomUUID();
        Equipo a = new Equipo("Equipo A");
        Equipo b = new Equipo("Equipo B");
        LocalDateTime fechaHora = LocalDateTime.of(2026, 4, 15, 18, 0);
        partidoRepository.guardar(new Partido(ligaId, a, b, new FechaProgramada(fechaHora)));
        partidoRepository.guardar(new Partido(ligaId, a, b,
                new FechaProgramada(fechaHora.plusDays(2))));

        List<Partido> delDia = partidoRepository.buscarPorLigaYFecha(ligaId, fechaHora.toLocalDate());
        assertEquals(1, delDia.size());
        assertEquals(fechaHora, delDia.get(0).fechaProgramada().fechaHora());
    }
}