package com.cibertec.santuario.repository;

import java.time.LocalDate;
import java.util.List;
import com.cibertec.santuario.dto.FilaConsumoMedicamento;
import com.cibertec.santuario.dto.FilaIngresoLiberacion;
import com.cibertec.santuario.model.Liberacion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReporteRepository extends Repository<Liberacion, Integer> {

    @Query(value = """
            SELECT YEAR(m.fecha) AS anio, QUARTER(m.fecha) AS trimestre,
                   SUM(m.ingresos) AS ingresos, SUM(m.liberaciones) AS liberaciones
            FROM (
                SELECT fecha_ingreso AS fecha, 1 AS ingresos, 0 AS liberaciones FROM tbl_animal
                UNION ALL
                SELECT fecha_liberacion, 0, 1 FROM tbl_liberacion
            ) m
            WHERE m.fecha BETWEEN :desde AND :hasta
            GROUP BY YEAR(m.fecha), QUARTER(m.fecha)
            ORDER BY anio, trimestre
            """, nativeQuery = true)
    List<FilaIngresoLiberacion> ingresosYLiberacionesPorTrimestre(@Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = """
            SELECT YEAR(a.fecha_atencion) AS anio, MONTH(a.fecha_atencion) AS mes,
                   m.id_medicamento AS idMedicamento, m.nombre AS nombre,
                   m.presentacion AS presentacion, m.unidad_medida AS unidadMedida,
                   SUM(d.cantidad) AS cantidad,
                   ROUND(SUM(d.cantidad * d.costo_unitario), 2) AS costo
            FROM tbl_detalle_atencion d
            JOIN tbl_atencion_medica a ON a.id_atencion = d.id_atencion
            JOIN tbl_medicamento m ON m.id_medicamento = d.id_medicamento
            WHERE a.fecha_atencion >= :desde AND a.fecha_atencion < :hastaExclusivo
            GROUP BY YEAR(a.fecha_atencion), MONTH(a.fecha_atencion),
                     m.id_medicamento, m.nombre, m.presentacion, m.unidad_medida
            ORDER BY anio, mes, m.nombre
            """, nativeQuery = true)
    List<FilaConsumoMedicamento> consumoMedicamentosPorMes(@Param("desde") LocalDate desde,
            @Param("hastaExclusivo") LocalDate hastaExclusivo);
}
