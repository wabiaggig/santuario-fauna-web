package com.cibertec.santuario.controller;

import java.util.Map;
import com.cibertec.santuario.dto.TrasladoForm;
import com.cibertec.santuario.model.Alojamiento;
import com.cibertec.santuario.service.AlojamientoService;
import com.cibertec.santuario.service.ZonaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alojamientos")
@RequiredArgsConstructor
public class AlojamientoController {
    private final AlojamientoService alojamientoService;
    private final ZonaService zonaService;

    @InitBinder("form")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("idAlojamientoOrigen", "idZonaDestino", "observaciones");
    }

    @ModelAttribute("tiposZona")
    public Map<String, String> tiposZona() {
        return Map.of("C", "Cuarentena", "R", "Rehabilitación", "H", "Hábitat");
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String texto,
            @RequestParam(required = false) Integer idZona, Model model) {
        model.addAttribute("alojamientos", alojamientoService.listarVigentes(texto, idZona));
        model.addAttribute("zonas", zonaService.listar("", "", null, null));
        model.addAttribute("texto", texto);
        model.addAttribute("idZona", idZona);
        return "alojamientos/listado";
    }

    @GetMapping("/trasladar/{idAnimal}")
    public String mostrarTraslado(@PathVariable Integer idAnimal, Model model, RedirectAttributes flash) {
        try {
            Alojamiento actual = alojamientoService.obtenerVigente(idAnimal);
            if (!Boolean.TRUE.equals(actual.getAnimal().getActivo()) || !"A".equals(actual.getAnimal().getEstado())) {
                throw new IllegalArgumentException("Solo se pueden trasladar animales albergados con ficha activa.");
            }
            TrasladoForm form = new TrasladoForm();
            form.setIdAlojamientoOrigen(actual.getIdAlojamiento());
            model.addAttribute("form", form);
            return prepararTraslado(idAnimal, model);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/animales/" + idAnimal;
        }
    }

    @PostMapping("/trasladar/{idAnimal}")
    public String trasladar(@PathVariable Integer idAnimal, @ModelAttribute("form") TrasladoForm form,
            BindingResult resultado, Model model, HttpSession session, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Selecciona un alojamiento y una zona de destino válidos.");
        } else {
            try {
                alojamientoService.trasladar(idAnimal, form, (Integer) session.getAttribute("idUsuario"));
                flash.addFlashAttribute("mensaje", "Traslado registrado. El alojamiento anterior quedó cerrado y se conserva en el historial.");
                return "redirect:/animales/" + idAnimal;
            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                model.addAttribute("error", "No se pudo guardar el traslado. Se conservó el alojamiento anterior y no se guardaron cambios parciales.");
            } catch (TransientDataAccessException ex) {
                model.addAttribute("error", "Otro usuario está actualizando los alojamientos. Actualiza los datos y vuelve a intentarlo.");
            }
        }
        try {
            return prepararTraslado(idAnimal, model);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/animales/" + idAnimal;
        }
    }

    private String prepararTraslado(Integer idAnimal, Model model) {
        Alojamiento actual = alojamientoService.obtenerVigente(idAnimal);
        model.addAttribute("actual", actual);
        model.addAttribute("zonas", zonaService.listarActivas());
        model.addAttribute("ocupacion", zonaService.consultarOcupacion());
        model.addAttribute("cuarentenaAbierta", actual.getAnimal().getFechaInicioCuarentena() != null
                && actual.getAnimal().getFechaFinCuarentena() == null);
        return "alojamientos/traslado";
    }
}
