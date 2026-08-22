# Detalle de Cambios en Infraestructura para Soporte de Múltiples Temporadas

## [QUÉ]: 
Especificar los cambios necesarios en la capa de infrastructure (persistencia, adapters, configuración) para soportar el nuevo modelo donde una Liga puede tener múltiples Temporadas.

## [POR QUÉ]: 
La capa de infrastructure implementa los puertos definidos en la capa de application y maneja la persistencia de datos. Con los cambios en el modelo de dominio y los casos de uso, es necesario actualizar las entidades JPA, los repositorios, los adapters de fuentes externas y la configuración correspondiente.

## [ALTERNATIVAS]: 
Se considera mantener la compatibilidad con el esquema existente durante un período de transición mediante columnas duplicadas o vistas, pero el enfoque elegido es migrar completamente al nuevo esquema con scripts de migración de datos.

## [RELACIONES]: 
- Implementa los cambios en el modelo de dominio (Temporada como entity, Liga con colección)
- Soporta los cambios en los casos de uso (CU-01, CU-02, CU-03, CU-04, CU-10)
- Afecta directamente los repositorios JPA y los adapters de fuentes externas
- Relacionado con la configuración de la aplicación y los scripts de migración

# Detalles de Cambios en Infraestructura

## 1. Cambios en Entidades JPA y Tablas de Base de Datos

### 1.1 Nueva Entidad: TemporadaEntity
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/persistence/entity/TemporadaEntity.java`

```java
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import jakarta.persistence.*;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "temporadas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"liga_id", "nombre"}))
public class TemporadaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liga_id", nullable = false)
    private LigaEntity liga;

    @Column(name = "nombre", length = 100)
    private String nombre; // nombre_torneo de fuente #5: Apertura, Clausura, etc.

    @Column(name = "semestre")
    private Integer semestre; // 1 o 2, de fuente #5

    @Column(name = "_anio_inicio", nullable = false)
    private Integer anioInicio;

    @Column(name = "anio_fin", nullable = false)
    private Integer anioFin;

    // Campos adicionales opcionales para estado/tracking
    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoTemporada estado; // Nuevo enum: BORRADOR, ACTIVA, INACTIVA, FINALIZADA

    @Column(name = "fecha_creacion")
    private java.time.LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private java.time.LocalDateTime fechaActualizacion;

    // Constructores
    protected TemporadaEntity() {}

    public TemporadaEntity(UUID id, LigaEntity liga, String nombre, Integer semestre,
                           Integer anioInicio, Integer anioFin) {
        this.id = id;
        this.liga = liga;
        this.nombre = nombre;
        this.semestre = semestre;
        this.anioInicio = anioInicio;
        this.anioFin = anioFin;
        this.estado = EstadoTemporada.BORRADOR;
        this.fechaCreacion = java.time.LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    // Getters y Setters
    public UUID getId() { return id; }
    public LigaEntity getLiga() { return liga; }
    public String getNombre() { return nombre; }
    public Integer getSemestre() { return semestre; }
    public Integer getAnioInicio() { return anioInicio; }
    public Integer getAnioFin() { return anioFin; }
    public EstadoTemporada getEstado() { return estado; }
    public void setEstado(EstadoTemporada estado) { this.estado = estado; }
    public java.time.LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public java.time.LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(java.time.LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    // Equals y HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemporadaEntity that)) return false;
        return id.equals(that.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

// Enum para estado de temporada (similar a EstadoLiga pero para temporadas)
enum EstadoTemporada {
    BORRADOR, ACTIVA, INACTIVA, FINALIZADA
}
```

### 1.2 Modificaciones a LigaEntity
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/persistence/entity/LigaEntity.java`

**Cambios**:
- Eliminar columnas relacionadas con temporada: `temporada_anio_inicio`, `temporada_anio_fin`
- Añadir relación uno-a-muchos con TemporadaEntity

```java
@Entity
@Table(name = "ligas")
public class LigaEntity {
    // ... campos existentes (id, nombre, pais, urlSoccerway, apiId, estado, etc.) ...

    // NUEVA RELACIÓN: Una liga tiene muchas temporadas
    @OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TemporadaEntity> temporadas = new HashSet<>();

    // Getter para la relación
    public Set<TemporadaEntity> getTemporadas() {
        return Collections.unmodifiableSet(temporadas);
    }

    // Métodos de conveniencia para gestionar la colección
    public void addTemporada(TemporadaEntity temporada) {
        temporadas.add(temporada);
        temporada.setLiga(this);
    }

    public void removeTemporada(TemporadaEntity temporada) {
        temporadas.remove(temporada);
        temporada.setLiga(null);
    }
}
```

### 1.3 Modificaciones a PartidoEntity
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/persistence/entity/PartidoEntity.java`

**Cambios**:
- Reemplazar columna `liga_id` por `temporada_id` (FK a temporadas.id)
- Mantener columna `liga_id` opcional durante transición para compatibilidad

```java
@Entity
@Table(name = "partidos")
public class PartidoEntity {

    // ... campos existentes ...

    // CAMBIO: Referencia a temporada en lugar de liga directa
    @Column(name = "temporada_id", nullable = false)
    private UUID temporadaId;

    // MANTENER DURANTE TRANSICIÓN: Referencia a liga para compatibilidad
    @Column(name = "liga_id", nullable = true) // Hacer nullable durante transición
    private UUID ligaId;

    // Getters y Setters actualizados
    public UUID getTemporadaId() { return temporadaId; }
    public void setTemporadaId(UUID temporadaId) { this.temporadaId = temporadaId; }

    public UUID getLigaId() { return ligaId; }
    public void setLigaId(UUID ligaId) { this.ligaId = ligaId; }

    // Método de conveniencia para obtener ligaId desde temporada (requiere LigaRepository)
    // O bien, dejar que la capa de aplicación maneje esta relación
}
```

### 1.4 Modificaciones a DetalleFuenteExtraccionEntity
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/persistence/entity/DetalleFuenteExtraccionEntity.java`

**Cambios**:
- Reemplazar columna `liga_id` por `temporada_id` (FK a temporadas.id)

```java
@Entity
@Table(name = "detalles_fuente_extraccion",
       uniqueConstraints = @UniqueConstraint(columnNames = {"temporada_id", "tipo"}))
public class DetalleFuenteExtraccionEntity {

    // ... campos existentes (id, tipo, url, activa) ...

    // CAMBIO: Referencia a temporada en lugar de liga
    @Column(name = "temporada_id", nullable = false)
    private UUID temporadaId;

    // Getter y Setter actualizados
    public UUID getTemporadaId() { return temporadaId; }
    public void setTemporadaId(UUID temporadaId) { this.temporadaId = temporadaId; }

    // Relación muchos-a-uno con TemporadaEntity (opcional, para navegación)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporada_id", referencedColumnName = "id", insertable = false, updatable = false)
    private TemporadaEntity temporada;
}
```

### 1.5 Script de Migración de Datos
**Archivo**: `src/main/resources/db/migration/VXX__migracion_a_multiple_temporadas.sql`

```sql
-- 1. Crear nueva tabla temporadas
CREATE TABLE temporadas (
    id UUID PRIMARY KEY,
    liga_id UUID NOT NULL REFERENCES ligas(id) ON DELETE CASCADE,
    nombre VARCHAR(100),
    semestre INTEGER,
    anio_inicio INTEGER NOT NULL,
    anio_fin INTEGER NOT NULL,
    estado VARCHAR(20) DEFAULT 'BORRADOR',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_temporada_liga_nombre UNIQUE (liga_id, nombre)
);

-- 2. Poblar temporadas a partir de ligas existentes (asumiendo una temporada por liga)
INSERT INTO temporadas (id, liga_id, nombre, semestre, anio_inicio, anio_fin, estado, fecha_creacion, fecha_actualizacion)
SELECT 
    gen_random_uuid() as id,
    l.id as liga_id,
    'Temporada Predeterminada' as nombre,
    1 as semestre,
    l.temporada_anio_inicio as anio_inicio,
    l.temporada_anio_fin as anio_fin,
    CASE l.estado 
        WHEN 'ACTIVA' THEN 'ACTIVA' 
        ELSE 'BORRADOR' 
    END as estado,
    l.fecha_creacion as fecha_creacion,
    l.fecha_actualizacion as fecha_actualizacion
FROM ligas l
WHERE l.temporada_anio_inicio IS NOT NULL AND l.temporada_anio_fin IS NOT NULL;

-- 3. Actualizar partidos para referenciar temporadas
-- Asumimos que cada partido pertenece a la temporada de su liga
ALTER TABLE partidos ADD COLUMN IF NOT EXISTS temporada_id UUID;
UPDATE partidos p
SET temporada_id = t.id
FROM temporadas t
WHERE p.liga_id = t.liga_id;

-- Hacer temporada_id NOT NULL después de la actualización
ALTER TABLE partidos ALTER COLUMN temporada_id SET NOT NULL;

-- 4. Actualizar detalles_fuente_extraccion para referenciar temporadas
ALTER TABLE detalles_fuente_extraccion ADD COLUMN IF NOT EXISTS temporada_id UUID;
UPDATE detalles_fuente_extraccion d
SET temporada_id = t.id
FROM temporadas t
WHERE d.liga_id = t.liga_id;

-- Hacer temporada_id NOT NULL después de la actualización
ALTER TABLE detalles_fuente_extraccion ALTER COLUMN temporada_id SET NOT NULL;

-- 5. Añadir constraint único para detalles_fuente_extraccion
ALTER TABLE detalles_fuente_extraccion 
ADD CONSTRAINT uq_detalle_temporada_tipo UNIQUE (temporada_id, tipo);

-- 6. Eliminar columnas antiguas de ligas (después de verificar migración)
-- ALTER TABLE ligas DROP COLUMN temporada_anio_inicio;
-- ALTER TABLE ligas DROP COLUMN temporada_anio_fin;
-- Estos se eliminarán en una migración posterior tras validación
```

### 1.6 Nuevos Repositorios JPA
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/persistence/repository/TemporadaRepository.java`

```java
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TemporadaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TemporadaRepository extends JpaRepository<TemporadaEntity, UUID> {
    Optional<TemporadaEntity> findById(UUID id);
    Set<TemporadaEntity> findByLigaId(UUID ligaId);
    Optional<TemporadaEntity> findByLigaIdAndNombre(UUID ligaId, String nombre);
    boolean existsByLigaIdAndNombre(UUID ligaId, String nombre);
}
```

**Modificaciones a LigaRepository.java**:
```java
// Añadir método para buscar liga por nombre y pais (usado en CU-10)
Optional<LigaEntity> findByNombreAndPais(String nombre, String pais);

// Los métodos existentes por ID siguen funcionando
Optional<LigaEntity> findById(UUID id);
```

### 1.7 Adaptadores de Fuentes Externalas

#### 1.7.1 FlashscorePosicionesAdapter (ProveedorPosiciones)
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/adapters/FlashscorePosicionesAdapter.java`

**Cambios**:
- El constructor o método de configuración debe recibir el ID de la temporada para la cual obtener posiciones
- El método `obtenerPosiciones()` ahora opera en el contexto de una temporada específica
- Mapear la respuesta a PosicionTabla asociándola a la temporada correcta

```java
@Service
@Primary
@ConditionalOnProperty(value = "app.cache.enabled", havingValue = "false")
public class FlashscorePosicionesAdapter implements ProveedorPosiciones {

    // ... dependencias existentes ...

    // Nuevo: recibir ID de temporada en lugar de ID de liga
    public FlashscorePosicionesAdapter(
            ObjectMapper objectMapper,
            // ... otras dependencias ...
            ) {
        // ... inicialización existente ...
    }

    @Override
    @Cacheable(value = "posiciones", key = "#temporadaId", unless = "#result == null")
    public List<PosicionTabla> obtenerPosiciones(UUID temporadaId) {
        // Validar que la temporada existe y está activa
        TemporadaEntity temporada = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new DomainException("Temporada no encontrada: " + temporadaId));
        
        if (temporada.getEstado() != EstadoTemporada.ACTIVA) {
            throw new DomainException("No se pueden obtener posiciones de una temporada inactiva: " + temporadaId);
        }

        // Obtener URL de la fuente para esta temporada (a través de DetalleFuenteExtraccion)
        DetalleFuenteExtraccionEntity detalle = detalleFuenteExtraccionRepository
                .findByTemporadaIdAndTipo(temporadaId, TipoFuenteExtraccion.STANDINGS)
                .orElseThrow(() -> new DomainException("Fuente de posiciones no configurada para la temporada: " + temporadaId));
        
        String url = detalle.getUrl();
        
        // Llamada existente a la API externa
        String jsonResponse = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        // Mapeo existente de JSON a PosicionTabla
        // ... (sin cambios en el mapeo, pero ahora asociado a la temporada correcta)
        
        return posicicionesMapeadas;
    }
}
```

#### 1.7.2 SoccerwayCalendarioAdapter (ProveedorCalendario)
Archivo similar con cambios análogos:
- Recibir `temporadaId` como parámetro
- Obtener URL de la fuente para esa temporada específica
- Mapear respuesta a Partido(s) asociados a esa temporada

#### 1.7.3 WplayCuotasAdapter (ProveedorCuotas)
Archivo similar con cambios análogos:
- Recibir `temporadaId` como parámetro
- Filtrar partidos por esa temporada antes de obtener cuotas
- Mapear respuesta a Cuota(s) de esos partidos

## 2. Cambios en Configuración y Propiedades

### 2.1 Archivo application.properties
Añadir propiedades relacionadas con el nuevo comportamiento:
```properties
# Comportamiento por defecto cuando no se especifica temporada
app.temporada.default-selection-strategy=ACTIVA_THEN_MOST_RECENT
app.temporada.allow-ambiguous-selection=false
```

### 2.2 Configuración de Caché
Actualizar claves de caché para incluir temporada_id:
- Posiciones: cache key incluye temporada_id
- Calendario: cache key incluye temporada_id
- Las cuotas ya estaban asociadas a partido, que ahora pertenece a temporada

## 3. Actualización de DTOs (si es necesario)

Aunque la mayoría de los cambios se manejan en la capa de dominio y application, algunos DTOs podrían necesitare actualización:

### 3.1 LigaDTO (application.dto)
Añadir campos para representar mejor las temporadas:
```java
public class LigaDTO {
    private UUID id;
    private String nombre;
    private String pais;
    // ... campos existentes ...
    
    // Nuevo: Información de temporada activa o predeterminada
    private TemporadaBasicaDTO temporadaActiva;
    private List<TemporadaBasicaDTO> todasLasTemporadas;
    
    // Constructores, getters, setters
}

public class TemporadaBasicaDTO {
    private UUID id;
    private String nombre;
    private Integer semestre;
    private Integer anioInicio;
    private Integer anioFin;
    private EstadoTemporada estado;
}
```

### 3.2 Nuevos DTOs Específicos de Temporada
Para endpoints que operan específicamente en temporadas:
```java
public class TemporadaDetalleDTO {
    private UUID id;
    private UUID ligaId;
    private String ligaNombre;
    private String nombre; // nombre del torneo
    private Integer semestre;
    private Integer anioInicio;
    private Integer anioFin;
    private EstadoTemporada estado;
    private Integer cantidadPosiciones;
    private Integer cantidadPartidos;
    // ... otros resúmenes útiles ...
}
```

## 4. Impacto en la Configuración de la Aplicación

### 4.1 Dependencias
No se requieren nuevas dependencias externas más allá de las ya existentes para JPA y Spring Data.

### 4.2 Configuración de Transacciones
Los cambios en cascada (CascadeType.ALL) en la relación Liga-Temporada aseguran que:
- Cuando se elimina una Liga, se eliminan todas sus Temporadas
- Cuando se guarda una Liga, se guardan sus Temporadas
- Este comportamiento debe verificarse para asegurar que coincida con los requisitos de negocio

## 5. Validación y Testing de Infraestructura

### 5.1 Tests de Entidades JPA
- Verificar mapeo correcto de campos
- Verificar relaciones uno-a-muchos y muchos-a-uno
- Verificar constraints únicos (liga_id, nombre en temporadas; temporada_id, tipo en detalles)

### 5.2 Tests de Repositorios con Testcontainers
- Escenarios de persistencia y recuperación de Ligas con múltiples Temporadas
- Verificar eliminación en cascada
- Verificar consultas por temporada_id vs liga_id
- Probar métodos de repositorio nuevos (findByNombreAndPais, findByLigaId, etc.)

### 5.3 Tests de Adaptadores
- Verificar que los adaptadores usan correctamente el temporada_id
- Verificar manejo de errores cuando temporada no existe o no está activa
- Verificar que las llamadas a APIs externas usan la URL correcta para la temporada específica

## 6. Estrategia de Despliegue y Migración

### 6.1 Fase 1: Dual Escritura (Opcional)
- Durante un período temporal, escribir tanto al esquema antiguo como al nuevo
- Permite volver atrás si hay problemas
- Complejidad alta, solo recomendado si el tiempo de inactividad no es permitido

### 6.2 Fase 2: Migración de Datos + Despliegue
- Ejecutar script de migración de datos en ventana de mantenimiento
- Desplegar nueva versión del aplicación
- Verificar funcionamiento
- En una fase posterior, eliminar columnas antiguas

### 6.3 Fase 3: Limpieza
- Eliminar columnas temporales de compatibilidad (liga_id en partidos y detalles si se mantuvo)
- Limpiar código de compatibilidad en capas superiores

## 7. Estimación de Esfuerzo

### 7.1 Entidades JPA y Migración: 2-3 días
### 7.2 Repositorios: 1-2 días
### 7.3 Adaptadores: 2-3 días (tres adaptadores principales)
### 7.4 Configuración y DTOs: 1 día
### 7.5 Testing de Infraestructura: 2-3 días
### **Total estimado: 8-12 días**

Este detalle proporciona la especificación técnica necesaria para implementar los cambios en la capa de infrastructure que soportan el nuevo modelo de múltiples temporadas por liga, asegurando consistencia con los cambios en dominio y application mientras se mantiene un camino claro hacia la compatibilidad hacia atrás y la calidad del código.