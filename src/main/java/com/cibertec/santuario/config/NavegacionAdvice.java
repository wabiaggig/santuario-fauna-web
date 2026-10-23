package com.cibertec.santuario.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavegacionAdvice {
    @ModelAttribute("rutaActual")
    public String rutaActual(HttpServletRequest request) {
        return request.getServletPath();
    }
}
