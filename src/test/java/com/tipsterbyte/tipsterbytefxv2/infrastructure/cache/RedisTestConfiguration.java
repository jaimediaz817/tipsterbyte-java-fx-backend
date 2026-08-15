// ─────────────────────────────────────────────
// [QUÉ]: Configuración de test que levanta un único Redis (Testcontainers) como bean
//        @ServiceConnection para los tests de integración del cache.
// [POR QUÉ]: Mismo patrón que PostgresTestConfiguration: un solo contenedor compartido
//            entre las clases que lo importan evita el fallo de contexto cacheado de
//            Spring (puerto del primer contenedor reutilizado tras detenerse).
// [ALTERNATIVAS]: Contenedor Redis por clase de test; se descarta por el mismo motivo
//                 documentado en PostgresTestConfiguration.
// [RELACIONES]: Importada por RedisCacheAdapterTest (FASE 12).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.cache;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestConfiguration {

    // [QUÉ]: Contenedor Redis compartido para los tests de cache (puerto interno 6379).
    @Bean
    @ServiceConnection("redis")
    GenericContainer redis() {
        return new GenericContainer("redis:7-alpine").withExposedPorts(6379);
    }
}
