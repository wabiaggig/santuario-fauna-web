package com.cibertec.santuario.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_usuario")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;
    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;
    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;
    @Column(name = "correo", nullable = false, length = 120)
    private String correo;
    @Column(name = "clave_hash", length = 255)
    private String claveHash;
    @Column(name = "activo", nullable = false)
    private Boolean activo;
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public String getFullName() {
        return String.format("%s %s", nombres, apellidos);
    }
}
