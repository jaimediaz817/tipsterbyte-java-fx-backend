// ─────────────────────────────────────────────
// [QUÉ]: Servicio de dominio que resuelve un nombre de equipo externo (ej: Wplay)
//        contra la plantilla de equipos de una temporada, usando una estrategia
//        en cascada: exacto → difuso por núcleo → alias persistido.
// [POR QUÉ]: HU-14 AC4.2 — los nombres de Wplay ("Fluminense RJ") rara vez coinciden
//            1:1 con los registrados desde otras fuentes ("Fluminense"). El resolutor
//            oculta esa complejidad y devuelve un resultado tipado (CASADO/AMBIGUO/
//            SIN_MATCH) para que el use case decida si crear el partido.
// [ALTERNATIVAS]: Matching fuzzy genérico (Levenshtein); se descarta por falsos
//                 positivos ("Boca Unidos" vs "Boca Juniors"). Se prefiere la cascada
//                 de reglas de alta confianza reutilizando DetectorDuplicadosEquipos.
// [RELACIONES]: HU-14 AC4.2 → EquipoAliasRepository (puerto de alias) +
//               NormalizadorNombresEquipos + DetectorDuplicadosEquipos.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ResolutorEquipoExtraccion {

    // Sufijos geográficos/comunes que Wplay agrega y que se stripan para el match difuso.
    private static final Set<String> SUFIJOS_GEOGRAFICOS = Set.of(
            "rj", "sp", "mg", "rs", "pr", "sc", "ba", "pe", "ce", "to",
            "fc", "cf", "cd", "sc", "ac", "ec", "de", "do");

    private ResolutorEquipoExtraccion() {
    }

    // [QUÉ]: Resuelve un nombre externo contra la lista de candidatos de la plantilla.
    //        Cascada: (1) exacto normalizado → (2) difuso por núcleo de tokens → (3) alias.
    public static ResultadoMatchEquipo resolver(String nombreExterno, List<Equipo> candidatos,
                                                 List<String> aliasesConocidos) {
        if (nombreExterno == null || nombreExterno.isBlank()) {
            return new ResultadoMatchEquipo.SinMatch(nombreExterno);
        }
        if (candidatos == null || candidatos.isEmpty()) {
            return new ResultadoMatchEquipo.SinMatch(nombreExterno);
        }

        String normalizadoExterno = NormalizadorNombresEquipos.normalizar(nombreExterno);

        // Estrategia 1: match exacto normalizado.
        List<Equipo> exactos = candidatos.stream()
                .filter(c -> NormalizadorNombresEquipos.normalizar(c.nombre()).equals(normalizadoExterno))
                .toList();
        if (exactos.size() == 1) {
            return new ResultadoMatchEquipo.Casado(exactos.getFirst());
        }
        if (exactos.size() > 1) {
            return new ResultadoMatchEquipo.Ambiguo(nombreExterno, exactos);
        }

        // Estrategia 2: match difuso por núcleo de tokens (strip sufijos geográficos).
        Set<String> nucleoExterno = nucleoSinSufijos(normalizadoExterno);
        if (!nucleoExterno.isEmpty()) {
            List<Equipo> difusos = candidatos.stream()
                    .filter(c -> {
                        Set<String> nucleoCandidato = nucleoSinSufijos(
                                NormalizadorNombresEquipos.normalizar(c.nombre()));
                        return nucleoCandidato.equals(nucleoExterno)
                                || nucleoCandidato.containsAll(nucleoExterno)
                                || nucleoExterno.containsAll(nucleoCandidato);
                    })
                    .toList();
            if (difusos.size() == 1) {
                return new ResultadoMatchEquipo.Casado(difusos.getFirst());
            }
            if (difusos.size() > 1) {
                return new ResultadoMatchEquipo.Ambiguo(nombreExterno, difusos);
            }
        }

        // Estrategia 3: alias persistido (auto-aprendido o manual).
        if (aliasesConocidos != null && !aliasesConocidos.isEmpty()) {
            String aliasNormalizado = NormalizadorNombresEquipos.normalizar(nombreExterno);
            for (String alias : aliasesConocidos) {
                if (NormalizadorNombresEquipos.normalizar(alias).equals(aliasNormalizado)) {
                    // El alias apunta a un equipo; lo buscamos por nombre en los candidatos.
                    // [POR QUÉ]: El alias store el nombre canónico del equipo, no el id.
                    String nombreCanonico = alias;
                    List<Equipo> porAlias = candidatos.stream()
                            .filter(c -> NormalizadorNombresEquipos.normalizar(c.nombre())
                                    .equals(NormalizadorNombresEquipos.normalizar(nombreCanonico)))
                            .toList();
                    if (porAlias.size() == 1) {
                        return new ResultadoMatchEquipo.Casado(porAlias.getFirst());
                    }
                }
            }
        }

        return new ResultadoMatchEquipo.SinMatch(nombreExterno);
    }

    // [QUÉ]: Extrae el núcleo de tokens de un nombre normalizado, quitando sufijos
    //        geográficos y formas jurídicas (reutiliza la lógica de DetectorDuplicadosEquipos).
    private static Set<String> nucleoSinSufijos(String nombreNormalizado) {
        Set<String> nucleo = new LinkedHashSet<>();
        for (String token : nombreNormalizado.split(" ")) {
            String limpio = token.replaceAll("[^a-z0-9]", "");
            if (!limpio.isBlank() && !SUFIJOS_GEOGRAFICOS.contains(limpio)) {
                nucleo.add(limpio);
            }
        }
        return nucleo;
    }
}
