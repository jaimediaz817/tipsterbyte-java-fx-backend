// ─────────────────────────────────────────────
// [QUÉ]: Decorador cache-aside del puerto ProveedorEquiposPorLiga: si la plantilla de
//        la liga está en cache (Redis, TTL largo), la devuelve sin golpear la fuente
//        externa (#6); si no, delega en el adapter real (SoccerwayEquiposAdapter),
//        guarda el JSON y devuelve.
// [POR QUÉ]: El poblamiento geográfico consulta #6 por cada liga de países de interés:
//            sin cache, cada re-ejecución del poblamiento re-scrapearía plantillas que
//            cambian raramente. La clave es normalizada país+liga para que la
//            invalidación del caso de uso y este decorador usen exactamente la misma.
// [ALTERNATIVAS]: Cachear en el caso de uso; se descarta porque application no debe
//                 conocer serialización JSON ni Redis.
// [RELACIONES]: Implementa application.port.ProveedorEquiposPorLiga; usa CacheLecturas +
//               CacheClaves.equipos(); envuelve SoccerwayEquiposAdapter. Consumido por
//               SincronizarCatalogoUseCase (CU-10 encadenado, HU-11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorEquiposPorLiga;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.exception.InfraestructureException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Component
@Primary
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = false)
public class ProveedorEquiposCacheable implements ProveedorEquiposPorLiga {

    private final ProveedorEquiposPorLiga delegado;
    private final CacheLecturas cache;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    // [QUÉ]: Construye el decorador con el adapter real (#6), el cache, el serializador
    //        JSON (Jackson 3, bean de Spring) y el TTL configurable (app.cache.ttl-equipos-seg).
    // [POR QUÉ]: El delegado es el decorador de cortesia (H-06): cadena
    //            consumidor -> cache -> cortesia -> scraper.
    public ProveedorEquiposCacheable(@Qualifier("soccerwayEquiposAdapter") ProveedorEquiposPorLiga delegado,
                                     CacheLecturas cache,
                                     ObjectMapper objectMapper,
                                     @Value("${app.cache.ttl-equipos-seg:2592000}") long ttlSeg) {
        this.delegado = delegado;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeg);
    }

    @Override
    public List<EquipoFuente> obtenerEquipos(String countryName, String leagueName) {
        String clave = CacheClaves.equipos(countryName, leagueName);
        return cache.obtener(clave)
                .map(json -> deserializar(json, clave))
                .orElseGet(() -> {
                    List<EquipoFuente> equipos = delegado.obtenerEquipos(countryName, leagueName);
                    guardarJson(clave, equipos);
                    return equipos;
                });
    }

    // [QUÉ]: Serializa la lista a JSON y la guarda con TTL.
    private void guardarJson(String clave, List<EquipoFuente> equipos) {
        try {
            cache.guardar(clave, objectMapper.writeValueAsString(equipos), ttl);
        } catch (Exception ex) {
            // [POR QUÉ]: Un fallo de serialización no debe tumbar la operación; el
            //            decorador degrada a no-cache (deja pasar la lectura real).
            throw new InfraestructureException("No se pudo serializar equipos para cache: " + clave, ex);
        }
    }

    // [QUÉ]: Deserializa el JSON cacheado a la lista de DTOs de fuente.
    private List<EquipoFuente> deserializar(String json, String clave) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<EquipoFuente>>() {
            });
        } catch (Exception ex) {
            throw new InfraestructureException("No se pudo deserializar equipos desde cache: " + clave, ex);
        }
    }
}
