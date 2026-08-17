# 📘 Procedimiento Base: Flujo de APIs del Backend `ecosistema_java`

> **Objetivo**: Catálogo VIGENTE de todos los endpoints REST del backend TipsterByte, con métodos HTTP, roles, request/response y el orden exacto para replicar el flujo completo en Postman.
>
> **Versión**: 292 tests en verde · roles unificados `CLIENTE/TIPSTER/SUPERADMIN` · catálogo geográfico (países + ligas BORRADOR) · activación del catálogo (momento 0) · campo `jornada` en partidos · `fechaCreacion` en auth.
>
> **Dependencia de datos**: los endpoints de sincronización (`/sincronizaciones/*`) y el poblamiento del catálogo (CU-10) requieren el **scraper Python en `http://127.0.0.1:8001`** (proyecto `tipsterByte_fx`). Los GET de lectura funcionan con lo que haya en BD.

---

## 1. Resumen de Recursos y Endpoints

| #   | Recurso         | Base Path                        | Métodos   |
| --- | --------------- | -------------------------------- | --------- |
| 1   | Auth            | `/api/v1/auth`                   | POST      |
| 2   | Roles           | `/api/v1/roles`                  | GET       |
| 3   | Catálogo        | `/api/v1/catalogo`               | GET, POST |
| 4   | Países          | `/api/v1/paises`                 | GET       |
| 5   | Ligas           | `/api/v1/ligas`                  | GET, POST |
| 6   | Fuentes         | `/api/v1/fuentes`                | GET, POST |
| 7   | Fuentes de Liga | `/api/v1/ligas/{ligaId}/fuentes` | GET, PUT  |
| 8   | Partidos        | `/api/v1/partidos`               | GET, POST |
| 9   | Pronósticos     | `/api/v1/pronosticos`            | GET, POST |
| 10  | Suscripciones   | `/api/v1/suscripciones`          | GET, POST |

> **Nota**: el `POST` del recurso **Ligas** corresponde solo a sub-recursos (`/{ligaId}/activacion`, `/{ligaId}/sincronizaciones/*`). **NO existe `POST /api/v1/ligas`** (alta manual descartada; el catálogo se puebla vía CU-10/scraper).

---

## 2. Enums (Valores Válidos)

| Enum                   | Valores                                                    |
| ---------------------- | ---------------------------------------------------------- |
| `Rol`                  | `CLIENTE`, `TIPSTER`, `SUPERADMIN`                          |
| `EstadoLiga`           | `BORRADOR`, `ACTIVA`, `INACTIVA`                            |
| `TipoFuenteExtraccion` | `STANDINGS`, `ODDS_WPLAY`, `CALENDAR`                       |
| `Mercado`              | `UNO_X_DOS`, `DOBLE_OPORTUNIDAD`, `OVER_UNDER`              |
| `EstadoPartido`        | `PROGRAMADO`, `EN_VIVO`, `FINALIZADO`, `SUSPENDIDO`         |
| `EstadoPronostico`     | `BORRADOR`, `PUBLICADO`, `ANULADO`                          |
| `EstadoSuscripcion`    | `ACTIVA`, `CANCELADA`, `EXPIRADA`                           |
| `ResultadoReciente`    | `GANADO`, `EMPATE`, `PERDIDO`                               |

---

## 3. Seguridad y Roles

Según `SecurityConfig.java`:

| Ruta                            | Acceso                                   |
| ------------------------------- | ---------------------------------------- |
| `POST /api/v1/auth/**`          | 🔓 **Público** (registro y login)        |
| `GET /api/v1/roles`             | 🔓 **Público** (catálogo de roles)       |
| `GET /actuator/health`          | 🔓 **Público**                           |
| `/api/v1/catalogo/**`           | `SUPERADMIN` (activación y estado)       |
| `GET /api/v1/fuentes`           | `SUPERADMIN`, `TIPSTER`, `CLIENTE`       |
| `/api/v1/ligas/**`              | `SUPERADMIN`, `TIPSTER`                  |
| `/api/v1/paises/**`             | `SUPERADMIN`, `TIPSTER`                  |
| `/api/v1/partidos/**`           | `SUPERADMIN`, `TIPSTER`                  |
| `/api/v1/pronosticos/**`        | `SUPERADMIN`, `TIPSTER`, `CLIENTE`       |
| `/api/v1/suscripciones/**`      | `CLIENTE`                                |
| Cualquier otra                  | Autenticado (JWT válido)                 |

> **Nota**: todas las peticiones (excepto auth, roles y health) requieren el header `Authorization: Bearer {{access_token}}`. `OPTIONS` (CORS) está permitido para todos.

---

## 4. Formato de Respuesta de Error (ApiError)

Todos los errores devuelven el mismo contrato (JSON):

```json
{
  "timestamp": "2026-08-16T06:15:55.044Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "mensaje": "Liga no activable: fuentes de datos no operativas (BR-001)",
  "path": "/api/v1/ligas/abc-123/activacion"
}
```

| Código | Cuándo                                                          | Origen                                                        |
| ------ | --------------------------------------------------------------- | ------------------------------------------------------------- |
| `400`  | Validación de body, JSON malformado, query param faltante o inválido (ej: `?estado=XYZ`) | `GlobalExceptionHandler`        |
| `401`  | Token JWT ausente, inválido o expirado                           | `ApiErrorAuthenticationEntryPoint`                            |
| `403`  | Rol insuficiente (ej: `CLIENTE` en `/paises`)                    | `ApiErrorAccessDeniedHandler`                                 |
| `422`  | Violación de regla de negocio BR-xx o recurso no encontrado      | `DomainException` → `GlobalExceptionHandler`                  |
| `503`  | Fuente externa/scraper/Redis no disponible                       | `InfraestructureException` → `GlobalExceptionHandler`         |
| `500`  | Error interno (se loguea con stack trace; respuesta genérica)    | `GlobalExceptionHandler.manejarGeneral`                       |

---

## 5. Flujo de Peticiones (Orden Recomendado)

### 🔐 FASE 1: Autenticación (Público)

#### 1.1 Roles disponibles
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/roles`
- **Respuesta**: `200 OK` → `[{codigo, nombre}]`
```json
[
  { "codigo": "CLIENTE", "nombre": "Cliente" },
  { "codigo": "TIPSTER", "nombre": "Tipster" },
  { "codigo": "SUPERADMIN", "nombre": "Super Administrador" }
]
```

#### 1.2 Registro de Usuario
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/auth/registro`
- **Body** (JSON):
```json
{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "password": "secreto123",
  "rol": "CLIENTE"
}
```
- **Respuesta**: `201 Created` → `AuthResponse`:
```json
{
  "usuarioId": "6f1b9c1e-8a2d-4f5b-9c3e-7a1b2c3d4e5f",
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "rol": "CLIENTE",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "fechaCreacion": "2026-08-16T06:15:55"
}
```
- **Notas**: `password` ≥ 6 caracteres. El endpoint ya devuelve el token (no hace falta login después del registro).

#### 1.3 Login
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/auth/login`
- **Body** (JSON):
```json
{
  "email": "juan@example.com",
  "password": "secreto123"
}
```
- **Respuesta**: `200 OK` → `AuthResponse` con `token` (JWT) y `fechaCreacion`.
- **Acción en Postman** (pestaña **Tests**):
```javascript
const jsonData = pm.response.json();
pm.collectionVariables.set("access_token", jsonData.token);
pm.collectionVariables.set("clienteId", jsonData.usuarioId);
```

---

### 🗺️ FASE 2: Catálogo Geográfico (Activación → Países → Ligas BORRADOR)

> **Rol**: `SUPERADMIN` o `TIPSTER` (la **activación** del catálogo es solo `SUPERADMIN`)

#### 2.1 Activar Catálogo (momento 0)
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/catalogo/activar`
- **Descripción**: dispara la sincronización del catálogo completo (CU-10): puebla países (fuente #1) y ligas BORRADOR por país (fuente #5) desde el scraper Python en `:8001`. Es **síncrono**: la respuesta llega cuando termina de persistir.
- **Respuesta**: `200 OK` → `CatalogoEstadoResponse` (el estado resultante tras activar):
```json
{
  "estado": "POBLADO",
  "totalPaises": 176,
  "totalLigas": 620
}
```
- **Errores**: `503` si el scraper `:8001` no está disponible (`InfraestructureException`); `403` si el rol no es `SUPERADMIN`.

#### 2.2 Estado del Catálogo
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/catalogo/estado`
- **Descripción**: estado derivado de los datos reales persistidos (sin tabla de estado). `POBLADO` solo cuando hay países **y** ligas; si falta alguna → `VACIO`.
- **Respuesta**: `200 OK` → `CatalogoEstadoResponse`:
```json
{
  "estado": "VACIO",
  "totalPaises": 0,
  "totalLigas": 0
}
```

#### 2.3 Listar Países
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/paises`
- **Query params (opcionales)**: `continente` (ej: `Europa`), `mapeado` (`true`/`false`)
- **Respuesta**: `200 OK` → `List<PaisResponse>` (orden alfabético por `nombre`):
```json
[
  {
    "id": "3f6b9c1e-8a2d-4f5b-9c3e-7a1b2c3d4e5f",
    "nombre": "España",
    "isoAlpha2": "ES",
    "continente": "Europa",
    "code": "ESP",
    "href": "/teams/espana/8/",
    "mapeado": true
  }
]
```

#### 2.4 Listar Ligas del Catálogo por Estado
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas?estado=BORRADOR`
- **Query params**: `estado` (`BORRADOR`\|`ACTIVA`\|`INACTIVA`, default `ACTIVA`), `pais` (opcional, exacto case-insensitive)
- **Respuesta**: `200 OK` → `List<LigaResponse>` (orden `pais → nombre` cuando hay filtros):
```json
[
  {
    "id": "a1b2c3d4-5e6f-7890-abcd-ef1234567890",
    "nombre": "LaLiga EA Sports",
    "pais": "España",
    "estado": "BORRADOR",
    "temporada": "2026/2027",
    "urlSoccerway": "/path/to/scrape/calendar",
    "apiId": "api-football-140"
  }
]
```
- **Nota**: sin query params devuelve **solo `ACTIVA`** (compatibilidad total). `?estado` inválido → `400`.

---

### 📚 FASE 3: Gestión de Fuentes (Catálogo)

> **Rol**: `SUPERADMIN` o `TIPSTER` (el GET de `/fuentes` también lo ven `CLIENTE`)

#### 3.1 Registrar Fuente
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/fuentes`
- **Body** (JSON):
```json
{
  "nombre": "Flashscore Posiciones",
  "tipo": "STANDINGS",
  "activa": true
}
```
- **Respuesta**: `201 Created` (header `Location: /api/v1/fuentes`, sin body)

#### 3.2 Listar Fuentes
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/fuentes`
- **Respuesta**: `200 OK` → `[{id, nombre, tipo, url, activa}]`

---

### ⚽ FASE 4: Gestión de Ligas

> **Rol**: `SUPERADMIN` o `TIPSTER`

#### 4.1 Listar Ligas Activas
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas`
- **Respuesta**: `200 OK` → `List<LigaResponse>` (con `urlSoccerway`/`apiId` cuando existen, si no `null`)

#### 4.2 Asociar URL de Fuente a Liga
- **Método**: `PUT`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/fuentes/{{tipo}}`
- **Path params**: `ligaId` (UUID), `tipo` (`STANDINGS` \| `ODDS_WPLAY` \| `CALENDAR`)
- **Body** (JSON):
```json
{
  "tipo": "STANDINGS",
  "url": "https://www.flashscore.com/posiciones/",
  "activa": true
}
```
- **Respuesta**: `204 No Content`

#### 4.3 Listar Fuentes de una Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/fuentes`
- **Respuesta**: `200 OK` → `[{id, nombre, tipo, url, activa}]`

#### 4.4 Activar Liga
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/activacion`
- **Body** (JSON):
```json
{
  "urlPosiciones": "https://www.flashscore.com/posiciones/",
  "urlCalendario": "https://www.soccerway.com/calendario/",
  "urlCuotas": "https://www.wplay.co/cuotas/"
}
```
- **Respuesta**: `204 No Content` (falla con `422` si falta alguna fuente — BR-001)

#### 4.5 Sincronizar Posiciones / Calendario / Cuotas
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/sincronizaciones/posiciones` (y `/calendario`, `/cuotas`)
- **Respuesta**: `200 OK` → `{"eventosEmitidos": 1}`
- **Notas**: requieren el scraper Python en `:8001` y la liga `ACTIVA`. Invalidan el cache Redis antes de consultar.

#### 4.6 Detalle de Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}`
- **Respuesta**: `200 OK` → `LigaDetalleResponse` (id, nombre, pais, estado, temporada, posiciones[])

#### 4.7 Posiciones de Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/posiciones`
- **Respuesta**: `200 OK` → `List<PosicionTablaResponse>` (equipoId, equipoNombre, posicion, J/G/E/P, GF/GC, puntos, ultimosResultados)

---

### 🏟️ FASE 5: Gestión de Partidos

> **Rol**: `SUPERADMIN` o `TIPSTER`

#### 5.1 Listar Partidos por Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos?ligaId={{ligaId}}`
- **Respuesta**: `200 OK` → `List<PartidoResponse>` (id, ligaId, equipoLocal, equipoVisitante, fechaProgramada, estado, resultado, cuotas[])

#### 5.2 Listar Partidos por Fecha
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos?ligaId={{ligaId}}&fecha=2026-08-15`
- **Respuesta**: `200 OK` → `List<PartidoResponse>`

#### 5.3 Listar Próximos Partidos
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos?ligaId={{ligaId}}&proximos=true`
- **Respuesta**: `200 OK` → `List<PartidoResponse>`

#### 5.4 Cuotas de un Partido
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos/{{partidoId}}/cuotas`
- **Respuesta**: `200 OK` → `List<CuotaResponse>` → `[{mercado, valor}]`

#### 5.5 Registrar Resultado
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/partidos/{{partidoId}}/resultado`
- **Body** (JSON):
```json
{
  "golesLocal": 2,
  "golesVisitante": 1
}
```
- **Respuesta**: `204 No Content`

---

### 🔮 FASE 6: Pronósticos

> **Rol**: `SUPERADMIN`, `TIPSTER` o `CLIENTE` (consultar)

#### 6.1 Crear Pronóstico (Borrador)
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/pronosticos`
- **Body** (JSON):
```json
{
  "tipsterId": "{{tipsterId}}",
  "partidoId": "{{partidoId}}",
  "mercado": "UNO_X_DOS",
  "resultadoEsperado": "LOCAL",
  "cuotaValor": 1.85
}
```
- **Respuesta**: `201 Created` → `{"id": "..."}` + header `Location`

#### 6.2 Publicar Pronóstico
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/pronosticos/{{pronosticoId}}/publicacion`
- **Respuesta**: `204 No Content`

#### 6.3 Consultar Pronósticos Publicados
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/pronosticos?clienteId={{clienteId}}&ligaId={{ligaId}}&fecha=2026-08-15`
- **Query params**: `clienteId` y `ligaId` (UUID, obligatorios), `fecha` (`YYYY-MM-DD`, obligatorio)
- **Respuesta**: `200 OK` → `List<PronosticoResponse>` (pronosticoId, tipsterId, partidoId, equipos, fechaHora, mercado, resultadoEsperado, cuotaValor)

---

### 💎 FASE 7: Suscripciones

> **Rol**: `CLIENTE`

#### 7.1 Crear Suscripción
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/suscripciones`
- **Body** (JSON):
```json
{
  "clienteId": "{{clienteId}}",
  "tipsterId": "{{tipsterId}}",
  "planNombre": "Plan Mensual",
  "planPrecio": 9.99,
  "planDuracionDias": 30,
  "fechaInicio": "2026-08-15T10:00:00"
}
```
- **Respuesta**: `201 Created` → `SuscripcionResponse` (suscripcionId, clienteId, tipsterId, planNombre, fechaInicio, fechaFin, estado=ACTIVA)

#### 7.2 Listar Suscripciones del Cliente
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/suscripciones?clienteId={{clienteId}}`
- **Respuesta**: `200 OK` → `List<SuscripcionResponse>`
- **Nota**: `clienteId` debe coincidir con el usuario autenticado; si no → `422` "solo puedes consultar tus propias suscripciones".

---

## 6. Variables de Postman

| Variable       | Valor Inicial           | Descripción                              |
| -------------- | ----------------------- | ---------------------------------------- |
| `baseUrl`      | `http://localhost:8080` | URL base del backend                     |
| `access_token` | *(vacío)*               | Token JWT (se llena tras login)          |
| `ligaId`       | *(vacío)*               | UUID de la liga                          |
| `partidoId`    | *(vacío)*               | UUID del partido                         |
| `clienteId`    | *(vacío)*               | UUID del cliente (usuarioId)             |
| `tipsterId`    | *(vacío)*               | UUID del tipster                         |
| `pronosticoId` | *(vacío)*               | UUID del pronóstico                      |
| `fecha`        | `2026-08-15`            | Fecha en formato YYYY-MM-DD              |

---

## 7. Script de Login (Tests) para guardar el token

En la pestaña **Tests** de la petición `Login`:

```javascript
const jsonData = pm.response.json();
pm.collectionVariables.set("access_token", jsonData.token);
pm.collectionVariables.set("clienteId", jsonData.usuarioId);
```

> Esto llena automáticamente `{{access_token}}` y `{{clienteId}}` para las peticiones posteriores.

---

## 8. Resumen del Orden de Ejecución

```
 1. GET  /roles                       → catálogo de roles (público)
 2. POST /auth/registro               → crear usuario (opcional; devuelve token)
 3. POST /auth/login                  → obtener JWT (guardar access_token + clienteId)
 4. POST /catalogo/activar            → poblar el catálogo (momento 0, SUPERADMIN; scraper :8001)
 5. GET  /catalogo/estado             → verificar estado POBLADO + conteos
 6. GET  /paises                      → catálogo geográfico (SUPERADMIN/TIPSTER)
 7. GET  /ligas?estado=BORRADOR       → ligas del catálogo (capturar ligaId)
 8. GET  /ligas?estado=BORRADOR&pais= → filtro por país
 9. POST /fuentes                     → registrar fuentes del catálogo
10. GET  /fuentes                     → listar fuentes
11. GET  /ligas                       → ligas ACTIVA
12. PUT  /ligas/{ligaId}/fuentes/{tipo} → asociar URL de fuente a liga
13. GET  /ligas/{ligaId}/fuentes      → verificar URLs asociadas
14. POST /ligas/{ligaId}/activacion   → activar liga (BR-001)
15. POST /ligas/{ligaId}/sincronizaciones/posiciones → sincronizar (scraper :8001)
16. POST /ligas/{ligaId}/sincronizaciones/calendario → sincronizar (scraper :8001)
17. POST /ligas/{ligaId}/sincronizaciones/cuotas     → sincronizar (scraper :8001)
18. GET  /ligas/{ligaId}              → detalle con posiciones
19. GET  /ligas/{ligaId}/posiciones   → tabla de posiciones
20. GET  /partidos?ligaId=            → listar partidos (capturar partidoId)
21. GET  /partidos?ligaId=&fecha=     → partidos por fecha
22. GET  /partidos?ligaId=&proximos=true → próximos partidos
23. GET  /partidos/{partidoId}/cuotas → ver cuotas
24. POST /partidos/{partidoId}/resultado → registrar resultado
25. POST /pronosticos                 → crear pronóstico (capturar pronosticoId)
26. POST /pronosticos/{pronosticoId}/publicacion → publicar
27. GET  /pronosticos?clienteId=&ligaId=&fecha= → consultar pronósticos publicados
28. POST /suscripciones               → crear suscripción (rol CLIENTE)
29. GET  /suscripciones?clienteId=    → listar suscripciones del cliente
```