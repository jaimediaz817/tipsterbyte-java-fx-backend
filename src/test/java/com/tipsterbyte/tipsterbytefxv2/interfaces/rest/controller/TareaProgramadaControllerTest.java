// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de TareaProgramadaController (MockMvc standalone): CU-15 con
//        POST (cron/frecuencia amigable), PUT (pausar/reanudar/editar), DELETE, listado
//        con próxima ejecución, estado de ejecución, historial de logs y fuentes
//        disponibles, más errores 400/422.
// [POR QUÉ]: Valida el contrato HTTP del panel "Automatización → Tareas programadas"
//            sin levantar Spring: status codes, parseo de request y DomainException.
// [RELACIONES]: TareaProgramadaController → GestionarTareasProgramasUseCase (CU-15)
//               + EstadoEjecucionTareas (scheduler).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.FuenteDisponible;
import com.tipsterbyte.tipsterbytefxv2.application.port.EstadoEjecucionTareas;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarTareasProgramasUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TareaProgramadaControllerTest {

    @Mock
    private GestionarTareasProgramasUseCase gestionarTareasProgramasUseCase;
    @Mock
    private ObjectProvider<EstadoEjecucionTareas> estadoEjecucionTareas;
    @Mock
    private EstadoEjecucionTareas estadoEjecucion;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TareaProgramadaController(gestionarTareasProgramasUseCase, estadoEjecucionTareas))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_crear_tarea_con_frecuencia_amigable() throws Exception {
        when(gestionarTareasProgramasUseCase.registrar(any())).thenReturn(unaTarea());

        mockMvc.perform(post("/api/v1/tareas-programadas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ligaId": null, "tipoFuente": null, "prioridad": "1",
                                 "frecuencia": {"valor": 6, "unidad": "HORAS"}, "activa": true}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cronExpression").value("0 0 */6 * * *"));
    }

    @Test
    void debe_pausar_tarea_con_put_activa_false() throws Exception {
        when(gestionarTareasProgramasUseCase.actualizar(any(), any()))
                .thenReturn(new TareaProgramada(UUID.randomUUID(), null, null, "1",
                        "0 0 * * * *", false, "2026-01-01T00:00:00Z"));

        mockMvc.perform(put("/api/v1/tareas-programadas/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activa": false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));
    }

    @Test
    void debe_devolver_422_cuando_el_cron_es_invalido() throws Exception {
        doThrow(new DomainException("Expresión cron inválida: no-es-un-cron"))
                .when(gestionarTareasProgramasUseCase).registrar(any());

        mockMvc.perform(post("/api/v1/tareas-programadas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cron": "no-es-un-cron"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void debe_listar_tareas_con_proxima_ejecucion_derivada() throws Exception {
        TareaProgramada tarea = new TareaProgramada(UUID.randomUUID(), null, null, "1",
                "0 0 3 * * *", true, "2026-01-01T00:00:00Z");
        when(gestionarTareasProgramasUseCase.listar()).thenReturn(List.of(tarea));

        mockMvc.perform(get("/api/v1/tareas-programadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cronExpression").value("0 0 3 * * *"))
                .andExpect(jsonPath("$[0].nextExecution").isNotEmpty());
    }

    @Test
    void debe_reportar_ids_de_tareas_en_ejecucion() throws Exception {
        when(estadoEjecucionTareas.getIfAvailable()).thenReturn(estadoEjecucion);
        when(estadoEjecucion.tareasEnEjecucion()).thenReturn(Set.of(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/tareas-programadas/ejecucion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isNotEmpty());
    }

    @Test
    void debe_listar_fuentes_disponibles() throws Exception {
        when(gestionarTareasProgramasUseCase.listarFuentesDisponibles()).thenReturn(List.of(
                new FuenteDisponible(null, "Catálogo global", null, false)));

        mockMvc.perform(get("/api/v1/tareas-programadas/disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ligaNombre").value("Catálogo global"));
    }

    @Test
    void debe_listar_logs_de_una_tarea() throws Exception {
        when(gestionarTareasProgramasUseCase.obtenerLogs(any())).thenReturn(List.of(
                new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-1", Instant.now(),
                        "ERROR", 50L, "RuntimeException", "fallo")));

        mockMvc.perform(get("/api/v1/tareas-programadas/{id}/logs", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ERROR"))
                .andExpect(jsonPath("$[0].errorCode").value("RuntimeException"));
    }

    @Test
    void debe_listar_ultimas_ejecuciones_globales_con_limite() throws Exception {
        when(gestionarTareasProgramasUseCase.obtenerUltimasEjecuciones(3)).thenReturn(List.of(
                new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-2", Instant.now(),
                        "ERROR", 90L, "RuntimeException", "fallo"),
                new TareaLog(UUID.randomUUID(), UUID.randomUUID(), "exec-1", Instant.now(),
                        "SUCCESS", 150L, null, "ok")));

        mockMvc.perform(get("/api/v1/tareas-programadas/logs").param("limite", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].executionId").value("exec-2"))
                .andExpect(jsonPath("$[0].errorCode").value("RuntimeException"))
                .andExpect(jsonPath("$[1].status").value("SUCCESS"));
    }

    @Test
    void debe_eliminar_tarea() throws Exception {
        mockMvc.perform(delete("/api/v1/tareas-programadas/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(gestionarTareasProgramasUseCase).eliminar(any());
    }

    private TareaProgramada unaTarea() {
        return new TareaProgramada(UUID.randomUUID(), null, null, "1",
                "0 0 */6 * * *", true, "2026-01-01T00:00:00Z");
    }
}