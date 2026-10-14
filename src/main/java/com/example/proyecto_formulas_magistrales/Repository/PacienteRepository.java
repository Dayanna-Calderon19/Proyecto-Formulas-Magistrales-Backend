package com.example.proyecto_formulas_magistrales.Repository;

import com.example.proyecto_formulas_magistrales.Model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findByNombresContainingIgnoreCase(String nombres);

    List<Paciente> findByApellidosContainingIgnoreCase(String apellidos);

    Optional<Paciente> findByDni(String dni);

    boolean existsByDni(String dni);

    List<Paciente> findByEstado(Boolean estado);

    long countByEstado(Boolean estado);


    @Query("SELECT p FROM Paciente p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "p.dni LIKE CONCAT('%', :search, '%')) AND " +
           "(:estado IS NULL OR p.estado = :estado) " +
           "ORDER BY p.apellidos ASC, p.nombres ASC")
    List<Paciente> buscarConFiltros(@Param("search") String search, @Param("estado") Boolean estado);
}
