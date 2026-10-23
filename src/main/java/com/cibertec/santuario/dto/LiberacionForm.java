package com.cibertec.santuario.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class LiberacionForm {
    private Integer idAnimal;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaLiberacion;

    private String lugar;
    private String observaciones;
}
