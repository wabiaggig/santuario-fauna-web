package com.cibertec.santuario.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.cibertec.santuario.dto.FilaConsumoMedicamento;
import com.cibertec.santuario.dto.FilaIngresoLiberacion;
import com.cibertec.santuario.repository.ReporteRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {
    private static final Locale ES = Locale.forLanguageTag("es-PE");
    private static final long MAX_MESES = 60;

    private final ReporteRepository reporteRepository;

    @Getter
    @RequiredArgsConstructor
    public static class FilaTrimestre {
        private final String etiqueta;
        private final long ingresos;
        private final long liberaciones;
        private final BigDecimal porcentaje;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ReporteLiberaciones {
        private final List<FilaTrimestre> filas;
        private final long totalIngresos;
        private final long totalLiberaciones;
        private final BigDecimal porcentajeTotal;
    }

    public ReporteLiberaciones liberacionesSobreIngresos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        Map<String, long[]> datos = new HashMap<>();
        for (FilaIngresoLiberacion f : reporteRepository.ingresosYLiberacionesPorTrimestre(desde, hasta)) {
            datos.put(f.getAnio().intValue() + "-" + f.getTrimestre().intValue(),
                    new long[] { f.getIngresos().longValue(), f.getLiberaciones().longValue() });
        }
        List<FilaTrimestre> filas = new ArrayList<>();
        long totalIngresos = 0;
        long totalLiberaciones = 0;
        LocalDate cursor = inicioTrimestre(desde);
        while (!cursor.isAfter(hasta)) {
            int anio = cursor.getYear();
            int trimestre = cursor.get(IsoFields.QUARTER_OF_YEAR);
            long[] valores = datos.getOrDefault(anio + "-" + trimestre, new long[] { 0, 0 });
            filas.add(new FilaTrimestre("T" + trimestre + " " + anio, valores[0], valores[1],
                    porcentaje(valores[1], valores[0])));
            totalIngresos += valores[0];
            totalLiberaciones += valores[1];
            cursor = cursor.plusMonths(3);
        }
        return new ReporteLiberaciones(filas, totalIngresos, totalLiberaciones,
                porcentaje(totalLiberaciones, totalIngresos));
    }

    @Getter
    @RequiredArgsConstructor
    public static class FilaConsumo {
        private final String mes;
        private final String medicamento;
        private final String presentacion;
        private final String unidadMedida;
        private final BigDecimal cantidad;
        private final BigDecimal costo;
    }

    @Getter
    @RequiredArgsConstructor
    public static class TotalMes {
        private final String mes;
        private final BigDecimal costo;
    }

    @Getter
    @RequiredArgsConstructor
    public static class TotalMedicamento {
        private final String medicamento;
        private final String unidadMedida;
        private final BigDecimal cantidad;
        private final BigDecimal costo;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ReporteConsumo {
        private final List<FilaConsumo> detalle;
        private final List<TotalMes> porMes;
        private final List<TotalMedicamento> porMedicamento;
        private final BigDecimal costoTotal;
    }

    public ReporteConsumo consumoMedicamentos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<FilaConsumo> detalle = new ArrayList<>();
        Map<YearMonth, BigDecimal> costoMes = new LinkedHashMap<>();
        Map<Integer, TotalMedicamento> porMedicamento = new LinkedHashMap<>();
        BigDecimal costoTotal = BigDecimal.ZERO;

        for (YearMonth ym = YearMonth.from(desde); !ym.isAfter(YearMonth.from(hasta)); ym = ym.plusMonths(1)) {
            costoMes.put(ym, BigDecimal.ZERO);
        }
        for (FilaConsumoMedicamento f : reporteRepository.consumoMedicamentosPorMes(desde, hasta.plusDays(1))) {
            YearMonth ym = YearMonth.of(f.getAnio().intValue(), f.getMes().intValue());
            BigDecimal cantidad = decimal(f.getCantidad());
            BigDecimal costo = decimal(f.getCosto());
            String nombre = f.getNombre();
            detalle.add(new FilaConsumo(etiquetaMes(ym), nombre, f.getPresentacion(), f.getUnidadMedida(), cantidad, costo));
            costoMes.merge(ym, costo, BigDecimal::add);
            porMedicamento.merge(f.getIdMedicamento(),
                    new TotalMedicamento(nombre + " - " + f.getPresentacion(), f.getUnidadMedida(), cantidad, costo),
                    (a, b) -> new TotalMedicamento(a.getMedicamento(), a.getUnidadMedida(),
                            a.getCantidad().add(b.getCantidad()), a.getCosto().add(b.getCosto())));
            costoTotal = costoTotal.add(costo);
        }
        List<TotalMes> porMes = new ArrayList<>();
        costoMes.forEach((ym, costo) -> porMes.add(new TotalMes(etiquetaMes(ym), costo)));
        List<TotalMedicamento> totales = new ArrayList<>(porMedicamento.values());
        totales.sort((a, b) -> b.getCosto().compareTo(a.getCosto()));
        return new ReporteConsumo(detalle, porMes, totales, costoTotal);
    }

    public void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Indica las fechas desde y hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a hasta.");
        }
        if (ChronoUnit.MONTHS.between(YearMonth.from(desde), YearMonth.from(hasta)) >= MAX_MESES) {
            throw new IllegalArgumentException("El rango máximo es de 5 años.");
        }
    }

    private static LocalDate inicioTrimestre(LocalDate fecha) {
        int primerMes = (fecha.get(IsoFields.QUARTER_OF_YEAR) - 1) * 3 + 1;
        return LocalDate.of(fecha.getYear(), primerMes, 1);
    }

    private static BigDecimal porcentaje(long parte, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(parte * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Number n) {
        if (n == null) {
            return BigDecimal.ZERO;
        }
        return n instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                : new BigDecimal(n.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String etiquetaMes(YearMonth ym) {
        String mes = ym.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " " + ym.getYear();
    }
}
