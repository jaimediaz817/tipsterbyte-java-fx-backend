// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto PasswordHasher usando BCrypt de Spring Security.
// [POR QUÉ]: BCrypt añade salt automático y es resistente a ataques de diccionario;
//            es el estándar recomendado para contraseñas. Implementa el puerto para
//            que application no dependa de Spring Security.
// [ALTERNATIVAS]: SHA-256 o PBKDF2; se descartan porque son rápidos (bruteforce) o
//                 requieren configuración manual de salt. Argon2; se descarta por
//                 no estar incluido en Spring Security sin dependencia extra.
// [RELACIONES]: Implementa application.port.PasswordHasher (CU-12/CU-13).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import com.tipsterbyte.tipsterbytefxv2.application.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String contrasena) {
        return encoder.encode(contrasena);
    }

    @Override
    public boolean verificar(String contrasena, String hash) {
        return encoder.matches(contrasena, hash);
    }
}
