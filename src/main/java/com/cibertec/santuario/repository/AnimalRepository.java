package com.cibertec.santuario.repository;

import java.util.List;
import java.util.Optional;
import com.cibertec.santuario.model.Animal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Integer> {
    @EntityGraph(attributePaths = "especie")
    List<Animal> findByActivoTrueAndEstadoOrderByNombreAsc(String estado);

    @EntityGraph(attributePaths = "especie")
    Optional<Animal> findByIdAnimalAndActivoTrueAndEstado(Integer idAnimal, String estado);
}
