# Plan de Refactor: Soporte para Múltiples Torneos/Temporadas por Liga

> ## ✅ ESTADO: COMPLETADO (Bridge Fix + fase dedicada)
> **[QUÉ] se implementó** (todo en verde, 411 tests):
> 1. **Dominio**: `Temporada` es Entity (id UUID, ligaId, nombre, semestre opcional 1|2, anioInicio/anioFin, `EstadoTemporada` PLANIFICADA/ACTIVA/FINALIZADA) con identidad propia; `Liga` contiene `Set<Temporada>` y **delega** equipos/posiciones en su *temporada vigente* (activa o primera registrada), conservando la API `liga.equipos()/posiciones()/actualizarPosiciones()`.
> 2. **Equipos y posiciones pertenecen a la temporada** (`Temporada` los compone en dominio; `equipos.temporada_id` y `posiciones_tabla.temporada_id` en BD): un equipo que desciende en 2024 no está en la tabla 2025 y cada temporada conserva su historial.
> 3. **Partidos y detalles de fuente por temporada**: `Partido.temporadaId`, `DetalleFuenteExtraccion.temporadaId`; consultas "por liga" resuelven vía JOIN interno (compatibilidad con adapters de fuentes Flashscore/Soccerway); Wplay resuelve su URL por la temporada del partido.
> 4. **FK real ligas.pais_id → paises.id** (CU-10 la llena al poblar; el nombre queda denormalizado para display).
> 5. **CU-04/CU-11 asocian URLs de fuentes a la temporada vigente**; nuevo método de puerto `buscarPorTemporadaYTipo`.
> 6. **Migraciones aplicadas a BD dev**: `docs/migration/V3__bridge_fix_temporadas.sql` + `docs/migration/V4__temporadas_equipos_posiciones.sql`.
>
> **Pendiente (fases futuras)**: endpoints dedicados `/temporadas/**` para el frontend, transición PLANIFICADA→ACTIVA de temporadas (hoy nadie cambia el estado de una temporada), matching fuzzy de equipos entre fuentes (FASE 17), endurecer `ligas.pais_id` a NOT NULL cuando todo el catálogo se re-pueble.

## [QUÉ]: 
Modificar el modelo de dominio para que una Liga pueda tener múltiples Temporadas (torneos), permitiendo modelar correctamente competencias con varios torneos por año (ej: Apertura y Clausura en Colombia).

## [POR QUÉ]: 
El modelo actual asume una sola Temporada por Liga, lo que no representa correctamente ligas como la Betplay colombiana que tiene dos torneos distintos por año (Apertura y Clausura), cada uno con su propia tabla de posiciones, calendario y equipos. Esta limitación fue identificada durante el análisis del modelo relacional y afecta la capacidad del sistema para representar con precisión las estructuras de competiciones reales.

## [ALTERNATIVAS]: 
1. Mantener el modelo actual y duplicar Ligas para cada torneo (descartado porque viola el ubiquitous language y crea entidades Liga artificiales)
2. Añadir un campo "torneo" o "semestre" a Temporada manteniéndola como VO (descartado porque no resuelve el problema fundamental de múltiples temporadas por liga)
3. Introducir una nueva entidad Torneo entre Liga y Temporada (descartado porque el usuario confirmó que "torneo es una temporada")
4. **Enfoque elegido**: Convertir Temporada de Value Object a Entity y permitir que Liga contenga múltiples Temporadas

## [RELACIONES]: 
- Modifica el aggregate raíz Liga (CU-01, CU-02, CU-03, CU-04, CU-10)
- Afecta al aggregate Partido (CU-02, CU-03, CU-05)
- Afecta al entity DetalleFuenteExtraccion (CU-11)
- Impacta los repositorios de Liga, Partido y DetalleFuenteExtraccion
- Requiere actualización de adapters de fuentes externas
- Afecta los DTOs y controllers relacionados

# Detalles del Refactor

## 1. Cambios en el Modelo de Dominio

### 1.1 Temporada: De Value Object a Entity
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/domain/model/Temporada.java`

Transformar `Temporada` de `@record` a clase entity con:
- Identidad propia (UUID)
- Referencia opcional a Liga padre (para facilitar navegación)
- Conservar los campos `anioInicio` y `anioFin` como VO anidado o atributos simples
- Añadir campos opcionales proveniente de fuente #5:
  - `nombre` (nombre_torneo: ej: "Apertura", "Clausura")
  - `semestre` (1 o 2 para indicar mitad de año)

### 1.2 Liga: De Una Temporada a Colección de Temporadas
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/domain/model/Liga.java`

Cambios:
- Reemplazar `private final Temporada temporada;` por `private final Set<Temporada> temporadas;`
- Actualizar constructores y métodos factory:
  - Constructor BORRADOR: inicializar con conjunto vacío
  - Constructor de catálogo (CU-10): buscar o crear Liga, luego añadir Temporada al conjunto
  - Métodos de reconstruir(): aceptar colección de temporadas
- Añadir métodos de gestión:
  - `addTemporada(Temporada temporada)`
  - `removeTemporada(Temporada temporada)`
  - `getTemporadaPorNombre(String nombre)`
  - `getTemporadaActual()` (opcional, basado en fecha actual)
  - `getTemporadas()`: devolver copia inmutable

### 1.3 Partido: Referencia a Temporada en lugar de Liga
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/domain/model/Partido.java`

Cambios:
- Reemplazar `private final UUID ligaId;` por `private final UUID temporadaId;`
- Mantener `ligaId` como campo opcional durante transición para compatibilidad
- Actualizar constructores y factory methods
- Añadir método `ligaId()` que devuelva el ligaId de la temporada (requiere acceso a LigaRepository o relación directa)

### 1.4 DetalleFuenteExtraccion: Referencia a Temporada
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/domain/model/DetalleFuenteExtraccion.java`

Cambios:
- Reemplazar `private final UUID ligaId;` por `private final UUID temporadaId;`
- Actualizar constructores y factory methods
- El `ligaId` se puede obtener a través de la temporada

## 2. Cambios en Casos de Uso

### 2.1 CU-10: Sincronizar Catálogo de Países y Ligas
**Modificación**:
- Al procesar source #5 (`/ext-soccerway-leagues-by-country`):
  - Buscar Liga existente por nombre y pais (identidad de competencia)
  - Si no existe, crear nueva Liga
  - Crear nueva Temporada con datos del registro (nombre_torneo, semestre, anio)
  - Añadir la Temporada a la Liga
  - Persistir Liga (si es nueva) y Temporada

### 2.2 CU-04: Activar Liga
**Modificación**:
- El comando `ActivarLigaComando` debe especificar para qué torneo/temporada activar
- O bien, activar todas las temporadas de una liga que tengan fuentes configuradas
- La validación BR-001 se aplica por temporada (cada temporada necesita sus 3 fuentes)
- Se crea/actualiza DetalleFuenteExtraccion para la temporada específica

### 2.3 CU-01: Sincronizar Tabla de Posiciones
**Modificación**:
- Obtener temporada activa (o especificada) en lugar de liga activa
- Actualizar posiciones de esa temporada específica
- Cada temporada mantiene su propia tabla de posiciones

### 2.4 CU-02: Sincronizar Calendario de Partidos
**Modificación**:
- Obtener temporada activa (o especificada)
- Crear/actualizar partidos pertenecientes a esa temporada
- Cada temporada tiene su propio calendario

### 2.5 CU-03: Sincronizar Cuotas
**Modificación**:
- Obtener próximos partidos de una temporada específica
- Actualizar cuotas de esos partidos

## 3. Cambios en Infraestructura

### 3.1 Nuevas Tablas JPA

**Tabla `temporadas`**:
```sql
CREATE TABLE temporadas (
    id UUID PRIMARY KEY,
    liga_id UUID NOT NULL REFERENCES ligas(id),
    nombre VARCHAR(100),
    semestre INTEGER,
    anio_inicio INTEGER NOT NULL,
    anio_fin INTEGER NOT NULL,
    -- otros campos según sea necesario
    CONSTRAINT uq_temporada_liga_nombre UNIQUE (liga_id, nombre)
);
```

**Modificaciones a tabla `ligas`**:
- Eliminar columnas `temporada_anio_inicio` y `temporada_anio_fin` (se mueven a tabla temporadas)
- Mantener: id, nombre, pais, urlSoccerway, apiId, estado, etc.

**Modificaciones a tabla `partidos`**:
- Cambiar columna `liga_id` por `temporada_id` (FK → temporadas.id)
- Mantener columna `liga_id` opcional durante transición

**Modificaciones a tabla `detalles_fuente_extraccion`**:
- Cambiar columna `liga_id` por `temporada_id` (FK → temporadas.id)

### 3.2 Entidades JPA

**TemporadaEntity.java**:
```java
@Entity
@Table(name = "temporadas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"liga_id", "nombre"})
})
public class TemporadaEntity {
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liga_id", nullable = false)
    private LigaEntity liga;
    
    private String nombre;
    private Integer semestre;
    private Integer anioInicial;
    private Integer anioFinal;
    // getters y setters
}
```

**LigaEntity.java**:
- Eliminar columnas de temporada
- Añadir `@OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true)`
- `private Set<TemporadaEntity> temporadas = new HashSet<>();`

### 3.3 Repositorios
- `TemporadaRepository` extiende `JpaRepository<TemporadaEntity, UUID>`
- Actualizar `LigaRepository` para manejar la relación uno-a-muchos
- Actualizar `PartidoRepository` y `DetalleFuenteExtraccionRepository` para usar temporada_id

## 4. Cambios en Infrastructure Adapters

### 4.1 FlashscorePosicionesAdapter (ProveedorPosiciones)
- Al obtener posición para una liga, debe especificar para qué temporada
- La URL de la fuente probablemente ya esté asociada a una temporada específica
- Mapear respuesta a PosicionTabla asociándola a la temporada correcta

### 4.2 SoccerwayCalendarioAdapter (ProveedorCalendario)
- Similar: obtener calendario para una temporada específica
- El endpoint de Soccerway probablemente devuelva datos para un torneo/semestre específico

### 4.3 WplayCuotasAdapter (ProveedorCuotas)
- Obtener cuotas para partidos de una temporada específica

## 5. Estrategia de Compatibilidad Hacia Atrás

### 5.1 Durante la Transición
- Mantener campos `ligaId` en Partido y DetalleFuenteExtraccion junto con nuevos `temporadaId`
- Proveer métodos de conveniencia en Liga:
  ```java
  // Devuelve la única temporada si existe exactamente una, o lanza excepción
  public Temporada getTemporadaUnica() {
      if (temporadas.size() == 1) {
          return temporadas.iterator().next();
      }
      throw new DomainException("La liga no tiene exactamente una temporada");
  }
  ```
- Actualizar controladores para usar temporada_id cuando esté disponible, fallback a liga_id
- Nuevos endpoints API pueden usar temporada_id, manteniendo los antiguos por compatibilidad

### 5.2 Impacto en Frontend Angular #1
- Endpoints que actualmente devuelven datos por liga_id pueden necesitar especificar temporada_id
- Posibles enfoques:
  1. Nuevos endpoints: `/api/v1/temporadas/{temporadaId}/posiciones`, etc.
  2. Parámetro opcional: `/api/v1/ligas/{ligaId}/posiciones?temporadaId=...`
  3. Lógica de servidor: si se especifica liga_id pero no temporada_id, devolver datos de la temporada activa o más reciente
- Se recomienda enfoque 2 para minimizar cambios en frontend

## 6. Plan de Testing

### 6.1 Tests Unitarios
- Actualizar tests de Temporada para verificar identidad y navegación
- Actualizar tests de Liga para verificar gestión de colección de temporadas
- Actualizar tests de Partido y DetalleFuenteExtraccion para referenciar temporada

### 6.2 Tests de Integración
- Tests de repositorios JPA con Testcontainers verificando:
  - Relación uno-a-muchos Liga-Temporada
  - Eliminación en cascada apropiada
  - Constraints de unicidad (liga_id, nombre)
- Tests de casos de uso:
  - CU-10: crear ligas con múltiples temporadas
  - CU-04: activar temporada específica
  - CU-01/02/03: operar en temporada específica

### 6.3 Tests de Controller
- Verificar nuevos endpoints o parámetros de temporada_id
- Verificar compatibilidad hacia atrás con endpoints existentes

## 7. Estimación de Esfuerzo y Riesgos

### 7.1 Complejidad
- **Alto**: Cambios transversales en múltiples capas del sistema
- **Medio-Alto**: Número de archivos afectados (dominio, aplicación, infraestructura)
- **Bajo-Medio**: Complejidad individual de cada cambio (la mayoría son refactorings mecánicos)

### 7.2 Riesgos
- **Riesgo alto**: Romper funcionalidad existente si no se mantiene compatibilidad
  - *Mitigrar*: Estrategia de compatibilidad hacia atrás cuidadosa
- **Riesgo medio**: Inconsistencias en datos durante migración
  - *Mitigrar*: Scripts de migración de datos que asumen una temporada por liga existente
- **Riesgo bajo**: Errores de lógica en reglas de negocio
  - *Mitigrar*: Tests exhaustivos antes y después

### 7.3 Esfuerzo Estimado
- Dominio: 2-3 días
- Aplicación (use cases): 3-4 días
- Infraestructura: 3-4 días
- Testing: 2-3 días
- **Total estimado**: 10-14 días de desarrollo

## 8. Próximos Pasos Inmediatos

Si se aprueba este plan:
1. Crear rama de功能 para el refactor
2. Implementar cambios en dominio (Temporada como entity, Liga con colección)
3. Actualizar casos de uso afectados empezando por CU-10 (punto de entrada de datos)
4. Progresivamente actualizar otros casos de uso
5. Implementar cambios en infraestructura y testing en paralelo
6. Realizar pruebas exhaustivas antes de merge

## 9. Validación con el Usuario

Este plan debe ser revisado y aprobado antes de iniciar la implementación. Es particularmente importante confirmar:
- Que el enfoque de Temporada como entidad múltiples por Liga corresponde al entendimiento del usuario de "torneo es una temporada"
- Que la estrategia de compatibilidad hacia atrás es aceptable para el frontend Angular #1
- Que el nivel de esfuerzo y riesgos es comprendido

---
*Documento creado como parte del proceso de refactorización dirigido por el usuario. Basado en el análisis del modelo relacional y las necesidades identificadas para soportar correctamente ligas con múltiples torneos por temporada.*