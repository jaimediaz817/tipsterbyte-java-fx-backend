// ─────────────────────────────────────────────
// [QUÉ]: Decorador cache-aside del puerto ProveedorCalendario: devuelve el calendario
//        de la liga desde cache (Redis, TTL) si existe; si no, delega en el adapter real
//        (SoccerwayCalendarioAdapter), guarda el JSON y devuelve.
// [POR QUÉ]: FASE 12 evita llamar al scraper (#4) en cada sincronización de CU-02 dentro
//            del TTL (app.cache.ttl-calendario-seg); CU-02 elimina la clave antes de
//            consultar para forzar datos frescos.
// [ALTERNATIVAS]: Cache dentro del caso de uso; se descarta para no acoplar application
//                 a serialización/Redis. Decorador sobre el puerto es el patrón cache-aside.
// [RELACIONES]: Implementa application.port.ProveedorCalendario; usa CacheLecturas +
//               CacheClaves; envuelve SoccerwayCalendarioAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCalendario;
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
public class ProveedorCalendarioCacheable implements ProveedorCalendario {

    private final ProveedorCalendario delegado;
    private final CacheLecturas cache;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    // [QUÉ]: Construye el decorador con el adapter real, el cache y el TTL configurable.
    public ProveedorCalendarioCacheable(@Qualifier("soccerwayCalendarioAdapter") ProveedorCalendario delegado,
                                        CacheLecturas cache,
                                        ObjectMapper objectMapper,
                                        @Value("${app.cache.ttl-calendario-seg:300}") long ttlSeg) {
        this.delegado = delegado;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeg);
    }

    @Override
    public List<PartidoFuente> obtenerCalendario(UUID ligaId) {
        String clave = CacheClaves.calendario(ligaId);
        return cache.obtener(clave)
                .map(json -> deserializar(json, clave))
                .orElseGet(() -> {
                    List<PartidoFuente> calendario = delegado.obtenerCalendario(ligaId);
                    guardarJson(clave, calendario);
                    return calendario;
                });
    }

    private void guardarJson(String clave, List<PartidoFuente> calendario) {
        try {
            cache.guardar(clave, objectMapper.writeValueAsString(calendario), ttl);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar calendario para cache: " + clave, ex);
        }
    }

    private List<PartidoFuente> deserializar(String json, String clave) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PartidoFuente>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo deserializar calendario desde cache: " + clave, ex);
        }
    }
}
