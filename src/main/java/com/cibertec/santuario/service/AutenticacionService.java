package com.cibertec.santuario.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.cibertec.santuario.dto.AutenticacionFilter;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutenticacionService {
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Usuario authenticate(AutenticacionFilter filter) {
        if (filter.correo() == null || filter.clave() == null
                || filter.correo().isBlank() || filter.correo().length() > 120
                || filter.clave().isBlank()
                || filter.clave().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return null;
        }
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(filter.correo().trim());
        if (usuario == null || usuario.getClaveHash() == null) {
            return null;
        }
        if (!encoder.matches(filter.clave(), usuario.getClaveHash())) {
            return null;
        }
        return usuario;
    }
}
