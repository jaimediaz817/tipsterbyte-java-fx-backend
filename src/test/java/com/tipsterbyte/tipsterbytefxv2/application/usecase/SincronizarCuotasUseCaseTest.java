package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CuotaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizarCuotasUseCaseTest {

    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private ProveedorCuotas proveedorCuotas;

    private SincronizarCuotasUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new SincronizarCuotasUseCase(partidoRepository, proveedorCuotas);
    }

    private Partido partidoProximo(UUID ligaId) {
        return new Partido(ligaId, new Equipo("Real Madrid"), new Equipo("FC Barcelona"),
                new FechaProgramada(LocalDateTime.of(2026, 3, 1, 20, 0)));
    }

    @Test
    void debe_actualizar_cuotas_y_emitir_cuota_actualizada() {
        UUID ligaId = UUID.randomUUID();
        Partido partido = partidoProximo(ligaId);
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(partido));
        when(proveedorCuotas.obtenerCuotas(partido.id())).thenReturn(List.of(
                new CuotaFuente(Mercado.UNO_X_DOS, new BigDecimal("1.85")),
                new CuotaFuente(Mercado.UNO_X_DOS, new BigDecimal("2.10"))));

        List<DomainEvent> eventos = casoDeUso.ejecutar(ligaId);

        assertEquals(2, partido.cuotas().size());
        assertTrue(partido.cuotas().stream().map(Cuota::valor)
                .allMatch(v -> v.compareTo(BigDecimal.ONE) > 0));
        verify(partidoRepository).guardar(partido);
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("CuotaActualizada")));
    }

    @Test
    void debe_preservar_el_mercado_de_las_cuotas_de_la_fuente() {
        UUID ligaId = UUID.randomUUID();
        Partido partido = partidoProximo(ligaId);
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(partido));
        when(proveedorCuotas.obtenerCuotas(partido.id())).thenReturn(List.of(
                new CuotaFuente(Mercado.UNO_X_DOS, new BigDecimal("1.85")),
                new CuotaFuente(Mercado.DOBLE_OPORTUNIDAD, new BigDecimal("1.45"))));

        casoDeUso.ejecutar(ligaId);

        assertEquals(Mercado.UNO_X_DOS, partido.cuotas().get(0).mercado());
        assertEquals(Mercado.DOBLE_OPORTUNIDAD, partido.cuotas().get(1).mercado());
    }

    @Test
    void debe_descartar_cuotas_invalidas_br007() {
        UUID ligaId = UUID.randomUUID();
        Partido partido = partidoProximo(ligaId);
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of(partido));
        when(proveedorCuotas.obtenerCuotas(partido.id())).thenReturn(List.of(
                new CuotaFuente(Mercado.UNO_X_DOS, new BigDecimal("0.90"))));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(ligaId));
        verify(partidoRepository, never()).guardar(any());
    }

    @Test
    void debe_no_emitir_eventos_cuando_no_hay_partidos_proximos() {
        UUID ligaId = UUID.randomUUID();
        when(partidoRepository.buscarProximosPorLiga(ligaId)).thenReturn(List.of());

        List<DomainEvent> eventos = casoDeUso.ejecutar(ligaId);

        assertTrue(eventos.isEmpty());
        verify(proveedorCuotas, never()).obtenerCuotas(any());
    }
}