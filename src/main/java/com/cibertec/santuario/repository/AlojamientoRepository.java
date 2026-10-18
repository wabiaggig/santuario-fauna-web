package com.cibertec.santuario.repository;

import java.util.List;
import com.cibertec.santuario.model.Alojamiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlojamientoRepository extends JpaRepository<Alojamiento, Integer> {
    long countByZonaIdZonaAndFechaSalidaIsNull(Integer idZona);
    boolean existsByAnimalIdAnimalAndFechaSalidaIsNull(Integer idAnimal);

    @EntityGraph(attributePaths = "zona")
    List<Alojamiento> findByAnimalIdAnimalOrderByFechaEntradaDesc(Integer idAnimal);
}
