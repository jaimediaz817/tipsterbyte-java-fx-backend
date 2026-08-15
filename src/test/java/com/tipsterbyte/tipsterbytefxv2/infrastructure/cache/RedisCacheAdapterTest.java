// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de RedisCacheAdapter contra un Redis real (Testcontainers).
// [POR QUÉ]: Valida el contrato real del puerto CacheLecturas (obtener/guardar/eliminar
//            + TTL) contra Redis de verdad, no un mock en memoria (regla testing.md).
// [ALTERNATIVAS]: Mock de StringRedisTemplate; se descarta porque no valida TTL ni el
//                 comportamiento de redis.delete/opsForValue.
// [RELACIONES]: Cubre infrastructure.cache.RedisCacheAdapter (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.cache;

import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.cache.enabled=true")
@Import(RedisTestConfiguration.class)
class RedisCacheAdapterTest {

    @Autowired
    private CacheLecturas cacheLecturas;

    @Test
    void debe_guardar_y_obtener_valor() {
        cacheLecturas.guardar("clave:1", "valor", Duration.ofMinutes(5));

        Optional<String> valor = cacheLecturas.obtener("clave:1");

        assertEquals(Optional.of("valor"), valor);
    }

    @Test
    void debe_devolver_vacio_cuando_clave_no_existe() {
        Optional<String> valor = cacheLecturas.obtener("clave:inexistente");

        assertTrue(valor.isEmpty());
    }

    @Test
    void debe_eliminar_clave() {
        cacheLecturas.guardar("clave:2", "valor", Duration.ofMinutes(5));

        cacheLecturas.eliminar("clave:2");

        assertTrue(cacheLecturas.obtener("clave:2").isEmpty());
    }

    @Test
    void debe_respetar_el_ttl() throws InterruptedException {
        cacheLecturas.guardar("clave:ttl", "valor", Duration.ofSeconds(1));

        assertEquals(Optional.of("valor"), cacheLecturas.obtener("clave:ttl"));

        Thread.sleep(1500);

        assertTrue(cacheLecturas.obtener("clave:ttl").isEmpty());
    }

    @Test
    void debe_sobrescribir_valor_existente() {
        cacheLecturas.guardar("clave:3", "primero", Duration.ofMinutes(5));

        cacheLecturas.guardar("clave:3", "segundo", Duration.ofMinutes(5));

        assertEquals(Optional.of("segundo"), cacheLecturas.obtener("clave:3"));
    }
}
