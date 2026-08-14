package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrearSuscripcionUseCaseTest {

    @Mock
    private SuscripcionRepository suscripcionRepository;

    private CrearSuscripcionUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new CrearSuscripcionUseCase(suscripcionRepository);
    }

    @Test
    void debe_crear_suscripcion_activa_y_emitir_evento() {
        UUID clienteId = UUID.randomUUID();
        UUID tipsterId = UUID.randomUUID();
        Plan plan = new Plan("Pro", new BigDecimal("9.99"), 30);
        LocalDateTime fechaInicio = LocalDateTime.of(2026, 3, 1, 10, 0);

        List<DomainEvent> eventos = casoDeUso.ejecutar(clienteId, tipsterId, plan, fechaInicio);

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(suscripcionRepository).guardar(captor.capture());
        Suscripcion creada = captor.getValue();
        assertEquals(clienteId, creada.clienteId());
        assertEquals(tipsterId, creada.tipsterId());
        assertEquals(plan, creada.plan());
        assertEquals(fechaInicio.plusDays(30), creada.fechaFin());
        assertEquals(EstadoSuscripcion.ACTIVA, creada.estado());
        assertEquals(1, eventos.size());
        assertTrue(eventos.stream().anyMatch(e -> e.getClass().getSimpleName().equals("SuscripcionCreada")));
    }
}