# Instalar el skill `diagram-design` en un proyecto con OpenCode

Procedimiento para instalar el skill de diagramas [`diagram-design`](https://github.com/cathrynlavery/diagram-design) en un proyecto que **ya tiene** repo git inicializado y carpeta `.opencode/skills` creada.

> Windows / Git Bash

## 1. Ubícate en el proyecto

```bash
cd /c/ruta/a/tu-proyecto
```

## 2. Clona el skill en una ubicación temporal

```bash
git clone https://github.com/cathrynlavery/diagram-design.git /tmp/diagram-design
```

## 3. Copia el skill a tu carpeta `.opencode/skills`

```bash
cp -r /tmp/diagram-design/skills/diagram-design .opencode/skills/diagram-design
```

Verifica que quedó bien:

```bash
ls .opencode/skills/diagram-design
```

Deberías ver `SKILL.md`, `assets/`, `references/`.

## 4. Limpia el clon temporal

```bash
rm -rf /tmp/diagram-design
```

## 5. (Opcional) Decide si lo versionas en git

Mantenerlo fuera del repo (solo local):

```bash
echo ".opencode/skills/diagram-design" >> .gitignore
```

Si prefieres versionarlo con el resto del equipo, sáltate este paso.

## 6. Arranca OpenCode y confirma

```bash
opencode
```

Luego pregúntale:

```
¿qué skills tienes disponibles?
```

Debería aparecer `diagram-design` en la lista.

## 7. (Opcional) Onboarding de marca — 60 segundos

Si quieres que los diagramas usen los colores/tipografía de tu marca en vez de la paleta por defecto (jet-black + atomic-tangerine):

```
onboard diagram-design a https://tusitio.com
```

El agente extrae paleta y fuentes del sitio, propone un diff de tokens semánticos (`paper`, `ink`, `muted`, `accent`, `link`), y al confirmar los escribe en `references/style-guide.md`.

## 8. Probar generando un diagrama

Ejemplos de prompts dentro de la sesión de OpenCode:

```
Hazme un diagrama de arquitectura de mi app: frontend, backend, base de datos, cache Redis.
Guárdalo en diagrams/arquitectura.html
```

```
Necesito un cuadrante mostrando features por impacto vs esfuerzo.
```

## 9. Ver los diagramas generados

Los diagramas se generan como **HTML autocontenido con SVG inline** (no PNG/JPG). Se abren directo en el navegador:

```bash
start ruta/al/diagrama.html
```

(`start` funciona en Git Bash sobre Windows porque delega al sistema). Si no te funciona, también puedes hacer doble clic al archivo desde el Explorador de Windows.

---

### Resumen del patrón

```
clonar en temporal → copiar carpeta interna skills/diagram-design a .opencode\skills\diagram-design → borrar temporal → verificar en OpenCode
```
