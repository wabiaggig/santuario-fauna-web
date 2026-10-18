package com.cibertec.santuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cibertec.santuario.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Rol findByNombre(String nombre);
}
