// ─────────────────────────────────────────────
// [QUÉ]: Configuración de test que levanta un único PostgreSQL (Testcontainers) como bean
//        @ServiceConnection para todos los tests de integración JPA.
// [POR QUÉ]: Declarar el contenedor como @Container estático en cada clase de test hace
//            que Spring cachee el contexto con el puerto del primer contenedor y lo reutilice
//            en las demás clases (cuyo contenedor ya se detuvo) → Connection refused. Como
//            bean en un @TestConfiguration, el contenedor vive con el contexto cacheado y se
//            comparte entre todas las clases que lo importan.
// [ALTERNATIVAS]: @Container estático por clase; se descarta por el fallo de cacheo descrito.
// [RELACIONES]: Importada por AbstractRepositoryJpaAdapterTest (FASE 8).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    // [QUÉ]: Contenedor PostgreSQL compartido para los tests de persistencia.
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17");
    }
}