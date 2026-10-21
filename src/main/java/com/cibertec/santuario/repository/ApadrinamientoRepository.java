package com.cibertec.santuario.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cibertec.santuario.model.Apadrinamiento;

import jakarta.persistence.LockModeType;

public interface ApadrinamientoRepository extends JpaRepository<Apadrinamiento, Integer> {
 
    @EntityGraph(attributePaths = {"padrino", "animal", "animal.especie"})
    List<Apadrinamiento> findByEstadoSolicitudOrderByFechaSolicitudAscIdApadrinamientoAsc(
            String estadoSolicitud);
 
    @EntityGraph(attributePaths = {"animal", "animal.especie"})
    List<Apadrinamiento> findByPadrino_IdPadrinoOrderByIdApadrinamientoDesc(Integer idPadrino);
 
    @EntityGraph(attributePaths = {"padrino", "animal", "animal.especie"})
    List<Apadrinamiento> findAllByOrderByIdApadrinamientoDesc();
 
    boolean existsByPadrino_IdPadrinoAndAnimal_IdAnimalAndFechaFinIsNullAndEstadoSolicitudIn(
            Integer idPadrino, Integer idAnimal, Collection<String> estados);
 
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Apadrinamiento a where a.idApadrinamiento = :id")
    Optional<Apadrinamiento> bloquearParaRevision(@Param("id") Integer id);
}
