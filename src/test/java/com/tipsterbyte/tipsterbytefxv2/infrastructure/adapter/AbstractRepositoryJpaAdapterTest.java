// ─────────────────────────────────────────────
// [QUÉ]: Base común de tests de integración JPA con Testcontainers (PostgreSQL 17).
// [POR QUÉ]: Los adapters JPA se prueban contra un PostgreSQL real en contenedor para
//            validar DDL, constraints y SQL del motor objetivo (regla testing.md).
//            Importa PostgresTestConfiguration para compartir un único contenedor entre
//            todos los tests (evita el fallo de contexto cacheado descrito en esa clase).
// [ALTERNATIVAS]: H2 embebido o la BD local :5433; se descartan porque no replican
//                 fielmente PostgreSQL ni aíslan la BD de desarrollo.
// [RELACIONES]: Base de los tests de los 4 repository adapters (FASE 8).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
public abstract class AbstractRepositoryJpaAdapterTest {
}