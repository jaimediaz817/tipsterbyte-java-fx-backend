# Historias de Usuario — tipsterbyte-fx-v2

> Complemento a `casos-de-uso.md`. Las HU responden *qué* y *por qué* desde la perspectiva del actor; los casos de uso responden *cómo*. Cada HU tiene trazabilidad hacia su caso de uso.

---

## Formato

```
Como [actor],
quiero [capacidad],
para [beneficio/valor].
```

**Criterios de aceptación (AC)**: condiciones verificables que deben cumplirse para considerar la HU terminada.

---

## Épicas

| Épica | HU asociadas |
| --- | --- |
| Ingesta de datos deportivos | HU-01 a HU-04 |
| Gestión de partidos | HU-05 |
| Pronósticos | HU-06 a HU-08, HU-16 |
| Suscripciones | HU-09 |
| Orquestación de extracción (Robots) | HU-13 |
| Programación de extracciones | HU-14, HU-15 |

---

## HU-01 — Sincronizar tabla de posiciones

**Como** administrador, **quiero** sincronizar la tabla de posiciones de una liga activa, **para** que los pronósticos se basen en datos actualizados.

**Criterios de aceptación**:
- AC1: La sincronización se dispara por job programado o manualmente.
- AC2: Los datos provienen de un adapter de fuente de posiciones (football-data.org o API-Football).
- AC3: Las posiciones guardadas respetan consistencia aritmética (BR-008).
- AC4: Se registra el resultado de la extracción (éxito/fallo).

**Trazabilidad**: → CU-01 → `ProveedorPosiciones` + `LigaRepository`

---

## HU-02 — Sincronizar calendario de partidos

**Como** administrador, **quiero** sincronizar el calendario de partidos jugados y pendientes de una liga, **para** tener partidos programados y resultados históricos.

**Criterios de aceptación**:
- AC1: Se crean partidos nuevos en estado `PROGRAMADO`.
- AC2: Se actualizan resultados de partidos ya jugados.
- AC3: Cada partido nuevo emite `PartidoProgramado`.

**Trazabilidad**: → CU-02 → `ProveedorCalendario` + `PartidoRepository`

---

## HU-03 — Sincronizar cuotas

**Como** administrador, **quiero** sincronizar las cuotas de los partidos próximos de una liga, **para** que los tipsters publiquen pronósticos con odds vigentes.

**Criterios de aceptación**:
- AC1: Se actualizan cuotas solo de partidos `PROGRAMADO` o `EN_VIVO`.
- AC2: Cuotas inválidas (≤ 1.0) se descartan (BR-007).
- AC3: Se emite `CuotaActualizada` por cada partido actualizado.

**Trazabilidad**: → CU-03 → `ProveedorCuotas` + `PartidoRepository`

---

## HU-04 — Activar liga

**Como** administrador, **quiero** activar una liga solo cuando sus fuentes de datos estén operativas, **para** no iniciar extracción de datos incompletos.

**Criterios de aceptación**:
- AC1: La activación valida que las fuentes de la liga estén configuradas (BR-001).
- AC2: Si falta alguna fuente, la liga permanece `INACTIVA` con mensaje claro.
- AC3: Al activarse se emite `LigaActivada`.

**Trazabilidad**: → CU-04 → `LigaRepository`

---

## HU-05 — Registrar resultado de partido

**Como** administrador, **quiero** registrar el resultado final de un partido, **para** marcar el partido como `FINALIZADO` y permitir verificar pronósticos.

**Criterios de aceptación**:
- AC1: Solo se asigna resultado a partidos `EN_VIVO` o `PROGRAMADO` ya jugados (BR-003).
- AC2: Una vez `FINALIZADO`, el resultado no se modifica.

**Trazabilidad**: → CU-05 → `PartidoRepository`

---

## HU-06 — Crear pronóstico

**Como** tipster, **quiero** crear un pronóstico en borrador, **para** prepararlo sin exponerlo a los clientes.

**Criterios de aceptación**:
- AC1: El partido debe estar `PROGRAMADO` o `EN_VIVO` (BR-004).
- AC2: La cuota seleccionada es vigente y > 1.0 (BR-007).
- AC3: El pronóstico nace en estado `BORRADOR`.

**Trazabilidad**: → CU-06 → `PronosticoRepository` + `PartidoRepository`

---

## HU-07 — Publicar pronóstico

**Como** tipster, **quiero** publicar un pronóstico, **para** que mis suscriptores puedan verlo.

**Criterios de aceptación**:
- AC1: Solo se publica un pronóstico en `BORRADOR` (BR-005).
- AC2: Al publicarse se emite `PronosticoPublicado`.
- AC3: Un pronóstico publicado no se edita; solo se anula.

**Trazabilidad**: → CU-07 → `PronosticoRepository`

---

## HU-08 — Consultar pronósticos por liga y fecha

**Como** cliente, **quiero** consultar los pronósticos publicados por mis tipsters suscritos según liga y fecha, **para** decidir mis apuestas.

**Criterios de aceptación**:
- AC1: Solo se muestran pronósticos de tipsters con suscripción activa (BR-006).
- AC2: Solo se muestran pronósticos `PUBLICADO`.
- AC3: La respuesta incluye mercado, selección y cuota.

**Trazabilidad**: → CU-08 → `PronosticoRepository` + `SuscripcionRepository`

---

## HU-09 — Crear suscripción

**Como** cliente, **quiero** suscribirme a un tipster con un plan, **para** acceder a sus pronósticos.

**Criterios de aceptación**:
- AC1: Se crea una suscripción `ACTIVA` con fechas y plan.
- AC2: Se emite `SuscripcionCreada`.
- AC3: La suscripción expira al llegar `fechaFin`.

**Trazabilidad**: → CU-09 → `SuscripcionRepository`

---

## HU-10 — Configurar países de interés

**Como** SUPERADMIN/TIPSTER, **quiero** marcar los países preferidos con su orden y un límite opcional de ligas por país (`maxLigasPorPais`), **para** que el poblamiento geográfico los procese primero y acote cuántas ligas extrae de cada uno.

**Criterios de aceptación**:
- AC1: Alta/lista/eliminación/reemplazo en bloque vía `/api/v1/paises-interes` (upsert conserva prioridad).
- AC2: Solo se aceptan países presentes en la fuente #1.
- AC3: CU-10 procesa primero estos países (por prioridad) y aplica su límite en la fuente #5 y localmente; el resto del mundo nunca se omite.

**Trazabilidad**: → CU-14 → `PaisInteresRepository` + `ProveedorPaises` (+ `CacheLecturas` para `/paises/disponibles`)

---

## HU-11 — Poblar equipos de cada liga del catálogo (fuente #6)

**Como** SUPERADMIN/TIPSTER, **quiero** que, durante el poblamiento geográfico y tras crear las ligas y sus temporadas de cada país, el sistema consuma la fuente #6 (`ext-soccerway-teams-by-league`) **país por país y liga por liga**, **para** que cada temporada quede con su plantilla oficial de equipos (nombre + escudo) antes de activar las fuentes operativas (#2/#3/#4).

**Criterios de aceptación**:
- AC1: Por cada liga procesada se consulta `#6` con `country_name = liga.pais()` y `league_name = liga.nombre()`; los equipos se asignan a la **temporada vigente** (activa o primera registrada).
- AC2: Matching por nombre **normalizado** (sin tildes + trim + case-insensitive): si el equipo ya existe en la plantilla se reutiliza su id (y se actualiza `logo_url` si cambió); si no existe, se crea.
- AC3: El resultado es idempotente: re-ejecutar el poblamiento no duplica equipos ni ligas.
- AC4: La consulta a `#6` usa cache-aside Redis (clave país+liga, TTL largo); el caso de uso invalida la clave antes de consultar.
- AC5: Un fallo al poblar los equipos de una liga NO aborta el poblamiento: se registra (log MDC con contexto país+liga) y se continúa con la siguiente.
- AC6: Los equipos quedan persistidos en `equipos (id, nombre, logo_url, temporada_id)` referenciando su temporada.

**Trazabilidad**: → CU-10 (encadenado post-catalogación) → `ProveedorEquiposPorLiga` + `LigaRepository` + `CacheLecturas`

---

## HU-12 — Poblamiento granular del catálogo (países y ligas por país)

**Como** SUPERADMIN, **quiero** poblar el catálogo por pasos (primero países, luego ligas de un país concreto vía `isoAlpha2`), **para** entender y controlar cada etapa del flujo sin ejecutar el recorrido completo mundial.

**Criterios de aceptación**:
- AC1: `POST /api/v1/catalogo/poblar-paises` pobla **solo países** desde la fuente #1 (`ProveedorPaises`): persiste los nuevos por `isoAlpha2`, es idempotente, responde `200 OK` síncrono con `{totalPaises, nuevos}` y actualiza `GET /catalogo/estado`.
- AC2: `POST /api/v1/catalogo/poblar-ligas/{isoAlpha2}` pobla **solo ligas de ese país** desde la fuente #5 (`ProveedorLigasPorPais` con `country_name` + `limit` derivado de `maxLigasPorPais`), crea `Liga` en `BORRADOR` + `Temporada` PLANIFICADA (`anio`) + `pais_id` FK; si el país es de interés, además puebla equipos vía `#6`.
- AC3: `POST /poblar-ligas/{isoAlpha2}` es **async 202** (`executionId` + `urlEstado` `/api/v1/catalogo/activar/{executionId}` reutilizando el polling de FASE T3 - `TareaLog` RUNNING/SUCCESS/ERROR), con anti-solapamiento por país (dos corridas del mismo `isoAlpha2` → `409`).
- AC4: Validación: `isoAlpha2` requerido (2 letras, case-insensitive, normalizado a upper); `404/422` si el país no existe en `paises` tras `poblar-paises`, o si la fuente #5 no devuelve ligas (no es error, `totalLigas=0`).
- AC5: Requiere rol `SUPERADMIN` (403 resto), y `GET /catalogo/estado` refleja los conteos tras cada paso.

**Trazabilidad**: → CU-17 `SincronizarPaisesUseCase` (sync) + CU-18 `SincronizarLigasPorPaisUseCase`/`SincronizarLigasPorPaisAsyncUseCase` (async) → `ProveedorPaises`/`ProveedorLigasPorPais`/`ProveedorEquiposPorLiga` + `PaisRepository` + `LigaRepository` + `PaisInteresRepository` + `CacheLecturas` + `TareaLogRepository`/`ProgresoPoblamiento`

---

## HU-13 — Orquestación de Robots de extracción con semaforización y visibilidad en tiempo real

> **Concepto de Robot**: un *robot* es un **trabajador persistente por fuente** (uno por `TipoFuenteExtraccion` operativo). NO hay un robot por liga: con 15 ligas activas siguen siendo **3-4 robots en total**, y cada robot procesa una **cola interna de tareas** (una tarea por liga, alimentada por las `TareaProgramada` existentes). El robot barre su cola liga por liga, lo que garantiza de raíz que el scraper Python jamás recibe martilleo de una misma fuente.

| Robot | Fuente scraper | TipoFuenteExtraccion | Qué extrae (su cola) | Cadencia del ciclo |
| --- | --- | --- | --- | --- |
| **RobotCuotas** | #2 Wplay (`ext-next-matches-wplay-by-league`) | `ODDS_WPLAY` | Cuotas de partidos `PROGRAMADO` próximos de cada liga activa | Frecuente (ej: cada 5-10 min); cola priorizada por **proximidad de kickoff** (ligas con partidos en las próximas 2 h van primero) |
| **RobotPosiciones** | #3 Flashscore (`ext-position-table-by-league-stable`) | `STANDINGS` | Tabla de posiciones + últimos 5 resultados de cada torneo ACTIVO | Moderada (ej: cada 30-60 min) |
| **RobotCalendario** | #4 Soccerway (`ext-calendar-league-by-league-v2`) | `CALENDAR` | Calendario + resultados de encuentros finalizados | Moderada (ej: cada 30-60 min); las ligas con partido recién `FINALIZADO` saltan al frente de la cola |
| *(RobotEquipos)* | #6 Soccerway (`ext-soccerway-teams-by-league`) | `EQUIPOS` | Plantillas de equipos (solo poblamiento, no operativo continuo) | Bajo demanda (HU-11/HU-12) |

**Como** SUPERADMIN/TIPSTER, **quiero** pocos robots persistentes (uno por fuente) que barran colas de tareas de las ligas activas con concurrencia acotada y reintentos, **para** escalar a decenas de ligas sin saturar el scraper Python ni perder corridas, sabiendo en todo momento desde el frontend qué robot está corriendo, en qué liga va y cuál falló.

**Criterios de aceptación**:

> Patrón explícito (a registrar como ADR): *Worker Pool especializado por dominio* — un worker por fuente con cola de prioridad y semáforo de techo global. Se descartan alternativas: pool genérico central (pierde la cortesía/fairness por fuente), Akka (dependencia pesada para escala que no tenemos), virtual threads + channel (el cuello de botella es el proceso Chrome externo, no el hilo JVM — el `Semaphore` captura ese límite físico), Quartz (reemplazaría un dispatcher propio ya probado; ShedLock llega en FASE 15 si algún día hay multi-instancia).

- AC1 — **Un robot por fuente, componente aparte del dispatcher**: exactamente un trabajador por `tipoFuente` operativo. La separación de responsabilidades es estricta: `CatalogoScheduler` (existente) sigue siendo el **scheduling** (qué debe correr según cron/prioridad) y el nuevo orquestador CU-19 es la **orquestación de ejecución** (cómo se barre la cola con concurrencia controlada), con su **propio ciclo `@Scheduled` independiente** y su propio guard anti-solapamiento (no compartido con el dispatcher). Su cola se construye al inicio de cada ciclo leyendo las `TareaProgramada` activas elegibles (consulta; no modifica el mecanismo de disparo). Desactivar una tarea = esa liga sale de la cola.
- AC2 — **Cadencia diferenciada por robot + arranque escalonado**: cada robot tiene su propio intervalo de ciclo (`app.extraccion.robot.{fuente}.intervalo`): RobotCuotas frecuente (5-10 min) porque las cuotas cambian cerca del kickoff; RobotPosiciones/RobotCalendario moderado (30-60 min). Barrido secuencial con k workers internos configurables (`app.extraccion.robot.{fuente}.workers`, default 1, máx 2). El primer ciclo tras el boot se retrasa con un delay escalonado por robot (`jitter` configurable, ej: 0-60 s) para evitar manada trueno contra el scraper recién levantado.
- AC3 — **Prioridad por proximidad de kickoff (RobotCuotas)**: dentro de su cola, las ligas con partidos en las próximas horas van primero. Esto sustituye ventanas horarias codificadas: la urgencia emerge del ordenamiento orgánico de la cola.
- AC4 — **Techo global compartido por TODO consumidor del scraper**: máx M scrapes simultáneos totales (configurable, default 3) aplicado no solo a los robots sino también a los flujos manuales que consumen el scraper (poblamiento granular HU-12, catálogo T3): un SUPERADMIN lanzando un poblamiento mundial mientras los robots barren NO debe poder superar el techo de Chromes. Puerto `SemafaroExtraccion` (application.port) con adapter en memoria (`java.util.concurrent.Semaphore`); los flujos manuales lo adquieren vía decorador sobre sus adapters (#1/#5/#6).
- AC5 — **Anti-solapamiento**: (a) un robot no inicia nuevo ciclo si el anterior vive (WARN, no error); (b) una misma liga no puede estar dos veces en su cola en un mismo ciclo.
- AC6 — **Política única de reintentos + timeout duro por tarea**: la cortesía H-06 existente DENTRO del adapter es la única capa de reintento HTTP (pausa + backoff); el robot **no reintenta tareas**: si una liga falla tras agotar la cortesía, se registra `FALLO` en `tarea_log` y el worker continúa con la siguiente (sin multiplicar intentos 3×3=9). Además toda tarea tiene **timeout duro** (default 8 min > read-timeout HTTP de 6 min): excedido → `FALLO("timeout")` y el worker queda libre para la siguiente (protege contra scrapes colgados tipo Chrome zombi). El circuit breaker por robot se mantiene: si acumula **N tareas consecutivas fallidas por conexión al scraper** (infraestructura, NO scraping en sí; default N=3), entra en estado `CIRCUITO_ABIERTO` y pausa su ciclo un tiempo fijo (default 5 min), sin arrastrar a otros robots.
- AC7 — **Elegibilidad completa**: solo entran a la cola tareas `activa=true` de ligas que cumplan los tres filtros: temporada vigente, fuente operativa configurada (`DetalleFuenteExtraccion` activa para ese `tipoFuente` en la temporada — BR-001: sin URL no hay `path_to_scrape`) y filtro propio del robot (ej: RobotCuotas exige partidos `PROGRAMADO` próximos; prioridad kickoff según AC3).
- AC8 — **Prioridad dinámica vía evento respetando la Dependency Rule**: cuando un partido pasa a `FINALIZADO`, el evento de dominio se publica como application event interno (`ApplicationEventPublisher`); un listener dedicado llama al puerto correspondiente para insertar al frente de la cola del RobotCalendario. Inserción directa en memoria (no flag en BD: es señal de baja durabilidad y alta reactividad; el barrido base la cubriría igual tras un reinicio). Domain nunca toca colas directamente.
- AC9 — **Visibilidad total (frontend)**: `GET /api/v1/robots/estado` (roles SUPERADMIN/TIPSTER, polling ~5s):
  - Semáforos: uso/capacidad del techo global, workers activos por robot.
  - Robots (3-4 filas legibles de un vistazo): fuente, estado (`IDLE|CORRIENDO|EXITO_CICLO|FALLO_PARCIAL|CIRCUITO_ABIERTO`), progreso del ciclo (`ligaActual`, `ligasProcesadas`, `ligasTotales`, `porcentajeAvance`), resultado/duración de última corrida por liga, tamaño de cola pendiente y, si está en circuito abierto, minutos hasta el reintento.
  - Estado vivo en memoria (snapshot por robot); historia en `tarea_log`. Cero migraciones nuevas.
- AC10 — **Barrido de huérfanos al arranque**: en `ApplicationReadyEvent` (o equivalente), el orquestador marca como `ERROR` ("interrumpido por reinicio") toda fila `tarea_log` en `RUNNING` cuyo ciclo/robot ya no esté vivo. Evita registros RUNNING zombis eternos (gap real detectado: reinicios a mitad de tarea dejan RUNNING huérfanos que contaminan reportes). Query simple, sin migraciones.
- AC11 — **Apagado ordenado (graceful shutdown)**: hook de cierre del contexto Spring que interrumpe los workers, marca la tarea en curso como `ERROR ("interrumpido por shutdown")` en `tarea_log` y libera permisos del semáforo antes de morir. Complementa al AC10: AC10 limpia lo imprevisto (crash/OOM), AC11 evita crear huérfanos en paradas planificadas (deploys).
- AC12 — **Observabilidad**: cada tarea ejecutada persiste `TareaLog` (éxito/fallo/duración/error) y emite logs MDC con contexto `robot/fuente/liga`; el panel del scheduler existente queda intacto.
- AC13 — **Estrategia de pruebas**: unit tests de puertos/adapters en memoria con `Sleeper` y reloj inyectables (patrón existente); tests deterministas del circuit breaker (contador de fallos simulados) y del timeout de tarea; test MockMvc del contrato `GET /robots/estado`; test de integración con Testcontainers del barrido de huérfanos (AC10) y del apagado ordenado (AC11).

**Fuera de alcance (fases futuras)**: SSE/WebSocket para push en vivo (hoy polling basta), techo distribuido multi-instancia (ShedLock FASE 15), escalado del scraper Python a múltiples workers/máquinas, circuit breaker distribuido (Resilience4j solo si aparece necesidad real), change detection para omitir escrituras sin cambios, ETA de fin de ciclo en el panel, métricas Micrometer por fuente (encaja en FASE 16).

**Trazabilidad**: → CU-19 `OrquestarRobotsExtraccionUseCase` (ciclo propio @Scheduled por robot: construye cola desde `TareaProgramadaRepository`, ejecuta barrido aplicando `SemafaroExtraccion`, gestiona circuit breaker y reporta a `RegistroEstadoRobots`) + listener de application event para prioridad dinámica + barrido de huérfanos al arranque + CU-20 `ConsultarEstadoRobotsUseCase` (lectura para el panel) → puertos nuevos `SemafaroExtraccion` + `RegistroEstadoRobots` (application.port, adapters en memoria) → reutiliza `TareaProgramadaRepository` + `ProveedorCuotas`/#2, `ProveedorPosiciones`/#3, `ProveedorCalendario`/#4 + `TareaLogRepository`.

---

## HU-14 — Primera tarea programada: cuotas Wplay por liga, con primer disparo y elegibilidad por detalle activo

> **Contexto**: la primera liga está oficialmente ACTIVA con sus 3 fuentes asociadas (cuotas Wplay #2, calendario #4, posiciones #3). Esta HU crea la primera extracción programada real (cuotas Wplay, ciclo horario) y de paso cierra dos gaps detectados al auditar CU-15/CatalogoScheduler: (a) la tarea no re-valida que el `DetalleFuenteExtraccion` siga activo en cada disparo; (b) guardar una tarea implica ejecutarse pronto — falta controlar cuándo arranca.

> **Decisiones de diseño** (acordadas antes de escribir los ACs):
> - **Modelo de almacenamiento intacto**: la unidad sigue siendo `(ligaId, tipoFuente)` — las fuentes tienen cadencias naturalmente distintas y la unicidad + TareaLog ya funcionan así (y la futura HU-13 robots consume esas tuplas).
> - **Interacción liga-céntrica**: el selector del frontend muestra la LIGA; sus fuentes activas salen del backend (`/disponibles` ya filtra detalles inactivos). El lote multi-fuente en un solo guardado queda como evolución; esta HU cubre solo ODDS_WPLAY.
> - **Elegibilidad en cada ciclo**: si el detalle de fuente se desactiva después de crear la tarea, el scheduler deja de ejecutarla (hoy NO lo hace: gap real).

**Como** SUPERADMIN/TIPSTER, **quiero** programar la extracción de cuotas Wplay de mi liga activa para que corra automáticamente cada hora a partir de una hora de inicio que yo elijo, **para** tener las cuotas de la jornada próxima siempre frescas sin disparos manuales ni ejecuciones prematuras.

**Criterios de aceptación**:

- AC1 — **Primer disparo configurable**: nuevo campo `primer_disparo` (TIMESTAMPTZ, nullable) en `tareas_programadas` (migración V8). Al crear/editar desde `/tareas-programadas` se envía `primerDisparo` (ISO-8601); el dispatcher **omite la tarea mientras `now < primerDisparo`**. Guardar nunca dispara de inmediato: la primera ejecución real = max(primerDisparo, próximo match del cron). Null = comportamiento actual (arranca en el próximo match).
- AC2 — **Elegibilidad por detalle activo en cada ciclo**: al evaluar tareas de tipos con URL (`ODDS_WPLAY/STANDINGS/CALENDAR`), el dispatcher verifica que exista un `DetalleFuenteExtraccion` **activo** para la temporada vigente de la liga; si no, omite la ejecución y registra WARN con contexto (no es error). EQUIPOS queda exento (H-07: no usa path_to_scrape).
- AC3 — **Vista por liga (nodo raíz)**: `GET /api/v1/tareas-programadas?ligaId={id}` lista solo las tareas de esa liga (además del listado global actual), para que el panel muestre "liga → sus tareas" sin filtrar en cliente.
- AC4 — **Ingestión Wplay (CU-03 extendido)**: al ejecutarse la tarea ODDS_WPLAY, el use case consulta #2 con la URL del detalle activo de la temporada vigente y por cada match del payload:
  - AC4.1: parsea `date_match` ("15 Ago") + `time_match` ("14:30") inferiendo el año (actual; rollover dic→ene asume siguiente) y normalizando de America/Bogota a UTC (servicio de dominio dedicado, testable).
  - AC4.2: **resolutor de equipos multi-fuente en cascada** (servicio de dominio nuevo, sobre `NormalizadorNombresEquipos` + núcleo de `DetectorDuplicadosEquipos`): los nombres de Wplay ("Fluminense RJ") rara vez coinciden 1:1 con los registrados desde otras fuentes ("Fluminense"). Estrategia por orden: (1) match exacto normalizado; (2) match difuso por núcleo de tokens quitando sufijos geográficos/jurídicos (RJ, SP, FC, CF, CD…); (3) diccionario de alias persistido `equipos_alias` (fuente_tipo, nombre_externo, equipo_id, temporada_id) que se **auto-aprende** tras cada match difuso exitoso y admite override manual del SUPERADMIN. Resultados posibles por equipo: `CASADO`, `AMBIGUO` (>1 candidato) o `SIN_MATCH`.
  - AC4.3: **si ambos equipos quedan CASADOS pero el partido no existe en BD, se crea en estado `PROGRAMADO`** con la fecha parseada usando los nombres CANÓNICOS de la plantilla (OPCIÓN A confirmada). Si algún equipo queda `AMBIGUO` o `SIN_MATCH`, el partido se OMITE con WARN y contador `noCasados` — nunca se crean equipos con nombres crudos de Wplay (contaminaría la plantilla con duplicados); quedan visibles como pendientes de alias para resolución manual. Mientras Wplay aún no monte la jornada (payload vacío), la tarea permanece programada y registra SUCCESS con 0 (AC5); cuando el scraper retorna datos, se procesan.
  - AC4.4: persiste 1X2 (`Mercado.UNO_X_DOS`) y doble oportunidad (`Mercado.DOBLE_OPORTUNIDAD`, name_quota "1x"/"12"/"2x"); descarta cuotas ≤ 1.0 (BR-007); emite `CuotaActualizada` por partido actualizado.
  - AC4.5: **historial de cuotas append-only, escritura INCONDICIONAL**: cada sincronización registra en `cuota_historial` (id, partido_id FK, mercado, seleccion nullable para doble oportunidad, valor, fuente, capturada_en TIMESTAMPTZ, índice `(partido_id, capturada_en)`) TODAS las cuotas observadas en esa corrida — **sin deduplicación**, aunque sean idénticas a la anterior: la serie debe ser un registro fiel de observaciones (una cuota que baja y vuelve a su valor original perdería el rebote si se deduplicara). Volumen acotado (~1.440 filas/día por liga con ciclo horario); retención/purga futura fuera de alcance. Hoy `actualizarCuotas()` REEMPLAZA la lista y la cuota anterior se pierde, imposible saber si varió. Escritura vía puerto nuevo `CuotaHistorialRepository` tras guardar el aggregate (el dominio `Partido` queda ajeno al historial); migración V10. La lectura/consulta vive en HU-15.
- AC5 — **Ciclo resiliente**: corrida con payload vacío o sin partidos casables = `SUCCESS` con conteo 0 en `TareaLog` (la tarea sigue viva para la próxima hora); fallo HTTP/scraping = `ERROR` con reintento en el próximo ciclo (sin reintentos intra-ciclo: cortesía H-06 ya opera en el adapter). Auto-pausa fin-de-temporada fuera de alcance.
- AC6 — **Cadencia por tarea (diferenciable por fuente)**: la frecuencia vive en CADA `TareaProgramada` (campo `cronExpression`, derivado del VO `Frecuencia` que traduce "cada N unidades" a cron validado) — NO en el `DetalleFuenteExtraccion` (ese responde qué/con qué URL; la tarea responde cuándo/con qué cadencia). Como la clave incluye `tipoFuente`, cada fuente de la liga tiene su propia cadencia: ej. `(Liga Profesional, ODDS_WPLAY)` cada 1 hora mientras la jornada esté viva, y `(Liga Profesional, STANDINGS)` cada 3 días — dos tareas independientes, cada una con su `primer_disparo`. Default recomendado para Wplay: `Frecuencia.of(1, "HORAS")`; editable vía PUT existente.
- AC7 — **Observabilidad**: cada corrida persiste `TareaLog` (tareaProgramadaId + duración + conteos en mensaje) y logs MDC `liga/fuente`; `GET /{id}/logs` muestra el historial horario.
- AC8 — **Control de ejecución: individual y masiva por liga**:
  - *Individual (ya existe)*: `PUT /api/v1/tareas-programadas/{id}` con `{"activa": false}` pausa una tarea; `true` la reanuda.
  - *Masiva (NUEVO)*: `PUT /api/v1/tareas-programadas/liga/{ligaId}/estado` con `{"activa": false|true}` pausa/reanuda DE GOLPE todas las tareas de esa liga (las 3 fuentes) en una sola llamada. Respuesta 200 con `{ligaId, activa, tareas: [{tipoFuente, activa}, ...]}`; 404 si la liga no tiene tareas registradas. La pausa NO altera `cronExpression` ni `primer_disparo`: reanudar retoma el ciclo tal cual quedó.
  - Roles SUPERADMIN/TIPSTER (mismo perfil del panel). Caso de uso típico: frenar extracciones durante mantenimiento del scraper Python o fuera de temporada, sin borrar configuración.
  - Complementariedad con AC2: la pausa manual (capa operativa) es independiente de la elegibilidad por detalle activo (capa de datos); cualquiera de las dos detiene la ejecución.

**Fuera de alcance (fases futuras)**: creación en lote multi-fuente desde un solo guardado (evolución natural de esta HU), auto-pausa por fin de temporada, robots HU-13 (esta HU le prepara el terreno creando la tarea canónica).

**Trazabilidad**: → CU-15 extendido (`primerDisparo` en registrar/actualizar, filtro `?ligaId`, pausa/reanudación masiva por liga vía `TareaProgramadaRepository.buscarPorLigaId` nuevo) + CatalogoScheduler (elegibilidad AC1/AC2) + CU-03 extendido con parser de fechas Wplay y resolutor multi-fuente de equipos (servicios de dominio nuevos) → migraciones V8 `primer_disparo`, V9 `equipos_alias`, V10 `cuota_historial` → reutiliza `ProveedorCuotas`/#2 + `PartidoRepository` + `DetalleFuenteExtraccionRepository` + `TareaLogRepository` + puerto nuevo `CuotaHistorialRepository`.

---

## HU-15 — Consulta de cuotas próximas con historial por hora e indicadores de volatilidad

> **Contexto**: HU-14 escribe `cuota_historial` en cada corrida horaria. Esta HU expone la LECTURA para el frontend: el usuario NO quiere un listado crudo de todas las capturas (caótico); quiere el snapshot más reciente por partido con una señal visual de cuánto se movió la cuota (decisor clave para descartar partidos poco apetecibles para el modelo de pronóstico). La volatilidad se calcula SERVER-SIDE: el frontend solo renderiza la clase, sin matemáticas propias.

**Como** TIPSTER, **quiero** ver los próximos partidos de mi liga con su cuota más reciente y un indicador de si esa cuota se movió mucho o se mantuvo firme, **para** decidir rápido qué partidos analizar sin revisar históricos manualmente.

**Criterios de aceptación**:

- AC1 — **Snapshot con indicador (endpoint principal)**: `GET /api/v1/ligas/{ligaId}/cuotas-proximas?ventanaHoras=24` devuelve los partidos `PROGRAMADO` con fecha futura, cada uno con:
  - Datos del partido (equipos, fecha UTC, jornada).
  - Cuotas más recientes por mercado/selección.
  - `volatilidad`: clase calculada server-side por mercado comparando la última captura contra la BASELINE (primera captura dentro de la ventana): `ESTABLE` (<3% de variación), `MODERADA` (3-10%), `VOLATIL` (≥10%), `SIN_BASELINE` (<2 capturas). Umbrales configurables (`app.cuotas.volatilidad.*`). El frontend mapea clase → color/ícono/span (ej: verde firme, ámbar moderada, rojo volátil).
- AC2 — **Historial por hora (drill-down)**: `GET /api/v1/partidos/{partidoId}/cuotas/historial?horas=24&mercado=UNO_X_DOS` (filtros opcionales) devuelve la serie cronológica de capturas agrupada por mercado/selección, para gráfico/detalle al expandir un partido.
- AC3 — **Rendimiento**: ambos endpoints leen `cuota_historial` indexado por `(partido_id, capturada_en)`; el snapshot agrega en memoria acotada por ventana (sin N+1). Cache Redis opcional de corta duración para el snapshot (clave liga+ventana, invalidada en cada corrida ODDS_WPLAY de la liga).
- AC4 — **Contrato estable para badges**: la respuesta incluye SIEMPRE los campos `volatilidad` y `variacionPorcentual` (null cuando `SIN_BASELINE`) — el frontend nunca calcula ni hardcodea umbrales; cambiar thresholds es cambio de properties, no de código frontend.
- AC5 — **Roles**: lectura SUPERADMIN/TIPSTER (mismo perfil que pronósticos).

**Fuera de alcance**: alertas/push por volatilidad, series largas (>30 días), comparativas entre casas de apuestas.

**Trazabilidad**: → CU nuevo CU-21 `ConsultarCuotasProximasUseCase` (snapshot + volatilidad) + CU-22 `ConsultarHistorialCuotasUseCase` → puerto `CuotaHistorialRepository` (creado en HU-14) sobre `cuota_historial`; consume lo escrito por CU-03 extendido.

---

## Matriz de trazabilidad HU → CU → Puertos

| HU | Caso de uso | Puertos |
| --- | --- | --- |
| HU-01 | CU-01 | `ProveedorPosiciones`, `LigaRepository` |
| HU-02 | CU-02 | `ProveedorCalendario`, `PartidoRepository` |
| HU-03 | CU-03 | `ProveedorCuotas`, `PartidoRepository` |
| HU-04 | CU-04 | `LigaRepository` |
| HU-05 | CU-05 | `PartidoRepository` |
| HU-06 | CU-06 | `PronosticoRepository`, `PartidoRepository` |
| HU-07 | CU-07 | `PronosticoRepository` |
| HU-08 | CU-08 | `PronosticoRepository`, `SuscripcionRepository` |
| HU-09 | CU-09 | `SuscripcionRepository` |
| HU-10 | CU-14 | `PaisInteresRepository`, `ProveedorPaises`, `CacheLecturas` |
| HU-11 | CU-10 (encadenado) | `ProveedorEquiposPorLiga`, `LigaRepository`, `CacheLecturas` |
| HU-12 | CU-17 (países sync) + CU-18 (ligas por país async) | `ProveedorPaises`, `ProveedorLigasPorPais`, `ProveedorEquiposPorLiga`, `PaisRepository`, `LigaRepository`, `PaisInteresRepository`, `CacheLecturas`, `TareaLogRepository` |
| HU-13 | CU-19 (orquestador robots) + CU-20 (estado para panel) | `SemafaroExtraccion` + `RegistroEstadoRobots` (nuevos, adapters en memoria), reutiliza `TareaProgramadaRepository`, `CatalogoScheduler`, `ProveedorCuotas`, `ProveedorPosiciones`, `ProveedorCalendario`, `TareaLogRepository` |
| HU-14 | CU-15 extendido (primerDisparo, filtro liga) + CU-03 extendido (ingestión Wplay, resolutor multi-fuente, historial) + CatalogoScheduler (elegibilidad por detalle activo) | `ProveedorCuotas`, `PartidoRepository`, `DetalleFuenteExtraccionRepository`, `TareaProgramadaRepository`, `TareaLogRepository`, `CuotaHistorialRepository` (nuevo) |
| HU-15 | CU-21 (snapshot cuotas + volatilidad) + CU-22 (historial por hora) | `CuotaHistorialRepository` (creado en HU-14), `LigaRepository` |
| HU-16 | CU-23 (gestionar estrategias) + CU-24 (evaluar estrategia) + CU-25 (consultar sugerencias) | `EstrategiaRepository`, `SenalPartidoRepository`, `ZonaDescensoRepository` (nuevos), reutiliza `PartidoRepository`, `CuotaHistorialRepository`, `LigaRepository` |

---

## HU-16 — Estrategias de pronóstico con criterios dinámicos y zona de descenso

> **Contexto**: hoy el pronóstico es manual: el tipster revisa cuotas, posiciones y forma, y decide. HU-16 automatiza ese proceso con un motor de criterios configurable. Cada **estrategia** define una receta de criterios que se evalúan contra cada partido programado; los que pasan generan **pronósticos sugeridos** con un score de confianza. Se complementa con la configuración de **zona de descenso por liga** (posición que determina descenso, varía por torneo) y el patrón de **reacción del equipo herido** (equipos en zona de descenso con mala racha suelen ganar el siguiente partido).

**Como** TIPSTER, **quiero** definir estrategias con criterios dinámicos (cuotas, posiciones, forma, zona de descenso) para que el sistema filtre y puntué automáticamente los mejores partidos para mis pronósticos, **para** ahorrar tiempo de análisis y mejorar la calidad de mis predicciones.

**Criterios de aceptación**:

### Dominio — Estrategia y Criterios

- AC1 — **Estrategia aggregate**: `Estrategia` es una entity (id UUID, nombre, tipsterId, `Mercado`, método, maxPartidos, activa, createdAt). Compone una lista de `Criterio` (embeddable, no entity independiente). Un tipster puede tener múltiples estrategias; solo las activas se evalúan.
- AC2 — **Criterio modelado**: cada `Criterio` se auto-describe con 7 campos:
  - `fuente`: de dónde viene el dato (`CUOTAS`, `POSICIONES`, `FORMA`, `ZONA_DESCENSO`).
  - `campo`: qué campo se evalúa (ej: `cuota_1x`, `cuota_local`, `diferencia_posiciones`, `ultimos_5`, `en_zona_descenso`, `racha_perdidas`).
  - `operador`: comparación (`>=`, `<=`, `==`, `>`, `<`, `CONTIENE`, `NO_CONTIENE`).
  - `valor`: umbral (String genérico; el evaluador parsea según el campo: `"1.40"` para cuotas, `"3"` para posiciones, `"G,E,G"` para forma).
  - `referencia`: sobre qué equipo aplica (`LOCAL`, `VISITANTE`, `AMBOS`).
  - `peso`: importancia del criterio en el score final (BigDecimal 0..1, default 0.25).
  - `orden`: evaluación secuencial (Integer, para criterios con dependencia).
- AC3 — **Tipos de criterio soportados** (cada uno tiene su evaluador):
  - `CUOTAComparativa`: compara una cuota contra un umbral (ej: `cuota_1x >= 1.40` para LOCAL). Fuentes: cuotas más recientes del partido.
  - `CUOTACruzada`: compara cuotas entre equipos (ej: `cuota_1x_LOCAL < cuota_1x_VISITANTE`). El LOCAL es más favorito.
  - `POSICION_DIFERENCIA`: diferencia de posiciones en la tabla (ej: `LOCAL.pos - VISITANTE.pos >= 3`). Fuente: posiciones de la temporada vigente.
  - `REGULARIDAD`: analiza patrón de últimos 5 resultados (ej: `LOCAL: max 1 P en últimos 5`). Calculado desde `PosicionTabla.ultimosResultados`.
  - `HERIDO`: detecta racha negativa (ej: `VISITANTE: sin 2+ P seguidas`). Un equipo "herido" está en zona de descenso O con 2+ derrotas/empates consecutivos.
  - `ZONA_DESCENSO`: verifica si un equipo está en la zona de descenso configurada para esa liga (ej: `VISITANTE.en_zona_descenso == true`). Requiere `ZonaDescenso` configurado.
  - `LOCALIA`: combina localía con condición (ej: `LOCAL.es_local AND LOCAL.posicion <= 5`).
  - `REACCION_DESCENSO`: detecta el patrón "equipo herido reacciona" — equipo en zona de descenso con mala racha que tiende a ganar el siguiente (señal compuesta: `en_zona_descenso AND racha_perdidas >= 2`). Peso alto por ser señal de alta confianza.

### Zona de descenso por liga

- AC4 — **Configuración por temporada**: `ZonaDescenso` es una entity (id UUID, temporadaId, posicionDescenso Integer, descripcion String nullable). La posición varía por liga: en algunas ligas desciende el último, en otras los 2 últimos, en otras los 3 últimos. Se configura por temporada (no por liga genérica) porque puede cambiar entre ediciones.
- AC5 — **CRUD de zona de descenso**: `POST/GET/PUT /api/v1/temporadas/{temporadaId}/zona-descenso`. GET devuelve `{posicionDescenso, descripcion}`. PUT actualiza la posición. Solo SUPERADMIN puede configurar. Si no hay zona configurada para una liga, los criterios `ZONA_DESCENSO` y `REACCION_DESCENSO` retornan `SIN_DATOS` (no fallan, solo no contribuyen al score).
- AC6 — **Cálculo de `en_zona_descenso`**: un equipo está "en zona de descenso" si su `PosicionTabla.posicion >= zonaDescenso.posicionDescenso` (ej: si `posicionDescenso=17`, los equipos en posición 17, 18, 19, 20 están en zona). Se calcula en tiempo de evaluación de la estrategia, no pre-computado.

### Motor de evaluación

- AC7 — **Evaluación por partido**: `EvaluarEstrategiaUseCase` recibe una estrategia y un partido programado. Para cada criterio (ordenado por `orden`): resuelve la fuente de datos, evalúa la condición, retorna `SenalCriterio(criterio, pass, valorObservado, peso)`. Si todos los criterios pasan, calcula el score.
- AC8 — **Cálculo de score**: `score = Σ(senal.peso * senal.valor) / Σ(pesos)`. Donde `valor = 1.0 si pass, 0.0 si fail`. El score queda entre 0 y 1. La estrategia tiene un `confianzaMinima` (BigDecimal 0..1); solo los partidos con `score >= confianzaMinima` se consideran.
- AC9 — **Señales pre-computadas (opcional, fase 2)**: para no evaluar criterios en tiempo real contra cada partido, se puede crear una tabla `senal_partido` (partidoId, fuente, campo, valor, calculadaEn) que se llena después de cada sincronización (CU-01 posiciones, CU-03 cuotas). La evaluación de la estrategia lee señales ya computadas. Implementación: migración V12, puerto `SenalPartidoRepository`, servicio `CalculadoraSeñales` que se ejecuta post-sync.
- AC10 — **Evaluación automática post-sync**: el `CatalogoScheduler`, después de ejecutar CU-01 (posiciones) o CU-03 (cuotas) para una liga, evalúa todas las estrategias activas contra los partidos programados de esa liga. Los partidos que pasan el umbral se guardan como `PronosticoSugerido` (entity nueva: estrategiaId, partidoId, score, confianza, criteriosCumplidos, criteriosFallidos, createdAt).
- AC11 — **Filtro de ligas**: cada estrategia tiene una lista de `ligaIds` (nullable; null = todas las ligas activas). Solo se evalúa contra partidos de las ligas configuradas.

### REST API

- AC12 — **CRUD de estrategias** (roles TIPSTER/SUPERADMIN):
  - `POST /api/v1/estrategias` → 201, crea estrategia con criterios.
  - `GET /api/v1/estrategias` → lista estrategias del tipster autenticado (o todas si SUPERADMIN).
  - `GET /api/v1/estrategias/{id}` → detalle con criterios.
  - `PUT /api/v1/estrategias/{id}` → actualiza criterios/config.
  - `DELETE /api/v1/estrategias/{id}` → elimina.
  - `POST /api/v1/estrategias/{id}/evaluar` → evaluación on-demand de todos los partidos programados de las ligas configuradas.
- AC13 — **Pronósticos sugeridos**: `GET /api/v1/estrategias/{id}/sugerencias` → partidos sugeridos con score, confianza, criterios que pasaron/fallaron. Filtros: `?ligaId=`, `?confianzaMinima=`.
- AC14 — **Tipos de criterio disponibles**: `GET /api/v1/estrategias/criterios/tipos` → catálogo de tipos de criterio soportados (fuente, campo, operador, valorEjemplo) para que el frontend construya el formulario dinámico.

### Particularidades del mercado doble oportunidad

- AC15 — **Criterios específicos DOBLE_OPORTUNIDAD**: la estrategia puede filtrar por tipo de doble oportunidad (1X, 12, 2X). Ejemplo: "solo partidos donde la cuota 1X >= 1.40 Y la cuota local >= 1.50". El mercado `DOBLE_OPORTUNIDAD` tiene 3 selecciones (1X, 12, 2X); el criterio `CUOTAComparativa` acepta un parámetro `seleccion` para indicar cuál evaluar.
- AC16 — **Scoring DOBLE_OPORTUNIDAD**: cuando la estrategia es de mercado DOBLE_OPORTUNIDAD, el score se calcula igual (criterios ponderados), pero se agrega un bonus factor si el criterio `REACCION_DESCENSO` passa (el patrón "herido reacciona" es más fuerte en doble oportunidad porque el empate también cubre).

### Validaciones

- AC17 — **Validación de criterios**: al crear/actualizar una estrategia, el backend valida que cada criterio tenga una combinación válida de (fuente, campo, operador). Ej: `CUOTAS` no puede usar campo `diferencia_posiciones`; `POSICIONES` no puede usar campo `cuota_1x`. Error 400 con detalle del criterio inválido.
- AC18 — **Un tipster no puede tener >10 estrategias activas** (límite razonable para no saturar el scheduler). Error 409 si se excede.

**Fuera de alcance (fases futuras)**: backtesting contra históricos, ML/predicción con modelos, criterios anidados (criterio dentro de criterio), alertas/push de pronósticos, parlay automático con stake, integración con casas de apuestas para ejecución.

**Trazabilidad**: → CU-23 `GestionarEstrategiasUseCase` (CRUD) + CU-24 `EvaluarEstrategiaUseCase` (evaluación) + CU-25 `ConsultarSugerenciasUseCase` (lectura) → puertos: `EstrategiaRepository`, `SenalPartidoRepository` (nuevos), reutiliza `PartidoRepository`, `CuotaHistorialRepository`, `PosicionTablaRepository` (vía LigaRepository) + `ZonaDescensoRepository` (nuevo) → migraciones V12 (estrategias, criterios, zona_descenso, senal_partido, pronostico_sugerido).