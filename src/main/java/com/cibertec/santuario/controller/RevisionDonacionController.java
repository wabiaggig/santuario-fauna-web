package com.cibertec.santuario.controller;

import com.cibertec.santuario.model.Donacion;
import com.cibertec.santuario.service.DonacionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/donaciones/pendientes")
@RequiredArgsConstructor
public class RevisionDonacionController {

    private final DonacionService donacionService;

    @GetMapping
    public String pendientes(HttpSession session, Model model) {
        validarCuidador(session);
        var lista = donacionService.listarPendientes();
        model.addAttribute("donaciones", lista);
        model.addAttribute("totalPendientes", lista.size());
        return "donaciones/pendientes";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Integer id, HttpSession session, Model model) {
        validarCuidador(session);
        model.addAttribute("donacion", donacionService.obtenerPendiente(id));
        return "donaciones/detalle";
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable("id") Integer id,
            @RequestParam(name = "_csrf", required = false) String token,
            HttpSession session, RedirectAttributes flash) {
        return revisar(id, "confirmar", Donacion.CONFIRMADA, null, token, session, flash);
    }

    @PostMapping("/{id}/observar")
    public String observar(@PathVariable("id") Integer id,
            @RequestParam(name = "motivoObservacion", required = false) String motivo,
            @RequestParam(name = "_csrf", required = false) String token,
            HttpSession session, RedirectAttributes flash) {
        return revisar(id, "observar", Donacion.OBSERVADA, motivo, token, session, flash);
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable("id") Integer id,
            @RequestParam(name = "motivoObservacion", required = false) String motivo,
            @RequestParam(name = "_csrf", required = false) String token,
            HttpSession session, RedirectAttributes flash) {
        return revisar(id, "rechazar", Donacion.RECHAZADA, motivo, token, session, flash);
    }

    private String revisar(Integer id, String verbo, String accion, String motivo,
            String token, HttpSession session, RedirectAttributes flash) {
        Integer idCuidador = validarCuidador(session);
        validarToken(session, token);
        try {
            donacionService.revisarDonacion(id, idCuidador, accion, motivo);
            flash.addFlashAttribute("mensaje", "Donación N.º " + id + ": acción " + verbo + " completada.");
            return "redirect:/donaciones/pendientes";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/donaciones/pendientes/" + id;
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/donaciones/pendientes";
        }
    }

    private Integer validarCuidador(HttpSession session) {
        if (session == null || !(session.getAttribute("idUsuario") instanceof Integer idUsuario)
                || !"Cuidador".equals(session.getAttribute("nombreRol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el cuidador puede revisar donaciones.");
        }
        return idUsuario;
    }

    private void validarToken(HttpSession session, String token) {
        if (token == null || !token.equals(session.getAttribute("csrfToken"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Formulario inválido. Actualiza la página e inténtalo nuevamente.");
        }
    }
}
