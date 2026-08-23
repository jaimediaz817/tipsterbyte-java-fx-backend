// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del DetectorDuplicadosEquipos (H-04).
// [POR QUÉ]: La PRECISIÓN es el requisito central (decisión del usuario): jamás debe
//            marcar clubes reales distintos ("Boca Unidos" ≠ "Boca Juniors"). Solo se
//            marcan contención de palabras y formas jurídicas.
// [RELACIONES]: H-04 → DetectarDiscrepanciasEquiposUseCase → endpoint discrepancias.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorDuplicadosEquiposTest {

    private static final String CONTENCION = DetectorDuplicadosEquipos.RAZON_CONTENCION;
    private static final String FORMA = DetectorDuplicadosEquipos.RAZON_FORMA_JURIDICA;

    @Test
    void debe_marcar_contencion_de_palabras() {
        var pares = DetectorDuplicadosEquipos.detectar(List.of(
                new Equipo("Gimnasia Mendoza"),
                new Equipo("Gimnasia y Esgrima Mendoza")));

        assertEquals(1, pares.size());
        assertEquals(CONTENCION, pares.get(0).razon());
    }

    @Test
    void debe_marcar_diferencia_solo_en_forma_juridica() {
        var pares = DetectorDuplicadosEquipos.detectar(List.of(
                new Equipo("Millonarios"),
                new Equipo("Millonarios F.C.")));

        assertEquals(1, pares.size());
        assertEquals(FORMA, pares.get(0).razon());
    }

    @Test
    void no_debe_marcar_clubes_reales_distintos_que_compartan_palabra() {
        // [PRECISIÓN]: "Boca Unidos" y "Boca Juniors" son clubes reales DISTINTOS.
        var pares = DetectorDuplicadosEquipos.detectar(List.of(
                new Equipo("Boca Unidos"),
                new Equipo("Boca Juniors")));

        assertTrue(pares.isEmpty(), "compartir una palabra NO es duplicado");
    }

    @Test
    void no_debe_marcar_equipos_totalmente_distintos() {
        var pares = DetectorDuplicadosEquipos.detectar(List.of(
                new Equipo("River Plate"),
                new Equipo("Vélez Sarsfield"),
                new Equipo("Independiente Rivadavia")));

        assertTrue(pares.isEmpty());
    }

    @Test
    void no_debe_marcar_cuando_ambos_quedan_sin_nucleo_tras_quitar_formas_juridicas() {
        // Caso límite: dos nombres que SOLO son forma jurídica — nada confiable que decir.
        var pares = DetectorDuplicadosEquipos.detectar(List.of(
                new Equipo("S.A."),
                new Equipo("F.C.")));

        assertTrue(pares.isEmpty());
    }

    @Test
    void lista_vacia_o_un_equipo_no_genera_pares() {
        assertTrue(DetectorDuplicadosEquipos.detectar(List.of()).isEmpty());
        assertTrue(DetectorDuplicadosEquipos.detectar(List.of(new Equipo("Nacional"))).isEmpty());
    }
}
