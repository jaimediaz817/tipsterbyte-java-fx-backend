// ─────────────────────────────────────────────
// [QUÉ]: Test del GlobalExceptionHandler: mapeo de cada excepción a su HTTP status
//        con el cuerpo ApiError esperado.
// [POR QUÉ]: Es el contrato de errores de la API (DomainException→422, validación→400,
//            body malformado→400, query param faltante→400, resto→500); los tests de
//            controllers verifican cada caso puntual, este test lo cubre de forma
//            centralizada con un controller stub.
// [RELACIONES]: GlobalExceptionHandler → DomainException, ApiError, excepciones de Spring.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.exception;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debe_mapear_domain_exception_a_422() throws Exception {
        mockMvc.perform(get("/stub/dominio"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.mensaje").value("BR-007: regla violada"))
                .andExpect(jsonPath("$.path").value("/stub/dominio"));
    }

    @Test
    void debe_mapear_validacion_a_400() throws Exception {
        mockMvc.perform(post("/stub/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"campo": ""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void debe_mapear_json_malformado_a_400() throws Exception {
        mockMvc.perform(post("/stub/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{campo: "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Cuerpo de la request inválido o malformado"));
    }

    @Test
    void debe_mapear_parametro_faltante_a_400() throws Exception {
        mockMvc.perform(get("/stub/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Parámetro obligatorio ausente: id"));
    }

    @Test
    void debe_mapear_parametro_con_tipo_invalido_a_400() throws Exception {
        mockMvc.perform(get("/stub/enum").param("tipo", "INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Parámetro inválido: tipo"));
    }

    @Test
    void debe_mapear_infraestructure_exception_a_503() throws Exception {
        mockMvc.perform(get("/stub/infra"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.mensaje").value("Redis no responde"));
    }

    @Test
    void debe_mapear_excepcion_generica_a_500() throws Exception {
        mockMvc.perform(get("/stub/generico"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }

    @Test
    void debe_incluir_campos_estandar_del_api_error() throws Exception {
        mockMvc.perform(get("/stub/dominio"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // [QUÉ]: Controller de prueba que dispara cada escenario capturado por el advice.
    @RestController
    static class StubController {

        @GetMapping("/stub/dominio")
        void dominio() {
            throw new DomainException("BR-007: regla violada");
        }

        @PostMapping("/stub/validacion")
        void validacion(@org.springframework.web.bind.annotation.RequestBody
                        @jakarta.validation.Valid ValidacionRequest request) {
        }

        @GetMapping("/stub/parametro")
        void parametro(@RequestParam("id") String id) {
        }

        @GetMapping("/stub/enum")
        void enumInvalido(@RequestParam("tipo") TipoStub tipo) {
        }

        @GetMapping("/stub/infra")
        void infra() {
            throw new InfraestructureException("Redis no responde");
        }

        @GetMapping("/stub/generico")
        void generico() {
            throw new IllegalStateException("boom");
        }
    }

    // [QUÉ]: DTO de prueba para validar el escenario @Valid → 400.
    record ValidacionRequest(@jakarta.validation.constraints.NotBlank String campo) {
    }

    // [QUÉ]: Enum de prueba para validar el escenario query param inválido → 400.
    enum TipoStub {
        A, B
    }
}
