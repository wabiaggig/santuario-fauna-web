package com.cibertec.santuario.controller;

import com.cibertec.santuario.dto.SolicitudApadrinamientoForm;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.service.ApadrinamientoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/apadrinamientos")
@RequiredArgsConstructor
public class ApadrinamientoController {

    private static final String BORRADOR = "borradorApadrinamiento";
    private static final String BORRADOR_ANIMAL = "borradorApadrinamientoAnimal";

    private final ApadrinamientoService apadrinamientoService;

    @GetMapping("/animales")
    public String listarAnimales(Model model) {
        model.addAttribute("animales", apadrinamientoService.listarAnimalesDisponibles());
        return "apadrinamiento/animales";
    }

    @GetMapping("/nuevo/{id}")
    public String mostrarFormulario(@PathVariable("id") Integer idAnimal, Model model) {
        Animal animal = apadrinamientoService.obtenerAnimalDisponible(idAnimal);
        model.addAttribute("animal", animal);
        model.addAttribute("solicitud", new SolicitudApadrinamientoForm());
        return "apadrinamiento/solicitud";
    }

    @PostMapping("/revisar/{id}")
    public String revisarSolicitud(@PathVariable("id") Integer idAnimal,
            @ModelAttribute("solicitud") SolicitudApadrinamientoForm solicitud,
            @RequestParam(name = "_csrf", required = false) String token,
            HttpSession session, Model model) {
        validarPadrino(session);
        validarToken(session, token);
        Animal animal = apadrinamientoService.obtenerAnimalDisponible(idAnimal);
        model.addAttribute("animal", animal);

        String error = apadrinamientoService.validarVistaPrevia(solicitud);
        if (error != null) {
            model.addAttribute("error", error);
            return "apadrinamiento/solicitud";
        }

        SolicitudApadrinamientoForm copia = new SolicitudApadrinamientoForm();
        copia.setAporteMensual(solicitud.getAporteMensual());
        copia.setObservaciones(solicitud.getObservaciones() == null
                ? null : solicitud.getObservaciones().trim());
        session.setAttribute(BORRADOR, copia);
        session.setAttribute(BORRADOR_ANIMAL, idAnimal);
        model.addAttribute("solicitud", copia);
        return "apadrinamiento/revision";
    }

    @PostMapping("/registrar/{id}")
    public String registrarSolicitud(@PathVariable("id") Integer idAnimal,
            @RequestParam(name = "_csrf", required = false) String token,
            HttpSession session, Model model) {
        Integer idPadrino = validarPadrino(session);
        validarToken(session, token);
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Object idBorrador = session.getAttribute(BORRADOR_ANIMAL);
        Object formulario = session.getAttribute(BORRADOR);
        if (!(formulario instanceof SolicitudApadrinamientoForm solicitud)
                || !idAnimal.equals(idBorrador)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Primero completa y revisa tu solicitud.");
        }

        try {
            var registrada = apadrinamientoService.registrarSolicitud(
                    idAnimal, idPadrino, idUsuario, solicitud);
            session.removeAttribute(BORRADOR);
            session.removeAttribute(BORRADOR_ANIMAL);
            return "redirect:/apadrinamientos/solicitud-registrada/"
                    + registrada.getIdApadrinamiento();
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            model.addAttribute("animal", apadrinamientoService.obtenerAnimalDisponible(idAnimal));
            model.addAttribute("solicitud", solicitud);
            model.addAttribute("error", "No se pudo registrar la solicitud: "
                    + (ex instanceof DataIntegrityViolationException
                       ? "ya existe un apadrinamiento pendiente o vigente para este animal."
                       : ex.getMessage()));
            return "apadrinamiento/revision";
        }
    }

    @GetMapping("/solicitud-registrada/{id}")
    public String solicitudRegistrada(@PathVariable("id") Integer id,
            HttpSession session, Model model) {
        Integer idPadrino = validarPadrino(session);
        var registro = apadrinamientoService.obtenerResumenPropio(id, idPadrino);
        model.addAttribute("registro", registro);
        return "apadrinamiento/registrada";
    }

    private Integer validarPadrino(HttpSession session) {
        if (!"Padrino".equals(session.getAttribute("nombreRol"))
                || !(session.getAttribute("idPadrino") instanceof Integer idPadrino)
                || !(session.getAttribute("idUsuario") instanceof Integer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un padrino autenticado puede registrar solicitudes.");
        }
        return idPadrino;
    }

    private void validarToken(HttpSession session, String token) {
        if (token == null || !token.equals(session.getAttribute("csrfToken"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Formulario no valido. Actualiza la pagina e intenta de nuevo.");
        }
    }
}
