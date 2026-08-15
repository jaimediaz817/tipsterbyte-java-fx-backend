// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto TokenEmisor usando JJWT (HS256): emite y lee JWT.
// [POR QUÉ]: Firma HS256 con una clave secreta configurable (app.jwt.secret) y
//            expiración configurable (app.jwt.expiration-ms). El subject del token
//            es el id del usuario; el rol viaja en un claim.
// [ALTERNATIVAS]: RS256 con par de claves; se descarta porque requiere gestionar
//                 certificados, innecesario para una API interna monorepo.
// [RELACIONES]: Implementa application.port.TokenEmisor; usado por CU-13 y por
//               JwtAuthenticationFilter (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import com.tipsterbyte.tipsterbytefxv2.application.port.TokenEmisor;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenEmisor implements TokenEmisor {

    private final SecretKey claveFirma;
    private final long expirationMs;

    // [QUÉ]: Construye el emisor con la clave secreta y expiración de configuración.
    public JwtTokenEmisor(@Value("${app.jwt.secret}") String secreto,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.claveFirma = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String emitirToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.id().toString())
                .claim("rol", usuario.rol().name())
                .claim("email", usuario.email().direccion())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(expirationMs)))
                .signWith(claveFirma)
                .compact();
    }

    @Override
    public String extraerIdUsuario(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(claveFirma)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
