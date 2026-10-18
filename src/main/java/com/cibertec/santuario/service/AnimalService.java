package com.cibertec.santuario.service;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import com.cibertec.santuario.dto.AnimalForm;
import com.cibertec.santuario.model.*;
import com.cibertec.santuario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AnimalService {
    private final AnimalRepository animalRepository;
    private final EspecieRepository especieRepository;
    private final ZonaRepository zonaRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<Animal> listar(String texto, String estado, Boolean activo) {
        if (!Set.of("", "A", "L", "F").contains(estado) || texto.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros no válidos.");
        }
        return animalRepository.buscar(texto.trim(), estado, activo);
    }

    @Transactional(readOnly = true)
    public Animal obtener(Integer id) {
        return animalRepository.findByIdAnimal(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal no encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Especie> listarEspecies() {
        return especieRepository.findByActivoTrueOrderByNombreComunAsc();
    }

    @Transactional(readOnly = true)
    public List<Zona> listarCuarentenas() {
        return zonaRepository.findByActivoTrueAndTipoOrderByNombreAsc("C");
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> consultarCupos() {
        Map<Integer, Long> cupos = new LinkedHashMap<>();
        for (Zona zona : listarCuarentenas()) {
            long ocupados = alojamientoRepository.countByZonaIdZonaAndFechaSalidaIsNull(zona.getIdZona());
            cupos.put(zona.getIdZona(), Math.max(0L, zona.getCapacidad() - ocupados));
        }
        return cupos;
    }

    @Transactional(readOnly = true)
    public List<Alojamiento> listarAlojamientos(Integer id) {
        return alojamientoRepository.findByAnimalIdAnimalOrderByFechaEntradaDesc(id);
    }

    public AnimalForm prepararEdicion(Animal animal) {
        AnimalForm form = new AnimalForm();
        form.setCodigo(animal.getCodigo());
        form.setNombre(animal.getNombre());
        form.setIdEspecie(animal.getEspecie().getIdEspecie());
        form.setSexo(animal.getSexo());
        form.setFechaIngreso(animal.getFechaIngreso());
        form.setProcedencia(animal.getProcedencia());
        form.setMotivoIngreso(animal.getMotivoIngreso());
        form.setFotoUrl(animal.getFotoUrl());
        form.setObservaciones(animal.getObservaciones());
        return form;
    }

    // El bloqueo de la zona y la lectura actual del cupo evitan sobreocuparla.
    // Si falla el alojamiento, tambien se revierte el alta del animal.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Animal registrar(AnimalForm form, Integer idUsuario) {
        Usuario cuidador = comprobarCuidador(idUsuario);
        validar(form, 0);
        if (form.getFechaIngreso() == null || form.getFechaIngreso().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Indica una fecha de ingreso válida que no sea futura.");
        }
        if (form.getIdZona() == null) {
            throw new IllegalArgumentException("Selecciona la zona de cuarentena inicial.");
        }
        Zona zona = zonaRepository.bloquearPorId(form.getIdZona())
                .orElseThrow(() -> new IllegalArgumentException("La zona seleccionada no existe."));
        if (!Boolean.TRUE.equals(zona.getActivo()) || !"C".equals(zona.getTipo())) {
            throw new IllegalArgumentException("El ingreso necesita una zona de cuarentena activa.");
        }
        if (alojamientoRepository.countByZonaIdZonaAndFechaSalidaIsNull(zona.getIdZona()) >= zona.getCapacidad()) {
            throw new IllegalArgumentException("La zona de cuarentena ya no tiene cupos. Elige otra zona o coordina una derivación; no se registró el ingreso.");
        }
        Animal animal = new Animal();
        copiarDatos(form, animal);
        animal.setFechaIngreso(form.getFechaIngreso());
        animal.setEstado("A");
        animal.setEstadoSalud("E");
        animal.setFechaInicioCuarentena(form.getFechaIngreso());
        animal.setActivo(true);
        animalRepository.saveAndFlush(animal);

        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setAnimal(animal);
        alojamiento.setZona(zona);
        alojamiento.setResponsable(cuidador);
        alojamiento.setFechaEntrada(form.getFechaIngreso().atStartOfDay());
        alojamiento.setObservaciones("Alojamiento inicial por ingreso al santuario.");
        alojamientoRepository.saveAndFlush(alojamiento);
        return animal;
    }

    @Transactional
    public Animal actualizar(Integer id, AnimalForm form, Integer idUsuario) {
        comprobarCuidador(idUsuario);
        Animal animal = bloquearAnimal(id);
        if (!Boolean.TRUE.equals(animal.getActivo())) {
            throw new IllegalArgumentException("Reactiva la ficha antes de editar sus datos.");
        }
        validar(form, id);
        copiarDatos(form, animal);
        // Se conservan ingreso, salud, cuarentena y salida: pertenecen al seguimiento.
        return animalRepository.saveAndFlush(animal);
    }

    @Transactional
    public void cambiarActivo(Integer id, boolean activo, Integer idUsuario) {
        comprobarCuidador(idUsuario);
        Animal animal = bloquearAnimal(id);
        if (!activo && (alojamientoRepository.existsByAnimalIdAnimalAndFechaSalidaIsNull(id)
                || animalRepository.contarApadrinamientosAbiertos(id) > 0)) {
            throw new IllegalArgumentException("No se puede dar de baja una ficha con alojamiento o apadrinamientos abiertos. Primero completa el proceso de salida o cierre correspondiente.");
        }
        if (activo && !Boolean.TRUE.equals(animal.getEspecie().getActivo())) {
            throw new IllegalArgumentException("La especie debe estar activa para reactivar la ficha.");
        }
        animal.setActivo(activo);
        animalRepository.saveAndFlush(animal);
    }

    private Animal bloquearAnimal(Integer id) {
        return animalRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animal no encontrado."));
    }

    private Usuario comprobarCuidador(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() == null
                || !"Cuidador".equals(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return usuario;
    }

    private void validar(AnimalForm form, Integer id) {
        validarTexto(form.getCodigo(), "código", 20);
        validarTexto(form.getNombre(), "nombre", 80);
        validarTexto(form.getProcedencia(), "procedencia", 150);
        validarTexto(form.getMotivoIngreso(), "motivo de ingreso", 255);
        if (animalRepository.existsByCodigoIgnoreCaseAndIdAnimalNot(form.getCodigo().trim(), id)) {
            throw new IllegalArgumentException("Ya existe una ficha con ese código. Utiliza uno diferente.");
        }
        if (form.getSexo() == null || !Set.of("M", "H", "D").contains(form.getSexo())) {
            throw new IllegalArgumentException("Selecciona un sexo válido.");
        }
        if (form.getIdEspecie() == null || especieRepository.findById(form.getIdEspecie())
                .filter(e -> Boolean.TRUE.equals(e.getActivo())).isEmpty()) {
            throw new IllegalArgumentException("Selecciona una especie activa.");
        }
        if (form.getObservaciones() != null && form.getObservaciones().length() > 4000) {
            throw new IllegalArgumentException("Las observaciones admiten hasta 4000 caracteres.");
        }
        String foto = form.getFotoUrl();
        if (foto != null && !foto.isBlank()) {
            if (foto.length() > 255) {
                throw new IllegalArgumentException("La dirección de la foto admite hasta 255 caracteres.");
            }
            try {
                URI uri = URI.create(foto.trim());
                if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                        || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("La foto debe ser una dirección http o https válida.");
            }
        }
    }

    private void validarTexto(String texto, String campo, int maximo) {
        if (texto == null || texto.isBlank() || texto.trim().length() > maximo) {
            throw new IllegalArgumentException("Completa el campo " + campo + " con un máximo de " + maximo + " caracteres.");
        }
    }

    private void copiarDatos(AnimalForm form, Animal animal) {
        animal.setCodigo(form.getCodigo().trim());
        animal.setNombre(form.getNombre().trim());
        animal.setEspecie(especieRepository.findById(form.getIdEspecie()).orElseThrow());
        animal.setSexo(form.getSexo());
        animal.setProcedencia(form.getProcedencia().trim());
        animal.setMotivoIngreso(form.getMotivoIngreso().trim());
        animal.setFotoUrl(form.getFotoUrl() == null || form.getFotoUrl().isBlank() ? null : form.getFotoUrl().trim());
        animal.setObservaciones(form.getObservaciones() == null || form.getObservaciones().isBlank() ? null : form.getObservaciones().trim());
    }
}
