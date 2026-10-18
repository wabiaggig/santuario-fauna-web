package com.cibertec.santuario.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AutenticacionInterceptor autenticacionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/login", "/iniciar-sesion", "/error",
                        "/favicon.ico", "/images/**", "/css/**", "/js/**",
                        "/apadrinamientos/animales");
    }
}
