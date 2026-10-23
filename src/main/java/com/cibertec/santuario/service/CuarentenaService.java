package com.cibertec.santuario.service;

import com.cibertec.santuario.dto.CuarentenaFilter;
import com.cibertec.santuario.model.Alojamiento;
import com.cibertec.santuario.model.Especie;
import com.cibertec.santuario.model.Zona;
import com.cibertec.santuario.repository.AlojamientoRepository;
import com.cibertec.santuario.repository.EspecieRepository;
import com.cibertec.santuario.repository.ZonaRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuarentenaService {
    private final AlojamientoRepository alojamientoRepository;
    private final EspecieRepository especieRepository;
    private final ZonaRepository zonaRepository;

    public List<Alojamiento> buscar(CuarentenaFilter filter) {
        if (filter.idEspecie() != null
                && (filter.idEspecie() <= 0 || !especieRepository.existsById(filter.idEspecie()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una especie válida.");
        }
        if (filter.idZona() != null
                && (filter.idZona() <= 0 || !zonaRepository.existsById(filter.idZona()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una zona válida.");
        }
        return alojamientoRepository.buscarCuarentenas(filter.idEspecie(), filter.idZona());
    }

    public List<Especie> listarEspecies() {
        return especieRepository.findAll(Sort.by("nombreComun"));
    }

    public List<Zona> listarZonas() {
        return zonaRepository.findAllByOrderByNombreAsc();
    }

    public Map<Integer, Long> calcularDias(List<Alojamiento> cuarentenas) {
        Map<Integer, Long> dias = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();
        for (Alojamiento alojamiento : cuarentenas) {
            long transcurridos = ChronoUnit.DAYS.between(alojamiento.getAnimal().getFechaInicioCuarentena(), hoy);
            dias.put(alojamiento.getIdAlojamiento(), Math.max(0L, transcurridos));
        }
        return dias;
    }
}
