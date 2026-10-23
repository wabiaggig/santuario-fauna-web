package com.cibertec.santuario.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;
import com.cibertec.santuario.controller.AnimalController;
import com.cibertec.santuario.controller.ZonaController;
import com.cibertec.santuario.controller.AlojamientoController;
import com.cibertec.santuario.controller.CuarentenaController;


@Component
public class AutenticacionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("idUsuario") instanceof Integer)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        String ruta = request.getServletPath();
        HandlerMethod metodo = (HandlerMethod) handler;
        Class<?> controlador = metodo.getBeanType();
        boolean consulta = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        boolean veterinario = "Veterinario".equals(session.getAttribute("nombreRol"));
        if (CuarentenaController.class.isAssignableFrom(controlador)
                && !"Cuidador".equals(session.getAttribute("nombreRol")) && !veterinario) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        if (AnimalController.class.isAssignableFrom(controlador)
                || ZonaController.class.isAssignableFrom(controlador)
                || AlojamientoController.class.isAssignableFrom(controlador)) {
            boolean fichaVeterinario = veterinario && consulta
                    && AnimalController.class.isAssignableFrom(controlador)
                    && "detalle".equals(metodo.getMethod().getName());
            if (!"Cuidador".equals(session.getAttribute("nombreRol")) && !fichaVeterinario) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            if (!consulta) {
                String token = request.getParameter("_csrf");
                if (token == null || !token.equals(session.getAttribute("csrfToken"))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
            }
        }
        if ((ruta.startsWith("/apadrinamientos/nuevo/")
                || ruta.startsWith("/apadrinamientos/revisar/"))
                && (!"Padrino".equals(session.getAttribute("nombreRol"))
                    || !(session.getAttribute("idPadrino") instanceof Integer))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        response.setHeader("Cache-Control", "no-store");
        return true;
    }
}
