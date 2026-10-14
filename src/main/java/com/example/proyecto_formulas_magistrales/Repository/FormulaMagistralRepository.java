package com.example.proyecto_formulas_magistrales.Repository;

import com.example.proyecto_formulas_magistrales.Model.FormulaMagistral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormulaMagistralRepository extends JpaRepository<FormulaMagistral, Long> {

    List<FormulaMagistral> findByNombreContainingIgnoreCase(String nombre);

    List<FormulaMagistral> findByComposicionContainingIgnoreCase(String composicion);

    List<FormulaMagistral> findByEstado(String estado);

    boolean existsByNombreIgnoreCase(String nombre);

    long countByEstadoIgnoreCase(String estado);

    long countByFechaPreparacionBetween(java.time.LocalDate start, java.time.LocalDate end);

    long countByConsulta_Medico_IdMedico(Long idMedico);

    long countByConsulta_Medico_IdMedicoAndFechaPreparacionBetween(Long idMedico, java.time.LocalDate start, java.time.LocalDate end);

    List<FormulaMagistral> findByFechaPreparacionBetween(java.time.LocalDate start, java.time.LocalDate end);

    List<FormulaMagistral> findByConsulta_Medico_IdMedicoAndFechaPreparacionBetween(Long idMedico, java.time.LocalDate start, java.time.LocalDate end);


    @Query("SELECT DISTINCT f FROM FormulaMagistral f " +
           "LEFT JOIN FETCH f.consulta c " +
           "LEFT JOIN FETCH c.paciente p " +
           "LEFT JOIN FETCH c.medico m " +
           "WHERE f.id = :id")
    Optional<FormulaMagistral> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT f FROM FormulaMagistral f " +
           "LEFT JOIN FETCH f.consulta c " +
           "LEFT JOIN FETCH c.paciente p " +
           "LEFT JOIN FETCH c.medico m " +
           "WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(f.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(f.composicion) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(f.descripcion) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (p IS NOT NULL AND (LOWER(p.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "                     LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "                     p.dni LIKE CONCAT('%', :search, '%'))) OR " +
           " (m IS NOT NULL AND (LOWER(m.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "                     LOWER(m.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "                     m.colegiatura LIKE CONCAT('%', :search, '%')))) " +
           "AND (:estado IS NULL OR :estado = '' OR :estado = 'TODOS' OR UPPER(f.estado) = UPPER(:estado)) " +
           "AND (:idMedico IS NULL OR (m IS NOT NULL AND m.idMedico = :idMedico)) " +
           "ORDER BY f.fechaPreparacion DESC, f.id DESC")
    List<FormulaMagistral> buscarConFiltros(@Param("search") String search,
                                            @Param("estado") String estado,
                                            @Param("idMedico") Long idMedico);
}
