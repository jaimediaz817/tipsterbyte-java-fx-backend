// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-14 (GestionarPaisesInteresUseCase): registrar, listar,
//        eliminar y reemplazar la lista de países de interés con prioridad derivada.
// [POR QUÉ]: Verifica la orquestación: validación contra la fuente #1, upsert con id
//            existente (unicidad iso_alpha2), prioridad siguiente/al final, el
//            reemplazo en bloque que elimina los no incluidos, y la propagación de
//            maxLigasPorPais (límite opcional de ligas por país).
// [RELACIONES]: CU-14 → PaisInteresRepository + ProveedorPaises.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarPaisInteresComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarPaisesInteresUseCaseTest {

    @Mock
    private PaisInteresRepository paisInteresRepository;
    @Mock
    private ProveedorPaises proveedorPaises;

    private GestionarPaisesInteresUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new GestionarPaisesInteresUseCase(paisInteresRepository, proveedorPaises);
    }

    @Test
    void debe_registrar_pais_de_interes_al_final_de_la_lista() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(unPais("Colombia", "CO")));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("ES", "España", 1, null)));
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.empty());

        casoDeUso.registrar(new RegistrarPaisInteresComando("co", "Colombia", null));

        ArgumentCaptor<PaisInteres> captor = ArgumentCaptor.forClass(PaisInteres.class);
        verify(paisInteresRepository).guardar(captor.capture());
        assertEquals("CO", captor.getValue().isoAlpha2());
        assertEquals("Colombia", captor.getValue().nombre());
        assertEquals(2, captor.getValue().prioridad());
        assertNull(captor.getValue().maxLigasPorPais());
    }

    @Test
    void debe_registrar_pais_con_limite_de_ligas_por_pais() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(unPais("Colombia", "CO")));
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of());
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.empty());

        casoDeUso.registrar(new RegistrarPaisInteresComando("CO", "Colombia", 5));

        ArgumentCaptor<PaisInteres> captor = ArgumentCaptor.forClass(PaisInteres.class);
        verify(paisInteresRepository).guardar(captor.capture());
        assertEquals(5, captor.getValue().maxLigasPorPais());
    }

    @Test
    void debe_actualizar_nombre_y_limite_manteniendo_prioridad_si_ya_existe() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(unPais("España", "ES")));
        PaisInteres existente = new PaisInteres(UUID.randomUUID(), "ES", "España", 3, null);
        when(paisInteresRepository.buscarPorIsoAlpha2("ES")).thenReturn(Optional.of(existente));

        casoDeUso.registrar(new RegistrarPaisInteresComando("ES", "España (nuevo)", 7));

        ArgumentCaptor<PaisInteres> captor = ArgumentCaptor.forClass(PaisInteres.class);
        verify(paisInteresRepository).guardar(captor.capture());
        assertEquals(existente.id(), captor.getValue().id());
        assertEquals("España (nuevo)", captor.getValue().nombre());
        assertEquals(3, captor.getValue().prioridad());
        assertEquals(7, captor.getValue().maxLigasPorPais());
    }

    @Test
    void debe_rechazar_pais_no_disponible_en_la_fuente() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(unPais("Colombia", "CO")));

        assertThrows(DomainException.class, () -> casoDeUso.registrar(
                new RegistrarPaisInteresComando("XX", "No existe", null)));
        verify(paisInteresRepository, never()).guardar(any());
    }

    @Test
    void debe_listar_paises_de_interes_por_prioridad() {
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("CO", "Colombia", 1, null),
                new PaisInteres("ES", "España", 2, null)));

        List<PaisInteres> lista = casoDeUso.listar();

        assertEquals(2, lista.size());
        assertEquals("CO", lista.get(0).isoAlpha2());
        assertEquals("ES", lista.get(1).isoAlpha2());
    }

    @Test
    void debe_eliminar_pais_de_interes_registrado() {
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.of(
                new PaisInteres("CO", "Colombia", 1, null)));

        casoDeUso.eliminar("CO");

        verify(paisInteresRepository).eliminar("CO");
    }

    @Test
    void debe_rechazar_eliminar_pais_no_registrado() {
        when(paisInteresRepository.buscarPorIsoAlpha2("CO")).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> casoDeUso.eliminar("CO"));
        verify(paisInteresRepository, never()).eliminar(any());
    }

    @Test
    void debe_reemplazar_preferencias_en_orden_y_eliminar_los_no_incluidos() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                unPais("Colombia", "CO"), unPais("España", "ES"), unPais("Francia", "FR")));
        when(paisInteresRepository.buscarPorIsoAlpha2(any())).thenReturn(Optional.empty());
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of(
                new PaisInteres("FR", "Francia", 1, null),
                new PaisInteres("CO", "Colombia", 2, null)));

        casoDeUso.reemplazarPreferencias(List.of(
                new RegistrarPaisInteresComando("CO", "Colombia", null),
                new RegistrarPaisInteresComando("ES", "España", null)));

        ArgumentCaptor<PaisInteres> captor = ArgumentCaptor.forClass(PaisInteres.class);
        verify(paisInteresRepository, org.mockito.Mockito.times(2)).guardar(captor.capture());
        assertEquals("CO", captor.getAllValues().get(0).isoAlpha2());
        assertEquals(1, captor.getAllValues().get(0).prioridad());
        assertEquals("ES", captor.getAllValues().get(1).isoAlpha2());
        assertEquals(2, captor.getAllValues().get(1).prioridad());
        verify(paisInteresRepository).eliminar("FR");
    }

    @Test
    void debe_reemplazar_preferencias_con_limites_de_ligas_por_pais() {
        when(proveedorPaises.obtenerPaises()).thenReturn(List.of(
                unPais("Colombia", "CO"), unPais("España", "ES")));
        when(paisInteresRepository.buscarPorIsoAlpha2(any())).thenReturn(Optional.empty());
        when(paisInteresRepository.listarPorPrioridad()).thenReturn(List.of());

        casoDeUso.reemplazarPreferencias(List.of(
                new RegistrarPaisInteresComando("CO", "Colombia", 10),
                new RegistrarPaisInteresComando("ES", "España", null)));

        ArgumentCaptor<PaisInteres> captor = ArgumentCaptor.forClass(PaisInteres.class);
        verify(paisInteresRepository, org.mockito.Mockito.times(2)).guardar(captor.capture());
        assertEquals(10, captor.getAllValues().get(0).maxLigasPorPais());
        assertNull(captor.getAllValues().get(1).maxLigasPorPais());
    }

    private PaisFuente unPais(String nombre, String isoAlpha2) {
        return new PaisFuente(nombre, "/" + nombre.toLowerCase() + "/", "81", isoAlpha2, "Europa", true);
    }
}
