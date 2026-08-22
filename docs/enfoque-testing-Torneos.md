# Enfoque de Testing para el Refactor de Múltiples Temporadas por Liga

## [QUÉ]: 
Definir una estrategia de testing integral para validar que el refactor para soportar múltiples temporadas por liga se implementa correctamente sin romper la funcionalidad existente, couvrant todos los niveles de testing desde unitario hasta de aceptación.

## [POR QUÉ]: 
El refactor toca múltiples capas del sistema (dominio, aplicación, infraestructura) y afecta a funcionalidades críticas como la sincronización de datos, activación de ligas y consultas de datos. Una estrategia de testing bien definida es esencial para asegurar que:
1. Todos los cambios funcionan como se espera
2. No se introduce regresión en funcionalidad existente
3. Los nuevos casos de uso (múltiples temporadas) funcionan correctamente
4. El sistema mantiene su calidad y confiabilidad

## [ALTERNATIVAS]: 
- Testing solo de unidad: Insuficiente para capturar problemas de integración
- Testing solo de extremo a extremo: Lento y difícil de mantener para casos edge
- **Enfoque elegido**: Pirámide de testing con énfasis en unitarios y de integración, complementado con tests de extremo a extremo críticos

## [RELACIONES]: 
- Relacionado con todos los documentos de planificación y especificación creados
- Debe validar los cambios en modelo de dominio, casos de uso, infraestructura y compatibilidad
- Alineado con la regla de testing existente del proyecto (JUnit 5 + Mockito para unitarios, Testcontainers para integración)

# Visión General de la Estrategia de Testing

Seguiremos la pirámide de testing recomendada en las reglas del proyecto:
1. **Tests de Unidad** (70%): Validar lógica pura en aislamiento
2. **Tests de Integración** (20%): Validar interacciones entre capas y con dependencias externas simuladas
3. **Tests de Extremo a Extremo** (10%): Validar flujos completos de usuario (pocos pero críticos)

# Plan de Testing por Capa

## 1. Tests de Unidad

### 1.1 Modelo de Dominio
**Objetivo**: Validar que las entidades de dominio se comportan correctamente según las reglas de negocio.

**Clases a Testear**:
- `Temporada` (nueva entity)
- `Liga` (modificada para colección de temporadas)
- `Partido` (modificado para referencia a temporada)
- `DetalleFuenteExtraccion` (modificado para referencia a temporada)

**Tests Específicos**:
- **Temporada**:
  - Construcción válida con diferentes rangos de años
  - Validación de rango de años (fin > inicio)
  - Métodos de navegación a liga (si se implementa)
  - Equals y hashCode basado en ID
- **Liga**:
  - Construcción con conjunto vacío de temporadas
  - Añadir/quitar temporadas
  - Consultar temporada por nombre
  - Determinar temporada activa/más reciente
  - Manejo de edge cases (liga sin temporadas, múltiples activas, etc.)
  - Constructores de fábrica actualizados
- **Partido**:
  - Construcción con temporadaId vs ligaId
  - Validaciones de campos
  - Métodos de negocio (actualizarCuotas, asignarResultado, etc.)
  - Equals y hashCode
- **DetalleFuenteExtraccion**:
  - Construcción con temporadaId
  - Validaciones de campos
  - Unicidad por (temporadaId, tipo)

**Herramientas**: JUnit 5 + Mockito (para mocks de dependencias externas si las hubiera)
**Cobertura Objetivo**: >85% en clases de dominio modificadas

### 1.2 Casos de Uso (Application)
**Objetivo**: Validar que los casos de uso orchestran correctamente las operaciones según las nuevas reglas.

**Casos de Uso a Testear**:
- CU-10: Sincronizar Catálogo de Países y Ligas
- CU-04: Activar Liga/Temporada
- CU-01: Sincronizar Tabla de Posiciones
- CU-02: Sincronizar Calendario de Partidos
- CU-03: Sincronizar Cuotas

**Tests Específicos**:
- **CU-10**:
  - Crear nueva liga desde fuente #5 cuando no existe
  - Añadir temporada a liga existente
  - No duplicar ligas existentes
  - Manejo correcto de datos de fuente #5 (nombre_torneo, semestre, anio)
  - Temporada creada en estado BORRADOR
- **CU-04**:
  - Activar temporada específica cuando se proporciona temporadaId
  - Activar temporada activa implícitamente cuando se proporciona solo ligaId
  - Manejo de ambigüedad (0 o >1 temporadas candidatas) → lanzar excepción específica
  - Validación BR-001 por temporada (no por liga)
  - Creación/actualización correcta de DetalleFuenteExtraccion
  - Emisión de eventos correcta (LigaActivada y/o TemporadaActivada)
- **CU-01/02/03**:
  - Operación exitosa cuando se especifica temporadaId explícitamente
  - Inferencia correcta de temporada cuando no se especifica (activa → más reciente)
  - Manejo de ambigüedad → lanzar excepción específica de caso de uso
  - Validación BR-002 (no extraer para temporadas inactivas)
  - Operaciones correctas de sincronización (reemplazar posiciones, crear/actualizar partidos, actualizar cuotas)
  - Emisión de eventos correcta donde corresponda
  - Manejo de casos edge (lista vacía de datos de fuente, etc.)

**Herramientas**: JUnit 5 + Mockito (mockear puertos: repositorios, proveedores de fuentes)
**Cobertura Objetivo**: >80% en cada caso de uso modificado

### 1.3 DTOs y Conversores
**Objetivo**: Validar que los objetos de transferencia de estado se mapean correctamente.

**Clases a Testear**:
- DTOs de entrada/salida modificados
- Conversores entre entidad y DTO
- Constructores de comando actualizados

**Tests Específicos**:
- Mapeo correcto de campos nuevos (temporadaId, información de temporada)
- Validación de campos requeridos
- Manejo de valores nulos y vacíos
- Serialización/deserialización JSON correcta

## 2. Tests de Integración

### 2.1 Tests de Repositorios JPA
**Objetivo**: Validar que las entidades se persisten y recuperan correctamente con las relaciones nuevas.

**Clases a Testear**:
- TemporadaEntity
- LigaEntity (con relación one-to-many)
- PartidoEntity (con temporada_id)
- DetalleFuenteExtraccionEntity (con temporada_id)

**Tests Específicos**:
- **TemporadaRepository**:
  - Guardar y recuperar temporada
  - Buscar por liga_id
  - Buscar por liga_id y nombre (unique constraint)
  - Eliminación en cascada cuando se elimina liga
- **LigaRepository**:
  - Guardar liga con colecciones vacías de temporadas
  - Añadir temporadas después de guardar
  - Consultar liga y verificar que se recuperan las temporadas
- **PartidoRepository**:
  - Guardar partido con temporada_id
  - Consultar partidos por temporada_id
  - Consulta combinada: liga_id + rango de fechas (requiere join o subconsulta)
- **DetalleFuenteExtraccionRepository**:
  - Guardar detalle con temporada_id
  - Validar unique constraint (temporada_id, tipo)
  - Consultar por temporada_id y tipo

**Herramientas**: 
- JUnit 5 + Testcontainers (para PostgreSQL real)
- @DataJpaTest para tests enfocados en repositorios
- Scripts de migración de datos aplicados en @BeforeEach si es necesario

**Cobertura Objetivo**: >80% en cada repositorio modificado

### 2.2 Tests de Adaptadores de Fuentes
**Objetivo**: Validar que los adaptadores interactúan correctamente con la base de datos y manejan adecuadamente las temporadas.

**Adapters a Testear**:
- FlashscorePosicionesAdapter
- SoccerwayCalendarioAdapter
- WplayCuotasAdapter

**Tests Específicos**:
- **FlashscorePosicionesAdapter**:
  - Obtener posiciones cuando temporada existe y está activa
  - Lanzar excepción cuando temporada no existe
  - Lanzar excepción cuando temporada no está activa
  - Lanzar excepción cuando falta configuración de fuente de posiciones
  - Mapeo correcto de respuesta JSON a PosicionTabla
  - Uso correcto de caché con temporadaId como clave
  - Llamada correcta a la API externa con URL de detalle de fuente
- **SoccerwayCalendarioAdapter**:
  - Análogos a Flashscore pero para calendario
  - Creación correcta de Partido con temporada_id
  - Manejo de jornada extraída de la fuente
  - Asignación correcta de resultado cuando está presente en la respuesta
- **WplayCuotasAdapter**:
  - Análogos pero para cuotas
  - Filtración correcta de partidos por temporada_id y rango de fechas
  - Mapeo correcto de respuesta JSON a Cuota(s)
  - Asociación correcta de cuotas a partidos específicos

**Herramientas**: 
- JUnit 5 + Mockito (mockear repositorios, WebClient)
- @SpringBootTest con @MockBean para dependencias externas
- Servidores mock de API externas (WireMock o similares) para tests más realistas

**Cobertura Objetivo**: >75% en cada adapter (mayor complejidad debido a dependencias externas)

### 2.3 Tests de Servicios de Application (Servicios que Orquestan Casos de Uso)
**Objetivo**: Validar la integración entre casos de uso, repositorios y manejo de transacciones.

**Servicios a Testear**:
- Servicios que expongan casos de uso como servicios (si existen)
- O bien, tests de casos de uso en contexto de Spring (menos mocks)

**Tests Específicos**:
- Transaccionalidad correcta: todas las operaciones en un caso de uso se commiten o se revierten juntas
- Manejo correcto de excepciones y rollback
- Interacción correcta entre múltiples repositorios en un caso de uso
- Uso correcto del gestionario de eventos de dominio (si se aplica)

**Herramientas**: 
- JUnit 5 + @SpringBootTest
- @Transactional para tests que necesitan verificar rollback

## 3. Tests de Extremo a Extremo (E2E)

### 3.1 Selección de Flujos Críticos
Debido a su costo y tiempo de ejecución, seleccionaremos apenas los flujos más críticos que validen el valor de negocio del refactor.

**Flujos Seleccionados**:
1. **Flujo de Catálogo y Activación Multitemporada**:
   - CU-10 procesa source #5 y crea liga con dos temporadas (Apertura y Clausura 2026)
   - CU-04 activa solo la temporada Apertura
   - Verificar que solo la temporada Apertura está en estado ACTIVA
2. **Flujo de Sincronización Específica por Temporada**:
   - CU-01 sincroniza posiciones solo para temporada Apertura
   - CU-02 crea partidos solo para temporada Apertura
   - CU-03 obtiene cuotas solo para partidos de Apertura
   - Verificar que los datos de Clausura permanecen sin tocar
3. **Flujo de Consulta con Resolución Automática**:
   - Consultar posiciones de liga sin especificar temporada
   - Verificar que se devuelven datos de la temporada correcta según reglas (activa → más reciente)
4. **Flujo de Manejo de Ambigüedad**:
   - Intentar consultar posiciones cuando hay múltiples temporadas activas
   - Verificar que se devuelve error 400 específico y descriptivo

### 3.2 Herramientas y Enfoque
- **Herramientas**: 
  - JUnit 5 + @SpringBootTest + TestRestTemplate o WebTestClient
  - Testcontainers para PostgreSQL y posiblemente mocks de API externas
  - O bien, usar @SpringBootTest con @MockBean para adaptadores de fuentes (tests más controlados pero menos reales)
- **Frecuencia**: Ejecutar en cada pull request pero considerar marcar como de ejecución más lenta que los unitarios
- **Cobertura Objetivo**: 5-10 escenarios E2E críticos que cubran las principales vías de código

## 4. Testing de Regresión y Compatibilidad

### 4.1 Testing de Regresión Funcional
**Objetivo**: Asegurar que la funcionalidad existente para ligas con una sola temporada continúa funcionando exactamente como antes.

**Tests Específicos**:
- Ejecutar todos los tests existentes del proyecto antes del refactor
- Verificar que todos siguen pasando después del refactor
- Enfocarse particularmente en:
  - Ligas europeas tradicionales (una temporada por año)
  - Flujos de activación y sincronización existentes
  - Endpoints REST utilizados por el frontend Angular #1
  - Flujos de pronósticos y suscripciones (menos afectados pero aún relevantes)

### 4.2 Testing de Compatibilidad Hacia Atrás
**Objetivo**: Validar que las estrategias de compatibilidad propuestas funcionan correctamente.

**Tests Específicos**:
- Endpoints existentes funcionan con el nuevo comportamiento de resolución de temporada
- Respuestas de endpoints existentes incluyen nuevos campos informativos sin romper clientes actuales
- Nuevos endpoints específicos de temporada funcionan como se espera
- Manejo correcto de errores de ambigüedad (400 con mensajes descriptivos)
- Headers de advertencia (si se implementan) se envían correctamente cuando corresponde

## 5. Plan de Ejecución y Herramientas

### 5.1 Herramientas de Testing
- **Unit Testing**: JUnit 5, Mockito, AssertJ
- **Integration Testing**: JUnit 5, Testcontainers (PostgreSQL), Spring Boot Test
- **Testing de API**: Spring Boot Test, TestRestTemplate/WebTestClient
- **Mocking Externo**: Mockito, WireMock (para simular APIs de fuentes externas)
- **Cobertura de Código**: JaCoCo (integrado en build de Gradle)

### 5.2 Integración en el Ciclo de Desarrollo
- **Pre-commit**: Ejecutar tests de unidad rápido (menos de 2 minutos)
- **Pre-push**: Ejecutar tests de unidad + integración (menos de 5 minutos)
- **Pull Request**: Ejecutar suite completa de testing (unitario + integración + E2E críticos)
- **Nightly**: Ejecutar suite completa incluyendo tests de regresión y compatibilidad

### 5.3 Umbrales de Cobertura
- **Unit Tests**: >85% en clases modificadas
- **Integration Tests**: >80% en repositorios y adapters críticos
- **Tests de Regresión**: 100% de tests existentes deben seguir pasando
- **Cobertura General**: Mantener >80% cobertura general del proyecto

### 5.4 Reporte y Seguimiento
- Reportes de cobertura generados en cada build
- Badges de cobertura en README.md
- Tendencia de cobertura trackeada sobre tiempo
- Fallo de build si cobertura nueva < 80% o tests existentes fallan

## 6. Estimación de Esfuerzo para Testing

### 6.1 Tests de Unidad: 3-4 días
- Dominio: 1 día
- Casos de uso: 1.5 días
- DTOs y conversores: 0.5 días
- Otros componentes menores: 1 día

### 6.2 Tests de Integración: 3-4 días
- Repositorios JPA: 1 día
- Adaptadores de fuentes: 1.5 días
- Servicios de application: 0.5 días
- Configuración y pruebas diversas: 1 día

### 6.3 Tests de Extremo a Extremo: 1-2 días
- Selección y creación de flujos críticos: 0.5 días
- Implementación y ajuste: 0.5-1 día

### 6.4 Testing de Regresión y Compatibilidad: 1-2 días
- Ejecutar y validar tests existentes: 0.5 días
- Escribir tests específicos de compatibilidad: 0.5-1 día
- Ajustes basado en resultados: 0.5 días

**Total estimado para testing: 8-12 días**

Este enfoque de testing proporciona una cobertura integral que valida tanto la corrección de las nuevas funcionalidades como la preservación de la existente, asegurando un refactor de calidad que minimiza el riesgo de regresión mientras permite la evolución segura del sistema hacia el soporte de múltiples temporadas por liga.