package com.cibertec.santuario.controller;

import com.cibertec.santuario.dto.SolicitudApadrinamientoForm;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.service.ApadrinamientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/apadrinamientos")
@RequiredArgsConstructor
public class ApadrinamientoController {
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

    // No registra apadrinamientos. La persistencia se habilitara cuando
    // el modulo de login permita obtener el padrino de la sesion autenticada.
    @PostMapping("/revisar/{id}")
    public String revisarSolicitud(@PathVariable("id") Integer idAnimal,
            @ModelAttribute("solicitud") SolicitudApadrinamientoForm solicitud,
            Model model) {
        Animal animal = apadrinamientoService.obtenerAnimalDisponible(idAnimal);
        model.addAttribute("animal", animal);
        String error = apadrinamientoService.validarVistaPrevia(solicitud);
        if (error != null) {
            model.addAttribute("error", error);
            return "apadrinamiento/solicitud";
        }
        return "apadrinamiento/revision";
    }
}
