package com.cibertec.santuario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Perfil opcional: habilita los usuarios originales; no crea usuarios ni padrinos.
@Component
@Profile("demo")
@RequiredArgsConstructor
public class CuentasDemo implements CommandLineRunner {
    private final UsuarioRepository usuarioRepository;
    @Value("${santuario.demo.clave:${SANTUARIO_DEMO_PASSWORD:}}")
    private String clave;

    @Override
    @Transactional
    public void run(String... args) {
        habilitar("elena.vargas@santuario.test", "veterinario@santuario.test");
        habilitar("luis.huaman@santuario.test", "cuidador@santuario.test");
        habilitar("ana.torres@correo.test", "padrino@santuario.test");
    }

    private void habilitar(String correoOriginal, String correoAcceso) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoOriginal);
        Usuario existente = usuarioRepository.findByCorreoIgnoreCase(correoAcceso);
        if (usuario == null) {
            if (existente != null) {
                return; // Ya preparado: no cambiar su clave al reiniciar.
            }
            throw new IllegalStateException("Falta el usuario original: " + correoOriginal);
        }
        if (existente != null && !existente.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IllegalStateException("El correo de acceso ya pertenece a otro usuario: " + correoAcceso);
        }
        if (clave == null || clave.length() < 12
                || clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Habilitar las cuentas requiere una clave local de al menos 12 caracteres y hasta 72 bytes UTF-8.");
        }
        usuario.setCorreo(correoAcceso);
        usuario.setClaveHash(new BCryptPasswordEncoder().encode(clave));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}
