package com.cibertec.santuario.controller;

import com.cibertec.santuario.service.ApadrinamientoService;
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
@RequestMapping("/apadrinamientos/solicitudes")
@RequiredArgsConstructor
public class RevisionApadrinamientoController {

    private final ApadrinamientoService apadrinamientoService;

    @GetMapping
    public String listarPendientes(HttpSession session, Model model) {
        validarCuidador(session);
        var solicitudes = apadrinamientoService.listarSolicitudesPendientes();
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("totalPendientes", solicitudes.size());
        return "apadrinamiento/pendientes";
    }
 
    @GetMapping("/historial")
    public String historial(HttpSession session, Model model) {
        validarCuidador(session);
        model.addAttribute("solicitudes", apadrinamientoService.listarHistorialGeneral());
        return "apadrinamiento/historial-solicitudes";
    }

    @GetMapping("/{id}")
    public String verDetalle(@PathVariable("id") Integer idSolicitud,
            HttpSession session, Model model) {
        validarCuidador(session);
        model.addAttribute("solicitud", apadrinamientoService.obtenerSolicitudPendiente(idSolicitud));
        return "apadrinamiento/detalle-solicitud";
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable("id") Integer idSolicitud,
            @RequestParam(name = "_csrf", required = false) String token,
            HttpSession session, RedirectAttributes flash) {
        Integer idRevisor = validarCuidador(session);
        validarToken(session, token);
        try {
            apadrinamientoService.aprobarSolicitud(idSolicitud, idRevisor);
            flash.addFlashAttribute("mensaje", "Solicitud N.º " + idSolicitud + " aprobada correctamente.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/apadrinamientos/solicitudes";
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable("id") Integer idSolicitud,
            @RequestParam(name = "motivoRechazo", required = false) String motivo,
            @RequestParam(name = "_csrf", required = false) String token,
            HttpSession session, RedirectAttributes flash) {
        Integer idRevisor = validarCuidador(session);
        validarToken(session, token);
        try {
            apadrinamientoService.rechazarSolicitud(idSolicitud, idRevisor, motivo);
            flash.addFlashAttribute("mensaje", "Solicitud N.º " + idSolicitud + " rechazada correctamente.");
            return "redirect:/apadrinamientos/solicitudes";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/apadrinamientos/solicitudes/" + idSolicitud;
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/apadrinamientos/solicitudes";
        }
    }

    private Integer validarCuidador(HttpSession session) {
        if (session == null || !(session.getAttribute("idUsuario") instanceof Integer idUsuario)
                || !"Cuidador".equals(session.getAttribute("nombreRol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el cuidador puede revisar solicitudes de apadrinamiento.");
        }
        return idUsuario;
    }

    private void validarToken(HttpSession session, String token) {
        if (session == null || token == null || !token.equals(session.getAttribute("csrfToken"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Formulario no válido. Actualiza la página e intenta de nuevo.");
        }
    }
}
