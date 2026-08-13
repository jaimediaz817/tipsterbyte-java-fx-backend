# Regla de Arquitectura — tipsterbyte-fx-v2

> Regla que SIEMPRE debe cumplirse al diseñar o implementar código. Se carga automáticamente en cada sesión de OpenCode.

## Capas y Dependency Rule

```
interfaces (REST/DTOs) → application (use cases + ports) → domain (núcleo) ← infrastructure (adaptadores)
```

- Las dependencias apuntan **siempre hacia el dominio**. El dominio NO conoce Spring, JPA, ni las APIs externas.
- `domain` no importa nada de `application`, `interfaces` ni `infrastructure`.
- `application` depende de `domain` y define **puertos (interfaces)** que `infrastructure` implementa.
- `interfaces` depende de `application` y `domain`.

## Ubicación de código (paquetes conceptuales)

| Capa | Paquete | Qué vive ahí |
| --- | --- | --- |
| Domain | `domain.model`, `domain.service`, `domain.event` | Entities, VOs, aggregates, reglas de negocio, eventos |
| Application | `application.usecase`, `application.port`, `application.dto` | Casos de uso (CU-01..09), puertos, DTOs |
| Interfaces | `interfaces.rest` | Controllers, request/response, exception handlers |
| Infrastructure | `infrastructure.persistence`, `infrastructure.adapter`, `infrastructure.config` | JPA, adapters a las 4 APIs, configuración |

## Ports/adapters obligatorios

- Las **4 APIs** (football-data.org, API-Football, The Odds API, SharpAPI) se consumen SOLO a través de puertos de dominio:
  - `ProveedorPosiciones` → adapters: football-data.org, API-Football
  - `ProveedorCalendario` → adapters: API-Football, football-data.org
  - `ProveedorCuotas` → adapters: API-Football, The Odds API, SharpAPI
- Nunca referenciar una librería HTTP de un adapter desde dominio/application.
- Cambiar de proveedor = agregar/ajustar un adapter, sin tocar dominio ni casos de uso.

## Referencias

- `docs/architecture/arquitectura-objetivo.md` — detalle de capas, ADRs, mapa completo.
- `docs/domain/modelo-dominio.md` — aggregates y reglas (BR-001..008).