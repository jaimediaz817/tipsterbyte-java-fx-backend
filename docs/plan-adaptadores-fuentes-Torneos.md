# Plan de Actualización de Adaptadores de Fuentes para Soporte de Múltiples Temporadas

## [QUÉ]: 
Detailar las modificaciones necesarias en los adapters de fuentes externas (FlashscorePosicionesAdapter, SoccerwayCalendarioAdapter, WplayCuotasAdapter) para que operen correctamente con el nuevo modelo donde las entidades de dominio (PosicionTabla, Partido, Cuota) están asociadas a Temporadas específicas en lugar de Ligas directamente.

## [POR QUÉ]: 
Los adaptadores actuales asumen que están obteniendo datos para una liga específica. Con el nuevo modelo donde una liga puede tener múltiples temporadas, es necesario asegurar que los datos obtenidos de las fuentes externas se asocien correctamente a la temporada apropiada, no solo a la liga.

## [ALTERNATIVAS]: 
1. Mantener los adaptadores tal como están y manejar la asociación temporada-liga en la capa de application (descartado porque viola la separación de responsabilidades y hace que los adapters devuelvan datos sin contexto suficiente)
2. Modificar los adaptadores para recibir temporada_id como parámetro y usarlo para obtener la URL correcta y validar el estado (enfoque elegido)
3. Crear versiones nuevas de los adaptadores específicamente para temporadas mientras se mantienen las versiones antiguas (descartado porque genera duplicación innecesaria)

## [RELACIONES]: 
- Implementa los cambios en los casos de uso CU-01, CU-02, CU-03
- Dependiente de los cambios en infraestructura (nuevas columnas temporada_id en tablas)
- Afecta la configuración de los beans de los adaptadores
- Relacionado con los DTOs de entrada/salida de los casos de uso

# Detalles de Modificaciones por Adapter

## 1. FlashscorePosicionesAdapter (Implementa ProveedorPosiciones)
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/adapters/FlashscorePosicionesAdapter.java`

### Cambios Necesarios:

#### 1.1 Modificación de la Interfaz ProveedorPosiciones
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/application/port/ProveedorPosiciones.java`

```java
public interface ProveedorPosiciones {
    /**
     * Obtiene la tabla de posiciones para una temporada específica.
     * 
     * @param temporadaId Identificador de la temporada para la cual obtener posiciones
     * @return Lista inmutable de posiciones de la tabla
     * @throws DomainException si la temporada no existe, no está activa o no tiene fuente configurada
     */
    List<PosicionTabla> obtenerPosiciones(UUID temporadaId);
}
```

#### 1.2 Implementación en FlashscorePosicionesAdapter
```java
@Service
@Primary
@ConditionalOnProperty(value = "app.cache.enabled", havingValue = "false")
public class FlashscorePosicionesAdapter implements ProveedorPosiciones {

    private final ObjectMapper objectMapper;
    private final DetalleFuenteExtraccionRepository detalleFuenteExtraccionRepository;
    private final TemporadaRepository temporadaRepository;
    private final WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(FlashscorePosicionesAdapter.class);

    public FlashscorePosicionesAdapter(
            ObjectMapper objectMapper,
            DetalleFuenteExtraccionRepository detalleFuenteExtraccionRepository,
            TemporadaRepository temporadaRepository,
            WebClient.Builder webClientBuilder) {
        this.objectMapper = objectMapper;
        this.detalleFuenteExtraccionRepository = detalleFuenteExtraccionRepository;
        this.temporadaRepository = temporadaRepository;
        this.webClient = webClientBuilder.build();
    }

    @Override
    @Cacheable(value = "posiciones", key = "#temporadaId", unless = "#result == null")
    public List<PosicionTabla> obtenerPosiciones(UUID temporadaId) {
        // 1. Validar que la temporada existe
        TemporadaEntity temporada = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new DomainException("Temporada no encontrada: " + temporadaId));
        
        // 2. Validar que la temporada esté activa (BR-002: no extraer para temporadas inactivas)
        if (temporada.getEstado() != EstadoTemporada.ACTIVA) {
            throw new DomainException("No se pueden obtener posiciones de una temporada inactiva: " + temporadaId);
        }
        
        // 3. Obtener el detalle de fuente de posiciones para esta temporada
        DetalleFuenteExtraccionEntity detalle = detalleFuenteExtraccionRepository
                .findByTemporadaIdAndTipo(temporadaId, TipoFuenteExtraccion.STANDINGS)
                .orElseThrow(() -> new DomainException(
                        "Fuente de posiciones (STANDINGS) no configurada para la temporada: " + temporadaId));
        
        String url = detalle.getUrl();
        if (url == null || url.isBlank()) {
            throw new DomainException("URL de fuente de posiciones vacía para la temporada: " + temporadaId);
        }
        
        // 4. Llamada a la API externa (Flashscore)
        try {
            String jsonResponse = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, clientResponse -> 
                            Mono.error(new DomainException("Error del cliente al acceder a Flashscore: " + clientResponse.statusCode())))
                    .onStatus(HttpStatus::is5xxServerError, clientResponse -> 
                            Mono.error(new DomainException("Error del servidor al acceder a Flashscore: " + clientResponse.statusCode())))
                    .bodyToMono(String.class)
                    .block();
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new DomainException("Respuesta vacía de Flashscore para la temporada: " + temporadaId);
            }
            
            // 5. Mapeo de JSON a PosicionTabla (lógica existente, sin cambios sustanciales)
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode posicionesNode = rootNode.path("positions"); // Ajustar según estructura real de Flashscore
            
            List<PosicionTabla> posiciones = new ArrayList<>();
            if (posicionesNode.isArray()) {
                for (JsonNode posicionNode : posicionesNode) {
                    // Extraer datos del JSON y crear PosicionTabla
                    // ... (lógica existente de mapeo) ...
                    
                    Equipo equipo = new Equipo(posicionNode.get("teamName").asText());
                    PosicionTabla posicion = new PosicionTabla(
                            equipo,
                            posicionNode.get("position").asInt(),
                            posicionNode.get("played").asInt(),
                            posicionNode.get("won").asInt(),
                            posicionNode.get("draw").asInt(),
                            posicionNode.get("lost").asInt(),
                            posicionNode.get("goalsFor").asInt(),
                            posicionNode.get("goalsAgainst").asInt(),
                            posicionNode.get("points").asInt(),
                            // últimos resultados si existen en la respuesta
                            extraerUltimosResultados(posicionNode)
                    );
                    posiciones.add(posicion);
                }
            }
            
            return Collections.unmodifiableList(posiciones);
            
        } catch (JsonProcessingException e) {
            throw new DomainException("Error al procesar JSON de Flashscore para la temporada " + temporadaId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DomainException("Error inesperado al obtener posiciones de Flashscore para la temporada " + temporadaId + ": " + e.getMessage(), e);
        }
    }
    
    // Método helper existente para extraer últimos resultados (sin cambios)
    private List<ResultadoReciente> extraerUltimosResultados(JsonNode posicionNode) {
        // ... implementación existente ...
    }
}
```

### 1.3 Consideraciones de Caché
- La anotación `@Cacheable` ahora usa `temporadaId` como clave en lugar de `ligaId`
- Esto asegura que las posiciones de cada temporada se cacheen por separado
- Tiempo de vida (TTL) de caché permanece igual

## 2. SoccerwayCalendarioAdapter (Implementa ProveedorCalendario)
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/adapters/SoccerwayCalendarioAdapter.java`

### Cambios Necesarios:

#### 2.1 Modificación de la Interfaz ProveedorCalendario
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/application/port/ProveedorCalendario.java`

```java
public interface ProveedorCalendario {
    /**
     * Obtiene el calendario de partidos para una temporada específica.
     * 
     * @param temporadaId Identificador de la temporada para la cual obtener calendario
     * @return Lista de partidos programados y jugados para la temporada
     * @throws DomainException si la temporada no existe, no está activa o no tiene fuente configurada
     */
    List<Partido> obtenerCalendario(Usuario temporadaId);
}
```

#### 2.2 Implementación en SoccerwayCalendarioAdapter
```java
@Service
@Primary
@ConditionalOnProperty(value = "app.cache.enabled", havingValue = "false")
public class SoccerwayCalendarioAdapter implements ProveedorCalendario {

    private final ObjectMapper objectMapper;
    private final DetalleFuenteExtraccionRepository detalleFuenteExtraccionRepository;
    private final TemporadaRepository temporadaRepository;
    private final PartidoRepository partidoRepository;
    private final EquipoRepository equipoRepository;
    private final WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(SoccerwayCalendarioAdapter.class);

    public SoccerwayCalendarioAdapter(
            ObjectMapper objectMapper,
            DetalleFuenteExtraccionRepository detalleFuenteExtraccionRepository,
            TemporadaRepository temporadaRepository,
            PartidoRepository partidoRepository,
            EquipoRepository equipoRepository,
            WebClient.Builder webClientBuilder) {
        this.objectMapper = objectMapper;
        this.detalleFuenteExtraccionRepository = detalleFuenteExtraccionRepository;
        this.temporadaRepository = temporadaRepository;
        this.partidoRepository = partidoRepository;
        this.equipoRepository = equipoRepository;
        this.webClient = webClientBuilder.build();
    }

    @Override
    @Cacheable(value = "calendario", key = "#temporadaId", unless = "#result == null")
    public List<Partido> obtenerCalendario(UUID temporadaId) {
        // 1. Validar que la temporada existe
        TemporadaEntity temporada = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new DomainException("Temporada no encontrada: " + temporadaId));
        
        // 2. Validar que la temporada esté activa
        if (temporada.getEstado() != EstadoTemporada.ACTIVA) {
            throw new DomainException("No se puede obtener calendario de una temporada inactiva: " + temporadaId);
        }
        
        // 3. Obtener el detalle de fuente de calendario para esta temporada
        DetalleFuenteExtraccionEntity detalle = detalleFuenteExtraccionRepository
                .findByTemporadaIdAndTipo(temporadaId, TipoFuenteExtraccion.CALENDAR)
                .orElseThrow(() -> new DomainException(
                        "Fuente de calendario (CALENDAR) no configurada para la temporada: " + temporadaId));
        
        String url = detalle.getUrl();
        if (url == null || url.isBlank()) {
            throw new DomainException("URL de fuente de calendario vacía para la temporada: " + temporadaId);
        }
        
        // 4. Llamada a la API externa (Soccerway)
        try {
            String jsonResponse = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, clientResponse -> 
                            Mono.error(new DomainException("Error del cliente al acceder a Soccerway: " + clientResponse.statusCode())))
                    .onStatus(HttpStatus::is5xxServerError, clientResponse -> 
                            Mono.error(new DomainException("Error del servidor al acceder a Soccerway: " + clientResponse.statusCode())))
                    .bodyToMono(String.class)
                    .block();
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new DomainException("Respuesta vacía de Soccerway para la temporada: " + temporadaId);
            }
            
            // 5. Mapeo de JSON a Partido (lógica existente, pero ahora asociando a temporadaId)
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode partidosNode = rootNode.path("matches"); // Ajustar según estructura real
            
            List<Partido> partidos = new ArrayList<>();
            if (partidosNode.isArray()) {
                for (JsonNode partidoNode : partidosNode) {
                    // Extraer datos del JSON
                    // ... (lógica existente de extracción de equipos, fecha, etc.) ...
                    
                    // Obtener o crear equipos
                    Equipo equipoLocal = obtenerOEquipo(equipoRepository, partidoNode.get("homeTeam").asText());
                    Equipo equipoVisitante = obtenerOEquipo(equipoRepository, partidoNode.get("awayTeam").asText());
                    
                    // Crear partido programado
                    FechaProgramada fechaProgramada = new FechaProgramada(
                            partidoNode.get("date").as(LocalDateTime.class),
                            partidoNode.get("time").as(LocalTime.class));
                    
                    Integer jornada = partidoNode.has("round") ? 
                            partidoNode.get("round").asInt() : null;
                    
                    Partido partido = new Partido(
                            temporadaId,  // CAMBIO: Usar temporadaId en lugar de ligaId
                            equipoLocal,
                            equipoVisitante,
                            fechaProgramada,
                            jornada);
                    
                    // Si el partido tiene resultado, asignarlo
                    if (partidoNode.has("homeScore") && partidoNode.has("awayScore")) {
                        Integer golesLocal = partidoNode.get("homeScore").asInt();
                        Integer golesVisitante = partidoNode.get("awayScore").asInt();
                        Resultado resultado = new Resultado(golesLocal, golesVisitante);
                        partido.asignarResultado(resultado); // Esto cambiará estado a FINALIZADO internamente
                    }
                    
                    partidos.add(partido);
                }
            }
            
            return Collections.unmodifiableList(partidos);
            
        } catch (JsonProcessingException e) {
            throw new DomainException("Error al procesar JSON de Soccerway para la temporada " + temporadaId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DomainException("Error inesperado al obtener calendario de Soccerway para la temporada " + temporadaId + ": " + e.getMessage(), e);
        }
    }
    
    // Método helper existente para obtener o crear equipo (sin cambios sustanciales)
    private Equipo obtenerOEquipo(EquipoRepository equipoRepository, String nombreEquipo) {
        return equipoRepository.findByNombre(nombreEquipo)
                .map(EquipoEntity::toDomainObject) // Asumiendo método de conversión
                .orElseGet(() -> {
                    Equipo nuevoEquipo = new Equipo(nombreEquipo);
                    EquipoEntity entidad = EquipoEntity.fromDomainObject(nuevoEquipo);
                    EquipoEntity guardada = equipoRepository.save(entidad);
                    return guardada.toDomainObject();
                });
    }
}
```

### 2.3 Consideraciones Específicas
- El campo `jornada` en Partido se mantiene y se extrae de la respuesta de Soccerway (campo "round")
- Los partidos creados ahora tienen `temporadaId` establecido correctamente
- El método `obtenerOEquipo` sigue funcionando igual ya que los equipos pertenecen a la liga, no a la temporada específica

## 3. WplayCuotasAdapter (Implementa ProveedorCuotas)
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/infrastructure/adapters/WplayCuotasAdapter.java`

### Cambios Necesarios:

#### 3.1 Modificación de la Interfaz ProveedorCuotas
**Archivo**: `src/main/java/com/tipsterbyte/tipsterbytefxv2/application/port/ProveedorCuotas.java`

```java
public interface ProveedorCuotas {
    /**
     * Obtiene las cuotas para los partidos próximos de una temporada específica.
     * 
     * @param temporadaId Identificador de la temporada para la cual obtener cuotas
     * @return Mapeo de partidoId a lista de cuotas para ese partido
     * @throws DomainException si la temporada no existe, no está activa o no hay partidos próximos
     */
    Map<UUID, List<Cuota>> obtenerCuotasProximosPartidos(UUID temporadaId, int horasAnticipacion);
}
```

#### 3.2 Implementación en WplayCuotasAdapter
```java
@Service
@Primary
@ConditionalOnProperty(value = "app.cache.enabled", havingValue = "false")
public class WplayCuotasAdapter implements ProveedorCuotas {

    private final ObjectMapper objectMapper;
    private final DetalleFuenteExtraccionRepository detalleFuenteExtraccionRepository;
    private final TemporadaRepository temporadaRepository;
    private final PartidoRepository partidoRepository;
    private final WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(WplayCuotasAdapter.class);

    public WplayCuotasAdapter(
            ObjectMapper objectMapper,
            DetalleFuenteExtraccionRepository detalleFuenteExtraccionRepository,
            TemporadaRepository temporadaRepository,
            PartidoRepository partidoRepository,
            WebClient.Builder webClientBuilder) {
        this.objectMapper = objectMapper;
        this.detalleFuenteExtraccionRepository = detalleFuenteExtraccionRepository;
        this.temporadaRepository = temporadaRepository;
        this.partidoRepository = partidoRepository;
        this.webClient = webClientBuilder.build();
    }

    @Override
    @Cacheable(value = "cuotas", key = "#temporadaId + '-' + #horasAnticipacion", unless = "#result == null")
    public Map<UUID, List<Cuota>> obtenerCuotasProximosPartidos(UUID temporadaId, int horasAnticipacion) {
        // 1. Validar que la temporada existe
        TemporadaEntity temporada = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new DomainException("Temporada no encontrada: " + temporadaId));
        
        // 2. Validar que la temporada esté activa
        if (temporada.getEstado() != EstadoTemporada.ACTIVA) {
            throw new DomainException("No se pueden obtener cuotas de una temporada inactiva: " + temporadaId);
        }
        
        // 3. Obtener el detalle de fuente de cuotas para esta temporada
        DetalleFuenteExtraccionEntity detalle = detalleFuenteExtraccionRepository
                .findByTemporadaIdAndTipo(temporadaId, TipoFuenteExtraccion.ODDS_WPLAY)
                .orElseThrow(() -> new DomainException(
                        "Fuente de cuotas (ODDS_WPLAY) no configurada para la temporada: " + temporadaId));
        
        String url = detalle.getUrl();
        if (url == null || url.isBlank()) {
            throw new DomainException("URL de fuente de cuotas vacía para la temporada: " + temporadaId);
        }
        
        // 4. Obtener partidos próximos de esta temporada
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime hasta = ahora.plusHours(horasAnticipacion);
        
        List<PartidoEntity> partidosEntity = partidoRepository.findByTemporadaIdAndFechaHoraBetween(
                temporadaId, ahora, hasta);
        
        if (partidosEntity.isEmpty()) {
            return Collections.emptyMap(); // No hay partidos próximos, devolver mapa vacío
        }
        
        // 5. Extraer IDs de partidos para la llamada a la API
        List<UUID> partidoIds = partidosEntity.stream()
                .map(PartidoEntity::getId)
                .collect(Collectors.toList());
        
        // 6. Llamada a la API externa (Wplay)
        try {
            // Construir parámetros de consulta si la API los requiere
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("partidoIds", String.join(",", partidoIds.stream()
                            .map(UUID::toString)
                            .collect(Collectors.toList())));
            
            String jsonResponse = webClient.get()
                    .uri(uriBuilder.build().toUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, clientResponse -> 
                            Mono.error(new DomainException("Error del cliente al acceder a Wplay: " + clientResponse.statusCode())))
                    .onStatus(HttpStatus::is5xxServerError, clientResponse -> 
                            Mono.error(new DomainException("Error del servidor al acceder a Wplay: " + clientResponse.statusCode())))
                    .bodyToMono(String.class)
                    .block();
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new DomainException("Respuesta vacía de Wplay para la temporada: " + temporadaId);
            }
            
            // 7. Mapeo de JSON a Cuota(s)
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode cuotasNode = rootNode.path("odds"); // Ajustar según estructura real de Wplay
            
            Map<UUID, List<Cuota>> resultado = new HashMap<>();
            // Inicializar lista vacía para cada partido
            for (UUID partidoId : partidoIds) {
                resultado.put(partidoId, new ArrayList<>());
            }
            
            if (cuotasNode.isArray()) {
                for (JsonNode cuotaNode : cuotasNode) {
                    UUID partidoId = UUID.fromString(cuotaNode.get("matchId").asText());
                    String mercadoStr = cuotaNode.get("market").asText();
                    BigDecimal valor = cuotaNode.get("odds").decimalValue();
                    
                    // Validar partidoId pertenece a uno de nuestros partidos
                    if (!partidoIds.contains(partidoId)) {
                        logger.warn("Recibida cuota para partido desconocido {} en temporada {}", partidoId, temporadaId);
                        continue;
                    }
                    
                    Mercado mercado = Mercado.fromString(mercadoStr); // Asumiendo método existente
                    Cuota cuota = new Cuota(mercado, valor);
                    
                    resultado.get(partidoId).add(cuota);
                }
            }
            
            // Devolver listas inmutables
            Map<UUID, List<Cuota>> resultadoInmutable = new HashMap<>();
            for (Map.Entry<UUID, List<Cuota>> entry : resultado.entrySet()) {
                resultadoInmutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            
            return Collections.unmodifiableMap(resultadoInmutable);
            
        } catch (JsonProcessingException e) {
            throw new DomainException("Error al procesar JSON de Wplay para la temporada " + temporadaId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DomainException("Error inesperado al obtener cuotas de Wplay para la temporada " + temporadaId + ": " + e.getMessage(), e);
        }
    }
}
```

### 3.3 Consideraciones Específicas
- El método ahora filtra partidos por `temporadaId` antes de obtener cuotas
- Esto asegura que solo obtengamos cuotas para partidos de la temporada específica
- El mapeo de respuesta a Cuota permanece esencialmente igual, pero ahora asociamos correctamente a los partidos de la temporada

## 4. Configuración de Beans y Condicionales

No se requieren cambios en la configuración de Spring más allá de lo ya existente, ya que:
- Los adapters ya están anotados con `@Service` y `@Primary`
- Los `@ConditionalOnProperty` para caché ya existen y siguen siendo apropiados
- Los repositorios necesarios se inyectan mediante constructor (ya existente)

## 5. Manejo de Errores y Logging

### 5.1 Mejores Prácticas Implementadas
- Validación temprana de existencia y estado de temporada
- Mensajes de error específicos que incluyen el `temporadaId` para facilitar depuración
- Manejo de errores HTTP específicos (4xx vs 5xx)
- Manejo de excepciones de JSON parsing
- Logging de advertencias para situaciones anómalas (como cuotas para partidos desconocidos)

### 5.2 Estrategia de Fallback
- No se implementan fallbacks a otras fuentes ya que eso sería responsabilidad de la capa de application (caso de uso)
- Los adapters lanzan excepciones específicas que los casos de uso pueden manejar según las reglas de negocio

## 6. Testing de Adaptadores

### 6.1 Tests de Unidad
- Mockear dependencias (repositorios, webClient)
- Verificar:
  - Lanza excepción cuando temporada no existe
  - Lanza excepción cuando temporada no está activa
  - Lanza excepción cuando falta configuración de fuente
  - Llama correctamente a la API externa con la URL adecuada
  - Mapea correctamente la respuesta a objetos de dominio
  - Maneja correctamente respuestas vacías o malformadas
  - Usa correctamente la caché con temporadaId como clave

### 6.2 Tests de Integración (con Testcontainers o mocks servidores)
- Verificar flujo completo con base de datos real
- Probar diferentes estados de temporada (BORRADOR, ACTIVA, INACTIVA)
- Verificar asociación correcta de datos a temporada_id
- Probar invalidación de caché cuando se actualizan datos

## 7. Estimación de Esfuerzo por Adapter

### 7.1 FlashscorePosicionesAdapter: 1-2 días
### 7.2 SoccerwayCalendarioAdapter: 1-2 días  
### 7.3 WplayCuotasAdapter: 1-2 días
### **Total estimado: 3-6 días**

Este plan proporciona la especificación detallada necesaria para actualizar los adapters de fuentes externas afin que operen correctamente con el nuevo modelo de múltiples temporadas por liga, asegurando que los datos obtenidos de las fuentes externas se asocien apropiadamente a las temporadas específicas y manteniendo la separación de responsabilidades y la calidad del código.