package com.cibertec.santuario.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class DonacionForm {
    private String tipoDonacion = "GENERAL";
    private Integer idApadrinamiento; 
    private String periodoAporte;
    private BigDecimal monto;
    private String medioPago;
    private String referencia;
    private String observaciones;
    private String comprobanteUrl;
}
