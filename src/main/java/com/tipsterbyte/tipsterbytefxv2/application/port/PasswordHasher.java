// ─────────────────────────────────────────────
// [QUÉ]: Puerto de hashing de contraseñas: encripta en registro y verifica en login.
// [POR QUÉ]: El dominio nunca conoce el algoritmo de hashing (BCrypt vive en
//            infrastructure). Este puerto desacopla la regla "nunca guardar la
//            contraseña en claro" de su implementación.
// [ALTERNATIVAS]: Llamar BCryptPasswordEncoder desde application; se descarta porque
//                 acoplaría la capa application a Spring Security.
// [RELACIONES]: CU-12/CU-13 → PasswordHasher → BcryptPasswordHasher (infrastructure).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

public interface PasswordHasher {

    // [QUÉ]: Genera el hash de una contraseña en claro (nunca se guarda la original).
    String hash(String contrasena);

    // [QUÉ]: Verifica si una contraseña en claro coincide con el hash almacenado.
    boolean verificar(String contrasena, String hash);
}
