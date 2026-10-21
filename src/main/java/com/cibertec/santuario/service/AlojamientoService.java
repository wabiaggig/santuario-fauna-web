package com.cibertec.santuario.service;

import java.time.LocalDateTime;
import java.util.List;
import com.cibertec.santuario.dto.TrasladoForm;
import com.cibertec.santuario.model.Alojamiento;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.model.Zona;
import com.cibertec.santuario.repository.AlojamientoRepository;
import com.cibertec.santuario.repository.AnimalRepository;
import com.cibertec.santuario.repository.UsuarioRepository;
import com.cibertec.santuario.repository.ZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AlojamientoService {
    private final AlojamientoRepository alojamientoRepository;
    private final AnimalRepository animalRepository;
    private final ZonaRepository zonaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<Alojamiento> listarVigentes(String texto, Integer idZona) {
        if (texto.length() > 80 || (idZona != null && idZona <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros no válidos.");
        }
        return alojamientoRepository.buscarVigentes(texto.trim(), idZona);
    }

    @Transactional(readOnly = true)
    public Alojamiento obtenerVigente(Integer idAnimal) {
        animalRepository.findById(idAnimal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal no encontrado."));
        return alojamientoRepository.findByAnimalIdAnimalAndFechaSalidaIsNull(idAnimal)
                .orElseThrow(() -> new IllegalArgumentException("El animal no tiene un alojamiento vigente para trasladar."));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void trasladar(Integer idAnimal, TrasladoForm form, Integer idUsuario) {
        Usuario cuidador = obtenerCuidador(idUsuario);
        Animal animal = animalRepository.bloquearPorId(idAnimal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal no encontrado."));
        if (!Boolean.TRUE.equals(animal.getActivo()) || !"A".equals(animal.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden trasladar animales albergados con ficha activa.");
        }
        Alojamiento origen = alojamientoRepository.findByAnimalIdAnimalAndFechaSalidaIsNull(idAnimal)
                .orElseThrow(() -> new IllegalArgumentException("El animal ya no tiene un alojamiento vigente. Actualiza la ficha."));
        if (!origen.getIdAlojamiento().equals(form.getIdAlojamientoOrigen())) {
            throw new IllegalArgumentException("El alojamiento cambió desde que abriste el formulario. Actualiza los datos antes de trasladar.");
        }
        Integer idOrigen = origen.getZona().getIdZona();
        Integer idDestino = form.getIdZonaDestino();
        if (idDestino == null || idDestino <= 0 || idDestino.equals(idOrigen)) {
            throw new IllegalArgumentException("Selecciona una zona de destino diferente de la actual.");
        }
        if (form.getObservaciones() != null && form.getObservaciones().length() > 255) {
            throw new IllegalArgumentException("Las observaciones del traslado admiten hasta 255 caracteres.");
        }
        Zona primera = bloquearZona(Math.min(idOrigen, idDestino));
        Zona segunda = bloquearZona(Math.max(idOrigen, idDestino));
        Zona destino = primera.getIdZona().equals(idDestino) ? primera : segunda;
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw new IllegalArgumentException("La zona de destino está dada de baja.");
        }
        boolean cuarentenaAbierta = animal.getFechaInicioCuarentena() != null && animal.getFechaFinCuarentena() == null;
        if (cuarentenaAbierta && !"C".equals(destino.getTipo())) {
            throw new IllegalArgumentException("El animal sigue en cuarentena. El veterinario debe autorizar su finalización antes de trasladarlo a rehabilitación o hábitat.");
        }
        if (!cuarentenaAbierta && "C".equals(destino.getTipo())) {
            throw new IllegalArgumentException("El animal no tiene una cuarentena abierta. El inicio de una nueva cuarentena requiere evaluación veterinaria.");
        }
        if (alojamientoRepository.countByZonaIdZonaAndFechaSalidaIsNull(idDestino) >= destino.getCapacidad()) {
            throw new IllegalArgumentException("La zona de destino ya no tiene cupos. El traslado no se registró.");
        }
        LocalDateTime fecha = LocalDateTime.now().withNano(0);
        if (fecha.isBefore(origen.getFechaEntrada()) || fecha.toLocalDate().isBefore(animal.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha del alojamiento es posterior a la fecha actual. Revisa la ficha antes de trasladar.");
        }
        origen.setFechaSalida(fecha);
        alojamientoRepository.saveAndFlush(origen);

        Alojamiento nuevo = new Alojamiento();
        nuevo.setAnimal(animal);
        nuevo.setZona(destino);
        nuevo.setResponsable(cuidador);
        nuevo.setFechaEntrada(fecha);
        nuevo.setObservaciones(form.getObservaciones() == null || form.getObservaciones().isBlank()
                ? null : form.getObservaciones().trim());
        alojamientoRepository.saveAndFlush(nuevo);
    }

    private Zona bloquearZona(Integer id) {
        return zonaRepository.bloquearPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("La zona seleccionada no existe."));
    }

    private Usuario obtenerCuidador(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() == null
                || !"Cuidador".equals(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return usuario;
    }
}
