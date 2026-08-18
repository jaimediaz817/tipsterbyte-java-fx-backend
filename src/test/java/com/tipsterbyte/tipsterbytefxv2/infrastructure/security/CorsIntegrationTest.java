// ─────────────────────────────────────────────
// [QUÉ]: Test de integración CORS: verifica que el preflight OPTIONS responde 200
//        con los headers CORS correctos para los orígenes Angular (4200 y 4201) y
//        que los orígenes no permitidos se rechazan sin headers CORS.
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

    @Test
    void debe_permitir_preflight_desde_el_segundo_origen_angular_4201() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4201")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4201"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void debe_permitir_metodo_patch_en_el_preflight() throws Exception {
        mockMvc.perform(options("/api/v1/paises-interes")
                        .header("Origin", "http://localhost:4201")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsString("PATCH")));
    }

    @Test
    void debe_rechazar_preflight_de_origen_no_permitido_sin_headers_cors() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:9999")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
