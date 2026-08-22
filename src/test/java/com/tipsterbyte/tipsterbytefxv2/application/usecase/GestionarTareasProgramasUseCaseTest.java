// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-15 (GestionarTareasProgramasUseCase): registrar con
//        unicidad por liga+tipo, listar por prioridad, eliminar y obtener por id.
// [POR QUÉ]: Verifica la orquestación del caso de uso sobre el puerto
//            TareaProgramadaRepository (valores por defecto de cron/activa, reglas de
//            unicidad y errores de dominio cuando no existe la tarea).
// [RELACIONES]: CU-15 → TareaProgramadaRepository + TareaProgramada (domain.model).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActualizarTareaProgramadaComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.FuenteDisponible;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarTareaProgramadaComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Frecuencia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.UnidadFrecuencia;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarTareasProgramasUseCaseTest {

    @Mock
    private TareaProgramadaRepository tareaProgramadaRepository;
    @Mock
    private TareaLogRepository tareaLogRepository;
    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private DetalleFuenteExtraccionRepository detalleRepository;

    private GestionarTareasProgramasUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new GestionarTareasProgramasUseCase(tareaProgramadaRepository, tareaLogRepository,
                ligaRepository, detalleRepository);
    }

    @Test
    void debe_registrar_tarea_con_cron_y_activa_por_defecto() {
        UUID ligaId = UUID.randomUUID();
        when(tareaProgramadaRepository.buscarPorLigaIdYTipoFuente(ligaId, TipoFuenteExtraccion.STANDINGS))
                .thenReturn(Optional.empty());

        casoDeUso.registrar(new RegistrarTareaProgramadaComando(
                ligaId, TipoFuenteExtraccion.STANDINGS, "1", null, null, null));

        ArgumentCaptor<TareaProgramada> captor = ArgumentCaptor.forClass(TareaProgramada.class);
        verify(tareaProgramadaRepository).guardar(captor.capture());
        assertEquals(ligaId, captor.getValue().ligaId());
        assertEquals(TipoFuenteExtraccion.STANDINGS, captor.getValue().tipoFuente());
        assertEquals("1", captor.getValue().prioridad());
        assertTrue(captor.getValue().activa());
        assertEquals("0 0 * * * *", captor.getValue().cronExpression());
    }

    @Test
    void debe_registrar_tarea_con_frecuencia_amigable_que_se_codifica_a_cron() {
        UUID ligaId = UUID.randomUUID();
        when(tareaProgramadaRepository.buscarPorLigaIdYTipoFuente(ligaId, TipoFuenteExtraccion.ODDS_WPLAY))
                .thenReturn(Optional.empty());

        casoDeUso.registrar(new RegistrarTareaProgramadaComando(
                ligaId, TipoFuenteExtraccion.ODDS_WPLAY, "1", null,
                new Frecuencia(6, UnidadFrecuencia.HORAS), true));

        ArgumentCaptor<TareaProgramada> captor = ArgumentCaptor.forClass(TareaProgramada.class);
        verify(tareaProgramadaRepository).guardar(captor.capture());
        assertEquals("0 0 */6 * * *", captor.getValue().cronExpression());
    }

    @Test
    void debe_registrar_cron_crudo_validado() {
        when(tareaProgramadaRepository.buscarGlobal()).thenReturn(Optional.empty());

        casoDeUso.registrar(new RegistrarTareaProgramadaComando(
                null, null, "0", "0 0 3 * * *", null, true));

        ArgumentCaptor<TareaProgramada> captor = ArgumentCaptor.forClass(TareaProgramada.class);
        verify(tareaProgramadaRepository).guardar(captor.capture());
        assertEquals("0 0 3 * * *", captor.getValue().cronExpression());
    }

    @Test
    void debe_rechazar_cron_crudo_invalido() {
        assertThrows(DomainException.class, () -> casoDeUso.registrar(
                new RegistrarTareaProgramadaComando(null, null, "0", "no-es-un-cron", null, true)));
        verify(tareaProgramadaRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_tarea_duplicada_para_la_misma_liga_y_tipo() {
        UUID ligaId = UUID.randomUUID();
        when(tareaProgramadaRepository.buscarPorLigaIdYTipoFuente(ligaId, TipoFuenteExtraccion.CALENDAR))
                .thenReturn(Optional.of(unaTarea(ligaId, TipoFuenteExtraccion.CALENDAR)));

        assertThrows(DomainException.class, () -> casoDeUso.registrar(
                new RegistrarTareaProgramadaComando(ligaId, TipoFuenteExtraccion.CALENDAR, "1", null, null, true)));
        verify(tareaProgramadaRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_duplicar_la_tarea_global_de_catalogo() {
        when(tareaProgramadaRepository.buscarGlobal()).thenReturn(Optional.of(unaTarea(null, null)));

        assertThrows(DomainException.class, () -> casoDeUso.registrar(
                new RegistrarTareaProgramadaComando(null, null, "0", null, null, true)));
        verify(tareaProgramadaRepository, never()).guardar(any());
    }

    @Test
    void debe_registrar_tarea_global_de_catalogo_con_liga_y_tipo_null() {
        when(tareaProgramadaRepository.buscarGlobal()).thenReturn(Optional.empty());

        casoDeUso.registrar(new RegistrarTareaProgramadaComando(null, null, "0", null, null, true));

        ArgumentCaptor<TareaProgramada> captor = ArgumentCaptor.forClass(TareaProgramada.class);
        verify(tareaProgramadaRepository).guardar(captor.capture());
        assertEquals(null, captor.getValue().ligaId());
        assertEquals(null, captor.getValue().tipoFuente());
    }

    @Test
    void debe_listar_tareas_por_prioridad_ascendente() {
        TareaProgramada primera = unaTarea(UUID.randomUUID(), TipoFuenteExtraccion.STANDINGS);
        TareaProgramada segunda = unaTarea(UUID.randomUUID(), TipoFuenteExtraccion.CALENDAR);
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(primera, segunda));

        List<TareaProgramada> lista = casoDeUso.listar();

        assertEquals(2, lista.size());
        assertEquals(primera.id(), lista.get(0).id());
    }

    @Test
    void debe_pausar_tarea_manteniendo_su_configuracion() {
        UUID id = UUID.randomUUID();
        TareaProgramada existente = unaTarea(id, null);
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.of(existente));

        TareaProgramada actualizada = casoDeUso.actualizar(id,
                new ActualizarTareaProgramadaComando(null, null, false, null));

        ArgumentCaptor<TareaProgramada> captor = ArgumentCaptor.forClass(TareaProgramada.class);
        verify(tareaProgramadaRepository).guardar(captor.capture());
        assertFalse(captor.getValue().activa());
        assertEquals(existente.cronExpression(), captor.getValue().cronExpression());
    }

    @Test
    void debe_reanudar_tarea_pausada() {
        UUID id = UUID.randomUUID();
        TareaProgramada existente = new TareaProgramada(id, null, null, "1",
                "0 0 * * * *", false, "2026-01-01T00:00:00Z");
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.of(existente));

        casoDeUso.actualizar(id, new ActualizarTareaProgramadaComando(null, null, true, null));

        ArgumentCaptor<TareaProgramada> captor = ArgumentCaptor.forClass(TareaProgramada.class);
        verify(tareaProgramadaRepository).guardar(captor.capture());
        assertTrue(captor.getValue().activa());
    }

    @Test
    void debe_cambiar_frecuencia_de_tarea_existente() {
        UUID id = UUID.randomUUID();
        TareaProgramada existente = unaTarea(id, null);
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.of(existente));

        casoDeUso.actualizar(id, new ActualizarTareaProgramadaComando(
                null, new Frecuencia(30, UnidadFrecuencia.MINUTOS), null, null));

        ArgumentCaptor<TareaProgramada> captor = ArgumentCaptor.forClass(TareaProgramada.class);
        verify(tareaProgramadaRepository).guardar(captor.capture());
        assertEquals("0 0/30 * * * *", captor.getValue().cronExpression());
    }

    @Test
    void debe_rechazar_actualizar_tarea_inexistente() {
        UUID id = UUID.randomUUID();
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> casoDeUso.actualizar(id, new ActualizarTareaProgramadaComando(null, null, false, null)));
    }

    @Test
    void debe_eliminar_tarea_existente() {
        UUID id = UUID.randomUUID();
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.of(unaTarea(id, null)));

        casoDeUso.eliminar(id);

        verify(tareaProgramadaRepository).eliminarPorId(id);
    }

    @Test
    void debe_rechazar_eliminar_tarea_inexistente() {
        UUID id = UUID.randomUUID();
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.eliminar(id));
        verify(tareaProgramadaRepository, never()).eliminarPorId(any());
    }

    @Test
    void debe_obtener_logs_de_tarea_existente() {
        UUID id = UUID.randomUUID();
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.of(unaTarea(id, null)));
        when(tareaLogRepository.buscarPorTareaProgramadaId(id)).thenReturn(List.of(
                new TareaLog(UUID.randomUUID(), id, "exec-1", java.time.Instant.now(),
                        "SUCCESS", 100L, null, "ok")));

        List<TareaLog> logs = casoDeUso.obtenerLogs(id);

        assertEquals(1, logs.size());
        assertEquals("SUCCESS", logs.get(0).status());
    }

    @Test
    void debe_rechazar_logs_de_tarea_inexistente() {
        UUID id = UUID.randomUUID();
        when(tareaProgramadaRepository.encontrarPorId(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.obtenerLogs(id));
    }

    @Test
    void debe_listar_fuentes_disponibles_con_catalogo_global_y_marcar_ya_programadas() {
        UUID ligaId = UUID.randomUUID();
        when(tareaProgramadaRepository.buscarGlobal()).thenReturn(Optional.empty());
        when(ligaRepository.buscarActivas()).thenReturn(List.of(
                Liga.reconstruir(ligaId, "Premier League", "Inglaterra", null, EstadoLiga.ACTIVA,
                        java.util.Set.of())));
        when(detalleRepository.buscarPorLiga(ligaId)).thenReturn(List.of(
                new DetalleFuenteExtraccion(ligaId, new FuenteExtraccion("Standings", TipoFuenteExtraccion.STANDINGS, true),
                        "/posiciones/", true)));
        when(tareaProgramadaRepository.buscarPorLigaIdYTipoFuente(ligaId, TipoFuenteExtraccion.STANDINGS))
                .thenReturn(Optional.empty());

        List<FuenteDisponible> disponibles = casoDeUso.listarFuentesDisponibles();

        assertEquals(2, disponibles.size());
        assertEquals("Catálogo global", disponibles.get(0).ligaNombre());
        assertFalse(disponibles.get(0).yaProgramada());
        assertEquals("Premier League", disponibles.get(1).ligaNombre());
        assertEquals(TipoFuenteExtraccion.STANDINGS, disponibles.get(1).tipoFuente());
    }

    @Test
    void debe_obtener_nombre_de_liga_para_la_vista() {
        UUID ligaId = UUID.randomUUID();
        when(ligaRepository.buscarPorId(ligaId)).thenReturn(Optional.of(
                Liga.reconstruir(ligaId, "Premier League", "Inglaterra", null, EstadoLiga.ACTIVA,
                        java.util.Set.of())));

        assertEquals("Premier League", casoDeUso.obtenerNombreLiga(ligaId));
        assertEquals(null, casoDeUso.obtenerNombreLiga(null));
    }

    @Test
    void debe_listar_ultimas_ejecuciones_de_todas_las_tareas() {
        when(tareaLogRepository.listarUltimas(5)).thenReturn(List.of(
                new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-2", java.time.Instant.now(),
                        "ERROR", 90L, "RuntimeException", "fallo"),
                new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-1", java.time.Instant.now(),
                        "SUCCESS", 150L, null, "ok")));

        List<TareaLog> logs = casoDeUso.obtenerUltimasEjecuciones(5);

        assertEquals(2, logs.size());
        assertEquals("ERROR", logs.get(0).status());
        assertEquals("RuntimeException", logs.get(0).errorCode());
    }

    @Test
    void debe_rechazar_limite_fuera_de_rango() {
        assertThrows(DomainException.class, () -> casoDeUso.obtenerUltimasEjecuciones(0));
        assertThrows(DomainException.class, () -> casoDeUso.obtenerUltimasEjecuciones(101));
        verify(tareaLogRepository, never()).listarUltimas(anyInt());
    }

    private TareaProgramada unaTarea(UUID id, TipoFuenteExtraccion tipoFuente) {
        return new TareaProgramada(id, UUID.randomUUID(), tipoFuente, "1",
                "0 0 * * * *", true, "2026-01-01T00:00:00Z");
    }
}