package com.cibertec.santuario.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_donacion")
@Getter
@Setter
@NoArgsConstructor
public class Donacion {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String OBSERVADA = "OBSERVADA";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String RECHAZADA = "RECHAZADA";

    public static final String EFECTIVO = "EFECTIVO";
    public static final String TRANSFERENCIA = "TRANSFERENCIA";
    public static final String YAPE = "YAPE";
    public static final String PLIN = "PLIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_donacion")
    private Integer idDonacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_padrino", nullable = false)
    private Padrino padrino;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_apadrinamiento")
    private Apadrinamiento apadrinamiento;

    @Column(name = "fecha_donacion", nullable = false)
    private LocalDateTime fechaDonacion;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "medio_pago", nullable = false, length = 20)
    private String medioPago;

    @Column(name = "referencia", nullable = false, length = 100)
    private String referencia;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Column(name = "estado_verificacion", nullable = false, length = 15)
    private String estadoVerificacion;

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revisor")
    private Usuario usuarioRevisor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "motivo_observacion", length = 255)
    private String motivoObservacion;


    @Column(name = "periodo_aporte")
    private LocalDate periodoAporte;

    
    @PrePersist
    private void prepararAlta() {
        if (estadoVerificacion == null) {
            estadoVerificacion = PENDIENTE;
        }
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
        if (fechaDonacion == null) {
            fechaDonacion = LocalDateTime.now();
        }
    }
}
