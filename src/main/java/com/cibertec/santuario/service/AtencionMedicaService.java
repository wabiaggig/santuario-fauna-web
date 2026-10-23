package com.cibertec.santuario.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cibertec.santuario.dto.AtencionMedicaRequestDTO;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.model.AtencionMedica;
import com.cibertec.santuario.model.DetalleAtencion;
import com.cibertec.santuario.model.Medicamento;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.AnimalRepository;
import com.cibertec.santuario.repository.AtencionMedicaRepository;
import com.cibertec.santuario.repository.MedicamentoRepository;
import com.cibertec.santuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AtencionMedicaService {
    private static final Set<String> ESTADOS_SALUD = Set.of("E", "T", "S");

    private final AtencionMedicaRepository atencionMedicaRepository;
    private final AnimalRepository animalRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicamentoRepository medicamentoRepository;

    @Transactional(readOnly = true)
    public List<Animal> listarAnimalesAtendibles() {
        return animalRepository.findByActivoTrueAndEstadoOrderByNombreAsc("A");
    }

    @Transactional(readOnly = true)
    public List<AtencionMedica> listarPorAnimal(Integer idAnimal) {
        return atencionMedicaRepository.findByAnimalIdAnimalOrderByFechaAtencionDesc(idAnimal);
    }

    @Transactional(readOnly = true)
    public List<AtencionMedica> listarRecientes(Integer idAnimal) {
        return idAnimal == null
                ? atencionMedicaRepository.findTop100ByOrderByFechaAtencionDescIdAtencionDesc()
                : atencionMedicaRepository.findTop100ByAnimalIdAnimalOrderByFechaAtencionDescIdAtencionDesc(idAnimal);
    }

    @Transactional(readOnly = true)
    public List<Animal> listarTodosLosAnimales() {
        return animalRepository.buscar("", "", null);
    }

    @Transactional(readOnly = true)
    public List<Medicamento> listarMedicamentosActivos() {
        return medicamentoRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional
    public AtencionMedica registrarAtencion(AtencionMedicaRequestDTO dto, Integer idVeterinario) {
        validarCabecera(dto);

        Usuario veterinario = usuarioRepository.findById(idVeterinario)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró al veterinario de la sesión."));
        if (veterinario.getRol() == null || !"Veterinario".equals(veterinario.getRol().getNombre())) {
            throw new IllegalArgumentException("Solo un veterinario puede registrar atenciones médicas.");
        }

        Animal animal = animalRepository.bloquearPorId(dto.getIdAnimal())
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un animal válido."));
        if (!Boolean.TRUE.equals(animal.getActivo()) || !"A".equals(animal.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden registrar atenciones de animales albergados.");
        }

        LocalDate fecha = dto.getFechaAtencion();
        if (fecha.isBefore(animal.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha de atención no puede ser anterior al ingreso del animal.");
        }

        AtencionMedica atencion = new AtencionMedica();
        atencion.setAnimal(animal);
        atencion.setVeterinario(veterinario);
        atencion.setFechaAtencion(fecha.atTime(LocalTime.now().withNano(0)));
        atencion.setDiagnostico(dto.getDiagnostico().trim());
        atencion.setTratamiento(dto.getTratamiento().trim());
        atencion.setEstadoSaludAnterior(animal.getEstadoSalud());
        atencion.setEstadoSaludResultante(dto.getEstadoSaludResultante());
        atencion.setObservaciones(textoOpcional(dto.getObservaciones()));

        Set<Integer> usados = new HashSet<>();
        for (AtencionMedicaRequestDTO.DetalleItemDTO item : dto.getDetalles()) {
            if (item == null || (item.getIdMedicamento() == null && item.getCantidad() == null)) {
                continue;
            }
            if (item.getIdMedicamento() == null) {
                throw new IllegalArgumentException("Selecciona el medicamento en cada fila con cantidad.");
            }
            if (item.getCantidad() == null || item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad de cada medicamento debe ser mayor que cero.");
            }
            if (!usados.add(item.getIdMedicamento())) {
                throw new IllegalArgumentException("Un medicamento aparece más de una vez. Suma las cantidades en una sola fila.");
            }

            Medicamento medicamento = medicamentoRepository.bloquearPorId(item.getIdMedicamento())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los medicamentos seleccionados no existe."));
            if (!Boolean.TRUE.equals(medicamento.getActivo())) {
                throw new IllegalArgumentException("El medicamento " + medicamento.getNombre() + " está inactivo.");
            }
            // Si no alcanza el stock se lanza la excepcion y se revierte toda la atencion.
            if (medicamento.getStockActual().compareTo(item.getCantidad()) < 0) {
                throw new IllegalArgumentException("Stock insuficiente de " + medicamento.getNombre()
                        + ": disponible " + medicamento.getStockActual().stripTrailingZeros().toPlainString()
                        + " " + medicamento.getUnidadMedida() + ".");
            }

            medicamento.setStockActual(medicamento.getStockActual().subtract(item.getCantidad()));

            DetalleAtencion detalle = new DetalleAtencion();
            detalle.setAtencion(atencion);
            detalle.setMedicamento(medicamento);
            detalle.setCantidad(item.getCantidad());
            detalle.setCostoUnitario(medicamento.getCostoUnitario());
            atencion.getDetalles().add(detalle);
        }

        animal.setEstadoSalud(dto.getEstadoSaludResultante());

        return atencionMedicaRepository.save(atencion);
    }

    private void validarCabecera(AtencionMedicaRequestDTO dto) {
        if (dto.getIdAnimal() == null) {
            throw new IllegalArgumentException("Selecciona el animal atendido.");
        }
        if (dto.getFechaAtencion() == null) {
            throw new IllegalArgumentException("Indica la fecha de atención.");
        }
        if (dto.getFechaAtencion().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de atención no puede ser futura.");
        }
        if (dto.getDiagnostico() == null || dto.getDiagnostico().isBlank()) {
            throw new IllegalArgumentException("El diagnóstico es obligatorio.");
        }
        if (dto.getTratamiento() == null || dto.getTratamiento().isBlank()) {
            throw new IllegalArgumentException("El tratamiento es obligatorio.");
        }
        if (dto.getEstadoSaludResultante() == null || !ESTADOS_SALUD.contains(dto.getEstadoSaludResultante())) {
            throw new IllegalArgumentException("Selecciona el estado de salud resultante.");
        }
        if (dto.getDetalles() == null) {
            dto.setDetalles(new java.util.ArrayList<>());
        }
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
