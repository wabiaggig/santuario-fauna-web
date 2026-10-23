package com.cibertec.santuario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class AtencionMedicaRequestDTO {
    private Integer idAnimal;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaAtencion;

    private String diagnostico;
    private String tratamiento;
    private String estadoSaludResultante;
    private String observaciones;

    private List<DetalleItemDTO> detalles = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetalleItemDTO {
        private Integer idMedicamento;
        private BigDecimal cantidad;
    }
}
