package com.cibertec.santuario.repository;

import java.util.List;
import java.util.Optional;
import com.cibertec.santuario.model.Animal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AnimalRepository extends JpaRepository<Animal, Integer> {
    @EntityGraph(attributePaths = "especie")
    List<Animal> findByActivoTrueAndEstadoOrderByNombreAsc(String estado);

    @EntityGraph(attributePaths = "especie")
    Optional<Animal> findByIdAnimalAndActivoTrueAndEstado(Integer idAnimal, String estado);

    @EntityGraph(attributePaths = "especie")
    @Query("select a from Animal a where (:activo is null or a.activo = :activo) "
            + "and (:estado = '' or a.estado = :estado) "
            + "and (:texto = '' or lower(a.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(a.codigo) like lower(concat('%', :texto, '%')) "
            + "or lower(a.especie.nombreComun) like lower(concat('%', :texto, '%'))) "
            + "order by a.codigo")
    List<Animal> buscar(@Param("texto") String texto, @Param("estado") String estado,
            @Param("activo") Boolean activo);

    @EntityGraph(attributePaths = "especie")
    Optional<Animal> findByIdAnimal(Integer idAnimal);

    boolean existsByCodigoIgnoreCaseAndIdAnimalNot(String codigo, Integer idAnimal);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Animal a where a.idAnimal = :id")
    Optional<Animal> bloquearPorId(@Param("id") Integer id);

    @Query(value = "select count(*) from tbl_apadrinamiento where id_animal = :id and fecha_fin is null and estado_solicitud in ('PENDIENTE', 'APROBADA')", nativeQuery = true)
    long contarApadrinamientosAbiertos(@Param("id") Integer id);
}
