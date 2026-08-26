# Runbook — Verificación end-to-end de extracción de fuentes

> [QUÉ]: Receta de comandos para probar las extracciones de fuentes externas (#2 cuotas, #3 posiciones, #4 calendario, #6 equipos) sin re-descubrir el flujo.
> [POR QUÉ]: La sesión de debug del 2026-08-24 (constraints sin 'EQUIPOS', V6) consumió un día entero re-descubriendo el flujo login → POST → polling. Este runbook lo fija.
> [RELACIONES]: HU-12/CU-17/CU-18 (poblamiento), CU-04 (activar liga), fuentes-externas.md (#1..#5), ADR-008.

## Precondiciones

1. PostgreSQL dev levantado y healthy (`docker compose up -d`, contenedor `tipsterbytefxv2-postgres` en `:5434`).
2. App corriendo (`./gradlew bootRun`, `localhost:8080`).
3. Usuario con rol `SUPERADMIN` o `TIPSTER` existente en BD.
4. País poblado (`paises`) y liga ACTIVA con URLs asociadas (CU-04) en la tabla `detalle_fuentes_extraccion`.

## Paso 1 — Login (obtener token JWT)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"clave-secreta"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo $TOKEN
```

## Paso 2 — Lanzar la extracción

Poblamiento granular de ligas por país (async, 202 + polling):

```bash
EXEC=$(curl -s -X POST "http://localhost:8080/api/v1/catalogo/poblar-ligas/AR" \
  -H "Authorization: Bearer $TOKEN" \
  | sed -n 's/.*"executionId":"\([^"]*\)".*/\1/p')
echo $EXEC
```

## Paso 3 — Polling hasta SUCCESS (~80s por scrape)

```bash
curl -s "http://localhost:8080/api/v1/catalogo/activar/$EXEC" -H "Authorization: Bearer $TOKEN"
```

Estados: `RUNNING` → repetir cada ~20s → `SUCCESS` o `ERROR`. Si `ERROR`: **mirar primero el stack trace en los logs de `bootRun`** (los 500 son silenciosos en la respuesta HTTP, ADR-008).

## Paso 4 — Verificar resultado en BD

```bash
docker exec tipsterbytefxv2-postgres psql -U postgres -d tipsterbytefxv2_dev -c "SELECT count(*) FROM ligas;"
```

## Diagnóstico rápido de fallos comunes

| Síntoma | Causa probable | Acción |
|---|---|---|
| ERROR + stack trace `check_violation` / `DataIntegrityViolationException` | CHECK constraint vieja en BD dev que no incluye un valor nuevo del enum | Comparar contra `V*.sql`; si la deriva es real, crear migración `V(n+1)__*.sql` (NUNCA ALTER manual). Auditar TODOS los CHECKs del enum |
| 409 al lanzar poblar-ligas | Extracción previa del mismo país aún RUNNING | Polling del `executionId` anterior o esperar |
| Tests en verde pero falla en dev | Deriva de esquema legacy pre-Flyway (`baseline-on-migrate` no reconcilia) | La verdad = migraciones + Testcontainers; verificar constraints reales en dev (query abajo) |

## Query de auditoría de CHECK constraints (enums)

```bash
docker exec tipsterbytefxv2-postgres psql -U postgres -d tipsterbytefxv2_dev -c \
"SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE contype='c' AND conname LIKE '%tipo%';"
```
