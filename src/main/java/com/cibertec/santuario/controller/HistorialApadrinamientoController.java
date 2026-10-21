package com.cibertec.santuario.controller;

import com.cibertec.santuario.service.ApadrinamientoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
 
@Controller
@RequestMapping("/apadrinamientos/mis-solicitudes")
@RequiredArgsConstructor
public class HistorialApadrinamientoController {

    private final ApadrinamientoService apadrinamientoService;

    @GetMapping
    public String misSolicitudes(HttpSession session, Model model) {
        if (session == null || !"Padrino".equals(session.getAttribute("nombreRol"))
                || !(session.getAttribute("idPadrino") instanceof Integer idPadrino)
                || !(session.getAttribute("idUsuario") instanceof Integer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un padrino autenticado puede consultar sus solicitudes.");
        }
        model.addAttribute("solicitudes", apadrinamientoService.listarMisSolicitudes(idPadrino));
        return "apadrinamiento/mis-solicitudes";
    }
}
