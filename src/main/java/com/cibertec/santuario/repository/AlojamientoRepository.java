package com.cibertec.santuario.repository;

import java.util.List;
import java.util.Optional;
import com.cibertec.santuario.model.Alojamiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlojamientoRepository extends JpaRepository<Alojamiento, Integer> {
    long countByZonaIdZonaAndFechaSalidaIsNull(Integer idZona);
    boolean existsByAnimalIdAnimalAndFechaSalidaIsNull(Integer idAnimal);

    @EntityGraph(attributePaths = "zona")
    List<Alojamiento> findByAnimalIdAnimalOrderByFechaEntradaDesc(Integer idAnimal);

    @EntityGraph(attributePaths = {"animal", "animal.especie", "zona", "responsable"})
    Optional<Alojamiento> findByAnimalIdAnimalAndFechaSalidaIsNull(Integer idAnimal);

    @EntityGraph(attributePaths = {"animal", "animal.especie", "zona", "responsable"})
    @Query("select a from Alojamiento a where a.fechaSalida is null "
            + "and (:idZona is null or a.zona.idZona = :idZona) "
            + "and (:texto = '' or lower(a.animal.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(a.animal.codigo) like lower(concat('%', :texto, '%'))) "
            + "order by a.zona.nombre, a.animal.nombre")
    List<Alojamiento> buscarVigentes(@Param("texto") String texto, @Param("idZona") Integer idZona);

    @EntityGraph(attributePaths = {"animal", "animal.especie", "zona"})
    @Query("""
            select a from Alojamiento a
            where a.fechaSalida is null
                and a.animal.activo = true and a.animal.estado = 'A'
                and a.animal.fechaInicioCuarentena is not null
                and a.animal.fechaFinCuarentena is null
                and (a.animal.especie.idEspecie = :idEspecie or :idEspecie is null)
                and (a.zona.idZona = :idZona or :idZona is null)
            order by a.animal.fechaInicioCuarentena, a.animal.codigo
            """)
    List<Alojamiento> buscarCuarentenas(@Param("idEspecie") Integer idEspecie,
            @Param("idZona") Integer idZona);

    @Query("select a.zona.idZona, count(a) from Alojamiento a where a.fechaSalida is null group by a.zona.idZona")
    List<Object[]> contarOcupacionPorZona();
}
