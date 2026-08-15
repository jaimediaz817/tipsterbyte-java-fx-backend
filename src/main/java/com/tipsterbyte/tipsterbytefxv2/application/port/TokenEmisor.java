// ─────────────────────────────────────────────
// [QUÉ]: Puerto de emisión y validación de tokens JWT para requests autenticados.
// [POR QUÉ]: application define el contrato de "crear/validar sesión stateless" sin
//            conocer JJWT ni la firma HMAC (infrastructure lo implementa). El filtro
//            de seguridad usa estos métodos para autenticar cada request.
// [ALTERNATIVAS]: Trabajar con tokens en application; se descarta porque el dominio/
//                 application no deben acoplarse a la librería JWT ni a la firma.
// [RELACIONES]: CU-13 (login) emite el token; JwtAuthenticationFilter lo valida.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;

public interface TokenEmisor {

    // [QUÉ]: Genera un JWT firmado con el id, email y rol del usuario autenticado.
    String emitirToken(Usuario usuario);

    // [QUÉ]: Extrae el id del usuario desde un token válido (firma y expiración OK).
    // [POR QUÉ]: El filtro lo usa para cargar el usuario y poblar el SecurityContext.
    String extraerIdUsuario(String token);
}
