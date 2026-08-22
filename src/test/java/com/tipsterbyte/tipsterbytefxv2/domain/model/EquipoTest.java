// ─────────────────────────────────────────────
// [QUÉ]: Test unitario del entity Equipo (club de una temporada).
// [POR QUÉ]: Verifica las invariantes: id y nombre obligatorios, y el escudo opcional
//            (logo_url de la fuente #6) con valor por defecto null.
// [RELACIONES]: Miembro del aggregate Liga vía Temporada; referenciado por Partido
//               y PosicionTabla.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipoTest {

    @Test
    void debe_crear_equipo_valido_sin_escudo() {
        Equipo equipo = new Equipo("Millonarios");

        assertEquals("Millonarios", equipo.nombre());
        assertNull(equipo.logoUrl());
    }

    @Test
    void debe_crear_equipo_con_escudo_de_la_fuente_6() {
        Equipo equipo = new Equipo("Millonarios", "https://escudos/millonarios.png");

        assertEquals("https://escudos/millonarios.png", equipo.logoUrl());
    }

    @Test
    void debe_reconstruir_con_identidad_y_escudo() {
        UUID id = UUID.randomUUID();

        Equipo equipo = new Equipo(id, "Nacional", "https://escudos/nal.png");

        assertEquals(id, equipo.id());
        assertEquals("Nacional", equipo.nombre());
        assertEquals("https://escudos/nal.png", equipo.logoUrl());
    }

    @Test
    void debe_rechazar_nombre_vacio() {
        assertThrows(DomainException.class, () -> new Equipo(" "));
    }

    @Test
    void equipos_con_distinto_id_son_entidades_distintas_auque_compartan_nombre() {
        Equipo a = new Equipo("Boca Juniors");
        Equipo b = new Equipo("Boca Juniors");

        assertEquals(a, a);
        assertEquals(b, b);
    }
}
