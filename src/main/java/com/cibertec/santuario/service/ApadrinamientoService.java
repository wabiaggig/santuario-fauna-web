package com.cibertec.santuario.service;

import java.math.BigDecimal;
import java.util.List;
import com.cibertec.santuario.dto.SolicitudApadrinamientoForm;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ApadrinamientoService {
    private final AnimalRepository animalRepository;

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

    /** Valida solo la vista previa: NO crea registros en tbl_apadrinamiento. */
    public String validarVistaPrevia(SolicitudApadrinamientoForm solicitud) {
        BigDecimal aporte = solicitud.getAporteMensual();
        if (aporte == null || aporte.compareTo(BigDecimal.ZERO) <= 0) {
            return "El aporte mensual debe ser mayor a S/ 0.00.";
        }
        if (aporte.scale() > 2) {
            return "El aporte mensual debe tener como máximo dos decimales.";
        }
        if (aporte.compareTo(new BigDecimal("99999999.99")) > 0) {
            return "El aporte mensual supera el máximo permitido por la base de datos.";
        }
        String observaciones = solicitud.getObservaciones();
        if (observaciones != null && observaciones.length() > 255) {
            return "Las observaciones no pueden superar los 255 caracteres.";
        }
        return null;
    }
}
