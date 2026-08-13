# Regla de Naming — tipsterbyte-fx-v2

> Regla SIEMPRE obligatoria. Nombres claros y significativos, sin excepciones.

## Java

| Elemento | Convención | Ejemplo |
| --- | --- | --- |
| Clases/interfaces | PascalCase, sustantivo del negocio | `Pronostico`, `ProveedorCuotas` |
| Métodos | camelCase, verbo + contexto | `obtenerPartidosPorLigaYFecha` |
| Variables | camelCase, significado claro | `equipoLocal`, `cuotaVigente` |
| Constantes | UPPER_SNAKE_CASE | `MAX_EMISORAS_CUOTAS` |
| Paquetes | minúsculas, agrupados por capa | `domain.model`, `application.usecase` |

## Dominio

- Usar el **Ubiquitous Language** de `docs/domain/modelo-dominio.md` (ej: `Partido`, `Cuota`, `Pronostico`). No inventar sinónimos.
- Puertos (interfaces de application): `XxxRepository` (persistencia) y `ProveedorXxx` (externo).
- Adapters: `XxxAdapter` o `XxxRepositoryJpaAdapter`.

## Qué evitar

- `tmp`, `data`, `info`, `d`, `helper`, `utils` como nombres significativos.
- Sufijos genéricos como `Manager`/`Service` sin responsabilidad clara.