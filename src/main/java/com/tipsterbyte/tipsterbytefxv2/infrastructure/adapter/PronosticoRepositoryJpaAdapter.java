// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto PronosticoRepository. Convierte entre el aggregate
//        Pronostico del dominio y PronosticoEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. La selección (mercado + resultado esperado) y la cuota se guardan
//            como columnas planas; el resultado final verificado es anulable (CU-05).
// [ALTERNATIVAS]: N/A.
// [RELACIONES]: Implementa application.port.PronosticoRepository (CU-06, CU-07, CU-08).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.SeleccionPronostico;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PronosticoEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PronosticoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PronosticoRepositoryJpaAdapter implements PronosticoRepository {

    private final PronosticoJpaRepository jpaRepository;

    public PronosticoRepositoryJpaAdapter(PronosticoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pronostico> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pronostico> buscarPublicadosPorPartidos(Collection<UUID> partidoIds) {
        return jpaRepository.findByEstadoAndPartidoIdIn(EstadoPronostico.PUBLICADO, partidoIds).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional
    public void guardar(Pronostico pronostico) {
        jpaRepository.save(toEntity(pronostico));
    }

    private Pronostico toDominio(PronosticoEntity entidad) {
        Resultado resultadoFinal = (entidad.getResultadoFinalGolesLocal() != null
                && entidad.getResultadoFinalGolesVisitante() != null)
                ? new Resultado(entidad.getResultadoFinalGolesLocal(), entidad.getResultadoFinalGolesVisitante())
                : null;
        return Pronostico.reconstruir(
                entidad.getId(), entidad.getTipsterId(), entidad.getPartidoId(),
                new SeleccionPronostico(entidad.getMercado(), entidad.getResultadoEsperado()),
                new Cuota(entidad.getCuotaValor()), entidad.getEstado(), resultadoFinal);
    }

    private PronosticoEntity toEntity(Pronostico pronostico) {
        return new PronosticoEntity(
                pronostico.id(), pronostico.tipsterId(), pronostico.partidoId(),
                pronostico.seleccion().mercado(), pronostico.seleccion().resultadoEsperado(),
                pronostico.cuota().valor(), pronostico.estado(),
                pronostico.resultadoFinal() != null ? pronostico.resultadoFinal().golesLocal() : null,
                pronostico.resultadoFinal() != null ? pronostico.resultadoFinal().golesVisitante() : null);
    }
}