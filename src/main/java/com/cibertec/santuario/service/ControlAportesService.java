package com.cibertec.santuario.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.santuario.model.Apadrinamiento;
import com.cibertec.santuario.model.Donacion;
import com.cibertec.santuario.repository.ApadrinamientoRepository;
import com.cibertec.santuario.repository.DonacionRepository;

import lombok.RequiredArgsConstructor;

/** Control informativo: distribuye los aportes, pero no mueve dinero ni altera donaciones. */
@Service
@RequiredArgsConstructor
public class ControlAportesService {

    private static final int MESES_VISIBLES = 12;
    private static final int MESES_FUTUROS = 12;
    private static final Set<String> EN_REVISION = Set.of(Donacion.PENDIENTE, Donacion.OBSERVADA);

    private final ApadrinamientoRepository apadrinamientoRepository;
    private final DonacionRepository donacionRepository;

    @Transactional(readOnly = true)
    public Resumen controlDelPadrino(Integer idPadrino) {
        List<Apadrinamiento> activos = apadrinamientoRepository
                .findByPadrino_IdPadrinoOrderByIdApadrinamientoDesc(idPadrino).stream()
                .filter(this::estaVigente).toList();
        return construir(activos);
    }

    @Transactional(readOnly = true)
    public Resumen controlDelCuidador() {
        List<Apadrinamiento> activos = apadrinamientoRepository
                .findAllByOrderByIdApadrinamientoDesc().stream()
                .filter(this::estaVigente).toList();
        return construir(activos);
    }

    private boolean estaVigente(Apadrinamiento a) {
        return Apadrinamiento.APROBADA.equals(a.getEstadoSolicitud())
                && a.getFechaInicio() != null && a.getFechaFin() == null
                && a.getAporteMensualAcordado() != null
                && a.getAporteMensualAcordado().compareTo(BigDecimal.ZERO) > 0;
    }

    private Resumen construir(List<Apadrinamiento> activos) {
        if (activos.isEmpty()) return new Resumen(List.of(), 0, 0, 0, 0, 0);

        LocalDate mesActual = LocalDate.now().withDayOfMonth(1);
        LocalDate primerMesVisible = mesActual.minusMonths(MESES_VISIBLES - 1);
        LocalDate ultimoMesAdelantado = mesActual.plusMonths(MESES_FUTUROS);

        // Se consulta desde el comienzo real de los apadrinamientos, incluso si
        // es anterior a los 12 meses visibles, para no aplicar dos veces saldos viejos.
        LocalDate primerMesConsulta = activos.stream()
                .map(a -> a.getFechaInicio().withDayOfMonth(1))
                .min(LocalDate::compareTo).orElse(primerMesVisible);

        List<Donacion> donaciones = donacionRepository
                .findByPeriodoAporteBetweenAndApadrinamientoIsNotNullOrderByIdDonacionDesc(
                        primerMesConsulta, ultimoMesAdelantado);

        Map<Clave, Totales> porMes = new HashMap<>();
        for (Donacion d : donaciones) {
            if (d.getApadrinamiento() == null || d.getPeriodoAporte() == null
                    || d.getMonto() == null || d.getMonto().signum() <= 0) continue;

            LocalDate periodo = d.getPeriodoAporte().withDayOfMonth(1);
            Clave clave = new Clave(d.getApadrinamiento().getIdApadrinamiento(), periodo);
            Totales total = porMes.computeIfAbsent(clave, k -> new Totales());

            if (Donacion.CONFIRMADA.equals(d.getEstadoVerificacion())) {
                total.confirmado = total.confirmado.add(d.getMonto());
            } else if (EN_REVISION.contains(d.getEstadoVerificacion())) {
                total.enRevision = total.enRevision.add(d.getMonto());
            } 
        }

        List<Fila> filas = new ArrayList<>();
        for (Apadrinamiento a : activos) {
            LocalDate inicio = a.getFechaInicio().withDayOfMonth(1);
            BigDecimal cuota = a.getAporteMensualAcordado();
            BigDecimal saldoConfirmado = BigDecimal.ZERO;
            BigDecimal saldoEnRevision = BigDecimal.ZERO;
            List<Fila> filasDelApadrinamiento = new ArrayList<>();
 
            for (LocalDate mes = inicio; !mes.isAfter(ultimoMesAdelantado); mes = mes.plusMonths(1)) {
                Totales ingresos = porMes.getOrDefault(
                        new Clave(a.getIdApadrinamiento(), mes), new Totales());
                saldoConfirmado = saldoConfirmado.add(ingresos.confirmado);
                saldoEnRevision = saldoEnRevision.add(ingresos.enRevision);

                BigDecimal confirmadoAplicado = saldoConfirmado.min(cuota);
                saldoConfirmado = saldoConfirmado.subtract(confirmadoAplicado);

                BigDecimal faltante = cuota.subtract(confirmadoAplicado);
                BigDecimal enRevisionAplicado = saldoEnRevision.min(faltante);
                saldoEnRevision = saldoEnRevision.subtract(enRevisionAplicado);

 
                boolean mostrar = !mes.isBefore(primerMesVisible)
                        && (!mes.isAfter(mesActual)
                                || confirmadoAplicado.signum() > 0
                                || enRevisionAplicado.signum() > 0);
                if (mostrar) {
                    filasDelApadrinamiento.add(new Fila(
                            a.getIdApadrinamiento(), a.getAnimal().getNombre(),
                            a.getPadrino().getNombres() + " " + a.getPadrino().getApellidos(),
                            mes, cuota, confirmadoAplicado, enRevisionAplicado, faltante,
                            determinarEstado(cuota, confirmadoAplicado, enRevisionAplicado)));
                }
            }
             
            for (int i = filasDelApadrinamiento.size() - 1; i >= 0; i--) {
                filas.add(filasDelApadrinamiento.get(i));
            }
        }

        int cubiertos = (int) filas.stream().filter(f -> "CUBIERTO".equals(f.estado())).count();
        int enRevision = (int) filas.stream().filter(f -> "EN_VERIFICACION".equals(f.estado())).count();
        int parciales = (int) filas.stream().filter(f -> "PARCIAL".equals(f.estado())).count();
        int pendientes = (int) filas.stream().filter(f -> "PENDIENTE".equals(f.estado())).count();
        return new Resumen(List.copyOf(filas), filas.size(), cubiertos, enRevision, parciales, pendientes);
    }
 
    public static String determinarEstado(BigDecimal cuota, BigDecimal confirmado, BigDecimal enRevision) {
        if (confirmado.compareTo(cuota) >= 0) return "CUBIERTO";
        if (confirmado.add(enRevision).compareTo(cuota) >= 0) return "EN_VERIFICACION";
        if (confirmado.compareTo(BigDecimal.ZERO) > 0) return "PARCIAL";
        return "PENDIENTE";
    }

    private record Clave(Integer idApadrinamiento, LocalDate periodo) { }

    private static final class Totales {
        private BigDecimal confirmado = BigDecimal.ZERO;
        private BigDecimal enRevision = BigDecimal.ZERO;
    }

    public record Fila(Integer idApadrinamiento, String animal, String padrino,
            LocalDate periodo, BigDecimal cuota, BigDecimal confirmado,
            BigDecimal enRevision, BigDecimal faltante, String estado) { }

    public record Resumen(List<Fila> filas, int total, int cubiertos,
            int enRevision, int parciales, int pendientes) { }
}
