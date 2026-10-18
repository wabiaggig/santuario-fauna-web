package com.cibertec.santuario.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_padrino")
@Getter
@Setter
public class Padrino {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_padrino")
    private Integer idPadrino;
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;
    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;
    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento;
    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;
    @Column(name = "correo_contacto", nullable = false, length = 120)
    private String correoContacto;
    @Column(name = "telefono", length = 20)
    private String telefono;
    @Column(name = "activo", nullable = false)
    private Boolean activo;
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
