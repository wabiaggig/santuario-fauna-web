package com.cibertec.santuario.repository;

import java.util.List;
import java.util.Optional;
import com.cibertec.santuario.model.Zona;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZonaRepository extends JpaRepository<Zona, Integer> {
    List<Zona> findByActivoTrueAndTipoOrderByNombreAsc(String tipo);
    List<Zona> findAllByOrderByNombreAsc();
    List<Zona> findByActivoTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCaseAndIdZonaNot(String nombre, Integer idZona);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from Zona z where z.idZona = :id")
    Optional<Zona> bloquearPorId(@Param("id") Integer id);
}
