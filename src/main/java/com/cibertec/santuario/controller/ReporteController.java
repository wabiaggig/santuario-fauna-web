package com.cibertec.santuario.controller;

import java.time.LocalDate;
import com.cibertec.santuario.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {
    private final ReporteService reporteService;

    @GetMapping("/liberaciones")
    public String liberaciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate d = desde != null ? desde : hoy.withDayOfMonth(1).minusMonths(12);
        LocalDate h = hasta != null ? hasta : hoy;
        prepararFechas(model, d, h);
        try {
            model.addAttribute("reporte", reporteService.liberacionesSobreIngresos(d, h));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "reportes/liberaciones";
    }

    @GetMapping("/medicamentos")
    public String medicamentos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate d = desde != null ? desde : hoy.withDayOfMonth(1).minusMonths(11);
        LocalDate h = hasta != null ? hasta : hoy;
        prepararFechas(model, d, h);
        try {
            model.addAttribute("reporte", reporteService.consumoMedicamentos(d, h));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "reportes/medicamentos";
    }

    private void prepararFechas(Model model, LocalDate desde, LocalDate hasta) {
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("hoy", LocalDate.now());
    }
}
