# Regla de Testing — tipsterbyte-fx-v2

> Regla SIEMPRE obligatoria. Un cambio no está completo sin su prueba.

## Pirámide de pruebas (principio rector)

1. **Unit tests** (mayoría): dominio y casos de uso, sin Spring ni DB. JUnit 5 + Mockito.
2. **Integration tests** (los necesarios): repositorios JPA (Testcontainers), adapters de APIs (mockeados o con test doble).
3. **Controller tests**: MockMvc con DTOs reales.

> NO todo debe ser integration test. Mantener la mayoría unitarios: rápidos, aislados y estables.

## Dónde colocar cada tipo

| Tipo | Qué cubre | Cómo |
| --- | --- | --- |
| Unit | VO, entities, reglas de negocio (BR-xx), calculadoras de dominio | JUnit5, sin contexto Spring |
| Unit | Casos de uso (CU-xx) | Mockito: mock de puertos |
| Integration | Repository adapters (JPA) | Testcontainers PostgreSQL (FASE 10) |
| Controller | Endpoints REST | MockMvc (FASE 10) |

## Estructura de test

- Mismo paquete que la clase probada, sufijo `Test` (ej: `PronosticoTest`).
- Nombres de test descriptivos: `debe_publicar_pronostico_cuando_estado_es_borrador`.
- Cada regla de negocio BR-001..008 debe tener al menos un test (éxito + violación).

## Verificación antes de dar por terminada una tarea

- [ ] `./gradlew test` (o el comando que defina FASE 3) en verde.
- [ ] R-001: comentarios `[QUÉ]/[POR QUÉ]/[ALTERNATIVAS]/[RELACIONES]` presentes.
- [ ] Regla de arquitectura respetada (dependency rule).