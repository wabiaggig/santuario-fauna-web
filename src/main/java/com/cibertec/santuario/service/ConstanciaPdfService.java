package com.cibertec.santuario.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.santuario.dto.FilaConstanciaDetalle;
import com.cibertec.santuario.model.AtencionMedica;
import com.cibertec.santuario.repository.AtencionMedicaRepository;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
@RequiredArgsConstructor
public class ConstanciaPdfService {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<String, String> SALUD =
            Map.of("E", "En evaluación", "T", "En tratamiento", "S", "Saludable");

    private final AtencionMedicaRepository atencionRepository;

    @Transactional(readOnly = true)
    public byte[] generar(Integer idAtencion) {
        // 1. Buscar la atención con todo lo necesario
        AtencionMedica atencion = atencionRepository.findConDetallesByIdAtencion(idAtencion)
                .orElseThrow(() -> new IllegalArgumentException("No existe la atención N.° " + idAtencion));

        // 2. Convertir los detalles en filas para la tabla del reporte
        List<FilaConstanciaDetalle> filas = atencion.getDetalles().stream()
                .map(d -> new FilaConstanciaDetalle(
                        d.getMedicamento().getNombre(),
                        d.getCantidad(),
                        d.getCostoUnitario(),
                        d.getCantidad().multiply(d.getCostoUnitario())))
                .toList();

        BigDecimal total = filas.stream()
                .map(FilaConstanciaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Parámetros: los mismos nombres que creaste en Jaspersoft Studio
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("idAtencion", atencion.getIdAtencion());
        parametros.put("fecha", atencion.getFechaAtencion().format(FORMATO_FECHA));
        parametros.put("animal", String.format("%s (%s · %s)",
                atencion.getAnimal().getNombre(),
                atencion.getAnimal().getCodigo(),
                atencion.getAnimal().getEspecie().getNombreComun()));
        parametros.put("veterinario", atencion.getVeterinario().getFullName());
        parametros.put("diagnostico", atencion.getDiagnostico());
        parametros.put("tratamiento", atencion.getTratamiento());
        parametros.put("estadoResultante",
                SALUD.getOrDefault(atencion.getEstadoSaludResultante(), atencion.getEstadoSaludResultante()));
        parametros.put("total", total);

        // 4. Compilar el .jrxml, llenarlo con los datos y exportar a PDF
        try (InputStream jrxml = new ClassPathResource("reportes/constancia_atencion.jrxml").getInputStream()) {
            JasperReport reporte = JasperCompileManager.compileReport(jrxml);
            JasperPrint impreso = JasperFillManager.fillReport(
                    reporte, parametros, new JRBeanCollectionDataSource(filas));
            return JasperExportManager.exportReportToPdf(impreso);
        } catch (IOException | JRException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la constancia.", e);
        }
    }
}