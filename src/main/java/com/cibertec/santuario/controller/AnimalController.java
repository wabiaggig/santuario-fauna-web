package com.cibertec.santuario.controller;

import java.time.LocalDate;
import java.util.Map;
import com.cibertec.santuario.dto.AnimalForm;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.service.AnimalService;
import com.cibertec.santuario.service.AtencionMedicaService;
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
@RequestMapping("/animales")
@RequiredArgsConstructor
public class AnimalController {
    private final AnimalService animalService;
    private final AtencionMedicaService atencionMedicaService;

    @InitBinder("form")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("codigo", "nombre", "idEspecie", "sexo", "fechaIngreso",
                "procedencia", "motivoIngreso", "fotoUrl", "observaciones", "idZona");
    }

    @ModelAttribute("estados")
    public Map<String, String> estados() {
        return Map.of("A", "Albergado", "L", "Liberado", "F", "Fallecido");
    }

    @ModelAttribute("salud")
    public Map<String, String> salud() {
        return Map.of("E", "En evaluación", "T", "En tratamiento", "S", "Saludable");
    }

    @ModelAttribute("sexos")
    public Map<String, String> sexos() {
        return Map.of("M", "Macho", "H", "Hembra", "D", "Desconocido");
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "activos") String registros, Model model) {
        Boolean activo = switch (registros) {
            case "activos" -> true;
            case "inactivos" -> false;
            case "todos" -> null;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        };
        model.addAttribute("animales", animalService.listar(texto, estado, activo));
        model.addAttribute("texto", texto);
        model.addAttribute("estado", estado);
        model.addAttribute("registros", registros);
        return "animales/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        AnimalForm form = new AnimalForm();
        form.setFechaIngreso(LocalDate.now());
        model.addAttribute("form", form);
        return prepararFormulario(null, model);
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, Model model) {
        model.addAttribute("animal", animalService.obtener(id));
        model.addAttribute("alojamientos", animalService.listarAlojamientos(id));
        model.addAttribute("atenciones", atencionMedicaService.listarPorAnimal(id));
        return "animales/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Animal animal = animalService.obtener(id);
        if (!Boolean.TRUE.equals(animal.getActivo())) {
            return "redirect:/animales/" + id;
        }
        model.addAttribute("form", animalService.prepararEdicion(animal));
        return prepararFormulario(id, model);
    }

    @PostMapping("/guardar")
    public String registrar(@ModelAttribute("form") AnimalForm form, BindingResult resultado,
            Model model, HttpSession session, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revisa la fecha de ingreso y las opciones seleccionadas.");
            return prepararFormulario(null, model);
        }
        try {
            Animal animal = animalService.registrar(form, (Integer) session.getAttribute("idUsuario"));
            flash.addFlashAttribute("mensaje", "Ingreso registrado con su alojamiento inicial en cuarentena.");
            return "redirect:/animales/" + animal.getIdAnimal();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar el ingreso. Comprueba que el código no esté registrado y que los datos sigan vigentes. No se guardaron cambios parciales.");
        } catch (TransientDataAccessException ex) {
            model.addAttribute("error", "Otro usuario está actualizando esos datos. Vuelve a intentar el ingreso.");
        }
        return prepararFormulario(null, model);
    }

    @PostMapping("/{id}/guardar")
    public String actualizar(@PathVariable Integer id, @ModelAttribute("form") AnimalForm form,
            BindingResult resultado, Model model, HttpSession session, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revisa los valores del formulario.");
            return prepararFormulario(id, model);
        }
        try {
            animalService.actualizar(id, form, (Integer) session.getAttribute("idUsuario"));
            flash.addFlashAttribute("mensaje", "Datos de la ficha actualizados.");
            return "redirect:/animales/" + id;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudieron guardar los datos. Revisa el código y las opciones seleccionadas.");
        } catch (TransientDataAccessException ex) {
            model.addAttribute("error", "Otro usuario está actualizando esta ficha. Vuelve a intentarlo.");
        }
        return prepararFormulario(id, model);
    }

    @GetMapping("/{id}/baja")
    public String confirmarBaja(@PathVariable Integer id, Model model) {
        model.addAttribute("animal", animalService.obtener(id));
        return "animales/baja";
    }

    @PostMapping("/{id}/baja")
    public String darDeBaja(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        return cambiarActivo(id, false, session, flash);
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        return cambiarActivo(id, true, session, flash);
    }

    private String cambiarActivo(Integer id, boolean activo, HttpSession session, RedirectAttributes flash) {
        try {
            animalService.cambiarActivo(id, activo, (Integer) session.getAttribute("idUsuario"));
            flash.addFlashAttribute("mensaje", activo ? "Ficha reactivada." : "Ficha dada de baja. Se conserva su historial.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (TransientDataAccessException ex) {
            flash.addFlashAttribute("error", "Otro usuario está actualizando esta ficha. Vuelve a intentarlo.");
        }
        return "redirect:/animales/" + id;
    }

    private String prepararFormulario(Integer id, Model model) {
        model.addAttribute("idAnimal", id);
        model.addAttribute("especies", animalService.listarEspecies());
        model.addAttribute("hoy", LocalDate.now());
        if (id == null) {
            model.addAttribute("zonas", animalService.listarCuarentenas());
            model.addAttribute("cupos", animalService.consultarCupos());
        } else {
            model.addAttribute("animal", animalService.obtener(id));
        }
        return "animales/formulario";
    }
}
