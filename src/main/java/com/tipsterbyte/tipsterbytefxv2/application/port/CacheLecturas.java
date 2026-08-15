// ─────────────────────────────────────────────
// [QUÉ]: Puerto de cache de lecturas: operaciones genéricas clave→valor con TTL.
// [POR QUÉ]: FASE 12 aplica cache-aside sobre los proveedores de posiciones, calendario
//            y cuotas. Los decoradores de infrastructure dependen de ESTE puerto (y no
//            de Redis/Caffeine directamente), de modo que cambiar de cache o añadir una
//            capa L1 (Caffeine) no toca los casos de uso (misma filosofía de ports).
// [ALTERNATIVAS]: Usar @Cacheable de Spring directamente sobre los métodos del proveedor;
//                 se descarta porque acoplaría el cache al framework y a la anotación,
//                 y no permitiría la estrategia NoOp en tests sin tocar wiring.
// [RELACIONES]: Implementado por infrastructure.cache.RedisCacheAdapter y
//               NoOpCacheLecturas; consumido por los decoradores ProveedorXxxCacheable
//               (infrastructure.adapter, FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import java.time.Duration;
import java.util.Optional;

public interface CacheLecturas {

    // [QUÉ]: Devuelve el valor en cache si existe (Optional.empty si no está presente).
    Optional<String> obtener(String clave);

    // [QUÉ]: Guarda el valor con un TTL (tiempo de vida) a partir de ahora.
    void guardar(String clave, String valor, Duration ttl);

    // [QUÉ]: Elimina la clave del cache (invalidación explícita).
    void eliminar(String clave);
}
