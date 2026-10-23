package com.cibertec.santuario.controller;

import com.cibertec.santuario.service.ControlAportesService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/donaciones/control-mensual")
@RequiredArgsConstructor
public class ControlAportesController {
    private final ControlAportesService controlAportesService;

    @GetMapping
    public String controlPadrino(HttpSession session, Model model) {
        if (session == null || !(session.getAttribute("idUsuario") instanceof Integer)
                || !"Padrino".equals(session.getAttribute("nombreRol"))
                || !(session.getAttribute("idPadrino") instanceof Integer idPadrino)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el padrino puede acceder a sus cuotas.");
        }
        model.addAttribute("resumen", controlAportesService.controlDelPadrino(idPadrino));
        model.addAttribute("vistaCuidador", false);
        return "donaciones/control-mensual";
    }

    @GetMapping("/cuidador")
    public String controlCuidador(HttpSession session, Model model) {
        if (session == null || !(session.getAttribute("idUsuario") instanceof Integer)
                || !"Cuidador".equals(session.getAttribute("nombreRol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cuidador puede consultar el control general.");
        }
        model.addAttribute("resumen", controlAportesService.controlDelCuidador());
        model.addAttribute("vistaCuidador", true);
        return "donaciones/control-mensual";
    }
}
