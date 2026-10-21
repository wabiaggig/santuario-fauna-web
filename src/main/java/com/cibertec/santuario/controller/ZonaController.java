package com.cibertec.santuario.controller;

import java.util.Map;
import com.cibertec.santuario.dto.ZonaForm;
import com.cibertec.santuario.model.Zona;
import com.cibertec.santuario.service.AlojamientoService;
import com.cibertec.santuario.service.ZonaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/zonas")
@RequiredArgsConstructor
public class ZonaController {
    private final ZonaService zonaService;
    private final AlojamientoService alojamientoService;

    @InitBinder("form")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "tipo", "capacidad", "descripcion");
    }

    @ModelAttribute("tiposZona")
    public Map<String, String> tiposZona() {
        return Map.of("C", "Cuarentena", "R", "Rehabilitación", "H", "Hábitat");
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "activos") String registros,
            @RequestParam(required = false) Integer cuposMinimos, Model model) {
        Boolean activo = switch (registros) {
            case "activos" -> true;
            case "inactivos" -> false;
            case "todos" -> null;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        };
        model.addAttribute("zonas", zonaService.listar(texto, tipo, activo, cuposMinimos));
        model.addAttribute("ocupacion", zonaService.consultarOcupacion());
        model.addAttribute("texto", texto);
        model.addAttribute("tipo", tipo);
        model.addAttribute("registros", registros);
        model.addAttribute("cuposMinimos", cuposMinimos);
        return "zonas/listado";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("form", new ZonaForm());
        return prepararFormulario(null, model);
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, Model model) {
        model.addAttribute("zona", zonaService.obtener(id));
        model.addAttribute("alojamientos", alojamientoService.listarVigentes("", id));
        return "zonas/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Zona zona = zonaService.obtener(id);
        if (!Boolean.TRUE.equals(zona.getActivo())) {
            return "redirect:/zonas/" + id;
        }
        model.addAttribute("form", zonaService.prepararEdicion(zona));
        return prepararFormulario(id, model);
    }

    @PostMapping("/guardar")
    public String registrar(@ModelAttribute("form") ZonaForm form, BindingResult resultado,
            Model model, HttpSession session, RedirectAttributes flash) {
        return guardar(null, form, resultado, model, session, flash);
    }

    @PostMapping("/{id}/guardar")
    public String actualizar(@PathVariable Integer id, @ModelAttribute("form") ZonaForm form,
            BindingResult resultado, Model model, HttpSession session, RedirectAttributes flash) {
        return guardar(id, form, resultado, model, session, flash);
    }

    @PostMapping("/{id}/baja")
    public String darDeBaja(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean confirmar,
            HttpSession session, RedirectAttributes flash) {
        if (!confirmar) {
            flash.addFlashAttribute("error", "Marca la confirmación para dar de baja la zona.");
            return "redirect:/zonas/" + id;
        }
        return cambiarActivo(id, false, session, flash);
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        return cambiarActivo(id, true, session, flash);
    }

    private String guardar(Integer id, ZonaForm form, BindingResult resultado, Model model,
            HttpSession session, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revisa la capacidad: debe ser un número entero válido.");
            return prepararFormulario(id, model);
        }
        try {
            Integer idUsuario = (Integer) session.getAttribute("idUsuario");
            Zona zona = id == null ? zonaService.registrar(form, idUsuario) : zonaService.actualizar(id, form, idUsuario);
            flash.addFlashAttribute("mensaje", id == null ? "Zona registrada." : "Datos de la zona actualizados.");
            return "redirect:/zonas/" + zona.getIdZona();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar la zona. Comprueba que el nombre no esté registrado y revisa los datos.");
        } catch (TransientDataAccessException ex) {
            model.addAttribute("error", "Otro usuario está actualizando la zona. Vuelve a intentarlo.");
        }
        return prepararFormulario(id, model);
    }

    private String prepararFormulario(Integer id, Model model) {
        model.addAttribute("idZona", id);
        if (id != null) {
            model.addAttribute("zona", zonaService.obtener(id));
            model.addAttribute("ocupacion", zonaService.consultarOcupacion());
        }
        return "zonas/formulario";
    }

    private String cambiarActivo(Integer id, boolean activo, HttpSession session, RedirectAttributes flash) {
        try {
            zonaService.cambiarActivo(id, activo, (Integer) session.getAttribute("idUsuario"));
            flash.addFlashAttribute("mensaje", activo ? "Zona reactivada." : "Zona dada de baja. Su historial se conserva.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (TransientDataAccessException ex) {
            flash.addFlashAttribute("error", "La zona está siendo actualizada. Vuelve a intentarlo.");
        }
        return "redirect:/zonas/" + id;
    }
}
