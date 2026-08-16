// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de PaisController (MockMvc standalone): cubre GET /api/v1/paises
//        con filtros opcionales (continente, mapeado), orden alfabético y mapeo a
//        PaisResponse.
// [POR QUÉ]: Valida el contrato HTTP del catálogo geográfico sin levantar Spring:
//            filtrado, orden y DTOs de salida (regla testing.md: controllers con
//            MockMvc standalone).
// [RELACIONES]: PaisController → PaisRepository (puerto CU-10) → PaisResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaisControllerTest {

    @Mock
    private PaisRepository paisRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaisController(paisRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_listar_paises_ordenados_alfabeticamente() throws Exception {
        Pais colombia = unPais("Colombia", "CO", "Sudamérica", false);
        Pais españa = unPais("España", "ES", "Europa", true);
        when(paisRepository.buscarTodos()).thenReturn(List.of(colombia, españa));

        mockMvc.perform(get("/api/v1/paises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Colombia"))
                .andExpect(jsonPath("$[1].nombre").value("España"))
                .andExpect(jsonPath("$[1].isoAlpha2").value("ES"))
                .andExpect(jsonPath("$[1].continente").value("Europa"))
                .andExpect(jsonPath("$[1].mapeado").value(true));
    }

    @Test
    void debe_filtrar_por_continente() throws Exception {
        Pais colombia = unPais("Colombia", "CO", "Sudamérica", false);
        Pais españa = unPais("España", "ES", "Europa", true);
        when(paisRepository.buscarTodos()).thenReturn(List.of(colombia, españa));

        mockMvc.perform(get("/api/v1/paises").param("continente", "Europa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("España"));
    }

    @Test
    void debe_filtrar_por_mapeado() throws Exception {
        Pais colombia = unPais("Colombia", "CO", "Sudamérica", false);
        Pais españa = unPais("España", "ES", "Europa", true);
        when(paisRepository.buscarTodos()).thenReturn(List.of(colombia, españa));

        mockMvc.perform(get("/api/v1/paises").param("mapeado", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("España"));
    }

    @Test
    void debe_devolver_lista_vacia_si_no_hay_paises() throws Exception {
        when(paisRepository.buscarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/paises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private Pais unPais(String nombre, String isoAlpha2, String continente, boolean mapeado) {
        return new Pais(UUID.randomUUID(), nombre, isoAlpha2, continente, "COD-" + isoAlpha2,
                "/teams/" + nombre.toLowerCase(), mapeado);
    }
}