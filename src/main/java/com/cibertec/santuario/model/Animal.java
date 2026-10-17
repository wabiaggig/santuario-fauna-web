package com.cibertec.santuario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tbl_animal")
@Getter
@Setter
@NoArgsConstructor
public class Animal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_animal")
    private Integer idAnimal;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especie", nullable = false)
    private Especie especie;

    // A = albergado; L = liberado; F = fallecido.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "estado", nullable = false, length = 1)
    private String estado;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "estado_salud", nullable = false, length = 1)
    private String estadoSalud;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}
