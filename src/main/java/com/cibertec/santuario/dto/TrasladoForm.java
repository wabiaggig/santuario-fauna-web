package com.cibertec.santuario.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrasladoForm {
    private Integer idAlojamientoOrigen;
    private Integer idZonaDestino;
    private String observaciones;
}
