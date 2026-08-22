// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de PartidoRepositoryJpaAdapter contra PostgreSQL (Testcontainers).
// [POR QUÉ]: Verifica el ciclo guardar → recuperar del aggregate Partido, incluyendo
//            cuotas, resultado final, jornada y los filtros por liga (vía JOIN a través
//            de la temporada), próximos y por fecha. El partido referencia su temporada
//            (Bridge Fix Torneos/Temporadas): la fixture crea liga + temporada reales.
// [RELACIONES]: CU-02, CU-03, CU-05, CU-06, CU-07. Cubre el puerto PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
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
    @Autowired
    private LigaRepository ligaRepository;

    // [POR QUÉ]: La FK partidos.temporada_id → temporadas.id exige liga + temporada reales.
    private Temporada crearTemporadaReal() {
        Liga liga = new Liga("La Liga", "España");
        Temporada temporada = new Temporada(liga.id(), "2025/2026", null, 2025, 2026,
                EstadoTemporada.PLANIFICADA);
        liga.addTemporada(temporada);
        ligaRepository.guardar(liga);
        return temporada;
    }

    @Test
    void debe_guardar_y_recuperar_partido_con_cuotas_resultado_y_jornada() {
        Temporada temporada = crearTemporadaReal();
        Equipo local = new Equipo("Real Madrid");
        Equipo visitante = new Equipo("FC Barcelona");
        List<Cuota> cuotas = List.of(new Cuota(new BigDecimal("1.85")), new Cuota(new BigDecimal("3.20")));
        Partido partido = Partido.reconstruir(
                UUID.randomUUID(), temporada.id(), local, visitante,
                new FechaProgramada(LocalDateTime.of(2026, 3, 1, 20, 0)),
                EstadoPartido.FINALIZADO, cuotas, new Resultado(2, 1), 4);

        partidoRepository.guardar(partido);

        Partido recuperado = partidoRepository.buscarPorId(partido.id()).orElseThrow();
        assertEquals(temporada.id(), recuperado.temporadaId());
        assertEquals("Real Madrid", recuperado.equipoLocal().nombre());
        assertEquals(2, recuperado.cuotas().size());
        assertEquals(EstadoPartido.FINALIZADO, recuperado.estado());
        assertEquals(2, recuperado.resultado().golesLocal());
        assertEquals(4, recuperado.jornada());
        assertTrue(recuperado.pullEventos().isEmpty(), "reconstrucción no debe emitir eventos");
    }

    @Test
    void debe_buscar_proximos_por_liga() {
        Temporada temporada = crearTemporadaReal();
        UUID ligaId = temporada.ligaId();
        Equipo a = new Equipo("Equipo A");
        Equipo b = new Equipo("Equipo B");
        Equipo c = new Equipo("Equipo C");
        Equipo d = new Equipo("Equipo D");
        FechaProgramada fecha = new FechaProgramada(LocalDateTime.now().plusDays(3));
        partidoRepository.guardar(new Partido(temporada.id(), a, b, fecha));
        partidoRepository.guardar(Partido.reconstruir(
                UUID.randomUUID(), temporada.id(), c, d, fecha, EstadoPartido.FINALIZADO,
                List.of(new Cuota(new BigDecimal("2.10"))), new Resultado(1, 0), null));
        partidoRepository.guardar(new Partido(temporada.id(), a, c, fecha));

        List<Partido> proximos = partidoRepository.buscarProximosPorLiga(ligaId);
        assertEquals(2, proximos.size());
    }

    @Test
    void debe_buscar_por_liga_y_fecha() {
        Temporada temporada = crearTemporadaReal();
        UUID ligaId = temporada.ligaId();
        Equipo a = new Equipo("Equipo A");
        Equipo b = new Equipo("Equipo B");
        LocalDateTime fechaHora = LocalDateTime.of(2026, 4, 15, 18, 0);
        partidoRepository.guardar(new Partido(temporada.id(), a, b, new FechaProgramada(fechaHora)));
        partidoRepository.guardar(new Partido(temporada.id(), a, b,
                new FechaProgramada(fechaHora.plusDays(2))));

        List<Partido> delDia = partidoRepository.buscarPorLigaYFecha(ligaId, fechaHora.toLocalDate());
        assertEquals(1, delDia.size());
        assertEquals(fechaHora, delDia.get(0).fechaProgramada().fechaHora());
    }
}
