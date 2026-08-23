// ─────────────────────────────────────────────
// [QUÉ]: Servicio de dominio que detecta PARES SOSPECHOSOS de equipos duplicados en
//        una temporada (H-04): dos entradas que probablemente son el mismo club
//        registrado con escrituras distintas.
// [POR QUÉ]: El matching por nombre exacto-normalizado deja pasar diferencias de
//            abreviatura/forma jurídica/ciudad ampliada entre fuentes (#3 Flashscore vs
//            #6 Soccerway). Este detector es un PUENTE DE DIAGNÓSTICO hasta el fuzzy
//            matching de FASE 17: su salida es para REVISIÓN HUMANA, nunca elimina ni
//            fusiona automáticamente.
// [PRECISIÓN sobre recall] (decisión del usuario): se prefieren falsos negativos a
//            falsos positivos. "Boca Unidos" vs "Boca Juniors" son clubes reales
//            DISTINTOS: la similitud de edición genérica quedó DESCARTADA porque los
//            marcaría. Solo se marcan dos reglas de alta confianza:
//            R1 CONTENCIÓN — todas las palabras del nombre corto aparecen completas
//                           dentro del largo (≥2 palabras el corto).
//            R2 FORMA_JURIDICA — tras quitar sufijos legales (s.a., f.c., club...),
//                           ambos quedan con exactamente las mismas palabras.
// [ALTERNATIVAS]: Levenshtein/Jaro-Winkler con umbral; se descartan en v1 por falsos
//                 positivos del tipo citado. Quedan para el fuzzy de FASE 17.
// [RELACIONES]: Usa NormalizadorNombresEquipos; consumido por
//               DetectarDiscrepanciasEquiposUseCase (endpoint de diagnóstico H-04).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class DetectorDuplicadosEquipos {

    public static final String RAZON_CONTENCION = "CONTENCION_PALABRAS";
    public static final String RAZON_FORMA_JURIDICA = "FORMA_JURIDICA";

    // Formas jurídicas/comerciales que se descartan al comparar (R2). Sin tildes,
    // sin puntos (los tokens ya vienen limpios de caracteres no alfanuméricos).
    private static final Set<String> FORMAS_JURIDICAS =
            Set.of("sa", "saa", "ca", "cf", "fc", "sc", "ac", "club");

    private DetectorDuplicadosEquipos() {
    }

    // [QUÉ]: Resultado de una pareja sospechosa: ambos equipos + regla que los marcó.
    public record ParSospechoso(Equipo equipoA, Equipo equipoB, String razon) {
    }

    // [QUÉ]: Compara todas las parejas de la lista y devuelve las sospechosas.
    public static List<ParSospechoso> detectar(List<Equipo> equipos) {
        List<ParSospechoso> pares = new ArrayList<>();
        for (int i = 0; i < equipos.size(); i++) {
            for (int j = i + 1; j < equipos.size(); j++) {
                Equipo a = equipos.get(i);
                Equipo b = equipos.get(j);
                clasificar(a, b).ifPresent(razon -> pares.add(new ParSospechoso(a, b, razon)));
            }
        }
        return pares;
    }

    private static Optional<String> clasificar(Equipo a, Equipo b) {
        Set<String> tokensA = tokens(a.nombre());
        Set<String> tokensB = tokens(b.nombre());
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return Optional.empty();
        }

        // R2 FORMA_JURIDICA: iguales tras quitar formas jurídicas de ambos lados.
        Set<String> nucleoA = quitarFormasJuridicas(tokensA);
        Set<String> nucleoB = quitarFormasJuridicas(tokensB);
        boolean aSinNucleo = nucleoA.isEmpty();
        boolean bSinNucleo = nucleoB.isEmpty();
        if (!aSinNucleo && !bSinNucleo && nucleoA.equals(nucleoB)
                && !tokensA.equals(tokensB)) {
            return Optional.of(RAZON_FORMA_JURIDICA);
        }

        // R1 CONTENCIÓN: todas las palabras del corto están en el largo.
        // [POR QUÉ]: el corto debe tener ≥2 palabras — con 1 palabra ("boca") cualquier
        //            contención sería demasiado agresiva ("boca unidos" vs "boca juniors").
        var corto = tokensA.size() <= tokensB.size() ? tokensA : tokensB;
        var largo = tokensA.size() <= tokensB.size() ? tokensB : tokensA;
        if (corto.size() >= 2 && corto.size() < largo.size() && largo.containsAll(corto)) {
            return Optional.of(RAZON_CONTENCION);
        }

        return Optional.empty();
    }

    // [QUÉ]: Nombre normalizado partido en palabras limpias (sin signos, sin vacíos).
    private static Set<String> tokens(String nombre) {
        String normalizado = NormalizadorNombresEquipos.normalizar(nombre);
        Set<String> tokens = new LinkedHashSet<>();
        for (String palabra : normalizado.split(" ")) {
            String limpia = palabra.replaceAll("[^a-z0-9]", "");
            if (!limpia.isBlank()) {
                tokens.add(limpia);
            }
        }
        return tokens;
    }

    private static Set<String> quitarFormasJuridicas(Set<String> tokens) {
        Set<String> nucleo = new LinkedHashSet<>();
        for (String token : tokens) {
            if (!FORMAS_JURIDICAS.contains(token.toLowerCase(Locale.ROOT))) {
                nucleo.add(token);
            }
        }
        return nucleo;
    }
}
