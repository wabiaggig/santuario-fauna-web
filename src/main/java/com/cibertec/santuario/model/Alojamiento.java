package com.cibertec.santuario.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tbl_alojamiento")
@Getter
@Setter
@NoArgsConstructor
public class Alojamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alojamiento")
    private Integer idAlojamiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_animal", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_zona", nullable = false)
    private Zona zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable", nullable = false)
    private Usuario responsable;

    // DATETIME conserva la hora local registrada, sin convertirla entre zonas.
    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "fecha_entrada", nullable = false)
    private LocalDateTime fechaEntrada;

    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Column(length = 255)
    private String observaciones;
}
