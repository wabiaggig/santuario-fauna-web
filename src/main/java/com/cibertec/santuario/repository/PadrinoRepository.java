package com.cibertec.santuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cibertec.santuario.model.Padrino;

public interface PadrinoRepository extends JpaRepository<Padrino, Integer> {
    Padrino findByUsuarioIdUsuario(Integer idUsuario);
}
