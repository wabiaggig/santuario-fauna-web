package com.cibertec.santuario.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;


@Getter
@Setter
public class AnimalForm {
    private String codigo;
    private String nombre;
    private Integer idEspecie;
    private String sexo = "D";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;
    private String procedencia;
    private String motivoIngreso;
    private String fotoUrl;
    private String observaciones;
    private Integer idZona;
}
