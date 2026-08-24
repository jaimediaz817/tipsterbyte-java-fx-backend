// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad LigaEntity (tabla ligas).
// [POR QUÉ]: Provee las consultas derivadas que LigaRepositoryJpaAdapter necesita:
//            por id, por estado (ACTIVA) y persistencia. Abstae del SQL a Hibernate.
// [ALTERNATIVAS]: Consultas JPQL manuales; se descartan porque Spring Data deriva
//                 las queries con suficiente expresividad para este puerto.
// [RELACIONES]: Implementa el acceso a datos del puerto LigaRepository (CU-01, CU-02, CU-04).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.LigaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigaJpaRepository extends JpaRepository<LigaEntity, UUID> {

    List<LigaEntity> findByEstado(EstadoLiga estado);

    // [QUÉ]: Consulta derivada del catálogo geográfico: filtra por estado y país exacto
    //        (case-insensitive). El orden (pais, nombre) se aplica en la capa de
    //        interfaces para no acoplar el orden a Spring Data.
    // [QUÉ]: Filtro por nombre de país denormalizado (columna pais, display).
    //        El nombre del campo es paisNombre porque 'pais' es la asociación a PaisEntity.
    List<LigaEntity> findByEstadoAndPaisNombreIgnoreCase(EstadoLiga estado, String pais);

    Optional<LigaEntity> findByUrlSoccerway(String urlSoccerway);

    // [QUÉ]: Todas las ligas de un país (cualquier estado) — granular HU-12.
    List<LigaEntity> findByPaisNombreIgnoreCase(String pais);
}