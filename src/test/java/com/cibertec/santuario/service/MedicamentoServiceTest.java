package com.cibertec.santuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.cibertec.santuario.dto.MedicamentoForm;
import com.cibertec.santuario.model.Medicamento;
import com.cibertec.santuario.model.Rol;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.DetalleAtencionRepository;
import com.cibertec.santuario.repository.MedicamentoRepository;
import com.cibertec.santuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {
    @Mock MedicamentoRepository medicamentoRepository;
    @Mock DetalleAtencionRepository detalleAtencionRepository;
    @Mock UsuarioRepository usuarioRepository;
    @InjectMocks MedicamentoService service;

    private Medicamento medicamento;

    @BeforeEach
    void preparar() {
        Rol rol = new Rol();
        rol.setNombre("Veterinario");
        Usuario veterinario = new Usuario();
        veterinario.setRol(rol);
        veterinario.setActivo(true);
        lenient().when(usuarioRepository.findById(1)).thenReturn(Optional.of(veterinario));

        medicamento = new Medicamento();
        medicamento.setIdMedicamento(3);
        medicamento.setNombre("Ivermectina 1 %");
        medicamento.setStockActual(new BigDecimal("5.00"));
        medicamento.setStockMinimo(new BigDecimal("6.00"));
        medicamento.setActivo(true);
    }

    private static MedicamentoForm formulario() {
        MedicamentoForm form = new MedicamentoForm();
        form.setNombre("Ivermectina 1 %");
        form.setPresentacion("Frasco de 50 mL");
        form.setUnidadMedida("mL");
        form.setStockActual(new BigDecimal("50"));
        form.setStockMinimo(new BigDecimal("10"));
        form.setCostoUnitario(new BigDecimal("1.20"));
        return form;
    }

    @Test
    @DisplayName("Reponer stock suma la cantidad al stock actual")
    void reponerStock() {
        when(medicamentoRepository.bloquearPorId(3)).thenReturn(Optional.of(medicamento));
        when(medicamentoRepository.saveAndFlush(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Medicamento resultado = service.reponerStock(3, new BigDecimal("12.50"), 1);

        assertThat(resultado.getStockActual()).isEqualByComparingTo("17.50");
    }

    @Test
    @DisplayName("Detecta stock bajo cuando el stock actual no supera el mínimo")
    void stockBajo() {
        assertThat(service.tieneStockBajo(medicamento)).isTrue();
        medicamento.setStockActual(new BigDecimal("6.01"));
        assertThat(service.tieneStockBajo(medicamento)).isFalse();
    }

    @Test
    @DisplayName("No permite registrar dos veces el mismo nombre y presentación")
    void duplicado() {
        when(medicamentoRepository.existsByNombreIgnoreCaseAndPresentacionIgnoreCaseAndIdMedicamentoNot(
                anyString(), anyString(), anyInt())).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(formulario(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    @DisplayName("Rechaza cantidades con más de 2 decimales")
    void decimales() {
        MedicamentoForm form = formulario();
        form.setCostoUnitario(new BigDecimal("1.255"));

        assertThatThrownBy(() -> service.registrar(form, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 decimales");
    }

    @Test
    @DisplayName("Reponer una cantidad cero o negativa no se permite")
    void reponerCantidadInvalida() {
        when(medicamentoRepository.bloquearPorId(3)).thenReturn(Optional.of(medicamento));

        assertThatThrownBy(() -> service.reponerStock(3, BigDecimal.ZERO, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor que cero");
    }
}
