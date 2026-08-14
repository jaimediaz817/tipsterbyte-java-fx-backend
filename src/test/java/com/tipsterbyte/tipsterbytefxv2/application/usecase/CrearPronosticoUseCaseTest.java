package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CrearPronosticoComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearPronosticoUseCaseTest {

    @Mock
    private PronosticoRepository pronosticoRepository;
    @Mock
    private PartidoRepository partidoRepository;

    private CrearPronosticoUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new CrearPronosticoUseCase(pronosticoRepository, partidoRepository);
    }

    private Partido partidoProgramado() {
        return new Partido(UUID.randomUUID(), new Equipo("Real Madrid"), new Equipo("FC Barcelona"),
                new FechaProgramada(LocalDateTime.of(2026, 3, 1, 20, 0)));
    }

    @Test
    void debe_crear_pronostico_en_borrador_br004_br007() {
        Partido partido = partidoProgramado();
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        UUID id = casoDeUso.ejecutar(new CrearPronosticoComando(
                UUID.randomUUID(), partido.id(), Mercado.UNO_X_DOS, "1", new BigDecimal("1.85")));

        ArgumentCaptor<Pronostico> captor = ArgumentCaptor.forClass(Pronostico.class);
        verify(pronosticoRepository).guardar(captor.capture());
        Pronostico creado = captor.getValue();
        assertEquals(id, creado.id());
        assertEquals(Mercado.UNO_X_DOS, creado.seleccion().mercado());
        assertEquals("1", creado.seleccion().resultadoEsperado());
        assertEquals(new BigDecimal("1.85"), creado.cuota().valor());
        assertEquals(com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPronostico.BORRADOR, creado.estado());
    }

    @Test
    void debe_rechazar_cuota_menor_o_igual_a_1_br007() {
        Partido partido = partidoProgramado();
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(new CrearPronosticoComando(
                UUID.randomUUID(), partido.id(), Mercado.UNO_X_DOS, "1", BigDecimal.ONE)));
        verify(pronosticoRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_partido_finalizado_br004() {
        Partido partido = partidoProgramado();
        partido.finalizar();
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(new CrearPronosticoComando(
                UUID.randomUUID(), partido.id(), Mercado.UNO_X_DOS, "1", new BigDecimal("1.85"))));
        verify(pronosticoRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_seleccion_incoherente_con_mercado() {
        Partido partido = partidoProgramado();
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(new CrearPronosticoComando(
                UUID.randomUUID(), partido.id(), Mercado.OVER_UNDER, "1", new BigDecimal("1.85"))));
        verify(pronosticoRepository, never()).guardar(any());
    }
}