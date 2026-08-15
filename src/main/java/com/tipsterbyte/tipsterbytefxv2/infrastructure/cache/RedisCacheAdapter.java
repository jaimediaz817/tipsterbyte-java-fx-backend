// ─────────────────────────────────────────────
// [QUÉ]: Adapter del puerto CacheLecturas sobre Redis (StringRedisTemplate con TTL).
// [POR QUÉ]: FASE 12 cachea las lecturas de fuentes externas (posiciones, calendario,
//            cuotas) en Redis con expiración por clave. Se registra solo cuando
//            app.cache.enabled=true (propiedad por defecto); si no, entra NoOp.
// [ALTERNATIVAS]: Caffeine (local, por instancia); se descarta porque Redis es el cache
//                 planificado y compartido; Caffeine se preverá como L1 en FASE 17.
// [RELACIONES]: Implementa application.port.CacheLecturas; consumido por los decoradores
//               ProveedorXxxCacheable (infrastructure.adapter).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.cache;

import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = false)
public class RedisCacheAdapter implements CacheLecturas {

    private final StringRedisTemplate redis;

    // [QUÉ]: Construye el adapter con el template de Redis (String → String).
    public RedisCacheAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> obtener(String clave) {
        return Optional.ofNullable(redis.opsForValue().get(clave));
    }

    @Override
    public void guardar(String clave, String valor, Duration ttl) {
        redis.opsForValue().set(clave, valor, ttl);
    }

    @Override
    public void eliminar(String clave) {
        redis.delete(clave);
    }
}
