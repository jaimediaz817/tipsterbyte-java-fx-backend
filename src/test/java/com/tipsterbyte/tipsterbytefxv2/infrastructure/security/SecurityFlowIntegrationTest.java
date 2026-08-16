// ─────────────────────────────────────────────
// [QUÉ]: Test de integración del flujo de seguridad JWT completo: registro (CU-12),
//        login (CU-13) y acceso a un endpoint protegido con/sin token, sobre el
//        contexto Spring real (SecurityConfig + filtro JWT + Testcontainers).
// [POR QUÉ]: Valida end-to-end que la cadena de filtros autoriza solo con JWT válido
//            y por rol, y que /api/v1/auth/** permanece público.
// [ALTERNATIVAS]: Solo tests de controller standalone; se descartan porque no ejercitan
//                 la SecurityFilterChain ni el filtro JWT reales.
// [RELACIONES]: CU-12/CU-13 → AuthController → SecurityConfig → JwtAuthenticationFilter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter.AbstractRepositoryJpaAdapterTest;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityFlowIntegrationTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    @BeforeEach
    void limpiar() {
        usuarioJpaRepository.deleteAll();
    }

    @Test
    void debe_registrar_login_y_acceder_a_endpoint_protegido() throws Exception {
        registrarUsuario("admin@example.com", "TIPSTER");

        String token = loginYExtraerToken("admin@example.com", "clave-secreta");

        // Endpoint protegido con token válido → 200.
        mockMvc.perform(get("/api/v1/fuentes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void debe_rechazar_endpoint_protegido_sin_token() throws Exception {
        mockMvc.perform(get("/api/v1/fuentes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("No autenticado: se requiere un token JWT válido"));
    }

    @Test
    void debe_rechazar_endpoint_protegido_con_token_invalido() throws Exception {
        mockMvc.perform(get("/api/v1/fuentes")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void debe_rechazar_acceso_por_rol_insuficiente() throws Exception {
        registrarUsuario("cliente@example.com", "CLIENTE");
        String token = loginYExtraerToken("cliente@example.com", "clave-secreta");

        // /api/v1/ligas exige SUPERADMIN o TIPSTER; CLIENTE → 403 con ApiError.
        mockMvc.perform(get("/api/v1/ligas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado: el rol actual no tiene permiso para este recurso"));
    }

    @Test
    void debe_mantener_publico_el_endpoint_de_login() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "noexiste@example.com", "password": "cualquiera"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    private void registrarUsuario(String email, String rol) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Admin", "email": "%s",
                                 "password": "clave-secreta", "rol": "%s"}""".formatted(email, rol)))
                .andExpect(status().isCreated());
    }

    private String loginYExtraerToken(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}
