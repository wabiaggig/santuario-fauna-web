package com.cibertec.santuario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_especie")
@Getter
@Setter
@NoArgsConstructor
public class Especie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especie")
    private Integer idEspecie;

    @Column(name = "nombre_comun", nullable = false, length = 100)
    private String nombreComun;

    @Column(name = "nombre_cientifico", nullable = false, length = 120)
    private String nombreCientifico;
}
