package com.cibertec.santuario.repository;

import java.util.List;
import com.cibertec.santuario.model.Liberacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiberacionRepository extends JpaRepository<Liberacion, Integer> {
    @EntityGraph(attributePaths = {"animal", "animal.especie", "responsable"})
    List<Liberacion> findAllByOrderByFechaLiberacionDescIdLiberacionDesc();

    boolean existsByAnimalIdAnimal(Integer idAnimal);
}
