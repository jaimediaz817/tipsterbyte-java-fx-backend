// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CatalogoController (MockMvc standalone): cubre el contrato
//        ASÍNCRONO de FASE T3 — POST /catalogo/activar (202 + executionId), GET
//        /catalogo/activar/{executionId} (polling RUNNING/SUCCESS/404) y 409 si ya hay
//        una ejecución en curso.
// [POR QUÉ]: Valida el contrato HTTP del panel del SUPERADMIN sin levantar Spring:
//            lanzamiento en background, anti-solapamiento y progreso por país.
// [RELACIONES]: CatalogoController → SincronizarCatalogoAsyncUseCase +
//               TareaLogRepository + ProgresoPoblamiento.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CatalogoEstadoDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProgresoPoblamiento;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarEstadoCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoAsyncUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarLigasPorPaisAsyncUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPaisesUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.PoblamientoEnCursoException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoCatalogo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    private static final String EXECUTION_ID = "exec-123";

    @Mock
    private SincronizarCatalogoAsyncUseCase sincronizarCatalogoAsyncUseCase;

    @Mock
    private ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase;

    @Mock
    private TareaLogRepository tareaLogRepository;

    @Mock
    private ProgresoPoblamiento progresoPoblamiento;

    @Mock
    private SincronizarPaisesUseCase sincronizarPaisesUseCase;

    @Mock
    private SincronizarLigasPorPaisAsyncUseCase sincronizarLigasPorPaisAsyncUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogoController(
                        sincronizarCatalogoAsyncUseCase, consultarEstadoCatalogoUseCase,
                        tareaLogRepository, progresoPoblamiento,
                        sincronizarPaisesUseCase, sincronizarLigasPorPaisAsyncUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_lanzar_poblamiento_y_devolver_202_con_execution_id() throws Exception {
        when(sincronizarCatalogoAsyncUseCase.ejecutarAsync()).thenReturn(EXECUTION_ID);

        mockMvc.perform(post("/api/v1/catalogo/activar"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").value(EXECUTION_ID))
                .andExpect(jsonPath("$.estado").value("RUNNING"))
                .andExpect(jsonPath("$.urlEstado").value("/api/v1/catalogo/activar/" + EXECUTION_ID));
    }

    @Test
    void debe_devolver_409_si_ya_hay_un_poblamiento_en_curso() throws Exception {
        when(sincronizarCatalogoAsyncUseCase.ejecutarAsync())
                .thenThrow(new PoblamientoEnCursoException("Ya hay un poblamiento geográfico en curso"));

        mockMvc.perform(post("/api/v1/catalogo/activar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void debe_exponer_progreso_running_pais_en_curso() throws Exception {
        when(tareaLogRepository.buscarPorExecutionId(EXECUTION_ID)).thenReturn(List.of(
                new TareaLog(null, null, EXECUTION_ID, Instant.parse("2026-08-22T05:00:00Z"),
                        "RUNNING", null, null, "Poblamiento geográfico manual en curso")));
        when(progresoPoblamiento.snapshot()).thenReturn(Optional.of(
                new ProgresoPoblamiento.Progreso("Colombia", 12)));

        mockMvc.perform(get("/api/v1/catalogo/activar/{executionId}", EXECUTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(EXECUTION_ID))
                .andExpect(jsonPath("$.estado").value("RUNNING"))
                .andExpect(jsonPath("$.paisActual").value("Colombia"))
                .andExpect(jsonPath("$.paisesProcesados").value(12));
    }

    @Test
    void debe_exponer_estado_success_con_duracion_sin_pais_actual() throws Exception {
        when(tareaLogRepository.buscarPorExecutionId(EXECUTION_ID)).thenReturn(List.of(
                new TareaLog(null, null, EXECUTION_ID, Instant.parse("2026-08-22T05:00:00Z"),
                        "SUCCESS", 240_000L, null, "Poblamiento geográfico manual completado")));

        mockMvc.perform(get("/api/v1/catalogo/activar/{executionId}", EXECUTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUCCESS"))
                .andExpect(jsonPath("$.duracionMs").value(240_000))
                .andExpect(jsonPath("$.paisActual").doesNotExist());
    }

    @Test
    void debe_devolver_404_para_execution_id_desconocido() throws Exception {
        when(tareaLogRepository.buscarPorExecutionId(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/catalogo/activar/{executionId}", "no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debe_devolver_estado_vacio_cuando_no_hay_datos() throws Exception {
        when(consultarEstadoCatalogoUseCase.ejecutar())
                .thenReturn(new CatalogoEstadoDto(EstadoCatalogo.VACIO, 0, 0));

        mockMvc.perform(get("/api/v1/catalogo/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VACIO"))
                .andExpect(jsonPath("$.totalPaises").value(0))
                .andExpect(jsonPath("$.totalLigas").value(0));
    }

    // HU-12 granular: poblar-paises (200 sync)
    @Test
    void debe_poblar_paises_con_200() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/poblar-paises"))
                .andExpect(status().isOk());
    }

    @Test
    void debe_poblar_ligas_por_pais_con_202() throws Exception {
        when(sincronizarLigasPorPaisAsyncUseCase.ejecutarAsync("CO")).thenReturn(EXECUTION_ID);

        mockMvc.perform(post("/api/v1/catalogo/poblar-ligas/{isoAlpha2}", "CO"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").value(EXECUTION_ID))
                .andExpect(jsonPath("$.estado").value("RUNNING"));
    }

    @Test
    void debe_devolver_409_si_poblar_ligas_ya_en_curso_para_ese_iso() throws Exception {
        when(sincronizarLigasPorPaisAsyncUseCase.ejecutarAsync("CO"))
                .thenThrow(new PoblamientoEnCursoException("Ya hay un poblamiento en curso para el país CO"));

        mockMvc.perform(post("/api/v1/catalogo/poblar-ligas/{isoAlpha2}", "CO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
