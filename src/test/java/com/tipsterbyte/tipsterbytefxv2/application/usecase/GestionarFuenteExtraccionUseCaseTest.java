package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AsociarUrlFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarFuenteExtraccionUseCaseTest {

    @Mock
    private FuenteExtraccionRepository fuenteRepository;
    @Mock
    private DetalleFuenteExtraccionRepository detalleRepository;

    private GestionarFuenteExtraccionUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new GestionarFuenteExtraccionUseCase(fuenteRepository, detalleRepository);
    }

    @Test
    void debe_registrar_fuente_nueva() {
        casoDeUso.registrarFuente(new RegistrarFuenteComando("Posiciones Flashscore", TipoFuenteExtraccion.STANDINGS, true));

        ArgumentCaptor<FuenteExtraccion> captor = ArgumentCaptor.forClass(FuenteExtraccion.class);
        verify(fuenteRepository).guardar(captor.capture());
        assertEquals(TipoFuenteExtraccion.STANDINGS, captor.getValue().tipo());
    }

    @Test
    void debe_rechazar_tipo_ya_registrado() {
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.STANDINGS))
                .thenReturn(Optional.of(new FuenteExtraccion("Existente", TipoFuenteExtraccion.STANDINGS, true)));

        assertThrows(DomainException.class, () -> casoDeUso.registrarFuente(
                new RegistrarFuenteComando("Duplicada", TipoFuenteExtraccion.STANDINGS, true)));
        verify(fuenteRepository, never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debe_listar_fuentes_del_catalogo() {
        when(fuenteRepository.buscarTodas()).thenReturn(List.of(
                new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true),
                new FuenteExtraccion("Calendario", TipoFuenteExtraccion.CALENDAR, true)));

        List<FuenteExtraccion> fuentes = casoDeUso.listarFuentes();

        assertEquals(2, fuentes.size());
    }

    @Test
    void debe_asociar_url_de_fuente_a_liga() {
        UUID ligaId = UUID.randomUUID();
        FuenteExtraccion fuente = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.ODDS_WPLAY)).thenReturn(Optional.of(fuente));

        casoDeUso.asociarUrlFuente(new AsociarUrlFuenteComando(ligaId, TipoFuenteExtraccion.ODDS_WPLAY, "https://wplay.co/ligas", true));

        ArgumentCaptor<DetalleFuenteExtraccion> captor = ArgumentCaptor.forClass(DetalleFuenteExtraccion.class);
        verify(detalleRepository).guardar(captor.capture());
        assertEquals(ligaId, captor.getValue().ligaId());
        assertEquals("https://wplay.co/ligas", captor.getValue().url());
    }

    @Test
    void debe_actualizar_url_existente_en_lugar_de_duplicar() {
        UUID ligaId = UUID.randomUUID();
        FuenteExtraccion fuente = new FuenteExtraccion("Cuotas Wplay", TipoFuenteExtraccion.ODDS_WPLAY, true);
        DetalleFuenteExtraccion existente = new DetalleFuenteExtraccion(
                UUID.randomUUID(), ligaId, fuente, "https://wplay.co/vieja", true);
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.ODDS_WPLAY)).thenReturn(Optional.of(fuente));
        when(detalleRepository.buscarPorLigaYTipo(ligaId, TipoFuenteExtraccion.ODDS_WPLAY))
                .thenReturn(Optional.of(existente));

        casoDeUso.asociarUrlFuente(new AsociarUrlFuenteComando(ligaId, TipoFuenteExtraccion.ODDS_WPLAY, "https://wplay.co/nueva", true));

        ArgumentCaptor<DetalleFuenteExtraccion> captor = ArgumentCaptor.forClass(DetalleFuenteExtraccion.class);
        verify(detalleRepository).guardar(captor.capture());
        assertEquals(existente.id(), captor.getValue().id());
        assertEquals("https://wplay.co/nueva", captor.getValue().url());
    }

    @Test
    void debe_rechazar_asociacion_si_no_existe_fuente_del_tipo() {
        UUID ligaId = UUID.randomUUID();
        when(fuenteRepository.buscarPorTipo(TipoFuenteExtraccion.CALENDAR)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.asociarUrlFuente(
                new AsociarUrlFuenteComando(ligaId, TipoFuenteExtraccion.CALENDAR, "https://soccerway.com/cal", true)));
    }

    @Test
    void debe_listar_detalles_de_liga() {
        UUID ligaId = UUID.randomUUID();
        when(detalleRepository.buscarPorLiga(ligaId)).thenReturn(List.of(
                new DetalleFuenteExtraccion(ligaId,
                        new FuenteExtraccion("Posiciones", TipoFuenteExtraccion.STANDINGS, true),
                        "https://flashscore.com/tabla", true)));

        List<DetalleFuenteExtraccion> detalles = casoDeUso.listarDetallesDeLiga(ligaId);

        assertEquals(1, detalles.size());
    }
}
