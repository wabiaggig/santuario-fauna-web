package com.cibertec.santuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cibertec.santuario.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Usuario findByCorreoIgnoreCase(String correo);
}
