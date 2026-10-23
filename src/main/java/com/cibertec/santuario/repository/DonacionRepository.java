package com.cibertec.santuario.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cibertec.santuario.model.Donacion;

import jakarta.persistence.LockModeType;

public interface DonacionRepository extends JpaRepository<Donacion, Integer> {
    
    @EntityGraph(attributePaths = {"apadrinamiento", "apadrinamiento.animal"})
    List<Donacion> findByPadrino_IdPadrinoOrderByIdDonacionDesc(Integer idPadrino);
    
    @EntityGraph(attributePaths = {"padrino", "apadrinamiento", "apadrinamiento.animal"})
    List<Donacion> findByEstadoVerificacionOrderByFechaSolicitudAscIdDonacionAsc(
            String estadoVerificacion);
    
    @EntityGraph(attributePaths = {"padrino", "apadrinamiento", "apadrinamiento.animal"})
    List<Donacion> findByEstadoVerificacionInOrderByFechaSolicitudAscIdDonacionAsc(
            Collection<String> estados);

    @EntityGraph(attributePaths = {"apadrinamiento", "apadrinamiento.animal"})
    List<Donacion> findByPeriodoAporteBetweenAndApadrinamientoIsNotNullOrderByIdDonacionDesc(
            LocalDate desde, LocalDate hasta);

    boolean existsByMedioPagoAndReferencia(String medioPago, String referencia);

    Optional<Donacion> findByIdDonacionAndPadrino_IdPadrino(
            Integer idDonacion, Integer idPadrino);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Donacion d where d.idDonacion = :id")
    Optional<Donacion> bloquearParaRevision(@Param("id") Integer idDonacion);
}
