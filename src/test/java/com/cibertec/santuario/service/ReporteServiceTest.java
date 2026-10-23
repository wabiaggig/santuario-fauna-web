package com.cibertec.santuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cibertec.santuario.dto.FilaConsumoMedicamento;
import com.cibertec.santuario.dto.FilaIngresoLiberacion;
import com.cibertec.santuario.repository.ReporteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {
    @Mock ReporteRepository reporteRepository;
    @InjectMocks ReporteService service;

    private static FilaIngresoLiberacion trimestre(int anio, int t, long ingresos, long liberaciones) {
        return new FilaIngresoLiberacion() {
            public Number getAnio() { return anio; }
            public Number getTrimestre() { return t; }
            public Number getIngresos() { return BigDecimal.valueOf(ingresos); }
            public Number getLiberaciones() { return BigDecimal.valueOf(liberaciones); }
        };
    }

    private static FilaConsumoMedicamento consumo(int anio, int mes, int id, String nombre, String cantidad, String costo) {
        return new FilaConsumoMedicamento() {
            public Number getAnio() { return anio; }
            public Number getMes() { return mes; }
            public Integer getIdMedicamento() { return id; }
            public String getNombre() { return nombre; }
            public String getPresentacion() { return "Frasco"; }
            public String getUnidadMedida() { return "mL"; }
            public Number getCantidad() { return new BigDecimal(cantidad); }
            public Number getCosto() { return new BigDecimal(costo); }
        };
    }

    @Test
    @DisplayName("Reporte 1: lista todos los trimestres del rango y calcula el porcentaje")
    void liberacionesSobreIngresos() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 30);
        when(reporteRepository.ingresosYLiberacionesPorTrimestre(desde, hasta))
                .thenReturn(List.of(trimestre(2026, 2, 4, 1), trimestre(2026, 3, 2, 3)));

        ReporteService.ReporteLiberaciones r = service.liberacionesSobreIngresos(desde, hasta);

        assertThat(r.getFilas()).extracting(ReporteService.FilaTrimestre::getEtiqueta)
                .containsExactly("T1 2026", "T2 2026", "T3 2026");
        assertThat(r.getFilas().get(0).getIngresos()).isZero();
        assertThat(r.getFilas().get(0).getPorcentaje()).isNull();
        assertThat(r.getFilas().get(1).getPorcentaje()).isEqualByComparingTo("25.0");
        assertThat(r.getFilas().get(2).getPorcentaje()).isEqualByComparingTo("150.0");
        assertThat(r.getTotalIngresos()).isEqualTo(6);
        assertThat(r.getTotalLiberaciones()).isEqualTo(4);
        assertThat(r.getPorcentajeTotal()).isEqualByComparingTo("66.7");
    }

    @Test
    @DisplayName("Reporte 2: agrupa el costo por mes y por medicamento")
    void consumoMedicamentos() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        when(reporteRepository.consumoMedicamentosPorMes(desde, hasta.plusDays(1))).thenReturn(List.of(
                consumo(2026, 8, 1, "Multivitamínico", "2", "5.00"),
                consumo(2026, 8, 2, "Clorhexidina", "10", "2.00"),
                consumo(2026, 9, 1, "Multivitamínico", "4", "10.00")));

        ReporteService.ReporteConsumo r = service.consumoMedicamentos(desde, hasta);

        assertThat(r.getPorMes()).hasSize(3);
        assertThat(r.getPorMes().get(0).getCosto()).isEqualByComparingTo("7.00");
        assertThat(r.getPorMes().get(2).getCosto()).isEqualByComparingTo("0");
        assertThat(r.getCostoTotal()).isEqualByComparingTo("17.00");
        assertThat(r.getPorMedicamento().get(0).getCosto()).isEqualByComparingTo("15.00");
        assertThat(r.getPorMedicamento().get(0).getCantidad()).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("Rechaza un rango con la fecha desde posterior a hasta")
    void rangoInvalido() {
        assertThatThrownBy(() -> service.liberacionesSobreIngresos(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
