// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de autenticación (FASE 11): registro (CU-12) y login (CU-13).
// [POR QUÉ]: Expone la puerta de entrada de usuarios. POST /api/v1/auth/registro crea
//            un usuario; POST /api/v1/auth/login valida credenciales y devuelve el
//            JWT. Ambos endpoints son públicos (permitAll en SecurityConfig).
// [ALTERNATIVAS]: Delegar el login a Spring Security (form/login con UserDetails);
//                 se descarta porque la emisión del JWT con nuestros claims es más
//                 directa vía casos de uso y mantiene la Dependency Rule.
// [RELACIONES]: CU-12 → RegistrarUsuarioUseCase; CU-13 → AutenticarUsuarioUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticacionResultado;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.AutenticarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.LoginRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.RegistrarUsuarioRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    // [QUÉ]: Construye el controller con sus casos de uso (inyección por constructor).
    public AuthController(RegistrarUsuarioUseCase registrarUsuarioUseCase,
                          AutenticarUsuarioUseCase autenticarUsuarioUseCase) {
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.autenticarUsuarioUseCase = autenticarUsuarioUseCase;
    }

    // [QUÉ]: POST /api/v1/auth/registro — crea un usuario (CU-12) → 201.
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistrarUsuarioRequest request) {
        RegistrarUsuarioComando comando = new RegistrarUsuarioComando(
                request.nombre(), request.email(), request.password(), request.rol());
        registrarUsuarioUseCase.ejecutar(comando);
        AutenticacionResultado resultado = autenticarUsuarioUseCase.ejecutar(
                new AutenticarUsuarioComando(request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(resultado));
    }

    // [QUÉ]: POST /api/v1/auth/login — valida credenciales y devuelve el JWT (CU-13).
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AutenticacionResultado resultado = autenticarUsuarioUseCase.ejecutar(
                new AutenticarUsuarioComando(request.email(), request.password()));
        return ResponseEntity.ok(toResponse(resultado));
    }

    private AuthResponse toResponse(AutenticacionResultado resultado) {
        return new AuthResponse(
                resultado.usuarioId(), resultado.nombre(), resultado.email(),
                resultado.rol(), resultado.token());
    }
}
