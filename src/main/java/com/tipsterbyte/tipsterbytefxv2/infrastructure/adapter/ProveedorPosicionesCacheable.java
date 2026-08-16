// ─────────────────────────────────────────────
// [QUÉ]: Decorador cache-aside del puerto ProveedorPosiciones: si la tabla de
//        posiciones de la liga está en cache (Redis, TTL), la devuelve sin golpear la
//        fuente externa; si no, delega en el adapter real (FlashscorePosicionesAdapter),
//        guarda el JSON y devuelve.
// [POR QUÉ]: FASE 12 evita llamar al scraper (#3) en cada sincronización de CU-01 dentro
//            del TTL configurado (app.cache.ttl-posiciones-seg). Al sincronizar, el caso
//            de uso elimina la clave antes de consultar, forzando datos frescos.
// [ALTERNATIVAS]: Aplicar el cache dentro de los casos de uso; se descarta porque la
//                 capa application no debe conocer serialización JSON ni Redis; el
//                 decorador mantiene el puerto intocado para el caso de uso.
// [RELACIONES]: Implementa application.port.ProveedorPosiciones; usa CacheLecturas +
//               CacheClaves; envuelve FlashscorePosicionesAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PosicionFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPosiciones;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@Primary
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = false)
public class ProveedorPosicionesCacheable implements ProveedorPosiciones {

    private final ProveedorPosiciones delegado;
    private final CacheLecturas cache;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    // [QUÉ]: Construye el decorador con el adapter real, el cache, el serializador JSON
    //        (Jackson 3, bean de Spring) y el TTL configurable por tipo de dato.
    public ProveedorPosicionesCacheable(@Qualifier("flashscorePosicionesAdapter") ProveedorPosiciones delegado,
                                        CacheLecturas cache,
                                        ObjectMapper objectMapper,
                                        @Value("${app.cache.ttl-posiciones-seg:300}") long ttlSeg) {
        this.delegado = delegado;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeg);
    }

    @Override
    public List<PosicionFuente> obtenerPosiciones(UUID ligaId) {
        String clave = CacheClaves.posiciones(ligaId);
        return cache.obtener(clave)
                .map(json -> deserializar(json, clave))
                .orElseGet(() -> {
                    List<PosicionFuente> posiciones = delegado.obtenerPosiciones(ligaId);
                    guardarJson(clave, posiciones);
                    return posiciones;
                });
    }

    // [QUÉ]: Serializa la lista a JSON y la guarda con TTL.
    private void guardarJson(String clave, List<PosicionFuente> posiciones) {
        try {
            cache.guardar(clave, objectMapper.writeValueAsString(posiciones), ttl);
        } catch (Exception ex) {
            // [POR QUÉ]: Un fallo de serialización no debe tumbar la sincronización;
            //            el decorador degrada a no-cache (deja pasar la lectura real).
            throw new InfraestructureException("No se pudo serializar posiciones para cache: " + clave, ex);
        }
    }

    // [QUÉ]: Deserializa el JSON cacheado a la lista de DTOs.
    private List<PosicionFuente> deserializar(String json, String clave) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PosicionFuente>>() {
            });
        } catch (Exception ex) {
            throw new InfraestructureException("No se pudo deserializar posiciones desde cache: " + clave, ex);
        }
    }
}
