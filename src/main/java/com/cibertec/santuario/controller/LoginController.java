package com.cibertec.santuario.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cibertec.santuario.dto.AutenticacionFilter;
import com.cibertec.santuario.model.Padrino;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.PadrinoRepository;
import com.cibertec.santuario.service.AutenticacionService;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final AutenticacionService autenticacionService;
    private final PadrinoRepository padrinoRepository;

    @GetMapping("/login")
    public String login(Model model, HttpSession session, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());
        }
        model.addAttribute("filter", new AutenticacionFilter(null, null));
        return "login";
    }

    // POST evita enviar la contrasena en la URL, a diferencia de la demo inicial.
    @PostMapping("/iniciar-sesion")
    public String iniciarSesion(@ModelAttribute AutenticacionFilter filter,
            @RequestParam(name = "_csrf", required = false) String token,
            Model model, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        validarToken(request, token);
        Usuario usuario = autenticacionService.authenticate(filter);
        String error = null;
        Padrino padrino = null;
        if (usuario == null) {
            error = "Correo o contraseña incorrectos.";
        } else if (!Boolean.TRUE.equals(usuario.getActivo())) {
            error = "Tu cuenta está inactiva. Comunícate con el responsable del santuario.";
        } else if (usuario.getRol() == null
                || !java.util.Set.of("Veterinario", "Cuidador", "Padrino").contains(usuario.getRol().getNombre())) {
            error = "Tu cuenta no tiene un rol habilitado.";
        } else if ("Padrino".equals(usuario.getRol().getNombre())) {
            padrino = padrinoRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
            if (padrino == null || !Boolean.TRUE.equals(padrino.getActivo())) {
                error = "Tu cuenta necesita un padrino activo vinculado. Comunícate con el santuario.";
            }
        }
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("filter", new AutenticacionFilter(filter.correo(), null));
            return "login";
        }

        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());
        session.setAttribute("idUsuario", usuario.getIdUsuario());
        session.setAttribute("fullName", usuario.getFullName());
        session.setAttribute("idRol", usuario.getRol().getIdRol());
        session.setAttribute("nombreRol", usuario.getRol().getNombre());
        if (padrino != null) {
            session.setAttribute("idPadrino", padrino.getIdPadrino());
        }
        return "redirect:/panel";
    }

    @GetMapping("/panel")
    public String panel() {
        return "panel";
    }

    @PostMapping("/cerrar-sesion")
    public String cerrarSesion(HttpServletRequest request, RedirectAttributes flash,
            @RequestParam(name = "_csrf", required = false) String token) {
        validarToken(request, token);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        flash.addFlashAttribute("mensaje", "Cerraste sesión correctamente.");
        return "redirect:/login";
    }

    // Token del formulario: evita iniciar o cerrar una sesion desde otro sitio.
    private void validarToken(HttpServletRequest request, String token) {
        HttpSession session = request.getSession(false);
        if (session == null || token == null || !token.equals(session.getAttribute("csrfToken"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Formulario no válido. Vuelve a iniciar sesión.");
        }
    }
}
