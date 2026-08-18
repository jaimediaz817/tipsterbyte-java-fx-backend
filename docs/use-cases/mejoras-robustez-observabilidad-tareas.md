# Épico: Mejoras de Robustez y Observabilidad para la Gestión de Tareas Programadas

[QUÉ]: Conjunto de mejoras para garantizar operabilidad, trazabilidad y estabilidad en producción de la funcionalidad de tareas programadas implementada en FASE 12.6.
[POR QUÉ]: Tras la implementación inicial, se identificaron carencias críticas en áreas como autorización explícita, trazabilidad de ejecuciones, protección contra sobrecarga de APIs externas, manejo de errores estandarizado, escalabilidad de listados, validación de configuración, documentación automática y métricas operativas.
[ALTERNATIVAS]: Dejar estas mejoras para fases futuras sin abordar riesgos operativos conocidos.
[RELACIONES]: Se deriva de FASE 12.6 (Gestión y Monitoreo de Tareas) y soporta los CU-15 (GestionarTareaProgramadaUseCase), CU-01/02/03/10 (sincronizaciones).

## HU-XX1: Documentar roles autorizativos explícitos por endpoint
[QUÉ]: Añadir anotaciones `@PreAuthorize` o equivalente en `TareaProgramadaController` y actualizar Javadoc con los roles requeridos (SUPERADMIN/TIPSTER) para cada operación.
[POR QUÉ]: Aunque `SecurityConfig` protege los paths, falta visibilidad en el código y en Swagger sobre qué rol específica cada método exige, lo que genera ambigüedad para desarrolladores y equipos de auditoría.
[ALTERNATIVAS]: Depender únicamente de la configuración global de seguridad (actual) → se descarta por falta de trazabilidad a nivel de método.
[RELACIONES]: HU → CU-15 → `TareaProgramadaController` (interfaces.rest) + `SecurityConfig` (infrastructure.config).

## HU-XX2: Implementar entidad de trazabilidad de ejecuciones
[QUÉ]: Crear entidad `TareaExecution` (id, taskId, startTime, endTime, success, outputSnippet, triggeredBy) y repositorio asociado para registrar cada ejecución de tarea programada.
[POR QUÉ]: Actualmente no hay historial de cuándo se ejecutó una tarea, su resultado o quién la disparó (si aplica), lo que impide depurar fallos o auditar cumplimiento de SLAs.
[ALTERNATIVAS]: Usar logs de aplicación (actual) → se descarta por falta de consulta estructural y pérdida al rotar logs.
[RELACIONES]: HU → CU-15 → `TareaExecutionRepository` (application.port) + `CatalogoScheduler` (infrastructure.adapter).

## HU-XX3: Añadir rate limiting por fuente externa
[QUÉ]: Implementar un token-bucket o similar por `DetalleFuenteExtraccion` (o `TipoFuenteExtraccion`) que limite la frecuencia de llamadas a las APIs externas (ej. máximo 1 request/segundo por fuente).
[POR QUÉ]: Sin throttling, múltiples tareas activas simultáneamente podrían saturar las APIs externas (recibiendo 429) o violar términos de servicio, afectando la estabilidad del ecosistema.
[ALTERNATIVAS]: Esperar a que falle y reintentar (actual) → se descarta por riesgo de bloqueo prolongado y mala experiencia de usuario.
[RELACIONES]: HU → PUERTOS `ProveedorPosiciones/Calendario/Cuotas` (application.port) + `CatalogoScheduler` (infrastructure.adapter).

## HU-XX4: Estandarizar manejo de errores con ApiError
[QUÉ]: Definir una clase `ApiError` (timestamp, status, error, message, path) y asegurar que todas las excepciones en `TareaProgramadaController` y use cases la devuelvan como JSON estructurado (igual que `GlobalExceptionHandler` hace para `DomainException`).
[POR QUÉ]: Actualmente, errores como "ISO inválido" lanzan `DomainException` → 422, pero el frontend necesita mensajes consistentes y códigos de error predecibles para mostrar feedback amigable.
[ALTERNATIVAS]: Dejar mensajes sin estandarizar (actual) → se descarta por fragilidad en el consumo frontend.
[RELACIONES]: HU → CU-15 → `TareaProgramadaController` + `GlobalExceptionHandler` (interfaces.rest).

## HU-XX5: Añadir paginación y filtrado al listado de tareas
[QUÉ]: Modificar `GET /api/v1/tareas-programadas` para aceptar parámetros `page`, `size`, `sort` y filtros opcionales como `activa`, `nombre`, `prioridad`.
[POR QUÉ]: Listar todas las tareas sin límites se vuelve inviable al crecer el volumen (ej. cientos de tareas programadas por cliente), degradando rendimiento y usabilidad.
[ALTERNATIVAS]: Devolver lista completa (actual) → se descarta por no escalar.
[RELACIONES]: HU → CU-15 → `TareaProgramadaRepository` (application.port) + `TareaProgramadaController` (interfaces.rest).

## HU-XX6: Validar expresiones cron en registro
[QUÉ]: Añadir validación en `RegistrarTareaProgramadaComando` usando `CronExpression.isValid(cronExpression)` y persistir solo expresiones válidas.
[POR QUÉ]: Un cron mal formado (ej. "60 * * * *") rompería el scheduler silenciosamente o causaría ejecuciones inesperadas, afectando la confiabilidad del sistema.
[ALTERNATIVAS]: Validar solo en tiempo de ejecución (actual) → se descarta por detección tardía y falta de prevención.
[RELACIONES]: HU → CU-15 → `GestionarTareasProgramasUseCase` (application.usecase) + `TareaProgramada` (domain.model).

## HU-XX7: Documentar endpoints en Swagger/OpenAPI
[QUÉ]: Añadir anotaciones `@Operation`, `@ApiResponse` y ejemplos de request/response en `TareaProgramadaController` para que queden reflejados en la documentación automática (SpringDoc).
[POR QUÉ]: El equipo frontend necesita contratos claros (códigos de estado, cuerpos de ejemplo) para integrar correctamente los nuevos endpoints sin adivinación.
[ALTERNATIVAS]: Documentar solo en archivos externos (ej. Postman) → se descarta por riesgo de desincronización con el código real.
[RELACIONES]: HU → CU-15 → `TareaProgramadaController` (interfaces.rest) + configuración SpringDoc.

## HU-XX8: Añadir campo de última ejecución exitosa
[QUÉ]: Incluir `lastSuccessfulExecutionAt` (tipo `Instant`) en la entidad `TareaProgramada` y actualizarlo tras cada ejecución exitosa en el scheduler.
[POR QUÉ]: Los usuarios operativos necesitan saber rápidamente si una tarea está funcionando (ej. "¿La extracción de cuotas se ejecutó hoy?") sin consultar tablas de ejecución auxiliares.
[ALTERNATIVAS]: Consultar tabla `TareaExecution` cada vez (actual) → se descarta por sobrecarga innecesaria para consultas frecuentes.
[RELACIONES]: HU → CU-15 → `TareaProgramada` (domain.model) + `CatalogoScheduler` (infrastructure.adapter).

## HU-XX9: Definir y ejecutar pruebas de carga para el scheduler
[QUÉ]: Crear escenario de prueba (ej. con Gatling o JMeter) que simule 50+ tareas programadas concurrentes con cron disparando cada minuto, verificando estabilidad del pool de threads y ausencia de sobrecarga.
[POR QUÉ]: Sin validación bajo carga, riesgos como hambre de threads, ejecuciones solapadas no controladas o épuisamiento de conexiones DB podrían pasar desapercibidos hasta producción.
[ALTERNATIVAS]: Asumir que funciona con few tasks (actual) → se descarta por falta de garantía de escalabilidad.
[RELACIONES]: HU → Infraestructura general (scheduler, DB, use cases) + plan de pruebas.