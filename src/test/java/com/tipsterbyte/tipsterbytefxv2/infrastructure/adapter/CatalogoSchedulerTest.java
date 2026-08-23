// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CatalogoScheduler (FASE 12.6): dispatch al caso de uso según
//        tipoFuente, evaluación cron, tareas inactivas y persistencia de TareaLog
//        (éxito y error) con ejecución asíncrona en hilos virtuales.
// [POR QUÉ]: Verifica el comportamiento del dispatcher sin levantar Spring ni
//            contenedores: se construye con mocks y se espera la ejecución asíncrona
//            con Mockito timeout.
// [RELACIONES]: CatalogoScheduler → TareaProgramadaRepository + TareaLogRepository +
//               CU-01/02/03/10 (use cases de sincronización) + MDCTaskContext.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarEquiposLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoSchedulerTest {

    @Mock
    private TareaProgramadaRepository tareaProgramadaRepository;
    @Mock
    private TareaLogRepository tareaLogRepository;
    @Mock
    private SincronizarPosicionesUseCase sincronizarPosicionesUseCase;
    @Mock
    private SincronizarCalendarioUseCase sincronizarCalendarioUseCase;
    @Mock
    private SincronizarCuotasUseCase sincronizarCuotasUseCase;
    @Mock
    private SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;
    @Mock
    private SincronizarCatalogoUseCase sincronizarCatalogoUseCase;

    private CatalogoScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CatalogoScheduler(tareaProgramadaRepository, tareaLogRepository,
                sincronizarPosicionesUseCase, sincronizarCalendarioUseCase,
                sincronizarCuotasUseCase, sincronizarEquiposLigaUseCase,
                sincronizarCatalogoUseCase);
    }

    @Test
    void debe_ejecutar_tarea_global_de_catalogo_cuando_cron_coincide_y_persistir_log_exitoso() {
        TareaProgramada tarea = unaTarea(null, null, true, "* * * * * *");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        scheduler.ejecutarTareasProgramadas();

        verify(sincronizarCatalogoUseCase, timeout(5000).times(1)).ejecutar();
        ArgumentCaptor<TareaLog> captor = ArgumentCaptor.forClass(TareaLog.class);
        verify(tareaLogRepository, timeout(5000).times(1)).guardar(captor.capture());
        assertEquals("SUCCESS", captor.getValue().status());
        assertEquals(tarea.id(), captor.getValue().tareaProgramadaId());
    }

    @Test
    void debe_despachar_posiciones_al_caso_de_uso_con_liga() {
        UUID ligaId = UUID.randomUUID();
        TareaProgramada tarea = unaTarea(ligaId, TipoFuenteExtraccion.STANDINGS, true, "* * * * * *");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        scheduler.ejecutarTareasProgramadas();

        verify(sincronizarPosicionesUseCase, timeout(5000).times(1)).ejecutar(ligaId);
    }

    @Test
    void debe_despachar_calendario_y_cuotas_por_tipo_de_fuente() {
        UUID ligaCalendario = UUID.randomUUID();
        UUID ligaCuotas = UUID.randomUUID();
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(
                unaTarea(ligaCalendario, TipoFuenteExtraccion.CALENDAR, true, "* * * * * *"),
                unaTarea(ligaCuotas, TipoFuenteExtraccion.ODDS_WPLAY, true, "* * * * * *")));

        scheduler.ejecutarTareasProgramadas();

        verify(sincronizarCalendarioUseCase, timeout(5000).times(1)).ejecutar(ligaCalendario);
        verify(sincronizarCuotasUseCase, timeout(5000).times(1)).ejecutar(ligaCuotas);
    }

    @Test
    void debe_despachar_equipos_al_caso_de_uso_con_liga() {
        UUID ligaId = UUID.randomUUID();
        TareaProgramada tarea = unaTarea(ligaId, TipoFuenteExtraccion.EQUIPOS, true, "* * * * * *");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        scheduler.ejecutarTareasProgramadas();

        verify(sincronizarEquiposLigaUseCase, timeout(5000).times(1)).ejecutar(ligaId);
    }

    @Test
    void debe_ignorar_tarea_con_cron_en_blanco() {
        TareaProgramada tarea = unaTarea(null, null, true, "");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        scheduler.ejecutarTareasProgramadas();

        verify(sincronizarCatalogoUseCase, never()).ejecutar();
        verify(tareaLogRepository, never()).guardar(any());
    }

    @Test
    void debe_ignorar_tarea_inactiva() {
        TareaProgramada tarea = unaTarea(null, null, false, "* * * * * *");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        scheduler.ejecutarTareasProgramadas();

        verify(sincronizarCatalogoUseCase, never()).ejecutar();
        verify(tareaLogRepository, never()).guardar(any());
    }

    @Test
    void debe_persistir_log_de_error_cuando_el_caso_de_uso_falla() {
        TareaProgramada tarea = unaTarea(null, null, true, "* * * * * *");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));
        when(sincronizarCatalogoUseCase.ejecutar()).thenThrow(new RuntimeException("boom"));

        scheduler.ejecutarTareasProgramadas();

        ArgumentCaptor<TareaLog> captor = ArgumentCaptor.forClass(TareaLog.class);
        verify(tareaLogRepository, timeout(5000).times(1)).guardar(captor.capture());
        assertEquals("ERROR", captor.getValue().status());
        assertEquals("RuntimeException", captor.getValue().errorCode());
    }

    @Test
    void debe_permitir_nueva_ejecucion_de_la_tarea_tras_terminar() {
        TareaProgramada tarea = unaTarea(null, null, true, "* * * * * *");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        scheduler.ejecutarTareasProgramadas();
        verify(sincronizarCatalogoUseCase, timeout(5000).times(1)).ejecutar();

        // El segundo tick debe volver a despachar (anti-solapamiento solo durante la ejecución).
        scheduler.ejecutarTareasProgramadas();
        verify(sincronizarCatalogoUseCase, timeout(5000).times(2)).ejecutar();
    }

    @Test
    void debe_lanzar_excepcion_si_tipo_fuente_requiere_liga_sin_liga() {
        TareaProgramada tarea = unaTarea(null, TipoFuenteExtraccion.STANDINGS, true, "* * * * * *");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        scheduler.ejecutarTareasProgramadas();

        ArgumentCaptor<TareaLog> captor = ArgumentCaptor.forClass(TareaLog.class);
        verify(tareaLogRepository, timeout(5000).times(1)).guardar(captor.capture());
        assertEquals("ERROR", captor.getValue().status());
        assertTrue(captor.getValue().mensaje().contains("requiere ligaId"));
    }

    @Test
    void debe_reportar_tarea_en_ejecucion_mientras_corre_y_limpiarla_al_terminar() throws InterruptedException {
        UUID tareaId = UUID.randomUUID();
        TareaProgramada tarea = new TareaProgramada(tareaId, null, null, "1",
                "* * * * * *", true, "2026-01-01T00:00:00Z");
        when(tareaProgramadaRepository.listarPorPrioridadAsc()).thenReturn(List.of(tarea));

        CountDownLatch iniciado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        when(sincronizarCatalogoUseCase.ejecutar()).thenAnswer(inv -> {
            iniciado.countDown();
            liberar.await(5, TimeUnit.SECONDS);
            return null;
        });

        scheduler.ejecutarTareasProgramadas();

        assertTrue(iniciado.await(5, TimeUnit.SECONDS), "la tarea debería haber arrancado");
        assertTrue(scheduler.tareasEnEjecucion().contains(tareaId),
                "el estado de ejecución debe incluir la tarea en curso");

        liberar.countDown();
        verify(tareaLogRepository, timeout(5000).times(1)).guardar(any(TareaLog.class));
        assertTrue(scheduler.tareasEnEjecucion().isEmpty(),
                "al terminar, la tarea deja de estar en ejecución");
    }

    private TareaProgramada unaTarea(UUID ligaId, TipoFuenteExtraccion tipoFuente,
                                     boolean activa, String cron) {
        return new TareaProgramada(UUID.randomUUID(), ligaId, tipoFuente, "1",
                cron, activa, "2026-01-01T00:00:00Z");
    }
}