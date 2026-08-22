// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de SincronizarCatalogoAsyncUseCase (FASE T3 / H-02).
// [POR QUÉ]: Verifica el contrato asíncrono: devuelve executionId inmediato, persiste
//            TareaLog RUNNING al lanzar y SUCCESS/ERROR con duración al terminar,
//            anti-solapamiento (segunda llamada → PoblamientoEnCursoException) y
//            liberación del flag al terminar.
// [RELACIONES]: Delega en SincronizarCatalogoUseCase; persiste vía TareaLogRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.ProgresoPoblamiento;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.PoblamientoEnCursoException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SincronizarCatalogoAsyncUseCaseTest {

    @Mock
    private SincronizarCatalogoUseCase delegado;

    @Mock
    private TareaLogRepository tareaLogRepository;

    @Mock
    private ProgresoPoblamiento progreso;

    private SincronizarCatalogoAsyncUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        when(delegado.ejecutar()).thenReturn(List.of());
        casoDeUso = new SincronizarCatalogoAsyncUseCase(delegado, tareaLogRepository, progreso);
    }

    @Test
    void debe_devolver_execution_id_inmediato_y_registrar_running_luego_success() {
        String executionId = casoDeUso.ejecutarAsync();

        assertNotNull(executionId);
        // RUNNING se persiste de inmediato (antes de que termine el background).
        // Puede haberse registrado también el SUCCESS si el hilo virtual ya terminó.
        ArgumentCaptor<com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog> captor =
                ArgumentCaptor.forClass(com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog.class);
        verify(tareaLogRepository, atLeastOnce()).guardar(captor.capture());
        assertEquals("RUNNING", captor.getAllValues().get(0).status());
        assertEquals(executionId, captor.getAllValues().get(0).executionId());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().tareaProgramadaId(), "la vía manual no tiene tarea programada");

        // El SUCCESS llega cuando termina el hilo virtual.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(tareaLogRepository, atLeastOnce()).guardar(
                        org.mockito.ArgumentMatchers.argThat(log ->
                                "SUCCESS".equals(log.status())
                                        && executionId.equals(log.executionId())
                                        && log.durationMs() != null)));
    }

    @Test
    void debe_registrar_error_cuando_el_poblamiento_falla_y_no_bloquear_siguientes() {
        doAnswer(invocation -> {
            throw new RuntimeException("scraper caído");
        }).when(delegado).ejecutar();

        String executionId = casoDeUso.ejecutarAsync();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(tareaLogRepository, atLeastOnce()).guardar(
                        org.mockito.ArgumentMatchers.argThat(log ->
                                "ERROR".equals(log.status())
                                        && "RuntimeException".equals(log.errorCode()))));

        // El flag se libera: una nueva ejecución ya no es rechazada.
        await().atMost(5, TimeUnit.SECONDS).until(() -> !casoDeUso.estaEnCurso());
        String segunda = casoDeUso.ejecutarAsync();
        assertNotEquals(executionId, segunda, "tras un fallo se puede lanzar otra ejecución");
    }

    @Test
    void debe_rechazar_segundo_poblamiento_mientras_corre_uno() throws Exception {
        // Bloquea la ejecución del delegado hasta liberar la espera: simula poblamiento largo.
        java.util.concurrent.CountDownLatch bloqueo = new java.util.concurrent.CountDownLatch(1);
        doAnswer(invocation -> {
            bloqueo.await(5, TimeUnit.SECONDS);
            return List.<com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent>of();
        }).when(delegado).ejecutar();

        casoDeUso.ejecutarAsync();

        assertThrows(PoblamientoEnCursoException.class, () -> casoDeUso.ejecutarAsync());

        bloqueo.countDown();
        await().atMost(5, TimeUnit.SECONDS).until(() -> !casoDeUso.estaEnCurso());
    }

}
