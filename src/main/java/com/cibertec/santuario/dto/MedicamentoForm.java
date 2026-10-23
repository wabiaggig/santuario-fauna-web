package com.cibertec.santuario.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicamentoForm {
    private String nombre;
    private String presentacion;
    private String unidadMedida;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal costoUnitario;
}
