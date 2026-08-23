// ─────────────────────────────────────────────
// [QUÉ]: Decorador cache-aside del puerto ProveedorPaises: si el catálogo de países
//        está en cache (Redis, TTL), lo devuelve sin golpear la fuente externa (#1);
//        si no, delega en el adapter real (SoccerwayPaisesAdapter), guarda el JSON y
//        devuelve.
// [POR QUÉ]: GET /api/v1/paises/disponibles y la validación de CU-14 (marcar un país
//            como de interés) llamaban al scraper en vivo en CADA request: listar
//            países tardaba segundos y guardar un favorito volvía a golpear la fuente
//            (por eso el frontend veía respuestas lentas y errores tipo "Cannot read
//            properties of null"). El catálogo de países es casi estático (~176
//            registros), así que el cache con TTL largo lo vuelve instantáneo.
// [ALTERNATIVAS]: Cachear en los casos de uso; se descarta porque application no debe
//                 conocer serialización JSON ni Redis. Servir /disponibles desde la
//                 BD persistida (PaisRepository); se descarta porque el flujo elige
//                 preferencias ANTES de poblar (CU-14 precede a CU-10).
// [RELACIONES]: Implementa application.port.ProveedorPaises; usa CacheLecturas +
//               CacheClaves.paises(); envuelve SoccerwayPaisesAdapter. Consumido por
//               PaisController (GET /paises/disponibles), GestionarPaisesInteresUseCase
//               (CU-14) y SincronizarCatalogoUseCase (CU-10).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheClaves;
import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
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
public class ProveedorPaisesCacheable implements ProveedorPaises {

    private final ProveedorPaises delegado;
    private final CacheLecturas cache;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    // [QUÉ]: Construye el decorador con el adapter real (#1), el cache, el serializador
    //        JSON (Jackson 3, bean de Spring) y el TTL configurable (app.cache.ttl-paises-seg).
    // [POR QUÉ]: El delegado es el decorador de cortesia (H-06): cadena
    //            consumidor -> cache -> cortesia -> scraper. Los aciertos de cache no pagan pausa.
    public ProveedorPaisesCacheable(@Qualifier("soccerwayPaisesAdapter") ProveedorPaises delegado,
                                    CacheLecturas cache,
                                    ObjectMapper objectMapper,
                                    @Value("${app.cache.ttl-paises-seg:2592000}") long ttlSeg) {
        this.delegado = delegado;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeg);
    }

    @Override
    public List<PaisFuente> obtenerPaises() {
        String clave = CacheClaves.paises();
        return cache.obtener(clave)
                .map(json -> deserializar(json, clave))
                .orElseGet(() -> {
                    List<PaisFuente> paises = delegado.obtenerPaises();
                    guardarJson(clave, paises);
                    return paises;
                });
    }

    // [QUÉ]: Serializa la lista a JSON y la guarda con TTL.
    private void guardarJson(String clave, List<PaisFuente> paises) {
        try {
            cache.guardar(clave, objectMapper.writeValueAsString(paises), ttl);
        } catch (Exception ex) {
            // [POR QUÉ]: Un fallo de serialización no debe tumbar la operación; el
            //            decorador degrada a no-cache (deja pasar la lectura real).
            throw new InfraestructureException("No se pudo serializar países para cache: " + clave, ex);
        }
    }

    // [QUÉ]: Deserializa el JSON cacheado a la lista de DTOs de fuente.
    private List<PaisFuente> deserializar(String json, String clave) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PaisFuente>>() {
            });
        } catch (Exception ex) {
            throw new InfraestructureException("No se pudo deserializar países desde cache: " + clave, ex);
        }
    }
}