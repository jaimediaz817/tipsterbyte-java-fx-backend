// ─────────────────────────────────────────────
// [QUÉ]: Filtro de autenticación JWT (OncePerRequestFilter): lee el header
//        Authorization "Bearer <token>", valida el token y puebla el SecurityContext
//        con un Authentication del usuario si es válido.
// [POR QUÉ]: Convierte la sesión stateless (JWT) en un principal de Spring Security
//            para que authorizeHttpRequests y @PreAuthorize funcionen por rol. Usa
//            los puertos TokenEmisor y UsuarioRepository (application) para no
//            acoplarse a JJWT ni a JPA.
// [ALTERNATIVAS]: Customizer con resource server (oauth2ResourceServer); se descarta
//                 porque requiriría un servidor de autorización; aquí el token se
//                 emite y valida por la misma app (symmetric key).
// [RELACIONES]: Registrado en SecurityConfig; usa TokenEmisor + UsuarioRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import com.tipsterbyte.tipsterbytefxv2.application.port.TokenEmisor;
import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final TokenEmisor tokenEmisor;
    private final UsuarioRepository usuarioRepository;

    // [QUÉ]: Construye el filtro con los puertos (inyección por constructor).
    public JwtAuthenticationFilter(TokenEmisor tokenEmisor, UsuarioRepository usuarioRepository) {
        this.tokenEmisor = tokenEmisor;
        this.usuarioRepository = usuarioRepository;
    }

    // [QUÉ]: Extrae y valida el token del header; si es válido autentica la request.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(CABECERA);
        if (header != null && header.startsWith(PREFIJO) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(PREFIJO.length());
                String idUsuario = tokenEmisor.extraerIdUsuario(token);
                Usuario usuario = usuarioRepository.buscarPorId(UUID.fromString(idUsuario)).orElse(null);
                if (usuario != null && usuario.activo()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // [POR QUÉ]: Token inválido o expirado no autentica; la cadena decide
                //            el 401/403. No se propaga para no filtrar detalles.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
