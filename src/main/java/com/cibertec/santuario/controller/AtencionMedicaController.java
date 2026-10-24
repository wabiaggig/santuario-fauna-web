package com.cibertec.santuario.controller;

import java.time.LocalDate;

import java.util.Map;

import com.cibertec.santuario.dto.AtencionMedicaRequestDTO;
import com.cibertec.santuario.model.AtencionMedica;
import com.cibertec.santuario.service.AtencionMedicaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.cibertec.santuario.service.ConstanciaPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping("/atenciones")
@RequiredArgsConstructor
public class AtencionMedicaController {
    private final AtencionMedicaService atencionService;
    private final ConstanciaPdfService constanciaPdfService;

    @ModelAttribute("estadosSalud")
    public Map<String, String> estadosSalud() {
        return Map.of("E", "En evaluación", "T", "En tratamiento", "S", "Saludable");
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer idAnimal, Model model) {
        model.addAttribute("atenciones", atencionService.listarRecientes(idAnimal));
        model.addAttribute("animales", atencionService.listarTodosLosAnimales());
        model.addAttribute("idAnimal", idAnimal);
        return "atenciones/listado";
    }

    @GetMapping("/nuevo")
    public String nuevaAtencion(Model model) {
        AtencionMedicaRequestDTO form = new AtencionMedicaRequestDTO();
        form.setFechaAtencion(LocalDate.now());
        model.addAttribute("form", form);
        return prepararFormulario(model);
    }

    @PostMapping("/registrar")
    public String registrarAtencion(@ModelAttribute("form") AtencionMedicaRequestDTO form,
            BindingResult resultado, Model model, HttpSession session, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            model.addAttribute("error", "Revisa la fecha y las cantidades: deben tener un formato válido.");
            return prepararFormulario(model);
        }
        try {
            Integer idVeterinario = (Integer) session.getAttribute("idUsuario");
            AtencionMedica guardada = atencionService.registrarAtencion(form, idVeterinario);
            flash.addFlashAttribute("mensaje", "Atención N.° " + guardada.getIdAtencion()
                    + " registrada. Se actualizó el stock de " + guardada.getDetalles().size() + " medicamento(s).");
            return "redirect:/atenciones";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar la atención. Revisa los datos e inténtalo de nuevo.");
        } catch (TransientDataAccessException ex) {
            model.addAttribute("error", "Otro usuario está usando estos medicamentos. Vuelve a intentarlo.");
        }
        return prepararFormulario(model);
    }
    
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> constanciaPdf(@PathVariable("id") Integer id) {
        try {
            byte[] pdf = constanciaPdfService.generar(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=constancia_atencion_" + id + ".pdf")
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private String prepararFormulario(Model model) {
        model.addAttribute("animales", atencionService.listarAnimalesAtendibles());
        model.addAttribute("medicamentos", atencionService.listarMedicamentosActivos());
        model.addAttribute("hoy", LocalDate.now());
        return "atenciones/nuevo";
    }
}
