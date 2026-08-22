# Estrategias de Compatibilidad Hacia Atrás para Frontend Angular #1

## [QUÉ]: 
Detailar estrategias para mantener la compatibilidad con el frontend Angular #1 existente durante y después de la implementación del soporte para múltiples temporadas por liga, minimizando el impacto en la aplicación cliente mientras se permiten las nuevas funcionalidades.

## [POR QUÉ]: 
El frontend Angular #1 consume los endpoints REST existentes y espera ciertos formatos de datos. Cambiar drastamente la API podría romper la funcionalidad existente. Es necesario proporcionar un camino de migración suave que permita que tanto la funcionalidad antigua como la nueva coexistan durante un período de transición.

## [ALTERNATIVAS]: 
1. Rompimiento intencional: Actualizar todos los endpoints y requerir actualización inmediata del frontend (descartado porque viola el principio de minimizar impacto y requeriría trabajo coordinado)
2. Endpoints completamente nuevos: Crear un nuevo conjunto de endpoints bajo `/api/v2/` mientras se mantienen los antiguos (descarta duplicación de esfuerzo y posible confusión)
3. **Enfoque elegido**: Mantener los endpoints existentes con comportamiento adaptativo, añadiendo nuevos endpoints más precisos, y usando versionado ligero mediante parámetros o encabezados cuando sea necesario

## [RELACIONES]: 
- Relacionado con todos los cambios en casos de uso e infraestructura
- Afecta directamente los controllers REST y su exposición de endpoints
- Impacta la experiencia del usuario del frontend Angular #1
- Debe coordinarse con los planes de testing y despliegue

# Estado Actual de la API (Antes del Refactor)

Basado en el código existente y los casos de uso, los endpoints relevantes probablemente incluyen:

```
GET    /api/v1/ligas/{ligaId}/posiciones
GET    /api/v1/ligas/{ligaId}/calendario
GET    /api/v1/ligas/{ligaId}/partidos/{fecha}  // o similar para filtros
GET    /api/v1/partidos/{partidoId}/cuotas
POST   /api/v1/ligas/{ligaId}/activar
GET    /api/v1/ligas  // listado básico
GET    /api/v1/ligas/{ligaId}  // detalle básico
```

Estos endpoints asumen una liga = una temporada y devuelven datos asociados directamente a la liga.

# Nuevas Capacidades Requeridas

Con el refactor, el frontend ideally debería poder:
1. Listar todas las temporadas de una liga
2. Obtener posiciones/calendario/cuotas para una temporada específica
3. Ver qué temporada está activa
4. Crear/activar temporadas específicas

# Estrategia de Compatibilidad Propuesta

## Principios Rectores
1. **No romper funcionalidad existente**: Todos los endpoints actuales deben seguir funcionando
2. **Comportamiento predecible**: Cuando haya ambigüedad, el comportamiento debe estar documentado y ser consistente
3. **Mejora progresiva**: Nuevas funcionalidades se añaden sin eliminar las antiguas
4. **Claridad en documentación**: Diferenciar claramente entre comportamiento antiguo y nuevo

## 1. Estrategia para Endpoints de Lectura (GET)

### 1.1 Endpoints de Posiciones
**Actual**: `GET /api/v1/ligas/{ligaId}/posiciones`
**Nuevo**: `GET /api/v1/temporadas/{temporadaId}/posiciones`

**Enfoque de Compatibilidad**:
- Mantener el endpoint existente `/api/v1/ligas/{ligaId}/posiciones`
- Cuando se llame a este endpoint:
  - Si la liga tiene exactamente una temporada: devolver posiciones de esa temporada (comportamiento idéntico al antes)
  - Si la liga tiene múltiples temporadas:
    - Si exactamente una temporada está ACTIVA: devolver posiciones de esa temporada
    - Si ninguna temporada está ACTIVA: devolver posiciones de la temporada más reciente por fecha de inicio
    - Si múltiples temporadas están ACTIVAS: lanzar error 400 (Bad Request) con mensaje explicativo que indique que se debe especificar temporada_id
- Añadir nuevo endpoint: `GET /api/v1/temporadas/{temporadaId}/posiciones` para acceso explícito y sin ambigüedad

### 1.2 Endpoints de Calendario/Partidos
**Actual**: `GET /api/v1/ligas/{ligaId}/partidos` (con parámetros de filtro opcionales)
**Nuevo**: `GET /api/v1/temporadas/{temporadaId}/partidos`

**Enfoque de Compatibilidad**:
- Mantener endpoint existente `/api/v1/ligas/{ligaId}/partidos`
- Cuando se llame a este endpoint:
  - Aplicar la misma lógica de resolución de temporada que para posiciones
  - Devolver partidos filtrados por la temporada resuelta
  - Aplicar los mismos parámetros de filtro (fecha, estado, etc.) después de filtrar por temporada
- Añadir nuevo endpoint: `GET /api/v1/temporadas/{temporadaId}/partidos`

### 1.3 Endpoint de Detalle de Liga
**Actual**: `GET /api/v1/ligas/{ligaId}`
**Mejoras para Compatibilidad**:
- Mantener endpoint existente sin cambios en estructura básica
- Enriquecer respuesta con información de temporadas:
```json
{
  "id": "liga-uuid",
  "nombre": "Liga Betplay",
  "pais": "Colombia",
  // ... campos existentes ...
  
  // Nuevos campos informativos (no rompen clientes existentes que ignoran campos desconocidos)
  "temporadaActiva": {
    "id": "temporada-uuid",
    "nombre": "Apertura 2026",
    "semestre": 1,
    "anioInicio": 2026,
    "anioFin": 2026,
    "estado": "ACTIVA"
  },
  "temporadas": [
    {
      "id": "temporada1-uuid",
      "nombre": "Apertura 2026",
      "semestre": 1,
      "anioInicio": 2026,
      "anioFin": 2026,
      "estado": "ACTIVA"
    },
    {
      "id": "temporada2-uuid",
      "nombre": "Clausura 2026",
      "semestre": 2,
      "anioInicio": 2026,
      "anioFin": 2026,
      "estado": "BORRADOR"
    }
  ]
}
```
- Clientes existentes que no usan los nuevos campos continuarán funcionando sin cambios
- Clientes nuevos pueden aprovechar la información adicional

### 1.4 Endpoint de Lista de Ligas
**Actual**: `GET /api/v1/ligas`
**Mejoras Similares**:
- Mantener respuesta básica idéntica
- Opcionalmente añadir información resumida de temporada por liga (ej: temporada activa o más reciente)
- Los clientes existentes ignorarán campos nuevos

## 2. Estrategia para Endpoints de Escritura (POST/PUT/PATCH)

### 2.1 Endpoint de Activación
**Actual**: `POST /api/v1/ligas/{ligaId}/activar` (con cuerpo que contiene las 3 URLs)
**Nuevo**: `POST /api/v1/temporadas/{temporadaId}/activar`

**Enfoque de Compatibilidad**:
- Mantener endpoint existente `/api/v1/ligas/{ligaId}/activar`
- Cuando se llame a este endpoint:
  - Obtener la liga especificada
  - Aplicar lógica de resolución de temporada (una activa, más reciente, etc.)
  - Si se resuelve exactamente una temporada: activar esa temporada con las URLs proporcionadas
  - Si hay ambigüedad (0 o >1 temporadas candidatas): lanzar error 400 explicando que se debe especificar temporada_id
  - Alternativa: permitir activar múltiples temporadas si se proporciona un flag o se deduce del contexto
- Añadir nuevo endpoint: `POST /api/v1/temporadas/{temporadaId}/activar` para activación explícita y sin ambigüedad
- El cuerpo de la petición permanece el mismo (las 3 URLs)

### 2.2 Otros Endpoints de Escritura
- Endpoints como registro de pronósticos, suscripciones, etc. no se ven directamente afectados por este refactor ya que operan en otros agregados
- Cualquier endpoint que actualmente tome ligaId y implícitamente opere en la "temporada actual" seguiría la misma lógica de resolución descrita anteriormente

## 3. Manejo de Ambigüedad y Errores

### 3.1 Respuesta de Error Estándar
Cuando se produzca ambigüedad en la resolución de temporada, devolver:
```json
{
  "timestamp": "2026-08-18T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Ambigüedad en resolución de temporada para liga X. La liga tiene 2 temporadas activas: [Apertura 2026, Clausura 2026]. Por favor use el endpoint específico de temporada o especifique temporada_id.",
  "path": "/api/v1/ligas/liga-uuid/posiciones"
}
```

### 3.2 Encabezado de Advertencia (Opcional)
Para casos donde se tome una decisión por defecto pero se quiere informar al cliente:
```
Warning: 299 - "Liga X tiene múltiples temporadas; se usó la más reciente (Apertura 2026) por defecto"
```

## 4. Versionado de API (Consideraciones)

### 4.1 Evitar Versionado Pesado
- No crear `/api/v2/` a menos que sea absolutamente necesario
- El enfoque de endpoints nuevos + compatibilidad en endpoints existentes es preferible para cambios evolutivos

### 4.2 Uso de Parámetros de Consulta (Alternativa)
Como alternativa a endpoints completamente nuevos:
- `GET /api/v1/ligas/{ligaId}/posiciones?temporadaId={id}`
- Pero esto hace que los endpoints existentes tengan comportamiento diferente según parámetros, lo cual puede ser menos RESTful
- El enfoque de nuevos endpoints separados es más limpio

## 5. Impacto Específico en Frontend Angular #1

### 5.1 Servicios Existentes
Servicios como `LigaService`, `PartidoService`, etc. que actualmente llaman a:
- `ligas/{id}/posiciones`
seguirán funcionando sin cambios, pero su comportamiento puede variar según el estado de las temporadas de la liga.

### 5.2 Nuevas Oportunidades para el Frontend
El frontend podría evolucionar para:
1. Mostrar un selector de temporada al ver una liga
2. Mostrar indicador visual de qué temporada está activa
3. Permitir al usuario seleccionar explícitamente una temporada para ver sus datos
4. Usar los nuevos endpoints específicos cuando se beneficie de la precisión

### 5.3 Recomendaciones para Evolución del Frontend
- **Corto plazo**: Ningún cambio requerido; la aplicación seguirá funcionando
- **Medio plazo**: 
  - Añadir manejo de errores 400 específicos para ambigüedad de temporada
  - Enriquecer UI para mostrar información de temporadas cuando esté disponible en respuestas de liga
  - Añadir selectores de temporada en vistas pertinentes
- **Largo plazo**: Migrar gradualmente a usar los nuevos endpoints específicos de temporada donde tenga sentido

## 6. Plan de Comunicación y Deprecación

### 6.1 Fase de Dual Soporte (Indefinida)
- Mantener ambos comportamientos (endpoint legado con resolución automática + endpoint específico) indefinidamente o hasta que se decida que el frontend ha migrado suficientemente

### 6.2 Deprecación Futura (Opcional)
Si en el futuro se decide eliminar la compatibilidad:
1. Anunciar deprecación con 3-6 meses de antelación
2. Añadir encabezado de deprecación a respuestas de endpoints legado:
   ```
   Deprecation: true
   Sunset: 2027-02-01
   Link: </api/v1/temporadas/{temporadaId}/posiciones>; rel="successor-version"
   ```
3. Finalmente eliminar los endpoints legado o cambiar su comportamiento a lanzar error 410 Gone

## 7. Ejemplos de Flujos de Trabajo

### 7.1 Liga con Una Temporada (Ligas Europeas Tradicionales)
- Frontend llama a `/api/v1/ligas/real-madrid/posiciones`
- Backend ve que la liga tiene exactamente una temporada
- Devuelve posiciones de esa temporada
- **Comportamiento idéntico al antes**: cero impacto

### 7.2 Liga con Múltiples Temporadas, Una Activa (Colombia Betplay Durante Torneo)
- Frontend llama a `/api/v1/ligas/betplay/posiciones`
- Backend ve que la liga tiene dos temporadas pero solo una está ACTIVA
- Devuelve posiciones de la temporada activa
- **Comportamiento intuitivo**: muestra el torneo actualmente en curso

### 7.3 Liga con Múltiples Temporadas, Ninguna Activa (Entre Temporadas)
- Frontend llama a `/api/v1/ligas/betplay/posiciones`
- Backend ve que la liga tiene dos temporadas, ninguna ACTIVA
- Devuelve posiciones de la temporada más reciente
- **Comportamiento razonable**: muestra el torneo más recientemente completado

### 7.4 Liga con Múltiples Temporadas, Varias Activas (Error de Configuración)
- Frontend llama a `/api/v1/ligas/betplay/posiciones`
- Backend ve que la liga tiene múltiples temporadas activas
- Devuelve error 400 con mensaje explicativo
- **Frontend debe**: mostrar error al usuario y/o proporcionar UI para seleccionar temporada

### 7.5 Uso Explícito de Nuevo Endpoint
- Frontend llama a `/api/v1/temporadas/apertura-2026-id/posiciones`
- Backend devuelve posiciones de esa temporada específica
- **Comportamiento preciso**: garantiza que se obtiene lo solicitado

## 8. Estimación de Esfuerzo

### 8.1 Modificaciones en Controllers: 1-2 días
### 8.2 Enriquecimiento de DTOs de Respuesta: 1 día
### 8.3 Lógica de Resolución de Temporada (reutilizable): 1 día
### 8.4 Manejo de Errores y Respuestas: 0.5 días
### **Total estimado: 3.5 días**

Esta estrategia proporciona un camino claro para mantener la compatibilidad con el frontend Angular #1 existente mientras se habilita la nueva funcionalidad de múltiples temporadas por liga. El enfoque minimiza el riesgo de romper funcionalidad existente mientras proporciona una evolución clara hacia un modelo más preciso y explícito.