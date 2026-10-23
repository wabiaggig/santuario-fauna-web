package com.cibertec.santuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.cibertec.santuario.dto.LiberacionForm;
import com.cibertec.santuario.model.Alojamiento;
import com.cibertec.santuario.model.Animal;
import com.cibertec.santuario.model.Apadrinamiento;
import com.cibertec.santuario.model.Liberacion;
import com.cibertec.santuario.model.Rol;
import com.cibertec.santuario.model.Usuario;
import com.cibertec.santuario.repository.AlojamientoRepository;
import com.cibertec.santuario.repository.AnimalRepository;
import com.cibertec.santuario.repository.ApadrinamientoRepository;
import com.cibertec.santuario.repository.LiberacionRepository;
import com.cibertec.santuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiberacionServiceTest {
    @Mock LiberacionRepository liberacionRepository;
    @Mock AnimalRepository animalRepository;
    @Mock AlojamientoRepository alojamientoRepository;
    @Mock ApadrinamientoRepository apadrinamientoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @InjectMocks LiberacionService service;

    private final LocalDate hoy = LocalDate.now();
    private Usuario veterinario;
    private Animal animal;

    @BeforeEach
    void preparar() {
        Rol rol = new Rol();
        rol.setNombre("Veterinario");
        veterinario = new Usuario();
        veterinario.setIdUsuario(1);
        veterinario.setRol(rol);
        veterinario.setActivo(true);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(veterinario));

        animal = new Animal();
        animal.setIdAnimal(3);
        animal.setNombre("Luna");
        animal.setActivo(true);
        animal.setEstado("A");
        animal.setEstadoSalud("S");
        animal.setFechaIngreso(hoy.minusDays(100));
        animal.setFechaInicioCuarentena(hoy.minusDays(100));
        animal.setFechaFinCuarentena(null);
    }

    private LiberacionForm formulario() {
        LiberacionForm form = new LiberacionForm();
        form.setIdAnimal(3);
        form.setFechaLiberacion(hoy);
        form.setLugar("Reserva Nacional Tambopata");
        return form;
    }

    @Test
    @DisplayName("Libera al animal: cierra cuarentena, alojamiento y apadrinamientos")
    void liberaConEfectosEnCadena() {
        Alojamiento alojamiento = new Alojamiento();
        alojamiento.setFechaEntrada(hoy.minusDays(50).atStartOfDay());

        Apadrinamiento aprobado = new Apadrinamiento();
        aprobado.setEstadoSolicitud(Apadrinamiento.APROBADA);
        aprobado.setFechaInicio(hoy.minusDays(30));
        Apadrinamiento pendiente = new Apadrinamiento();
        pendiente.setEstadoSolicitud(Apadrinamiento.PENDIENTE);

        when(animalRepository.bloquearPorId(3)).thenReturn(Optional.of(animal));
        when(alojamientoRepository.findByAnimalIdAnimalAndFechaSalidaIsNull(3)).thenReturn(Optional.of(alojamiento));
        when(apadrinamientoRepository.findByAnimal_IdAnimalAndFechaFinIsNullAndEstadoSolicitudIn(eq(3), anyCollection()))
                .thenReturn(List.of(aprobado, pendiente));
        when(liberacionRepository.save(any(Liberacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Liberacion liberacion = service.liberar(formulario(), 1);

        assertThat(animal.getEstado()).isEqualTo("L");
        assertThat(animal.getFechaSalida()).isEqualTo(hoy);
        assertThat(animal.getFechaFinCuarentena()).isEqualTo(hoy);
        assertThat(alojamiento.getFechaSalida()).isNotNull();
        assertThat(aprobado.getFechaFin()).isEqualTo(hoy);
        assertThat(pendiente.getEstadoSolicitud()).isEqualTo(Apadrinamiento.RECHAZADA);
        assertThat(pendiente.getMotivoRechazo()).contains("liberado");
        assertThat(pendiente.getUsuarioRevisor()).isSameAs(veterinario);
        assertThat(liberacion.getResponsable()).isSameAs(veterinario);
        assertThat(liberacion.getLugar()).isEqualTo("Reserva Nacional Tambopata");
    }

    @Test
    @DisplayName("Solo se libera un animal en estado Saludable")
    void requiereSaludable() {
        animal.setEstadoSalud("T");
        when(animalRepository.bloquearPorId(3)).thenReturn(Optional.of(animal));

        assertThatThrownBy(() -> service.liberar(formulario(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saludable");
        verify(liberacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("La fecha de liberación no puede ser anterior al ingreso")
    void fechaAnteriorAlIngreso() {
        when(animalRepository.bloquearPorId(3)).thenReturn(Optional.of(animal));
        LiberacionForm form = formulario();
        form.setFechaLiberacion(hoy.minusDays(200));

        assertThatThrownBy(() -> service.liberar(form, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ingreso");
    }
}
