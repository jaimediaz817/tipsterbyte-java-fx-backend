// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios de ConsultarHistorialCuotasUseCase (CU-22, HU-15).
// [POR QUÉ]: Valida el historial cronológico de cuotas agrupado por mercado/selección.
// [RELACIONES]: CU-22 → CuotaHistorialRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.CuotaHistorial;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.HistorialCuotaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarHistorialCuotasUseCaseTest {

    @Mock
    private CuotaHistorialRepository cuotaHistorialRepository;

    private ConsultarHistorialCuotasUseCase useCase;

    private UUID partidoId;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarHistorialCuotasUseCase(cuotaHistorialRepository);
        partidoId = UUID.randomUUID();
    }

    @Test
    void debe_agrupar_historial_por_mercado_y_seleccion() {
        Instant ahora = Instant.now();
        Instant hace2h = ahora.minus(2, ChronoUnit.HOURS);

        CuotaHistorial local1 = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.UNO_X_DOS, "LOCAL",
                new BigDecimal("2.00"), "wplay", hace2h);
        CuotaHistorial local2 = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.UNO_X_DOS, "LOCAL",
                new BigDecimal("2.10"), "wplay", ahora);
        CuotaHistorial empate = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.UNO_X_DOS, "EMPATE",
                new BigDecimal("3.20"), "wplay", ahora);

        when(cuotaHistorialRepository.buscarPorPartidoYRango(eq(partidoId), any(), any()))
                .thenReturn(List.of(local1, local2, empate));

        List<HistorialCuotaResponse> resultado = useCase.ejecutar(partidoId, 24, null);

        assertEquals(2, resultado.size());
        // LOCAL debe tener 2 capturas, EMPATE 1.
        HistorialCuotaResponse localGroup = resultado.stream()
                .filter(r -> "LOCAL".equals(r.seleccion()))
                .findFirst().orElseThrow();
        assertEquals(2, localGroup.capturas().size());

        HistorialCuotaResponse empateGroup = resultado.stream()
                .filter(r -> "EMPATE".equals(r.seleccion()))
                .findFirst().orElseThrow();
        assertEquals(1, empateGroup.capturas().size());
    }

    @Test
    void debe_filtrar_por_mercado_cuando_se_especifica() {
        Instant ahora = Instant.now();
        Instant hace2h = ahora.minus(2, ChronoUnit.HOURS);

        CuotaHistorial unoxdos = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.UNO_X_DOS, "LOCAL",
                new BigDecimal("2.00"), "wplay", hace2h);
        CuotaHistorial doble = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.DOBLE_OPORTUNIDAD, "LOCAL_O_EMPATE",
                new BigDecimal("1.50"), "wplay", ahora);

        when(cuotaHistorialRepository.buscarPorPartidoYRango(eq(partidoId), any(), any()))
                .thenReturn(List.of(unoxdos, doble));

        List<HistorialCuotaResponse> resultado = useCase.ejecutar(partidoId, 24, "UNO_X_DOS");

        assertEquals(1, resultado.size());
        assertEquals("UNO_X_DOS", resultado.getFirst().mercado());
    }

    @Test
    void debe_devolver_lista_vacia_cuando_no_hay_capturas() {
        when(cuotaHistorialRepository.buscarPorPartidoYRango(eq(partidoId), any(), any()))
                .thenReturn(List.of());

        List<HistorialCuotaResponse> resultado = useCase.ejecutar(partidoId, 24, null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void debe_ordenar_capturas_cronologicamente() {
        Instant ahora = Instant.now();
        Instant hace1h = ahora.minus(1, ChronoUnit.HOURS);
        Instant hace3h = ahora.minus(3, ChronoUnit.HOURS);

        CuotaHistorial reciente = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.UNO_X_DOS, "LOCAL",
                new BigDecimal("2.10"), "wplay", ahora);
        CuotaHistorial medio = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.UNO_X_DOS, "LOCAL",
                new BigDecimal("2.05"), "wplay", hace1h);
        CuotaHistorial antiguo = new CuotaHistorial(UUID.randomUUID(), partidoId, Mercado.UNO_X_DOS, "LOCAL",
                new BigDecimal("2.00"), "wplay", hace3h);

        when(cuotaHistorialRepository.buscarPorPartidoYRango(eq(partidoId), any(), any()))
                .thenReturn(List.of(reciente, medio, antiguo));

        List<HistorialCuotaResponse> resultado = useCase.ejecutar(partidoId, 24, null);

        assertEquals(1, resultado.size());
        List<HistorialCuotaResponse.Captura> capturas = resultado.getFirst().capturas();
        assertEquals(3, capturas.size());
        // Primera debe ser la más antigua, última la más reciente.
        assertTrue(capturas.getFirst().capturadaEn().isBefore(capturas.getLast().capturadaEn()));
    }
}
