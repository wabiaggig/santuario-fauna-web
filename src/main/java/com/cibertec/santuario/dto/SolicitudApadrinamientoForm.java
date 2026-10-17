package com.cibertec.santuario.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Datos introducidos por el visitante. Nunca recibe idPadrino del navegador. */
@Getter
@Setter
public class SolicitudApadrinamientoForm {
    private BigDecimal aporteMensual;
    private String observaciones;
}
