package com.cibertec.santuario.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.cibertec.santuario.dto.MedicamentoForm;
import com.cibertec.santuario.model.DetalleAtencion;
import com.cibertec.santuario.model.Medicamento;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.DetalleAtencionRepository;
import com.cibertec.santuario.repository.MedicamentoRepository;
import com.cibertec.santuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MedicamentoService {
    private static final BigDecimal MAX_STOCK = new BigDecimal("9999999999.99");
    private static final BigDecimal MAX_COSTO = new BigDecimal("99999999.99");

    private final MedicamentoRepository medicamentoRepository;
    private final DetalleAtencionRepository detalleAtencionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<Medicamento> listar(String texto, boolean soloStockBajo, Boolean activo) {
        if (texto.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros no válidos.");
        }
        String busqueda = texto.trim().toLowerCase(Locale.ROOT);
        List<Medicamento> resultado = new ArrayList<>();
        for (Medicamento m : medicamentoRepository.findAllByOrderByNombreAscPresentacionAsc()) {
            boolean coincide = busqueda.isEmpty()
                    || m.getNombre().toLowerCase(Locale.ROOT).contains(busqueda)
                    || m.getPresentacion().toLowerCase(Locale.ROOT).contains(busqueda);
            if (coincide
                    && (activo == null || activo.equals(m.getActivo()))
                    && (!soloStockBajo || tieneStockBajo(m))) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public long contarConStockBajo() {
        return medicamentoRepository.contarConStockBajo();
    }

    @Transactional(readOnly = true)
    public Medicamento obtener(Integer id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento no encontrado."));
    }

    @Transactional(readOnly = true)
    public List<DetalleAtencion> listarConsumosRecientes(Integer id) {
        return detalleAtencionRepository.findTop15ByMedicamentoIdMedicamentoOrderByAtencionFechaAtencionDesc(id);
    }

    public boolean tieneStockBajo(Medicamento m) {
        return m.getStockActual().compareTo(m.getStockMinimo()) <= 0;
    }

    public MedicamentoForm prepararEdicion(Medicamento m) {
        MedicamentoForm form = new MedicamentoForm();
        form.setNombre(m.getNombre());
        form.setPresentacion(m.getPresentacion());
        form.setUnidadMedida(m.getUnidadMedida());
        form.setStockActual(m.getStockActual());
        form.setStockMinimo(m.getStockMinimo());
        form.setCostoUnitario(m.getCostoUnitario());
        return form;
    }

    @Transactional
    public Medicamento registrar(MedicamentoForm form, Integer idUsuario) {
        comprobarVeterinario(idUsuario);
        validar(form, 0);
        validarCantidad(form.getStockActual(), "El stock inicial", MAX_STOCK, true);
        Medicamento m = new Medicamento();
        copiarDatos(form, m);
        m.setStockActual(form.getStockActual());
        m.setActivo(true);
        return medicamentoRepository.saveAndFlush(m);
    }

    @Transactional
    public Medicamento actualizar(Integer id, MedicamentoForm form, Integer idUsuario) {
        comprobarVeterinario(idUsuario);
        Medicamento m = bloquear(id);
        if (!Boolean.TRUE.equals(m.getActivo())) {
            throw new IllegalArgumentException("Reactiva el medicamento antes de editarlo.");
        }
        validar(form, id);
        copiarDatos(form, m);
        return medicamentoRepository.saveAndFlush(m);
    }

    @Transactional
    public Medicamento reponerStock(Integer id, BigDecimal cantidad, Integer idUsuario) {
        comprobarVeterinario(idUsuario);
        Medicamento m = bloquear(id);
        if (!Boolean.TRUE.equals(m.getActivo())) {
            throw new IllegalArgumentException("Reactiva el medicamento antes de reponer su stock.");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor que cero.");
        }
        validarCantidad(cantidad, "La cantidad a reponer", MAX_STOCK, false);
        BigDecimal nuevoStock = m.getStockActual().add(cantidad);
        if (nuevoStock.compareTo(MAX_STOCK) > 0) {
            throw new IllegalArgumentException("El stock resultante supera el máximo permitido.");
        }
        m.setStockActual(nuevoStock);
        return medicamentoRepository.saveAndFlush(m);
    }

    @Transactional
    public void cambiarActivo(Integer id, boolean activo, Integer idUsuario) {
        comprobarVeterinario(idUsuario);
        Medicamento m = bloquear(id);
        m.setActivo(activo);
        medicamentoRepository.saveAndFlush(m);
    }

    private Medicamento bloquear(Integer id) {
        return medicamentoRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicamento no encontrado."));
    }

    private void comprobarVeterinario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() == null
                || !"Veterinario".equals(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void validar(MedicamentoForm form, Integer id) {
        if (form.getNombre() == null || form.getNombre().isBlank() || form.getNombre().trim().length() > 100) {
            throw new IllegalArgumentException("Completa el nombre con un máximo de 100 caracteres.");
        }
        if (form.getPresentacion() == null || form.getPresentacion().isBlank()
                || form.getPresentacion().trim().length() > 100) {
            throw new IllegalArgumentException("Completa la presentación con un máximo de 100 caracteres.");
        }
        if (form.getUnidadMedida() == null || form.getUnidadMedida().isBlank()
                || form.getUnidadMedida().trim().length() > 20) {
            throw new IllegalArgumentException("Completa la unidad de medida con un máximo de 20 caracteres.");
        }
        if (medicamentoRepository.existsByNombreIgnoreCaseAndPresentacionIgnoreCaseAndIdMedicamentoNot(
                form.getNombre().trim(), form.getPresentacion().trim(), id)) {
            throw new IllegalArgumentException("Ya existe un medicamento con ese nombre y presentación.");
        }
        validarCantidad(form.getStockMinimo(), "El stock mínimo", MAX_STOCK, true);
        validarCantidad(form.getCostoUnitario(), "El costo unitario", MAX_COSTO, true);
    }

    private void validarCantidad(BigDecimal valor, String campo, BigDecimal maximo, boolean obligatorio) {
        if (valor == null) {
            if (obligatorio) {
                throw new IllegalArgumentException(campo + " es obligatorio.");
            }
            return;
        }
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(campo + " no puede ser negativo.");
        }
        if (valor.compareTo(maximo) > 0) {
            throw new IllegalArgumentException(campo + " supera el máximo permitido.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(campo + " admite como máximo 2 decimales.");
        }
    }

    private void copiarDatos(MedicamentoForm form, Medicamento m) {
        m.setNombre(form.getNombre().trim());
        m.setPresentacion(form.getPresentacion().trim());
        m.setUnidadMedida(form.getUnidadMedida().trim());
        m.setStockMinimo(form.getStockMinimo());
        m.setCostoUnitario(form.getCostoUnitario());
    }
}
