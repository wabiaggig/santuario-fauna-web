package com.cibertec.santuario.repository;

import java.util.List;
import com.cibertec.santuario.model.DetalleAtencion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleAtencionRepository extends JpaRepository<DetalleAtencion, Integer> {
    @EntityGraph(attributePaths = {"atencion", "atencion.animal", "atencion.veterinario"})
    List<DetalleAtencion> findTop15ByMedicamentoIdMedicamentoOrderByAtencionFechaAtencionDesc(Integer idMedicamento);
}
