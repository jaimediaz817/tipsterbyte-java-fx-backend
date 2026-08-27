// ─────────────────────────────────────────────
// [QUÉ]: Tests unitarios de GestionarEstrategiasUseCase (CU-23, HU-16).
// [POR QUÉ]: Valida CRUD, validación de límite de estrategias activas y validación de criterios.
// [RELACIONES]: CU-23 → EstrategiaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.EstrategiaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Criterio;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Estrategia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionarEstrategiasUseCaseTest {

    @Mock
    private EstrategiaRepository estrategiaRepository;

    private GestionarEstrategiasUseCase useCase;

    private UUID tipsterId;

    @BeforeEach
    void setUp() {
        useCase = new GestionarEstrategiasUseCase(estrategiaRepository);
        tipsterId = UUID.randomUUID();
    }

    @Test
    void debe_crear_estrategia_con_criterios_validos() {
        when(estrategiaRepository.contarActivasPorTipsterId(tipsterId)).thenReturn(0L);

        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.CUOTAS, "cuota_1x",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "1.40",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        Estrategia resultado = useCase.crear("Estrategia Local", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), List.of(criterio), List.of());

        assertNotNull(resultado);
        assertEquals("Estrategia Local", resultado.nombre());
        verify(estrategiaRepository).guardar(any(Estrategia.class));
    }

    @Test
    void debe_rechazar_cuando_supera_limite_de_estrategias_activas() {
        when(estrategiaRepository.contarActivasPorTipsterId(tipsterId)).thenReturn(10L);

        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.CUOTAS, "cuota_1x",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "1.40",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        assertThrows(DomainException.class, () ->
                useCase.crear("Estrategia Extra", tipsterId, Mercado.UNO_X_DOS,
                        5, new BigDecimal("0.60"), List.of(criterio), List.of()));
    }

    @Test
    void debe_rechazar_criterio_con_fuente_cuota_y_campo_invalido() {
        when(estrategiaRepository.contarActivasPorTipsterId(tipsterId)).thenReturn(0L);

        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.CUOTAS, "diferencia_posiciones",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "3",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        assertThrows(DomainException.class, () ->
                useCase.crear("Estrategia Mala", tipsterId, Mercado.UNO_X_DOS,
                        5, new BigDecimal("0.60"), List.of(criterio), List.of()));
    }

    @Test
    void debe_rechazar_criterio_con_fuente_posiciones_y_campo_cuota() {
        when(estrategiaRepository.contarActivasPorTipsterId(tipsterId)).thenReturn(0L);

        Criterio criterio = new Criterio(
                Criterio.FuenteCriterio.POSICIONES, "cuota_1x",
                Criterio.OperadorCriterio.MAYOR_IGUAL, "3",
                Criterio.ReferenciaCriterio.LOCAL,
                new BigDecimal("0.25"), 1);

        assertThrows(DomainException.class, () ->
                useCase.crear("Estrategia Mala 2", tipsterId, Mercado.UNO_X_DOS,
                        5, new BigDecimal("0.60"), List.of(criterio), List.of()));
    }

    @Test
    void debe_listar_estrategias_del_tipster() {
        Estrategia estrategia = new Estrategia("Test", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), List.of(), List.of());
        when(estrategiaRepository.buscarPorTipsterId(tipsterId)).thenReturn(List.of(estrategia));

        List<Estrategia> resultado = useCase.listar(tipsterId);

        assertEquals(1, resultado.size());
        assertEquals("Test", resultado.getFirst().nombre());
    }

    @Test
    void debe_obtener_por_id() {
        UUID id = UUID.randomUUID();
        Estrategia estrategia = new Estrategia(id, "Test", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), true, List.of(), List.of(), java.time.Instant.now());
        when(estrategiaRepository.buscarPorId(id)).thenReturn(Optional.of(estrategia));

        Optional<Estrategia> resultado = useCase.obtenerPorId(id);

        assertTrue(resultado.isPresent());
        assertEquals(id, resultado.get().id());
    }

    @Test
    void debe_eliminar_estrategia_existente() {
        UUID id = UUID.randomUUID();
        when(estrategiaRepository.buscarPorId(id)).thenReturn(Optional.of(
                new Estrategia(id, "Test", tipsterId, Mercado.UNO_X_DOS,
                        5, new BigDecimal("0.60"), true, List.of(), List.of(), java.time.Instant.now())));

        useCase.eliminar(id);

        verify(estrategiaRepository).eliminar(id);
    }

    @Test
    void debe_lanzar_excepcion_al_eliminar_inexistente() {
        UUID id = UUID.randomUUID();
        when(estrategiaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> useCase.eliminar(id));
    }

    @Test
    void debe_cambiar_estado_estrategia() {
        UUID id = UUID.randomUUID();
        Estrategia estrategia = new Estrategia(id, "Test", tipsterId, Mercado.UNO_X_DOS,
                5, new BigDecimal("0.60"), false, List.of(), List.of(), java.time.Instant.now());
        when(estrategiaRepository.buscarPorId(id)).thenReturn(Optional.of(estrategia));
        when(estrategiaRepository.contarActivasPorTipsterId(tipsterId)).thenReturn(0L);

        useCase.cambiarEstado(id, true);

        verify(estrategiaRepository).guardar(argThat(e -> e.activa()));
    }
}
