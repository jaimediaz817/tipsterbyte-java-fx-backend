# 📘 Procedimiento Base: Flujo de APIs del Backend `ecosistema_java`

> **Objetivo**: Documentar el orden exacto en que se llaman las APIs del backend TipsterByte, los métodos HTTP, los cuerpos de request y los roles requeridos, para replicar el flujo completo en Postman.

---

## 1. Resumen de Recursos y Endpoints

| #   | Recurso         | Base Path                        | Métodos   |
| --- | --------------- | -------------------------------- | --------- |
| 1   | Auth            | `/api/v1/auth`                   | POST      |
| 2   | Ligas           | `/api/v1/ligas`                  | GET, POST |
| 3   | Partidos        | `/api/v1/partidos`               | GET, POST |
| 4   | Pronósticos     | `/api/v1/pronosticos`            | GET, POST |
| 5   | Suscripciones   | `/api/v1/suscripciones`          | GET, POST |
| 6   | Fuentes         | `/api/v1/fuentes`                | GET, POST |
| 7   | Fuentes de Liga | `/api/v1/ligas/{ligaId}/fuentes` | GET, PUT  |

---

## 2. Enums (Valores Válidos)

| Enum                   | Valores                                        |
| ---------------------- | ---------------------------------------------- |
| `Rol`                  | `TIPSTER`, `CLIENTE`, `ADMIN`                  |
| `Mercado`              | `UNO_X_DOS`, `DOBLE_OPORTUNIDAD`, `OVER_UNDER` |
| `TipoFuenteExtraccion` | `STANDINGS`, `ODDS_WPLAY`, `CALENDAR`          |

---

## 3. Seguridad y Roles

Según `SecurityConfig.java`:

| Ruta                       | Acceso                           |
| -------------------------- | -------------------------------- |
| `POST /api/v1/auth/**`     | 🔓 **Público** (registro y login) |
| `GET /api/v1/fuentes`      | `ADMIN`, `TIPSTER`, `CLIENTE`    |
| `/api/v1/ligas/**`         | `ADMIN`, `TIPSTER`               |
| `/api/v1/partidos/**`      | `ADMIN`, `TIPSTER`               |
| `/api/v1/pronosticos/**`   | `ADMIN`, `TIPSTER`, `CLIENTE`    |
| `/api/v1/suscripciones/**` | `CLIENTE`                        |
| Cualquier otra             | Autenticado (JWT válido)         |

> **Nota**: Todas las peticiones (excepto auth) requieren el header `Authorization: Bearer {{access_token}}`.

---

## 4. Flujo de Peticiones (Orden Recomendado)

### 🔐 FASE 1: Autenticación (Público)

#### 1.1 Registro de Usuario
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
- **Respuesta**: `201 Created` → `AuthResponse` (usuarioId, nombre, email, rol, token)

#### 1.2 Login
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/auth/login`
- **Body** (JSON):
```json
{
  "email": "juan@example.com",
  "password": "secreto123"
}
```
- **Respuesta**: `200 OK` → `AuthResponse` con el campo `token` (JWT)
- **Acción en Postman**: Guardar el token en la variable `access_token`:
```javascript
const jsonData = pm.response.json();
pm.collectionVariables.set("access_token", jsonData.token);
```

---

### 📚 FASE 2: Gestión de Fuentes (Catálogo)

> **Rol**: `ADMIN` o `TIPSTER`

#### 2.1 Registrar Fuente
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
- **Respuesta**: `201 Created`

#### 2.2 Listar Fuentes
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/fuentes`
- **Respuesta**: `200 OK` → Lista de `FuenteExtraccionResponse`

---

### ⚽ FASE 3: Gestión de Ligas

> **Rol**: `ADMIN` o `TIPSTER`

#### 3.1 Listar Ligas Activas
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas`
- **Respuesta**: `200 OK` → Lista de `LigaResponse`

#### 3.2 Asociar URL de Fuente a Liga
- **Método**: `PUT`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/fuentes/{{tipo}}`
- **Path params**: `ligaId` (UUID), `tipo` (`STANDINGS` | `ODDS_WPLAY` | `CALENDAR`)
- **Body** (JSON):
```json
{
  "tipo": "STANDINGS",
  "url": "https://www.flashscore.com/posiciones/",
  "activa": true
}
```
- **Respuesta**: `204 No Content`

#### 3.3 Listar Fuentes de una Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/fuentes`
- **Respuesta**: `200 OK` → Lista de `FuenteExtraccionResponse` con URLs

#### 3.4 Activar Liga
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
- **Respuesta**: `204 No Content`

#### 3.5 Sincronizar Posiciones
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/sincronizaciones/posiciones`
- **Respuesta**: `200 OK` → `SincronizacionResponse` (eventos)

#### 3.6 Sincronizar Calendario
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/sincronizaciones/calendario`
- **Respuesta**: `200 OK` → `SincronizacionResponse` (eventos)

#### 3.7 Sincronizar Cuotas
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/sincronizaciones/cuotas`
- **Respuesta**: `200 OK` → `SincronizacionResponse` (eventos)

#### 3.8 Detalle de Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}`
- **Respuesta**: `200 OK` → `LigaDetalleResponse` (con posiciones)

#### 3.9 Posiciones de Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/ligas/{{ligaId}}/posiciones`
- **Respuesta**: `200 OK` → Lista de `PosicionTablaResponse`

---

### 🏟️ FASE 4: Gestión de Partidos

> **Rol**: `ADMIN` o `TIPSTER`

#### 4.1 Listar Partidos por Liga
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos?ligaId={{ligaId}}`
- **Respuesta**: `200 OK` → Lista de `PartidoResponse`

#### 4.2 Listar Partidos por Fecha
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos?ligaId={{ligaId}}&fecha=2026-08-15`
- **Respuesta**: `200 OK` → Lista de `PartidoResponse`

#### 4.3 Listar Próximos Partidos
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos?ligaId={{ligaId}}&proximos=true`
- **Respuesta**: `200 OK` → Lista de `PartidoResponse`

#### 4.4 Cuotas de un Partido
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/partidos/{{partidoId}}/cuotas`
- **Respuesta**: `200 OK` → Lista de `CuotaResponse`

#### 4.5 Registrar Resultado
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

### 🔮 FASE 5: Pronósticos

> **Rol**: `ADMIN`, `TIPSTER` o `CLIENTE`

#### 5.1 Crear Pronóstico (Borrador)
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
- **Respuesta**: `201 Created` → `RecursoCreadoResponse` (id)

#### 5.2 Publicar Pronóstico
- **Método**: `POST`
- **URL**: `{{baseUrl}}/api/v1/pronosticos/{{pronosticoId}}/publicacion`
- **Respuesta**: `204 No Content`

#### 5.3 Consultar Pronósticos
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/pronosticos?clienteId={{clienteId}}&ligaId={{ligaId}}&fecha=2026-08-15`
- **Respuesta**: `200 OK` → Lista de `PronosticoResponse`

---

### 💎 FASE 6: Suscripciones

> **Rol**: `CLIENTE`

#### 6.1 Crear Suscripción
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
- **Respuesta**: `201 Created` → `SuscripcionResponse`

#### 6.2 Listar Suscripciones del Cliente
- **Método**: `GET`
- **URL**: `{{baseUrl}}/api/v1/suscripciones?clienteId={{clienteId}}`
- **Respuesta**: `200 OK` → Lista de `SuscripcionResponse`

---

## 5. Variables de Postman

| Variable       | Valor Inicial           | Descripción                     |
| -------------- | ----------------------- | ------------------------------- |
| `baseUrl`      | `http://localhost:8080` | URL base del backend            |
| `access_token` | *(vacío)*               | Token JWT (se llena tras login) |
| `ligaId`       | *(vacío)*               | UUID de la liga                 |
| `partidoId`    | *(vacío)*               | UUID del partido                |
| `clienteId`    | *(vacío)*               | UUID del cliente                |
| `tipsterId`    | *(vacío)*               | UUID del tipster                |
| `pronosticoId` | *(vacío)*               | UUID del pronóstico             |
| `fecha`        | `2026-08-15`            | Fecha en formato YYYY-MM-DD     |

---

## 6. Script de Login (Tests) para guardar el token

En la pestaña **Tests** de la petición `Login`:

```javascript
const jsonData = pm.response.json();
pm.collectionVariables.set("access_token", jsonData.token);
pm.collectionVariables.set("clienteId", jsonData.usuarioId);
```

> Esto llena automáticamente `{{access_token}}` y `{{clienteId}}` para las peticiones posteriores.

---

## 7. Resumen del Orden de Ejecución

```
1. POST /auth/registro          → crear usuario (opcional si ya existe)
2. POST /auth/login             → obtener JWT (guardar en access_token)
3. POST /fuentes                → registrar fuentes del catálogo
4. GET  /fuentes                → listar fuentes
5. GET  /ligas                  → obtener ligas activas (capturar ligaId)
6. PUT  /ligas/{ligaId}/fuentes/{tipo} → asociar URL de fuente a liga
7. GET  /ligas/{ligaId}/fuentes → verificar URLs asociadas
8. POST /ligas/{ligaId}/activacion      → activar liga
9. POST /ligas/{ligaId}/sincronizaciones/posiciones → sincronizar posiciones
10. POST /ligas/{ligaId}/sincronizaciones/calendario → sincronizar calendario
11. POST /ligas/{ligaId}/sincronizaciones/cuotas     → sincronizar cuotas
12. GET  /ligas/{ligaId}         → ver detalle con posiciones
13. GET  /partidos?ligaId=       → listar partidos (capturar partidoId)
14. GET  /partidos/{partidoId}/cuotas → ver cuotas
15. POST /partidos/{partidoId}/resultado → registrar resultado
16. POST /pronosticos            → crear pronóstico (capturar pronosticoId)
17. POST /pronosticos/{pronosticoId}/publicacion → publicar
18. POST /suscripciones          → crear suscripción (rol CLIENTE)
19. GET  /suscripciones?clienteId= → listar suscripciones
20. GET  /pronosticos?clienteId=&ligaId=&fecha= → consultar pronósticos publicados