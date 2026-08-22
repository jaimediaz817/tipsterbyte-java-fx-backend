# Especificación de Cambios en Casos de Uso para Soporte de Múltiples Temporadas

## [QUÉ]: 
Detallar las modificaciones necesarias en los casos de uso de la capa application para soportar el nuevo modelo donde una Liga puede tener múltiples Temporadas (torneos).

## [POR QUÉ]: 
Los casos de uso actuales asumen una relación uno-a-uno entre Liga y Temporada. Con el nuevo modelo donde una Liga puede tener múltiples Temporadas, es necesario actualizar los casos de uso para operar correctamente con esta nueva estructura.

## [ALTERNATIVAS]: 
Se considera especificar explícitamente la temporada en todos los casos de uso que actualmente trabajan con ligas, manteniendo compatibilidad mediante lógica de inferencia cuando sea posible.

## [RELACIONES]: 
- Modifica los casos de uso CU-01, CU-02, CU-03, CU-04 y CU-10
- Relacionado directamente con los cambios en el modelo de dominio (Temporada como entity)
- Impacta los DTOs de entrada/salida de estos casos de uso
- Afecta los controllers que exponen estos casos de uso como endpoints REST

# Detalles de Modificaciones por Caso de Uso

## CU-10: Sincronizar Catálogo de Países y Ligas
**Objetivo**: Poblar el catálogo base de países y ligas desde fuentes externas #1 y #5.

### Cambios Necesarios:
1. **Entrada**: Sin cambios significativos en los DTOs de entrada (sigue consumiendo fuentes #1 y #5)
2. **Lógica interna**:
   - Al procesar source #1 (paises): Sin cambios (crea/actualiza Pais)
   - Al procesar source #5 (ligas por país):
     - Para cada registro LigaFuente:
       - Buscar Liga existente por `nombre` y `pais` (identidad de competencia)
       - Si no existe: crear nueva Liga(estado=BORRADOR)
       - Crear nueva Temporada con:
         - `nombre` = LigaFuente.nombre_torneo
         - `semestre` = parsear de LigaFuente semestre si existe, sino derivar de contexto
         - `anioInicio` y `anioFin` = derivar de LigaFuente.anio (ej: anio y anio+1 según semestre)
         - `urlSoccerway` = LigaFuente.url_soccerway
         - `apiId` = LigaFuente.api_id si existe
       - Añadir la Temporada a la Liga
       - Persistir Liga (si es nueva) y Temporada
3. **Salida**: Sin cambios en DTOs de salida (continúa devolviendo Pais/Liga básicos)
4. **Regla de negocio**: Mantener que solo se crean ligas en BORRADOR; la activación queda para CU-04

### DTOs Afectados:
- `LigaFuente` (application.dto): Añadir campos `nombre_torneo`, `semestre` si no existen ya
- No se requieren nuevos DTOs, pero sí actualizar los existentes para mapear correctamente los campos de source #5

## CU-04: Activar Liga
**Objetivo**: Activar una liga (o temporada específica) cuando sus fuentes de datos están configuradas y operativas (APLICA BR-001).

### Cambios Necesarios:
1. **Entrada**: 
   - Modificar `ActivarLigaComando` para incluir identificador de temporada:
     ```java
     public class ActivarLigaComando {
         private UUID ligaId;           // Opcional, para compatibilidad
         private UUID temporadaId;      // Nuevo: temporada específica a activar
         private String urlPosiciones;  // URL para fuente de posiciones
         private String urlCalendario;  // URL para fuente de calendario
         private String urlCuotas;      // URL para fuente de cuotas
         // getters y setters
     }
     ```
   - Alternativa: Mantener solo `ligaId` y inferir temporada activa/más reciente (menos preciso)
2. **Lógica interna**:
   - Si se especifica `temporadaId`:
     - Obtener la Temporada específica
     - Validar que pertenece a la Liga indicada (si se proporciona ligaId)
     - Procesar las 3 URLs para crear/actualizar DetalleFuenteExtraccion para esa temporada
     - Validar BR-001 para esa temporada específica
     - Activar solo esa temporada (cambiar su estado a ACTIVA)
   - Si solo se especifica `ligaId` (modo legado):
     - Obtener todas las temporadas de la liga
     - Para cada temporada que tenga las 3 URLs configuradas:
       - Procesar las URLs para esa temporada
       - Validar BR-001 y activar si corresponde
     - Emitir evento LigaActivada por cada temporada activada
3. **Salida**: Sin cambios significativos (continúa devolviendo respuesta vacía o básica)
4. **Eventos**: 
   - Nuevo: `TemporadaActivada` (opcional, para granularidad)
   - Existente: `LigaActivada` se mantiene para compatibilidad

### DTOs Afectados:
- `ActivarLigaComando` (application.dto): Añadir campo `temporadaId`

## CU-01: Sincronizar Tabla de Posiciones
**Objetivo**: Actualizar la tabla de posiciones de una liga desde la fuente de posiciones (APLICA BR-002 y BR-008).

### Cambios Necesarios:
1. **Entrada**: 
   - El trabajo programado o administrador debe especificar para qué temporada sincronizar
   - O bien, el caso de uso infiere la temporada activa/más reciente de la liga
2. **Lógica interna**:
   - Obtener la Liga (por ID u otro criterio)
   - Determinar la Temporada objetivo:
     - Si se proporciona explícitamente: usar esa temporada
     - Si no: buscar la temporada ACTIVA, o si ninguna está activa, la más reciente por fecha
   - Validar que la temporada esté ACTIVA (BR-002: no extraer para ligas inactivas)
   - Consultar posiciones al ProveedorPosiciones para esa temporada específica
   - Mapear a PosicionTabla
   - Reemplazar la tabla de posiciones de esa temporada (no de la liga directamente)
3. **Salida**: Sin cambios
4. **Reglas de negocio**: 
   - BR-002 se aplica por temporada: "No se extrae para ligas inactivas" → interpreta como "No se extrae para temporadas inactivas"
   - BR-008 se mantiene igual (validación interna de PosicionTabla VO)

### Dependencias:
- Puede requerir nuevo método en ProveedorPosiciones: `obtenerPosicionesPorTemporada(UUID temporadaId)`
- O bien, el adapter ya tiene implícitamente la temporada a través de su configuración

## CU-02: Sincronizar Calendario de Partidos
**Objetivo**: Sincronizar el calendario de partidos de una liga desde la fuente de calendario.

### Cambios Necesarios:
1. **Entrada**: 
   - Especificar temporada para la cual sincronizar calendario
2. **Lógica interna**:
   - Obtener la Temporada objetivo (similar a CU-01: explícita o inferida)
   - Consultar calendario al ProveedorCalendario para esa temporada
   - Crear/actualizar Partido(s) asociándolos a esa temporada (no a la liga directamente)
   - Para cada partido nuevo: emitir evento PartidoProgramado
3. **Salida**: Sin cambios
4. **Consideraciones**:
   - El campo `jornada` en Partido sigue siendo pertinente (indica posición dentro de la temporada)
   - Los partidos pertenecen explícitamente a una temporada ahora

## CU-03: Sincronizar Cuotas
**Objetivo**: Sincronizar cuotas de partidos desde la fuente de odds.

### Cambios Necesarios:
1. **Entrada**: 
   - Especificar liga y opcionalmente rango de fechas/temporada
   - Mejor: especificar temporada para cuya fechas sincronizar cuotas
2. **Lógica interna**:
   - Obtener Temporada objetivo
   - Obtener partidos próximos de esa temporada (fecha >= ahora)
   - Consultar cuotas al ProveedorCuotas para esos partidos
   - Actualizar Cuota(s) de cada partido
   - Emitir evento CuotaActualizada por partido actualizado
3. **Salida**: Sin cambios
4. **Ventaja**: 
   - Ahora podemos obtener precisamente los partidos de una temporada específica (ej: solo Apertura 2026)
   - Evita obtener cuotas de partidos de otras temporadas mezcladas

## Consideraciones Generales para Todos los Casos de Uso

### Estrategia de Inferencia de Temporada
Cuando no se especifica explícitamente una temporada:
1. **Preferencia 1**: Temporada ACTIVA (si exactamente una está activa)
2. **Preferencia 2**: Temporada más reciente por fecha de inicio (si ninguna está activa o hay múltiples activas)
3. **Preferencia 3**: Lanzar excepción si no se puede determinar unambiguamente (forzando especificación explícita en contexto de trabajo programado)

### Actualización de Repositorios y Puertos
- `LigaRepository`: 
  - Añadir método `findByNombreAndPais(String nombre, String pais)` para CU-10
  - Mantener métodos existentes por ID
- Se podría crear `TemporadaRepository` para operaciones específicas de temporada
- Los métodos existentes de LigaRepository que retornaban una sola instancia ahora podrían retornar múltiples o requerir especificación de temporada

### Impacto en DTOs de Respuesta
Para mantener compatibilidad con frontend existente:
- Los DTOs de respuesta de liga pueden incluir:
  - `temporadaActiva`: DTO básico de la temporada activa o más reciente
  - `temporadas`: Lista de todas las temporadas (opcional, para uso administrativo)
- Nuevos endpoints específicos de temporada pueden devolver datos más detallados

# Implicaciones en la Arquitectura

## Capa de Application
- Los casos de uso ahora operan con un nivel de granularidad más fino (temporada vs liga)
- Esto mejora la alineación con el ubiquitous language del dominio
- Los casos de uso se vuelven más precisos en su intención operacional

## Capa de Interfaces (Controllers)
- Los endpoints REST pueden necesitar actualización:
  - Nuevos: `GET /api/v1/temporadas/{temporadaId}/partidos`
  - Modificados: `GET /api/v1/ligas/{ligaId}/partidos?temporadaId={id}` (manteniendo el antiguo por compatibilidad)
- Se recomienda mantener los endpoints existentes por compatibilidad con frontend #1, añadiendo nuevos endpoints más precisos

## Capa de Domain
- Refuerza el modelo de dominio al hacer explícito el concepto de temporada/torneo
- El aggregate Liga ahora representa correctamente la competencia deportiva
- El aggregate Temporada representa una instancia específica de competencia con su propio estado

# Pruebas Requeridas

## Tests de Unidad
- Tests de cada caso de uso verificando:
  - Operación correcta cuando se especifica temporada explícitamente
  - Inferencia correcta de temporada cuando no se especifica
  - Manejo adecuado de casos edge (liga sin temporadas, múltiples temporadas activas, etc.)

## Tests de Integración
- Escenarios end-to-end:
  - CU-10 crea liga con dos temporadas (Apertura y Clausura 2026)
  - CU-04 activa solo la temporada Apertura
  - CU-01 sincroniza posiciones solo para Apertura
  - CU-02 crea partidos solo para Apertura
  - Verificar que Clausura permanece inactiva y sin datos
  - Repetir proceso para Clausura

## Tests de Regresión
- Verificar que funcionalidad existente para ligas con una sola temporada sigue funcionando
- Especialmente importante para ligas europeas (una temporada por año)

Esta especificación proporciona el detalle necesario para implementar los cambios en los casos de uso manteniendo la trazabilidad con los requisitos originales y el diseño del sistema.