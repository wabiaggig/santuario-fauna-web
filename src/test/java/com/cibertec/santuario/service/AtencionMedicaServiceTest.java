package com.cibertec.santuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.cibertec.santuario.dto.AtencionMedicaRequestDTO;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.model.AtencionMedica;
import com.cibertec.santuario.model.Medicamento;
import com.cibertec.santuario.model.Rol;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.AnimalRepository;
import com.cibertec.santuario.repository.AtencionMedicaRepository;
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
class AtencionMedicaServiceTest {
    @Mock AtencionMedicaRepository atencionMedicaRepository;
    @Mock AnimalRepository animalRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock MedicamentoRepository medicamentoRepository;
    @InjectMocks AtencionMedicaService service;

    private Usuario veterinario;
    private Animal animal;
    private Medicamento antibiotico;
    private Medicamento vitaminas;

    @BeforeEach
    void preparar() {
        Rol rol = new Rol();
        rol.setNombre("Veterinario");
        veterinario = new Usuario();
        veterinario.setIdUsuario(1);
        veterinario.setRol(rol);
        veterinario.setActivo(true);

        animal = new Animal();
        animal.setIdAnimal(10);
        animal.setActivo(true);
        animal.setEstado("A");
        animal.setEstadoSalud("E");
        animal.setFechaIngreso(LocalDate.now().minusDays(30));

        antibiotico = medicamento(5, "Enrofloxacina 5 %", "20.00");
        vitaminas = medicamento(1, "Multivitamínico", "50.00");
    }

    private static Medicamento medicamento(int id, String nombre, String stock) {
        Medicamento m = new Medicamento();
        m.setIdMedicamento(id);
        m.setNombre(nombre);
        m.setUnidadMedida("mL");
        m.setStockActual(new BigDecimal(stock));
        m.setCostoUnitario(new BigDecimal("0.90"));
        m.setActivo(true);
        return m;
    }

    private static AtencionMedicaRequestDTO.DetalleItemDTO item(int idMedicamento, String cantidad) {
        AtencionMedicaRequestDTO.DetalleItemDTO item = new AtencionMedicaRequestDTO.DetalleItemDTO();
        item.setIdMedicamento(idMedicamento);
        item.setCantidad(new BigDecimal(cantidad));
        return item;
    }

    private AtencionMedicaRequestDTO formulario(AtencionMedicaRequestDTO.DetalleItemDTO... items) {
        AtencionMedicaRequestDTO dto = new AtencionMedicaRequestDTO();
        dto.setIdAnimal(10);
        dto.setFechaAtencion(LocalDate.now());
        dto.setDiagnostico("Herida en el ala");
        dto.setTratamiento("Curación y antibiótico");
        dto.setEstadoSaludResultante("T");
        dto.setDetalles(new java.util.ArrayList<>(List.of(items)));
        return dto;
    }

    @Test
    @DisplayName("Registra la atención, descuenta el stock y actualiza la salud del animal")
    void registraYDescuentaStock() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(veterinario));
        when(animalRepository.bloquearPorId(10)).thenReturn(Optional.of(animal));
        when(medicamentoRepository.bloquearPorId(5)).thenReturn(Optional.of(antibiotico));
        when(medicamentoRepository.bloquearPorId(1)).thenReturn(Optional.of(vitaminas));
        when(atencionMedicaRepository.save(any(AtencionMedica.class))).thenAnswer(inv -> inv.getArgument(0));

        AtencionMedica atencion = service.registrarAtencion(formulario(item(5, "4.50"), item(1, "2")), 1);

        assertThat(antibiotico.getStockActual()).isEqualByComparingTo("15.50");
        assertThat(vitaminas.getStockActual()).isEqualByComparingTo("48");
        assertThat(atencion.getDetalles()).hasSize(2);
        assertThat(atencion.getDetalles().get(0).getCostoUnitario()).isEqualByComparingTo("0.90");
        assertThat(atencion.getEstadoSaludAnterior()).isEqualTo("E");
        assertThat(atencion.getEstadoSaludResultante()).isEqualTo("T");
        assertThat(animal.getEstadoSalud()).isEqualTo("T");
    }

    @Test
    @DisplayName("Con stock insuficiente lanza error y no guarda la atención")
    void stockInsuficiente() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(veterinario));
        when(animalRepository.bloquearPorId(10)).thenReturn(Optional.of(animal));
        when(medicamentoRepository.bloquearPorId(5)).thenReturn(Optional.of(antibiotico));

        assertThatThrownBy(() -> service.registrarAtencion(formulario(item(5, "25")), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(atencionMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No permite repetir el mismo medicamento en dos filas")
    void medicamentoRepetido() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(veterinario));
        when(animalRepository.bloquearPorId(10)).thenReturn(Optional.of(animal));
        when(medicamentoRepository.bloquearPorId(5)).thenReturn(Optional.of(antibiotico));

        assertThatThrownBy(() -> service.registrarAtencion(formulario(item(5, "1"), item(5, "2")), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("más de una vez");
        verify(atencionMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No registra atenciones de animales liberados")
    void animalNoAlbergado() {
        animal.setEstado("L");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(veterinario));
        when(animalRepository.bloquearPorId(10)).thenReturn(Optional.of(animal));

        assertThatThrownBy(() -> service.registrarAtencion(formulario(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("albergados");
    }

    @Test
    @DisplayName("Rechaza una fecha de atención futura")
    void fechaFutura() {
        AtencionMedicaRequestDTO dto = formulario();
        dto.setFechaAtencion(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.registrarAtencion(dto, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futura");
    }
}
