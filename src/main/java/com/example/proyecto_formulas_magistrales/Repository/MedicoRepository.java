package com.example.proyecto_formulas_magistrales.Repository;

import com.example.proyecto_formulas_magistrales.Model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    List<Medico> findByNombresContainingIgnoreCase(String nombres);

    List<Medico> findByApellidosContainingIgnoreCase(String apellidos);

    List<Medico> findByEspecialidadContainingIgnoreCase(String especialidad);

    boolean existsByDni(String dni);

    Optional<Medico> findByDni(String dni);

    List<Medico> findByEstado(Boolean estado);

    long countByEstado(Boolean estado);


    boolean existsByColegiatura(String colegiatura);

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    Optional<Medico> findByUsuario_IdUsuario(Long idUsuario);

    @Query("SELECT DISTINCT m FROM Medico m LEFT JOIN FETCH m.usuario WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(m.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "m.dni LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(m.colegiatura) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:especialidad IS NULL OR :especialidad = '' OR :especialidad = 'TODAS' OR LOWER(m.especialidad) = LOWER(:especialidad)) AND " +
           "(:estado IS NULL OR m.estado = :estado) " +
           "ORDER BY m.apellidos ASC, m.nombres ASC")
    List<Medico> buscarConFiltros(@Param("search") String search,
                                  @Param("especialidad") String especialidad,
                                  @Param("estado") Boolean estado);

    @Query("SELECT DISTINCT m.especialidad FROM Medico m WHERE m.especialidad IS NOT NULL AND TRIM(m.especialidad) != '' ORDER BY m.especialidad ASC")
    List<String> findAllEspecialidades();
}
