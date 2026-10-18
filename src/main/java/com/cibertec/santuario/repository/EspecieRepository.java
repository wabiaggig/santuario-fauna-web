package com.cibertec.santuario.repository;

import java.util.List;
import com.cibertec.santuario.model.Especie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecieRepository extends JpaRepository<Especie, Integer> {
    List<Especie> findByActivoTrueOrderByNombreComunAsc();
}
