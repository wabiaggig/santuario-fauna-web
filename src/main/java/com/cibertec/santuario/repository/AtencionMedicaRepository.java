package com.cibertec.santuario.repository;

import java.util.List;
import com.cibertec.santuario.model.AtencionMedica;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Integer> {
    @EntityGraph(attributePaths = {"veterinario", "detalles", "detalles.medicamento"})
    List<AtencionMedica> findByAnimalIdAnimalOrderByFechaAtencionDesc(Integer idAnimal);

    @EntityGraph(attributePaths = {"animal", "animal.especie", "veterinario"})
    List<AtencionMedica> findTop100ByOrderByFechaAtencionDescIdAtencionDesc();

    @EntityGraph(attributePaths = {"animal", "animal.especie", "veterinario"})
    List<AtencionMedica> findTop100ByAnimalIdAnimalOrderByFechaAtencionDescIdAtencionDesc(Integer idAnimal);
}
