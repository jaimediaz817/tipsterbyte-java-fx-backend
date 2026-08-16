// ─────────────────────────────────────────────
// [QUÉ]: Test del RolController: verifica el catálogo GET /api/v1/roles.
// [POR QUÉ]: Garantiza el contrato HTTP del catálogo de roles que consume el
//            frontend Angular (select de registro y filtros de menú).
// [RELACIONES]: RolController → enum domain.model.Rol → RolResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RolController()).build();
    }

    @Test
    void debe_listar_todos_los_roles_con_codigo_y_nombre() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("CLIENTE"))
                .andExpect(jsonPath("$[0].nombre").value("Cliente"))
                .andExpect(jsonPath("$[1].codigo").value("TIPSTER"))
                .andExpect(jsonPath("$[1].nombre").value("Tipster"))
                .andExpect(jsonPath("$[2].codigo").value("SUPERADMIN"))
                .andExpect(jsonPath("$[2].nombre").value("Super Administrador"));
    }
}