package com.cibertec.santuario.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.cibertec.santuario.dto.SolicitudApadrinamientoForm;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.model.Apadrinamiento;
import com.cibertec.santuario.model.Padrino;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.AnimalRepository;
import com.cibertec.santuario.repository.ApadrinamientoRepository;
import com.cibertec.santuario.repository.PadrinoRepository;
import com.cibertec.santuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ApadrinamientoService {
    private final AnimalRepository animalRepository;
    private final PadrinoRepository padrinoRepository;
    private final ApadrinamientoRepository apadrinamientoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<Animal> listarAnimalesDisponibles() {
        return animalRepository.findByActivoTrueAndEstadoOrderByNombreAsc("A");
    }

    @Transactional(readOnly = true)
    public Animal obtenerAnimalDisponible(Integer idAnimal) {
        return animalRepository.findByIdAnimalAndActivoTrueAndEstado(idAnimal, "A")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Animal no disponible para apadrinamiento"));
    }

    public String validarVistaPrevia(SolicitudApadrinamientoForm solicitud) {
        if (solicitud == null) {
            return "Completa los datos de la solicitud.";
        }
        BigDecimal aporte = solicitud.getAporteMensual();
        if (aporte == null || aporte.compareTo(BigDecimal.ZERO) <= 0) {
            return "El aporte mensual debe ser mayor a S/ 0.00.";
        }
        if (aporte.scale() > 2) {
            return "El aporte mensual debe tener como maximo dos decimales.";
        }
        if (aporte.compareTo(new BigDecimal("99999999.99")) > 0) {
            return "El aporte mensual supera el maximo permitido por la base de datos.";
        }
        String observaciones = solicitud.getObservaciones();
        if (observaciones != null && observaciones.length() > 255) {
            return "Las observaciones no pueden superar los 255 caracteres.";
        }
        return null;
    }

    @Transactional
    public Apadrinamiento registrarSolicitud(Integer idAnimal,
            Integer idPadrino, Integer idUsuario,
            SolicitudApadrinamientoForm solicitud) {
        String error = validarVistaPrevia(solicitud);
        if (error != null) {
            throw new IllegalStateException(error);
        }

        Padrino padrino = padrinoRepository.findById(idPadrino)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Padrino no encontrado."));
        if (!Boolean.TRUE.equals(padrino.getActivo()) || padrino.getUsuario() == null
                || !idUsuario.equals(padrino.getUsuario().getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El padrino de la solicitud no corresponde a la cuenta autenticada.");
        }

        Animal animal = obtenerAnimalDisponible(idAnimal);
        if (apadrinamientoRepository
                .existsByPadrino_IdPadrinoAndAnimal_IdAnimalAndFechaFinIsNullAndEstadoSolicitudIn(
                        idPadrino, idAnimal, Set.of(Apadrinamiento.PENDIENTE, Apadrinamiento.APROBADA))) {
            throw new IllegalStateException(
                    "Ya tienes una solicitud pendiente o un apadrinamiento vigente con ese animal.");
        }

        Apadrinamiento nuevo = new Apadrinamiento();
        nuevo.setPadrino(padrino);
        nuevo.setAnimal(animal);
        nuevo.setAporteMensualAcordado(solicitud.getAporteMensual());
        nuevo.setObservaciones(solicitud.getObservaciones());
        nuevo.setEstadoSolicitud(Apadrinamiento.PENDIENTE); //  
        nuevo.setFechaSolicitud(LocalDateTime.now());
        nuevo.setFechaInicio(null); 
        nuevo.setFechaFin(null);
        nuevo.setFechaRevision(null);
        nuevo.setUsuarioRevisor(null);
        nuevo.setMotivoRechazo(null);
        return apadrinamientoRepository.saveAndFlush(nuevo);
    }

    @Transactional(readOnly = true)
    public ResumenSolicitud obtenerResumenPropio(Integer idSolicitud, Integer idPadrino) {
        Apadrinamiento registro = apadrinamientoRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada."));
        if (!idPadrino.equals(registro.getPadrino().getIdPadrino())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No puedes consultar solicitudes de otro padrino.");
        }
        return new ResumenSolicitud(registro.getIdApadrinamiento(),
                registro.getAnimal().getNombre(),
                registro.getAporteMensualAcordado(), registro.getEstadoSolicitud());
    }

 
    @Transactional(readOnly = true)
    public List<SolicitudPendienteVista> listarSolicitudesPendientes() {
        return apadrinamientoRepository
                .findByEstadoSolicitudOrderByFechaSolicitudAscIdApadrinamientoAsc(
                        Apadrinamiento.PENDIENTE)
                .stream().map(this::convertirVistaPendiente).toList();
    }
 
    @Transactional(readOnly = true)
    public SolicitudPendienteVista obtenerSolicitudPendiente(Integer idSolicitud) {
        Apadrinamiento solicitud = apadrinamientoRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada."));
        if (!Apadrinamiento.PENDIENTE.equals(solicitud.getEstadoSolicitud())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La solicitud ya no está pendiente.");
        }
        return convertirVistaPendiente(solicitud);
    }

    private SolicitudPendienteVista convertirVistaPendiente(Apadrinamiento solicitud) {
        Padrino padrino = solicitud.getPadrino();
        Animal animal = solicitud.getAnimal();
        return new SolicitudPendienteVista(
                solicitud.getIdApadrinamiento(),
                (padrino.getNombres() + " " + padrino.getApellidos()).trim(),
                padrino.getCorreoContacto(),
                animal.getNombre(), animal.getCodigo(),
                solicitud.getAporteMensualAcordado(),
                solicitud.getFechaSolicitud(),
                solicitud.getObservaciones(),
                solicitud.getEstadoSolicitud());
    }
 
    @Transactional
    public Integer aprobarSolicitud(Integer idSolicitud, Integer idUsuarioRevisor) {
        Usuario revisor = verificarUsuarioCuidador(idUsuarioRevisor);
        Apadrinamiento solicitud = bloquearPendiente(idSolicitud);
        Animal animal = solicitud.getAnimal();
        if (!Boolean.TRUE.equals(animal.getActivo()) || !"A".equals(animal.getEstado())) {
            throw new IllegalStateException(
                    "El animal ya no está disponible. Revisa su estado antes de aprobar.");
        }
        solicitud.setEstadoSolicitud(Apadrinamiento.APROBADA);
        solicitud.setFechaInicio(LocalDate.now());
        solicitud.setFechaRevision(LocalDateTime.now());
        solicitud.setUsuarioRevisor(revisor);
        solicitud.setMotivoRechazo(null);
        apadrinamientoRepository.saveAndFlush(solicitud);
        return solicitud.getIdApadrinamiento();
    }

    @Transactional
    public Integer rechazarSolicitud(Integer idSolicitud, Integer idUsuarioRevisor,
            String motivoRechazo) {
        Usuario revisor = verificarUsuarioCuidador(idUsuarioRevisor);
        if (motivoRechazo == null || motivoRechazo.trim().isEmpty()) {
            throw new IllegalArgumentException("Debes indicar el motivo del rechazo.");
        }
        String motivo = motivoRechazo.trim();
        if (motivo.length() > 255) {
            throw new IllegalArgumentException("El motivo no puede superar los 255 caracteres.");
        }
        Apadrinamiento solicitud = bloquearPendiente(idSolicitud);
        solicitud.setEstadoSolicitud(Apadrinamiento.RECHAZADA);
        solicitud.setFechaInicio(null);
        solicitud.setFechaRevision(LocalDateTime.now());
        solicitud.setUsuarioRevisor(revisor);
        solicitud.setMotivoRechazo(motivo);
        apadrinamientoRepository.saveAndFlush(solicitud);
        return solicitud.getIdApadrinamiento();
    }

    private Usuario verificarUsuarioCuidador(Integer idUsuario) {
        if (idUsuario == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No existe una sesión válida.");
        }
        Usuario revisor = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario revisor inexistente."));
        if (!Boolean.TRUE.equals(revisor.getActivo()) || revisor.getRol() == null
                || !"Cuidador".equals(revisor.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un cuidador activo puede revisar solicitudes.");
        }
        return revisor;
    }

    private Apadrinamiento bloquearPendiente(Integer idSolicitud) {
        Apadrinamiento solicitud = apadrinamientoRepository.bloquearParaRevision(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada."));
        if (!Apadrinamiento.PENDIENTE.equals(solicitud.getEstadoSolicitud())) {
            throw new IllegalStateException("Esta solicitud ya fue revisada por otro cuidador.");
        }
        return solicitud;
    }
 
    @Transactional(readOnly = true)
    public List<SolicitudHistorialVista> listarMisSolicitudes(Integer idPadrino) {
        if (idPadrino == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No hay padrino autenticado.");
        }
        return apadrinamientoRepository
                .findByPadrino_IdPadrinoOrderByIdApadrinamientoDesc(idPadrino)
                .stream().map(this::convertirVistaHistorial).toList();
    }
 
    @Transactional(readOnly = true)
    public List<SolicitudHistorialVista> listarHistorialGeneral() {
        return apadrinamientoRepository.findAllByOrderByIdApadrinamientoDesc()
                .stream().map(this::convertirVistaHistorial).toList();
    }

    private SolicitudHistorialVista convertirVistaHistorial(Apadrinamiento solicitud) {
        return new SolicitudHistorialVista(
                solicitud.getIdApadrinamiento(),
                (solicitud.getPadrino().getNombres() + " "
                        + solicitud.getPadrino().getApellidos()).trim(),
                solicitud.getAnimal().getNombre(),
                solicitud.getAnimal().getCodigo(),
                solicitud.getAporteMensualAcordado(),
                solicitud.getEstadoSolicitud(),
                solicitud.getFechaSolicitud(),
                solicitud.getFechaInicio(),
                solicitud.getFechaRevision(),
                solicitud.getMotivoRechazo(),
                solicitud.getObservaciones());
    }

    public record SolicitudHistorialVista(
            Integer id, String padrino, String animal, String codigoAnimal,
            BigDecimal monto, String estado, LocalDateTime fechaSolicitud,
            LocalDate fechaInicio, LocalDateTime fechaRevision,
            String motivoRechazo, String observaciones) {}

    public record SolicitudPendienteVista(Integer id, String padrino, String correo,
            String animal, String codigoAnimal, BigDecimal monto,
            LocalDateTime fechaSolicitud, String observaciones, String estado) {}

    public record ResumenSolicitud(Integer id, String animal, BigDecimal monto, String estado) {}
}
