package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.SeleccionPronostico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicarPronosticoUseCaseTest {

    @Mock
    private PronosticoRepository pronosticoRepository;
    @Mock
    private PartidoRepository partidoRepository;

    private PublicarPronosticoUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new PublicarPronosticoUseCase(pronosticoRepository, partidoRepository);
    }

    private Partido partidoProgramadoConCuota() {
        Partido partido = new Partido(UUID.randomUUID(), new Equipo("Real Madrid"), new Equipo("FC Barcelona"),
                new FechaProgramada(LocalDateTime.of(2026, 3, 1, 20, 0)));
        partido.actualizarCuotas(List.of(new Cuota(new BigDecimal("1.85"))));
        return partido;
    }

    private Pronostico pronosticoBorrador(Partido partido) {
        return new Pronostico(UUID.randomUUID(), partido.id(),
                new SeleccionPronostico(Mercado.UNO_X_DOS, "1"), new Cuota(new BigDecimal("1.85")));
    }

    @Test
    void debe_publicar_pronostico_y_emitir_evento_br004_br005() {
        Partido partido = partidoProgramadoConCuota();
        Pronostico pronostico = pronosticoBorrador(partido);
        when(pronosticoRepository.buscarPorId(pronostico.id())).thenReturn(Optional.of(pronostico));
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        List<DomainEvent> eventos = casoDeUso.ejecutar(pronostico.id());

        assertEquals(EstadoPronostico.PUBLICADO, pronostico.estado());
        verify(pronosticoRepository).guardar(pronostico);
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("PronosticoPublicado")));
    }

    @Test
    void debe_rechazar_publicacion_si_partido_finalizado_br004() {
        Partido partido = partidoProgramadoConCuota();
        partido.finalizar();
        assertEquals(EstadoPartido.FINALIZADO, partido.estado());
        Pronostico pronostico = pronosticoBorrador(partido);
        when(pronosticoRepository.buscarPorId(pronostico.id())).thenReturn(Optional.of(pronostico));
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(pronostico.id()));
        verify(pronosticoRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_publicacion_si_cuota_no_vigente_br004() {
        Partido partido = partidoProgramadoConCuota();
        Pronostico pronostico = new Pronostico(UUID.randomUUID(), partido.id(),
                new SeleccionPronostico(Mercado.UNO_X_DOS, "1"), new Cuota(new BigDecimal("2.50")));
        when(pronosticoRepository.buscarPorId(pronostico.id())).thenReturn(Optional.of(pronostico));
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(pronostico.id()));
        verify(pronosticoRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_publicacion_si_ya_publicado_br005() {
        Partido partido = partidoProgramadoConCuota();
        Pronostico pronostico = pronosticoBorrador(partido);
        pronostico.publicar(true, true);
        when(pronosticoRepository.buscarPorId(pronostico.id())).thenReturn(Optional.of(pronostico));
        when(partidoRepository.buscarPorId(partido.id())).thenReturn(Optional.of(partido));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(pronostico.id()));
        verify(pronosticoRepository, never()).guardar(any());
    }
}