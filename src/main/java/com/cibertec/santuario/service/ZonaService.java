package com.cibertec.santuario.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.cibertec.santuario.dto.ZonaForm;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.model.Zona;
import com.cibertec.santuario.repository.AlojamientoRepository;
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
public class ZonaService {
    private final ZonaRepository zonaRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<Zona> listar(String texto, String tipo, Boolean activo, Integer cuposMinimos) {
        if (texto.length() > 80 || !Set.of("", "C", "R", "H").contains(tipo)
                || (cuposMinimos != null && cuposMinimos < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros no válidos.");
        }
        Map<Integer, Long> ocupacion = consultarOcupacion();
        List<Zona> resultado = new ArrayList<>();
        String busqueda = texto.trim().toLowerCase(Locale.ROOT);
        for (Zona zona : zonaRepository.findAllByOrderByNombreAsc()) {
            long ocupados = ocupacion.getOrDefault(zona.getIdZona(), 0L);
            long disponibles = Boolean.TRUE.equals(zona.getActivo())
                    ? Math.max(0L, zona.getCapacidad() - ocupados) : 0L;
            if (zona.getNombre().toLowerCase(Locale.ROOT).contains(busqueda)
                    && (tipo.isEmpty() || tipo.equals(zona.getTipo()))
                    && (activo == null || activo.equals(zona.getActivo()))
                    && (cuposMinimos == null || disponibles >= cuposMinimos)) {
                resultado.add(zona);
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> consultarOcupacion() {
        Map<Integer, Long> ocupacion = new LinkedHashMap<>();
        for (Object[] fila : alojamientoRepository.contarOcupacionPorZona()) {
            ocupacion.put((Integer) fila[0], (Long) fila[1]);
        }
        return ocupacion;
    }

    @Transactional(readOnly = true)
    public List<Zona> listarActivas() {
        return zonaRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Zona obtener(Integer id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona no encontrada."));
    }

    public ZonaForm prepararEdicion(Zona zona) {
        ZonaForm form = new ZonaForm();
        form.setNombre(zona.getNombre());
        form.setTipo(zona.getTipo());
        form.setCapacidad(zona.getCapacidad());
        form.setDescripcion(zona.getDescripcion());
        return form;
    }

    @Transactional
    public Zona registrar(ZonaForm form, Integer idUsuario) {
        comprobarCuidador(idUsuario);
        validar(form, 0);
        Zona zona = new Zona();
        copiarDatos(form, zona);
        zona.setActivo(true);
        return zonaRepository.saveAndFlush(zona);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Zona actualizar(Integer id, ZonaForm form, Integer idUsuario) {
        comprobarCuidador(idUsuario);
        Zona zona = bloquear(id);
        if (!Boolean.TRUE.equals(zona.getActivo())) {
            throw new IllegalArgumentException("Reactiva la zona antes de editarla.");
        }
        validar(form, id);
        long ocupados = alojamientoRepository.countByZonaIdZonaAndFechaSalidaIsNull(id);
        if (form.getCapacidad() < ocupados) {
            throw new IllegalArgumentException("La capacidad no puede ser menor que los " + ocupados + " alojamientos vigentes.");
        }
        if (ocupados > 0 && !zona.getTipo().equals(form.getTipo())) {
            throw new IllegalArgumentException("No puedes cambiar el tipo de una zona mientras tenga animales alojados.");
        }
        copiarDatos(form, zona);
        return zonaRepository.saveAndFlush(zona);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cambiarActivo(Integer id, boolean activo, Integer idUsuario) {
        comprobarCuidador(idUsuario);
        Zona zona = bloquear(id);
        if (!activo && alojamientoRepository.countByZonaIdZonaAndFechaSalidaIsNull(id) > 0) {
            throw new IllegalArgumentException("La zona tiene animales alojados. Trasládalos antes de darla de baja.");
        }
        zona.setActivo(activo);
        zonaRepository.saveAndFlush(zona);
    }

    private Zona bloquear(Integer id) {
        return zonaRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona no encontrada."));
    }

    private void comprobarCuidador(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() == null
                || !"Cuidador".equals(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void validar(ZonaForm form, Integer id) {
        if (form.getNombre() == null || form.getNombre().isBlank() || form.getNombre().trim().length() > 80) {
            throw new IllegalArgumentException("Completa el nombre de la zona con un máximo de 80 caracteres.");
        }
        if (zonaRepository.existsByNombreIgnoreCaseAndIdZonaNot(form.getNombre().trim(), id)) {
            throw new IllegalArgumentException("Ya existe una zona con ese nombre.");
        }
        if (form.getTipo() == null || !Set.of("C", "R", "H").contains(form.getTipo())) {
            throw new IllegalArgumentException("Selecciona un tipo de zona válido.");
        }
        if (form.getCapacidad() == null || form.getCapacidad() <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser un número entero mayor que cero.");
        }
        if (form.getDescripcion() != null && form.getDescripcion().length() > 255) {
            throw new IllegalArgumentException("La descripción admite hasta 255 caracteres.");
        }
    }

    private void copiarDatos(ZonaForm form, Zona zona) {
        zona.setNombre(form.getNombre().trim());
        zona.setTipo(form.getTipo());
        zona.setCapacidad(form.getCapacidad());
        zona.setDescripcion(form.getDescripcion() == null || form.getDescripcion().isBlank()
                ? null : form.getDescripcion().trim());
    }
}
