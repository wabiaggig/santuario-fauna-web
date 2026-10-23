package com.cibertec.santuario.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cibertec.santuario.dto.DonacionForm;
import com.cibertec.santuario.model.Apadrinamiento;
import com.cibertec.santuario.model.Donacion;
import com.cibertec.santuario.model.Padrino;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.ApadrinamientoRepository;
import com.cibertec.santuario.repository.DonacionRepository;
import com.cibertec.santuario.repository.PadrinoRepository;
import com.cibertec.santuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DonacionService {

    private static final Set<String> MEDIOS = Set.of(
            Donacion.YAPE, Donacion.PLIN, Donacion.TRANSFERENCIA, Donacion.EFECTIVO);

    private final DonacionRepository donacionRepository;
    private final PadrinoRepository padrinoRepository;
    private final ApadrinamientoRepository apadrinamientoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ApadrinamientoOpcion> listarApadrinamientosAprobados(Integer idPadrino) {
        return apadrinamientoRepository.findByPadrino_IdPadrinoOrderByIdApadrinamientoDesc(idPadrino)
                .stream()
                .filter(a -> Apadrinamiento.APROBADA.equals(a.getEstadoSolicitud())
                        && a.getFechaInicio() != null && a.getFechaFin() == null)
                .map(a -> new ApadrinamientoOpcion(a.getIdApadrinamiento(),
                        a.getAnimal().getNombre(), a.getAnimal().getCodigo(),
                        a.getAporteMensualAcordado()))
                .toList();
    }
 
    @Transactional
    public Donacion registrar(Integer idPadrino, Integer idUsuario, DonacionForm form) {
        validarFormulario(form);

        Padrino padrino = padrinoRepository.findById(idPadrino)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "El padrino no está registrado."));
        if (!Boolean.TRUE.equals(padrino.getActivo()) || padrino.getUsuario() == null
                || !idUsuario.equals(padrino.getUsuario().getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta no corresponde al padrino de la donación.");
        }

        Apadrinamiento apadrinamiento = null;
        if ("APADRINAMIENTO".equals(form.getTipoDonacion())) {
            apadrinamiento = apadrinamientoRepository.findById(form.getIdApadrinamiento())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El apadrinamiento seleccionado no existe."));
            if (!idPadrino.equals(apadrinamiento.getPadrino().getIdPadrino())
                    || !Apadrinamiento.APROBADA.equals(apadrinamiento.getEstadoSolicitud())
                    || apadrinamiento.getFechaInicio() == null
                    || apadrinamiento.getFechaFin() != null) {
                throw new IllegalArgumentException(
                        "Solo puedes aportar a tus apadrinamientos aprobados y vigentes.");
            }
        }

        LocalDate periodo = null;
        if (apadrinamiento != null) {
            periodo = parsearPeriodo(form.getPeriodoAporte());
            LocalDate inicio = apadrinamiento.getFechaInicio().withDayOfMonth(1);
            LocalDate mesActual = LocalDate.now().withDayOfMonth(1);
            if (periodo.isBefore(inicio) || periodo.isAfter(mesActual)) {
                throw new IllegalArgumentException(
                        "El período debe estar entre el inicio del apadrinamiento y el mes actual.");
            }
        }

        String medio = form.getMedioPago().trim().toUpperCase();
        String referencia = form.getReferencia().trim();
        if (donacionRepository.existsByMedioPagoAndReferencia(medio, referencia)) {
            throw new IllegalArgumentException(
                    "La referencia ya está registrada para ese medio de pago.");
        }

        Donacion donacion = new Donacion();
        donacion.setPadrino(padrino);
        donacion.setApadrinamiento(apadrinamiento);  
        donacion.setPeriodoAporte(periodo);  
        donacion.setMonto(form.getMonto());
        donacion.setMedioPago(medio);
        donacion.setReferencia(referencia);
        donacion.setObservaciones(limpiar(form.getObservaciones()));
        donacion.setComprobanteUrl(limpiar(form.getComprobanteUrl()));
        donacion.setEstadoVerificacion(Donacion.PENDIENTE); 
        return donacionRepository.save(donacion);
    }

    public void validarFormulario(DonacionForm form) {
        if (form == null || form.getTipoDonacion() == null
                || !Set.of("GENERAL", "APADRINAMIENTO").contains(form.getTipoDonacion())) {
            throw new IllegalArgumentException("Selecciona un tipo de donación válido.");
        }
        if ("GENERAL".equals(form.getTipoDonacion()) && form.getIdApadrinamiento() != null) {
            throw new IllegalArgumentException(
                    "Una donación general no debe tener apadrinamiento asociado.");
        }
        if ("APADRINAMIENTO".equals(form.getTipoDonacion())
                && form.getIdApadrinamiento() == null) {
            throw new IllegalArgumentException("Selecciona un apadrinamiento aprobado.");
        }
        if ("GENERAL".equals(form.getTipoDonacion())
                && form.getPeriodoAporte() != null && !form.getPeriodoAporte().isBlank()) {
            throw new IllegalArgumentException("La donación general no tiene período mensual.");
        }
        if ("APADRINAMIENTO".equals(form.getTipoDonacion())) {
            parsearPeriodo(form.getPeriodoAporte());
        }
        BigDecimal monto = form.getMonto();
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0
                || monto.compareTo(new BigDecimal("99999999.99")) > 0
                || monto.scale() > 2) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor a cero, con máximo dos decimales.");
        }
        if (form.getMedioPago() == null
                || !MEDIOS.contains(form.getMedioPago().trim().toUpperCase())) {
            throw new IllegalArgumentException("Selecciona un medio de pago válido.");
        }
        if (form.getReferencia() == null || form.getReferencia().isBlank()
                || form.getReferencia().trim().length() > 100) {
            throw new IllegalArgumentException("Indica una referencia de 1 a 100 caracteres.");
        }
        if (form.getObservaciones() != null && form.getObservaciones().trim().length() > 255) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 255 caracteres.");
        }
        String url = limpiar(form.getComprobanteUrl());
        if (url != null) {
            if (url.length() > 500) {
                throw new IllegalArgumentException("El enlace del comprobante es demasiado largo.");
            }
            try {
                URI uri = URI.create(url);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || uri.getHost() == null || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException("El enlace del comprobante debe ser HTTPS.");
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Ingresa un enlace HTTPS válido para el comprobante.");
            }
        }
    }

    @Transactional(readOnly = true)
    public DonacionVista obtenerDonacionPropia(Integer idDonacion, Integer idPadrino) {
        Donacion d = donacionRepository.findByIdDonacionAndPadrino_IdPadrino(idDonacion, idPadrino)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aporte no encontrado."));
        return convertir(d);
    }

    @Transactional(readOnly = true)
    public List<DonacionVista> listarMisDonaciones(Integer idPadrino) {
        if (idPadrino == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No hay padrino autenticado.");
        }
        return donacionRepository.findByPadrino_IdPadrinoOrderByIdDonacionDesc(idPadrino)
                .stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<DonacionVista> listarPendientes() {
        return donacionRepository
                .findByEstadoVerificacionInOrderByFechaSolicitudAscIdDonacionAsc(
                        Set.of(Donacion.PENDIENTE, Donacion.OBSERVADA))
                .stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public DonacionVista obtenerPendiente(Integer idDonacion) {
        Donacion d = donacionRepository.findById(idDonacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aporte no encontrado."));
        if (!esRevisable(d.getEstadoVerificacion())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El aporte ya fue cerrado y no está en revisión.");
        }
        return convertir(d);
    }
 
    @Transactional
    public void revisarDonacion(Integer idDonacion, Integer idUsuarioRevisor,
                               String accion, String motivoObservacion) {
        Usuario revisor = usuarioRepository.findById(idUsuarioRevisor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cuidador no encontrado."));
        if (!Boolean.TRUE.equals(revisor.getActivo()) || revisor.getRol() == null
                || !"Cuidador".equals(revisor.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un cuidador activo puede verificar donaciones.");
        }
        if (!Set.of(Donacion.CONFIRMADA, Donacion.OBSERVADA, Donacion.RECHAZADA)
                .contains(accion)) {
            throw new IllegalArgumentException("Acción de revisión no válida.");
        }
        String motivo = limpiar(motivoObservacion);
        if (motivoObservacion != null && motivoObservacion.trim().length() > 255) {
            throw new IllegalArgumentException("El motivo no puede exceder 255 caracteres.");
        }
        if ((Donacion.OBSERVADA.equals(accion) || Donacion.RECHAZADA.equals(accion))
                && motivo == null) {
            throw new IllegalArgumentException("Escribe el motivo de la observación o rechazo.");
        }

        Donacion donacion = donacionRepository.bloquearParaRevision(idDonacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aporte no encontrado."));
        String anterior = donacion.getEstadoVerificacion();
        if (!esRevisable(anterior)) {
            throw new IllegalStateException("Este aporte ya fue confirmado o rechazado.");
        }
        if (Donacion.OBSERVADA.equals(anterior) && Donacion.OBSERVADA.equals(accion)) {
            throw new IllegalStateException("El aporte ya está observado; confírmalo o recházalo.");
        }

        donacion.setEstadoVerificacion(accion);
        donacion.setUsuarioRevisor(revisor);
        donacion.setFechaRevision(LocalDateTime.now());
        donacion.setMotivoObservacion(Donacion.CONFIRMADA.equals(accion) ? null : motivo);
        donacionRepository.saveAndFlush(donacion);
    }

    private boolean esRevisable(String estado) {
        return Donacion.PENDIENTE.equals(estado) || Donacion.OBSERVADA.equals(estado);
    }

    private DonacionVista convertir(Donacion d) {
        var a = d.getApadrinamiento();
        return new DonacionVista(d.getIdDonacion(), d.getPadrino().getNombres() + " "
                + d.getPadrino().getApellidos(), d.getPadrino().getCorreoContacto(),
                a == null ? "GENERAL" : "APADRINAMIENTO",
                a == null ? null : a.getIdApadrinamiento(),
                a == null ? null : a.getAnimal().getNombre(),
                d.getMonto(), d.getMedioPago(), d.getReferencia(), d.getObservaciones(),
                d.getComprobanteUrl(), d.getEstadoVerificacion(),
                d.getFechaSolicitud(), d.getFechaDonacion(), d.getFechaRevision(),
                d.getMotivoObservacion(),
                d.getUsuarioRevisor() == null ? null : d.getUsuarioRevisor().getFullName(),
                d.getPeriodoAporte());
    }

    private LocalDate parsearPeriodo(String valor) {
        if (valor == null || !valor.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Selecciona el mes del aporte (AAAA-MM).");
        }
        try {
            return YearMonth.parse(valor).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("El período del aporte no es válido.");
        }
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public record ApadrinamientoOpcion(Integer id, String animal, String codigoAnimal,
                                      BigDecimal montoMensual) { }

    public record DonacionVista(Integer id, String padrino, String correo,
            String tipo, Integer idApadrinamiento, String animal, BigDecimal monto,
            String medioPago, String referencia, String observaciones,
            String comprobanteUrl, String estado, LocalDateTime fechaSolicitud,
            LocalDateTime fechaDonacion, LocalDateTime fechaRevision,
            String motivoObservacion, String revisor, LocalDate periodoAporte) { }
}
