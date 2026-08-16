// ─────────────────────────────────────────────
// [QUÉ]: Test de integración CORS: verifica que el preflight OPTIONS responde 200
//        con los headers CORS correctos para el origen Angular (localhost:4200).
// [POR QUÉ]: El navegador bloquea peticiones cross-origin si el preflight falla o
//            falta algún header CORS. Este test valida la configuración de
//            SecurityConfig.corsConfigurationSource() en el contexto real.
// [ALTERNATIVAS]: Probar solo con curl/manual; se descarta porque la regla de
//                 automatización exige cobertura en la suite de CI.
// [RELACIONES]: SecurityConfig → CorsConfigurationSource; valida contrato con frontend.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter.AbstractRepositoryJpaAdapterTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CorsIntegrationTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debe_responder_preflight_options_con_200_y_headers_cors() throws Exception {
        mockMvc.perform(options("/api/v1/ligas")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void debe_responder_preflight_post_con_200() throws Exception {
        mockMvc.perform(options("/api/v1/ligas")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
