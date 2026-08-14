package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RegistrarResultadoUseCaseTest {

    @Mock
    private PartidoRepository partidoRepository;

    private RegistrarResultadoUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new RegistrarResultadoUseCase(partidoRepository);
    }

    private Partido partidoEnVivo() {
        return new Partido(UUID.randomUUID(), new Equipo("Real Madrid"), new Equipo("FC Barcelona"),
                new FechaProgramada(LocalDateTime.of(2026, 3, 1, 20, 0)));
    }

    @Test
    void debe_finalizar_y_asignar_resultado_br003() {
        Partido partido = partidoEnVivo();
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        casoDeUso.ejecutar(partido.id(), new Resultado(2, 1));

        assertEquals(EstadoPartido.FINALIZADO, partido.estado());
        assertEquals(2, partido.resultado().golesLocal());
        assertEquals(1, partido.resultado().golesVisitante());
        verify(partidoRepository).guardar(partido);
    }

    @Test
    void debe_rechazar_modificar_resultado_cuando_partido_ya_finalizado_br003() {
        Partido partido = partidoEnVivo();
        partido.finalizar();
        partido.asignarResultado(new Resultado(1, 0));
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(partido.id(), new Resultado(3, 2)));

        assertEquals(EstadoPartido.FINALIZADO, partido.estado());
        assertEquals(1, partido.resultado().golesLocal());
        verify(partidoRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_si_partido_no_existe() {
        UUID id = UUID.randomUUID();
        when(partidoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(id, new Resultado(1, 0)));
        verify(partidoRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_resultado_nulo() {
        Partido partido = partidoEnVivo();
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(partido.id(), null));
        verify(partidoRepository, never()).guardar(any());
    }
}