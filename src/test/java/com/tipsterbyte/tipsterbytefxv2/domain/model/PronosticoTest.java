package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PronosticoTest {

    private static final UUID TIPSTER_ID = UUID.randomUUID();
    private static final UUID PARTIDO_ID = UUID.randomUUID();
    private static final SeleccionPronostico SELECCION = new SeleccionPronostico(Mercado.UNO_X_DOS, "1");
    private static final Cuota CUOTA = new Cuota(new BigDecimal("1.85"));

    private static Pronostico pronosticoBorrador() {
        return new Pronostico(TIPSTER_ID, PARTIDO_ID, SELECCION, CUOTA);
    }

    @Test
    void debe_publicarse_cuando_partido_jugable_y_cuota_vigente_br004() {
        Pronostico pronostico = pronosticoBorrador();
        pronostico.publicar(true, true);
        assertEquals(EstadoPronostico.PUBLICADO, pronostico.estado());
    }

    @Test
    void debe_rechazar_publicacion_sin_partido_jugable_br004() {
        Pronostico pronostico = pronosticoBorrador();
        assertThrows(DomainException.class, () -> pronostico.publicar(false, true));
        assertEquals(EstadoPronostico.BORRADOR, pronostico.estado());
    }

    @Test
    void debe_rechazar_publicacion_sin_cuota_vigente_br004() {
        Pronostico pronostico = pronosticoBorrador();
        assertThrows(DomainException.class, () -> pronostico.publicar(true, false));
    }

    @Test
    void debe_rechazar_publicar_dos_veces() {
        Pronostico pronostico = pronosticoBorrador();
        pronostico.publicar(true, true);
        assertThrows(DomainException.class, () -> pronostico.publicar(true, true));
    }

    @Test
    void debe_anular_pronostico_publicado_br005() {
        Pronostico pronostico = pronosticoBorrador();
        pronostico.publicar(true, true);
        pronostico.anular();
        assertEquals(EstadoPronostico.ANULADO, pronostico.estado());
    }

    @Test
    void debe_rechazar_anular_un_borrador_br005() {
        Pronostico pronostico = pronosticoBorrador();
        assertThrows(DomainException.class, pronostico::anular);
    }
}