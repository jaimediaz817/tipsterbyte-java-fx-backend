# Definición del Proyecto — tipsterbyte-fx-v2

> **FASE 0** — Definición. No se escribe Java todavía. Este documento es el contrato de qué construiremos y por qué.

---

## 0.1 Nombre del proyecto

**tipsterbyte-fx-v2**

- `tipsterbyte` → reúsa el nombre del negocio existente (plataforma de pronósticos deportivos).
- `fx` → hace referencia al ecosistema de fútbol / betfair exchange (apuestas).
- `v2` → segunda generación del proyecto sobre el ecosistema Java (el anterior vivía en el ecosistema Python/FastAPI).

---

## 0.2 Problema que resuelve

Los tipsters (analistas de fútbol) generan pronósticos sobre partidos de ligas del mundo. Hoy la extracción de datos está dispersa: tablas de posiciones, calendarios y cuotas provienen de fuentes distintas (4 APIs/scrapers), sin una estructura unificada que permita:

- Consumir esas fuentes de forma consistente y abstraída (que cambiar de proveedor no rompa el negocio).
- Almacenar y consultar los datos deportivos de forma confiable.
- Generar, publicar y vender pronósticos basados en esos datos.

**El proyecto resuelve el problema de unificar la ingesta de datos deportivos (posiciones + calendario + cuotas) desde múltiples proveedores y exponerla como dominio de negocio para la generación de pronósticos.**

---

## 0.3 Usuarios

| Usuario | Qué hace |
| --- | --- |
| **Tipster** | Crea y publica pronósticos basados en los datos deportivos sincronizados. |
| **Cliente suscriptor** | Consulta ligas, partidos, cuotas y pronósticos publicados. |
| **Administrador** | Configura fuentes de datos, activa ligas y monitorea el estado de la extracción. |

---

## 0.4 Funcionalidades principales

Núcleo pequeño, demostrable en arquitectura. No se meten 30 funcionalidades.

| # | Funcionalidad | Descripción | Fase clave |
| --- | --- | --- | --- |
| 1 | **Ingesta de datos deportivos** | Sincronizar posiciones, calendario y cuotas desde las APIs externas (ports/adapters). | 5-8 |
| 2 | **Gestión de ligas y equipos** | Alta/consulta de ligas y equipos desde los datos sincronizados. | 5-8 |
| 3 | **Gestión de partidos** | Partidos programados y jugados con resultados y cuotas. | 5-8 |
| 4 | **Pronósticos** | Crear, publicar y consultar pronósticos de los tipsters. | 5-8 |
| 5 | **Suscripciones** | Un cliente se suscribe a un tipster para consumir sus pronósticos (pago simulable). | 11-13 |

> **Auth/Users** entra en la FASE 11 (Spring Security + JWT). **Notificaciones** en la FASE 13 (RabbitMQ). **Cache** en la FASE 12 (Redis). No se modelan ahora para no contaminar el núcleo.

---

## 0.5 Dominio (visión DDD)

Se detalla en `docs/domain/modelo-dominio.md`. Resumen:

- **Aggregates**: `Liga` (con su tabla de posiciones), `Partido` (con cuotas), `Pronostico`, `Suscripcion`.
- **Entities**: `Equipo`, `Tipster`, `Cliente`.
- **Value Objects**: `Temporada`, `Resultado`, `Cuota`, `PosicionTabla`, `Mercado`, `SeleccionPronostico`, `Email`, `Rol`.
- **Reglas de negocio**: una liga solo se activa cuando sus fuentes de datos están disponibles; un pronóstico solo aplica a partidos programados; una cuota debe ser válida en el momento de publicar.
- **Domain Events**: `LigaActivada`, `PartidoProgramado`, `CuotaActualizada`, `PronosticoPublicado`, `SuscripcionCreada`.

---

## 0.6 Casos de uso (núcleo)

Se detallan en `docs/use-cases/casos-de-uso.md`. Resumen:

1. Sincronizar tabla de posiciones.
2. Sincronizar calendario de partidos.
3. Sincronizar cuotas.
4. Activar liga (cuando sus fuentes están activas).
5. Crear partido / marcar resultado.
6. Crear pronóstico.
7. Publicar pronóstico.
8. Consultar pronósticos por liga/fecha.
9. Crear suscripción.

---

## Resultado de FASE 0

```
ecosistema_java/
├── README.md                        # Visión del proyecto
└── docs/
    ├── PROYECTO-PLAN.md             # Plan maestro (22 fases)
    ├── project-definition.md        # Este documento
    ├── domain/
    │   └── modelo-dominio.md        # DDD
    ├── use-cases/
    │   └── casos-de-uso.md          # Casos de uso
    └── architecture/
        └── arquitectura-objetivo.md # Clean Architecture + ports/adapters
```