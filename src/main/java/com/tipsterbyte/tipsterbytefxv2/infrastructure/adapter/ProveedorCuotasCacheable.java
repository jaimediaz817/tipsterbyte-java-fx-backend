// ─────────────────────────────────────────────
// [QUÉ]: Decorador cache-aside del puerto ProveedorCuotas: devuelve las cuotas del
//        partido desde cache (Redis, TTL) si existen; si no, delega en el adapter real
//        (WplayCuotasAdapter), guarda el JSON y devuelve.
// [POR QUÉ]: FASE 12 evita llamar al scraper (#2) en cada sincronización de CU-03 dentro
//            del TTL (app.cache.ttl-cuotas-seg). El cache es por partidoId (el puerto
//            consulta por partido); CU-03 elimina la clave antes de consultar para
//            forzar cuotas frescas de Wplay.
// [ALTERNATIVAS]: Cache dentro del caso de uso; se descarta para no acoplar application
//                 a serialización/Redis. Decorador sobre el puerto es el patrón cache-aside.
// [RELACIONES]: Implementa application.port.ProveedorCuotas; usa CacheLecturas +
//               CacheClaves; envuelve WplayCuotasAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CuotaFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
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
public class ProveedorCuotasCacheable implements ProveedorCuotas {

    private final ProveedorCuotas delegado;
    private final CacheLecturas cache;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    // [QUÉ]: Construye el decorador con el adapter real, el cache y el TTL configurable.
    public ProveedorCuotasCacheable(@Qualifier("wplayCuotasAdapter") ProveedorCuotas delegado,
                                    CacheLecturas cache,
                                    ObjectMapper objectMapper,
                                    @Value("${app.cache.ttl-cuotas-seg:120}") long ttlSeg) {
        this.delegado = delegado;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeg);
    }

    @Override
    public List<CuotaFuente> obtenerCuotas(UUID partidoId) {
        String clave = CacheClaves.cuotas(partidoId);
        return cache.obtener(clave)
                .map(json -> deserializar(json, clave))
                .orElseGet(() -> {
                    List<CuotaFuente> cuotas = delegado.obtenerCuotas(partidoId);
                    guardarJson(clave, cuotas);
                    return cuotas;
                });
    }

    private void guardarJson(String clave, List<CuotaFuente> cuotas) {
        try {
            cache.guardar(clave, objectMapper.writeValueAsString(cuotas), ttl);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar cuotas para cache: " + clave, ex);
        }
    }

    private List<CuotaFuente> deserializar(String json, String clave) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<CuotaFuente>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo deserializar cuotas desde cache: " + clave, ex);
        }
    }
}
