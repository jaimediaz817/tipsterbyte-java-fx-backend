// ─────────────────────────────────────────────
// [QUÉ]: Implementación NoOp del puerto CacheLecturas: no guarda ni lee nada.
// [POR QUÉ]: Cuando app.cache.enabled=false (tests sin contenedor Redis, o despliegue
//            sin cache), los decoradores ProveedorXxxCacheable y los casos de uso que
//            invalidan siguen funcionando sin tocar código: obtener devuelve vacío y
//            guardar/eliminar son no-ops.
// [ALTERNATIVAS]: @ConditionalOnMissingBean(CacheLecturas.class); se descarta porque con
//                 anotaciones @Component el orden de escaneo puede evaluar la condición
//                 antes de conocer los beans candidatos y dejar el puerto sin bean. Un
//                 flag explícito (app.cache.enabled=false) es determinista y sin huecos
//                 frente a RedisCacheAdapter (que se registra solo con =true).
// [RELACIONES]: Implementa application.port.CacheLecturas; alternativo a RedisCacheAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.cache;

import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "false")
public class NoOpCacheLecturas implements CacheLecturas {

    @Override
    public Optional<String> obtener(String clave) {
        return Optional.empty();
    }

    @Override
    public void guardar(String clave, String valor, Duration ttl) {
    }

    @Override
    public void eliminar(String clave) {
    }
}
