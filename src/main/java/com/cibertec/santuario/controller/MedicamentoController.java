package com.cibertec.santuario.controller;

import java.math.BigDecimal;
import com.cibertec.santuario.dto.MedicamentoForm;
import com.cibertec.santuario.model.Medicamento;
import com.cibertec.santuario.service.MedicamentoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/medicamentos")
@RequiredArgsConstructor
public class MedicamentoController {
    private final MedicamentoService medicamentoService;

    @InitBinder("form")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "presentacion", "unidadMedida", "stockActual", "stockMinimo", "costoUnitario");
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "false") boolean stockBajo,
            @RequestParam(defaultValue = "activos") String registros, Model model) {
        Boolean activo = switch (registros) {
            case "activos" -> true;
            case "inactivos" -> false;
            case "todos" -> null;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        };
        model.addAttribute("medicamentos", medicamentoService.listar(texto, stockBajo, activo));
        model.addAttribute("totalStockBajo", medicamentoService.contarConStockBajo());
        model.addAttribute("texto", texto);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("registros", registros);
        return "medicamentos/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new MedicamentoForm());
        return prepararFormulario(null, model);
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Integer id, Model model) {
        Medicamento medicamento = medicamentoService.obtener(id);
        model.addAttribute("medicamento", medicamento);
        model.addAttribute("stockBajo", medicamentoService.tieneStockBajo(medicamento));
        model.addAttribute("consumos", medicamentoService.listarConsumosRecientes(id));
        return "medicamentos/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Medicamento medicamento = medicamentoService.obtener(id);
        if (!Boolean.TRUE.equals(medicamento.getActivo())) {
            return "redirect:/medicamentos/" + id;
        }
        model.addAttribute("form", medicamentoService.prepararEdicion(medicamento));
        return prepararFormulario(id, model);
    }

    @PostMapping("/guardar")
    public String registrar(@ModelAttribute("form") MedicamentoForm form, BindingResult resultado,
            Model model, HttpSession session, RedirectAttributes flash) {
        return guardar(null, form, resultado, model, session, flash);
    }

    @PostMapping("/{id}/guardar")
    public String actualizar(@PathVariable Integer id, @ModelAttribute("form") MedicamentoForm form,
            BindingResult resultado, Model model, HttpSession session, RedirectAttributes flash) {
        return guardar(id, form, resultado, model, session, flash);
    }

    @PostMapping("/{id}/reponer")
    public String reponer(@PathVariable Integer id, @RequestParam(required = false) BigDecimal cantidad,
            HttpSession session, RedirectAttributes flash) {
        try {
            Medicamento m = medicamentoService.reponerStock(id, cantidad, idUsuario(session));
            flash.addFlashAttribute("mensaje", "Stock repuesto. Nuevo stock: "
                    + m.getStockActual().toPlainString() + " " + m.getUnidadMedida() + ".");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (TransientDataAccessException ex) {
            flash.addFlashAttribute("error", "El medicamento está siendo actualizado. Vuelve a intentarlo.");
        }
        return "redirect:/medicamentos/" + id;
    }

    @PostMapping("/{id}/baja")
    public String darDeBaja(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean confirmar,
            HttpSession session, RedirectAttributes flash) {
        if (!confirmar) {
            flash.addFlashAttribute("error", "Marca la confirmación para dar de baja el medicamento.");
            return "redirect:/medicamentos/" + id;
        }
        return cambiarActivo(id, false, session, flash);
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        return cambiarActivo(id, true, session, flash);
    }

    private String guardar(Integer id, MedicamentoForm form, BindingResult resultado, Model model,
            HttpSession session, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revisa las cantidades y el costo: deben ser números válidos.");
            return prepararFormulario(id, model);
        }
        try {
            Medicamento m = id == null
                    ? medicamentoService.registrar(form, idUsuario(session))
                    : medicamentoService.actualizar(id, form, idUsuario(session));
            flash.addFlashAttribute("mensaje", id == null ? "Medicamento registrado." : "Datos del medicamento actualizados.");
            return "redirect:/medicamentos/" + m.getIdMedicamento();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar. Comprueba que el nombre y la presentación no estén registrados.");
        } catch (TransientDataAccessException ex) {
            model.addAttribute("error", "Otro usuario está actualizando el medicamento. Vuelve a intentarlo.");
        }
        return prepararFormulario(id, model);
    }

    private String prepararFormulario(Integer id, Model model) {
        model.addAttribute("idMedicamento", id);
        if (id != null) {
            model.addAttribute("medicamento", medicamentoService.obtener(id));
        }
        return "medicamentos/formulario";
    }

    private String cambiarActivo(Integer id, boolean activo, HttpSession session, RedirectAttributes flash) {
        try {
            medicamentoService.cambiarActivo(id, activo, idUsuario(session));
            flash.addFlashAttribute("mensaje", activo
                    ? "Medicamento reactivado."
                    : "Medicamento dado de baja. Ya no aparecerá al registrar atenciones; su historial se conserva.");
        } catch (TransientDataAccessException ex) {
            flash.addFlashAttribute("error", "El medicamento está siendo actualizado. Vuelve a intentarlo.");
        }
        return "redirect:/medicamentos/" + id;
    }

    private Integer idUsuario(HttpSession session) {
        return (Integer) session.getAttribute("idUsuario");
    }
}
