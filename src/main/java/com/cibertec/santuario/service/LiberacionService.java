package com.cibertec.santuario.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import com.cibertec.santuario.dto.LiberacionForm;
import com.cibertec.santuario.model.Alojamiento;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.model.Apadrinamiento;
import com.cibertec.santuario.model.Liberacion;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.AlojamientoRepository;
import com.cibertec.santuario.repository.AnimalRepository;
import com.cibertec.santuario.repository.ApadrinamientoRepository;
import com.cibertec.santuario.repository.LiberacionRepository;
import com.cibertec.santuario.repository.UsuarioRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LiberacionService {
    private static final List<String> APADRINAMIENTOS_ABIERTOS =
            List.of(Apadrinamiento.PENDIENTE, Apadrinamiento.APROBADA);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LiberacionRepository liberacionRepository;
    private final AnimalRepository animalRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final ApadrinamientoRepository apadrinamientoRepository;
    private final UsuarioRepository usuarioRepository;

    @Getter
    @RequiredArgsConstructor
    public static class Impacto {
        private final Animal animal;
        private final Alojamiento alojamiento;
        private final List<Apadrinamiento> apadrinamientos;
    }

    @Transactional(readOnly = true)
    public List<Liberacion> listar() {
        return liberacionRepository.findAllByOrderByFechaLiberacionDescIdLiberacionDesc();
    }

    @Transactional(readOnly = true)
    public List<Animal> listarLiberables() {
        return animalRepository.findByActivoTrueAndEstadoOrderByNombreAsc("A").stream()
                .filter(a -> "S".equals(a.getEstadoSalud()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Impacto consultarImpacto(Integer idAnimal) {
        Animal animal = animalRepository.findByIdAnimal(idAnimal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal no encontrado."));
        Alojamiento alojamiento = alojamientoRepository.findByAnimalIdAnimalAndFechaSalidaIsNull(idAnimal).orElse(null);
        List<Apadrinamiento> apadrinamientos = apadrinamientoRepository
                .findByAnimal_IdAnimalAndFechaFinIsNullAndEstadoSolicitudIn(idAnimal, APADRINAMIENTOS_ABIERTOS);
        return new Impacto(animal, alojamiento, apadrinamientos);
    }

    @Transactional
    public Liberacion liberar(LiberacionForm form, Integer idVeterinario) {
        Usuario veterinario = comprobarVeterinario(idVeterinario);
        validarFormulario(form);
        LocalDate fecha = form.getFechaLiberacion();

        Animal animal = animalRepository.bloquearPorId(form.getIdAnimal())
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un animal válido."));
        if (!Boolean.TRUE.equals(animal.getActivo()) || !"A".equals(animal.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden liberar animales albergados.");
        }
        if (!"S".equals(animal.getEstadoSalud())) {
            throw new IllegalArgumentException("El animal debe estar en estado Saludable para ser liberado. Registra antes una atención médica que lo confirme.");
        }
        if (liberacionRepository.existsByAnimalIdAnimal(animal.getIdAnimal())) {
            throw new IllegalArgumentException("Este animal ya tiene una liberación registrada.");
        }
        if (fecha.isBefore(animal.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha de liberación no puede ser anterior al ingreso del animal.");
        }

        // Antes de marcarlo como liberado se cierran cuarentena, alojamiento y apadrinamientos.
        if (animal.getFechaInicioCuarentena() != null) {
            if (animal.getFechaFinCuarentena() == null) {
                if (fecha.isBefore(animal.getFechaInicioCuarentena())) {
                    throw new IllegalArgumentException("La fecha de liberación no puede ser anterior al inicio de la cuarentena.");
                }
                animal.setFechaFinCuarentena(fecha);
            } else if (animal.getFechaFinCuarentena().isAfter(fecha)) {
                throw new IllegalArgumentException("La fecha de liberación no puede ser anterior al fin de la cuarentena ("
                        + animal.getFechaFinCuarentena().format(FECHA) + ").");
            }
        }

        Optional<Alojamiento> vigente = alojamientoRepository.findByAnimalIdAnimalAndFechaSalidaIsNull(animal.getIdAnimal());
        if (vigente.isPresent()) {
            Alojamiento alojamiento = vigente.get();
            LocalDateTime salida = fecha.equals(LocalDate.now())
                    ? LocalDateTime.now().withNano(0)
                    : fecha.atTime(LocalTime.of(23, 59, 59));
            if (salida.isBefore(alojamiento.getFechaEntrada())) {
                throw new IllegalArgumentException("La fecha de liberación no puede ser anterior a la entrada en su zona actual ("
                        + alojamiento.getFechaEntrada().format(FECHA) + ").");
            }
            alojamiento.setFechaSalida(salida);
        }

        for (Apadrinamiento a : apadrinamientoRepository
                .findByAnimal_IdAnimalAndFechaFinIsNullAndEstadoSolicitudIn(animal.getIdAnimal(), APADRINAMIENTOS_ABIERTOS)) {
            if (Apadrinamiento.APROBADA.equals(a.getEstadoSolicitud())) {
                if (a.getFechaInicio() != null && fecha.isBefore(a.getFechaInicio())) {
                    throw new IllegalArgumentException("La fecha de liberación no puede ser anterior al inicio de un apadrinamiento ("
                            + a.getFechaInicio().format(FECHA) + ").");
                }
                a.setFechaFin(fecha);
            } else {
                a.setEstadoSolicitud(Apadrinamiento.RECHAZADA);
                a.setMotivoRechazo("El animal fue liberado el " + fecha.format(FECHA) + ".");
                a.setUsuarioRevisor(veterinario);
                a.setFechaRevision(LocalDateTime.now().withNano(0));
            }
        }

        animal.setEstado("L");
        animal.setFechaSalida(fecha);

        Liberacion liberacion = new Liberacion();
        liberacion.setAnimal(animal);
        liberacion.setResponsable(veterinario);
        liberacion.setFechaLiberacion(fecha);
        liberacion.setLugar(form.getLugar().trim());
        liberacion.setObservaciones(form.getObservaciones() == null || form.getObservaciones().isBlank()
                ? null : form.getObservaciones().trim());
        return liberacionRepository.save(liberacion);
    }

    private void validarFormulario(LiberacionForm form) {
        if (form.getIdAnimal() == null) {
            throw new IllegalArgumentException("Selecciona el animal a liberar.");
        }
        if (form.getFechaLiberacion() == null) {
            throw new IllegalArgumentException("Indica la fecha de liberación.");
        }
        if (form.getFechaLiberacion().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de liberación no puede ser futura.");
        }
        if (form.getLugar() == null || form.getLugar().isBlank() || form.getLugar().trim().length() > 150) {
            throw new IllegalArgumentException("Indica el lugar de liberación (máximo 150 caracteres).");
        }
        if (form.getObservaciones() != null && form.getObservaciones().length() > 2000) {
            throw new IllegalArgumentException("Las observaciones admiten hasta 2000 caracteres.");
        }
    }

    private Usuario comprobarVeterinario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() == null
                || !"Veterinario".equals(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return usuario;
    }
}
