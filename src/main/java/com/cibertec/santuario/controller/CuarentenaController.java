package com.cibertec.santuario.controller;

import com.cibertec.santuario.dto.CuarentenaFilter;
import com.cibertec.santuario.model.Alojamiento;
import com.cibertec.santuario.service.CuarentenaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cuarentenas")
@RequiredArgsConstructor
public class CuarentenaController {
    private final CuarentenaService cuarentenaService;

    @GetMapping
    public String listar(@ModelAttribute("filter") CuarentenaFilter filter, Model model) {
        List<Alojamiento> cuarentenas = cuarentenaService.buscar(filter);
        model.addAttribute("cuarentenas", cuarentenas);
        model.addAttribute("especies", cuarentenaService.listarEspecies());
        model.addAttribute("zonas", cuarentenaService.listarZonas());
        model.addAttribute("diasCuarentena", cuarentenaService.calcularDias(cuarentenas));
        return "cuarentenas/listado";
    }
}
