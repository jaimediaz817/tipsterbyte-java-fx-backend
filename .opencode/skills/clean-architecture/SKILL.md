---
name: clean-architecture
description: Use when designing layers, packages, ports/adapters or wiring dependencies in this repository. Enforces the Dependency Rule and ports/adapters for the 4 external APIs.
---

# Clean Architecture — tipsterbyte-fx-v2

## Cuándo usar

Al diseñar la estructura de paquetes, definir puertos/adapters, casos de uso o cualquier dependencia entre capas. Ver `.opencode/rules/architecture.md` (siempre activa) y `docs/architecture/arquitectura-objetivo.md`.

## Las 4 capas

```
interfaces → application → domain ← infrastructure
```

| Capa | Depende de | Ejemplos |
| --- | --- | --- |
| domain | nada | `Partido`, `Cuota`, `Pronostico`, reglas BR-xx |
| application | domain | casos de uso (CU-xx), puertos, DTOs |
| interfaces | application + domain | controllers REST, request/response |
| infrastructure | domain + application | adapters JPA, adapters APIs externas, config |

## Dependency Rule

- Las dependencias **siempre apuntan hacia adentro** (hacia domain).
- Un adapter puede implementar un puerto, pero el puerto (y quien lo usa) no conoce al adapter.
- El dominio no sabe que existe Spring, JPA, HTTP ni las APIs externas.

## Ports/adapters (no negociable)

- Puertos de dominio para externos: `ProveedorPosiciones`, `ProveedorCalendario`, `ProveedorCuotas`.
- Las 4 APIs (football-data.org, API-Football, The Odds API, SharpAPI) son adapters de esos puertos.
- Puertos de persistencia: `LigaRepository`, `PartidoRepository`, `PronosticoRepository`, `SuscripcionRepository`.

## Cómo verificar una implementación

1. ¿Domain importa algo de infra? → MAL.
2. ¿Un caso de uso conoce el nombre de una clase adapter? → MAL (debe usar el puerto).
3. ¿Cambiar de proveedor de cuotas toca dominio/use cases? → MAL.