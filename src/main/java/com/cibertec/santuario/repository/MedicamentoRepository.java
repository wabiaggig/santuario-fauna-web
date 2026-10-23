package com.cibertec.santuario.repository;

import java.util.List;
import java.util.Optional;
import com.cibertec.santuario.model.Medicamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Integer> {
    List<Medicamento> findByActivoTrueOrderByNombreAsc();

    List<Medicamento> findAllByOrderByNombreAscPresentacionAsc();

    boolean existsByNombreIgnoreCaseAndPresentacionIgnoreCaseAndIdMedicamentoNot(
            String nombre, String presentacion, Integer idMedicamento);

    @Query("select count(m) from Medicamento m where m.activo = true and m.stockActual <= m.stockMinimo")
    long contarConStockBajo();

    // Evita que dos atenciones descuenten el mismo stock a la vez.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medicamento m where m.idMedicamento = :id")
    Optional<Medicamento> bloquearPorId(@Param("id") Integer id);
}
