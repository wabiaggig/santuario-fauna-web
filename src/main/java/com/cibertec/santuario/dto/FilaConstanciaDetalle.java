package com.cibertec.santuario.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Una fila de la tabla de medicamentos del PDF.
// Los nombres de los campos coinciden con los Fields del reporte.
@Getter
@AllArgsConstructor
public class FilaConstanciaDetalle {
    private final String medicamento;
    private final BigDecimal cantidad;
    private final BigDecimal costoUnitario;
    private final BigDecimal subtotal;
}