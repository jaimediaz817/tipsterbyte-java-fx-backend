// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CatalogoController (MockMvc standalone): cubre POST
//        /api/v1/catalogo/activar y GET /api/v1/catalogo/estado (CU-10).
// [POR QUÉ]: Valida el contrato HTTP del panel del SUPERADMIN sin levantar Spring:
//            estado derivado, conteos y el 503 cuando la fuente externa falla.
// [RELACIONES]: CatalogoController → SincronizarCatalogoUseCase +
//               ConsultarEstadoCatalogoUseCase → CatalogoEstadoResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CatalogoEstadoDto;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarEstadoCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoCatalogo;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    @Mock
    private SincronizarCatalogoUseCase sincronizarCatalogoUseCase;

    @Mock
    private ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CatalogoController(sincronizarCatalogoUseCase, consultarEstadoCatalogoUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_activar_catalogo_y_devolver_estado_poblado() throws Exception {
        when(consultarEstadoCatalogoUseCase.ejecutar())
                .thenReturn(new CatalogoEstadoDto(EstadoCatalogo.POBLADO, 176, 620));

        mockMvc.perform(post("/api/v1/catalogo/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("POBLADO"))
                .andExpect(jsonPath("$.totalPaises").value(176))
                .andExpect(jsonPath("$.totalLigas").value(620));

        verify(sincronizarCatalogoUseCase).ejecutar();
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

    @Test
    void debe_devolver_503_cuando_la_fuente_externa_falla() throws Exception {
        doThrow(new InfraestructureException("Fuente de países no disponible"))
                .when(sincronizarCatalogoUseCase).ejecutar();

        mockMvc.perform(post("/api/v1/catalogo/activar"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}