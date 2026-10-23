package com.cibertec.santuario.controller;

import java.time.YearMonth;
import com.cibertec.santuario.dto.DonacionForm;
import com.cibertec.santuario.service.DonacionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/donaciones")
@RequiredArgsConstructor
public class DonacionController {

    private final DonacionService donacionService;

    @GetMapping("/mis-donaciones")
    public String historial(HttpSession session, Model model) {
        Integer idPadrino = validarPadrino(session);
        model.addAttribute("donaciones", donacionService.listarMisDonaciones(idPadrino));
        return "donaciones/mis-donaciones";
    }

    @GetMapping("/nueva")
    public String nueva(HttpSession session, Model model) {
        Integer idPadrino = validarPadrino(session);
        DonacionForm formulario = new DonacionForm();
        formulario.setPeriodoAporte(YearMonth.now().toString());
        model.addAttribute("donacion", formulario);
        model.addAttribute("apadrinamientos", donacionService.listarApadrinamientosAprobados(idPadrino));
        return "donaciones/nueva";
    }

    @PostMapping("/registrar")
    public String registrar(@ModelAttribute("donacion") DonacionForm form,
                            BindingResult bindingResult,
                            @RequestParam(name = "_csrf", required = false) String token,
                            HttpSession session, Model model) {
        Integer idPadrino = validarPadrino(session);
        validarToken(session, token);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Revisa el monto y los campos ingresados.");
            model.addAttribute("apadrinamientos", donacionService.listarApadrinamientosAprobados(idPadrino));
            return "donaciones/nueva";
        }
        try {
            var guardada = donacionService.registrar(
                    idPadrino, (Integer) session.getAttribute("idUsuario"), form);
            return "redirect:/donaciones/registrada/" + guardada.getIdDonacion();
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", ex instanceof DataIntegrityViolationException
                    ? "La referencia ya existe o la operación incumple una restricción de MySQL."
                    : ex.getMessage());
            model.addAttribute("apadrinamientos", donacionService.listarApadrinamientosAprobados(idPadrino));
            return "donaciones/nueva";
        }
    }

    @GetMapping("/registrada/{id}")
    public String registrada(@PathVariable("id") Integer id,
                            HttpSession session, Model model) {
        Integer idPadrino = validarPadrino(session);
        model.addAttribute("donacion", donacionService.obtenerDonacionPropia(id, idPadrino));
        return "donaciones/registrada";
    }

    private Integer validarPadrino(HttpSession session) {
        if (session == null || !(session.getAttribute("idUsuario") instanceof Integer)
                || !"Padrino".equals(session.getAttribute("nombreRol"))
                || !(session.getAttribute("idPadrino") instanceof Integer idPadrino)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un padrino autenticado puede registrar donaciones.");
        }
        return idPadrino;
    }

    private void validarToken(HttpSession session, String token) {
        if (token == null || !token.equals(session.getAttribute("csrfToken"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Formulario inválido. Actualiza la página e inténtalo nuevamente.");
        }
    }
}
