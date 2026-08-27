// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios de ConsultarCuotasProximasUseCase (CU-21, HU-15).
// [POR QUÉ]: Valida el comportamiento del snapshot de cuotas próximas con volatilidad:
//            filtrado por estado, ventana de tiempo, cálculo de volatilidad.
// [RELACIONES]: CU-21 → PartidoRepository, CuotaHistorialRepository, VolatilidadCuota.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.*;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.CuotaProximaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarCuotasProximasUseCaseTest {

    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private CuotaHistorialRepository cuotaHistorialRepository;

    private ConsultarCuotasProximasUseCase useCase;

    private UUID ligaId;
    private UUID temporadaId;
    private Equipo local;
    private Equipo visitante;
    private Partido partidoFuturo;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarCuotasProximasUseCase(partidoRepository, cuotaHistorialRepository);
        ligaId = UUID.randomUUID();
        temporadaId = UUID.randomUUID();
        local = new Equipo("Millonarios FC");
        visitante = new Equipo("Santa Fe");
        LocalDateTime fechaFutura = LocalDateTime.now().plusDays(3);
        partidoFuturo = new Partido(temporadaId, local, visitante,
                new FechaProgramada(fechaFutura), 10);
    }

    @Test
    void debe_devolver_snapshot_con_cuotas_y_volatilidad() {
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(partidoFuturo));

        Instant ahora = Instant.now();
        Instant desde = ahora.minus(24, ChronoUnit.HOURS);

        CuotaHistorial baselina = new CuotaHistorial(
                UUID.randomUUID(), partidoFuturo.id(), Mercado.UNO_X_DOS, null,
                new BigDecimal("2.00"), "wplay", desde);
        CuotaHistorial ultima = new CuotaHistorial(
                UUID.randomUUID(), partidoFuturo.id(), Mercado.UNO_X_DOS, null,
                new BigDecimal("2.10"), "wplay", ahora);

        when(cuotaHistorialRepository.buscarPorPartidosYRango(anyList(), any(), any()))
                .thenReturn(List.of(baselina, ultima));

        List<CuotaProximaResponse> resultado = useCase.ejecutar(ligaId, 24);

        assertEquals(1, resultado.size());
        CuotaProximaResponse response = resultado.getFirst();
        assertEquals(partidoFuturo.id(), response.partidoId());
        assertEquals("Millonarios FC", response.equipoLocal());
        assertEquals("Santa Fe", response.equipoVisitante());
        assertNotNull(response.cuotas());
        assertFalse(response.cuotas().isEmpty());
        assertNotNull(response.volatilidad());
    }

    @Test
    void debe_filtrar_solo_partidos_programados() {
        Partido enVivo = new Partido(temporadaId, local, visitante,
                new FechaProgramada(LocalDateTime.now().plusHours(2)), 11);
        enVivo.iniciar();
        Partido finalizado = Partido.reconstruir(
                UUID.randomUUID(), temporadaId, local, visitante,
                new FechaProgramada(LocalDateTime.now().minusDays(1)),
                EstadoPartido.FINALIZADO, List.of(), null, 9);

        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(enVivo, finalizado, partidoFuturo));
        when(cuotaHistorialRepository.buscarPorPartidosYRango(anyList(), any(), any()))
                .thenReturn(List.of());

        List<CuotaProximaResponse> resultado = useCase.ejecutar(ligaId, 24);

        assertEquals(1, resultado.size());
        assertEquals(partidoFuturo.id(), resultado.getFirst().partidoId());
    }

    @Test
    void debe_retornar_lista_vacia_cuando_no_hay_partidos() {
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of());

        List<CuotaProximaResponse> resultado = useCase.ejecutar(ligaId, 24);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void debe_retornar_volatilidad_sin_baseline_cuando_solo_hay_una_captura() {
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(partidoFuturo));

        CuotaHistorial unica = new CuotaHistorial(
                UUID.randomUUID(), partidoFuturo.id(), Mercado.UNO_X_DOS, null,
                new BigDecimal("2.00"), "wplay", Instant.now());

        when(cuotaHistorialRepository.buscarPorPartidosYRango(anyList(), any(), any()))
                .thenReturn(List.of(unica));

        List<CuotaProximaResponse> resultado = useCase.ejecutar(ligaId, 24);

        assertEquals(1, resultado.size());
        assertEquals(VolatilidadCuota.ClaseVolatilidad.SIN_BASELINE,
                resultado.getFirst().volatilidad().clase());
    }

    @Test
    void debe_devolver_varias_cuotas_por_mercado() {
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(partidoFuturo));

        Instant ahora = Instant.now();
        Instant desde = ahora.minus(24, ChronoUnit.HOURS);

        CuotaHistorial local = new CuotaHistorial(
                UUID.randomUUID(), partidoFuturo.id(), Mercado.UNO_X_DOS, "LOCAL",
                new BigDecimal("2.00"), "wplay", desde);
        CuotaHistorial empate = new CuotaHistorial(
                UUID.randomUUID(), partidoFuturo.id(), Mercado.UNO_X_DOS, "EMPATE",
                new BigDecimal("3.20"), "wplay", desde);
        CuotaHistorial visitante = new CuotaHistorial(
                UUID.randomUUID(), partidoFuturo.id(), Mercado.UNO_X_DOS, "VISITANTE",
                new BigDecimal("3.50"), "wplay", desde);

        when(cuotaHistorialRepository.buscarPorPartidosYRango(anyList(), any(), any()))
                .thenReturn(List.of(local, empate, visitante));

        List<CuotaProximaResponse> resultado = useCase.ejecutar(ligaId, 24);

        assertEquals(3, resultado.getFirst().cuotas().size());
    }
}
