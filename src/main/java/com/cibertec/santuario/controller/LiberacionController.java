package com.cibertec.santuario.controller;

import java.time.LocalDate;
import com.cibertec.santuario.dto.LiberacionForm;
import com.cibertec.santuario.model.Liberacion;
import com.cibertec.santuario.service.LiberacionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/liberaciones")
@RequiredArgsConstructor
public class LiberacionController {
    private final LiberacionService liberacionService;

    @InitBinder("form")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("idAnimal", "fechaLiberacion", "lugar", "observaciones");
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("liberaciones", liberacionService.listar());
        return "liberaciones/listado";
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam(required = false) Integer idAnimal, Model model) {
        LiberacionForm form = new LiberacionForm();
        form.setIdAnimal(idAnimal);
        form.setFechaLiberacion(LocalDate.now());
        model.addAttribute("form", form);
        return prepararFormulario(form, model);
    }

    @PostMapping("/registrar")
    public String registrar(@ModelAttribute("form") LiberacionForm form, BindingResult resultado,
            Model model, HttpSession session, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revisa la fecha: debe tener un formato válido.");
            return prepararFormulario(form, model);
        }
        try {
            Liberacion liberacion = liberacionService.liberar(form, (Integer) session.getAttribute("idUsuario"));
            flash.addFlashAttribute("mensaje", "Liberación registrada: " + liberacion.getAnimal().getNombre()
                    + " volvió a su hábitat.");
            return "redirect:/liberaciones";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo registrar la liberación. Revisa las fechas del animal e inténtalo de nuevo.");
        } catch (TransientDataAccessException ex) {
            model.addAttribute("error", "Otro usuario está actualizando este animal. Vuelve a intentarlo.");
        }
        return prepararFormulario(form, model);
    }

    private String prepararFormulario(LiberacionForm form, Model model) {
        var liberables = liberacionService.listarLiberables();
        model.addAttribute("animales", liberables);
        model.addAttribute("hoy", LocalDate.now());
        boolean seleccionValida = form.getIdAnimal() != null
                && liberables.stream().anyMatch(a -> a.getIdAnimal().equals(form.getIdAnimal()));
        if (seleccionValida) {
            model.addAttribute("impacto", liberacionService.consultarImpacto(form.getIdAnimal()));
        }
        return "liberaciones/nueva";
    }
}
