package com.example.proyecto_formulas_magistrales.Repository;

import com.example.proyecto_formulas_magistrales.Model.Consulta;
import com.example.proyecto_formulas_magistrales.Model.Cita;
import com.example.proyecto_formulas_magistrales.Model.Medico;
import com.example.proyecto_formulas_magistrales.Model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    // Buscar consultas por paciente
    List<Consulta> findByPaciente(Paciente paciente);

    // Buscar consultas por médico
    List<Consulta> findByMedico(Medico medico);

    // Buscar todas las consultas ordenadas por fecha de consulta
    List<Consulta> findAllByOrderByFechaConsultaAsc();

    // Buscar consultas de un paciente ordenadas por fecha de consulta
    List<Consulta> findByPacienteOrderByFechaConsultaAsc(Paciente paciente);

    // Buscar consultas de un médico ordenadas por fecha de consulta
    List<Consulta> findByMedicoOrderByFechaConsultaAsc(Medico medico);

    // Buscar una consulta asociada a una cita
    Consulta findByCita(Cita cita);

    // Verificar si una cita ya tiene consulta asignada
    boolean existsByCita_IdCita(Long idCita);

    // Buscar consultas por diagnóstico (ignora mayúsculas y minúsculas)
    List<Consulta> findByDiagnosticoContainingIgnoreCase(String diagnostico);

    // Historial de consultas de un paciente ordenado por fecha descendente
    @Query("SELECT c FROM Consulta c WHERE c.paciente.idPaciente = :pacienteId ORDER BY c.fechaConsulta DESC")
    List<Consulta> historialPaciente(@Param("pacienteId") Long pacienteId);

    // Historial de consultas de un médico ordenado por fecha descendente
    @Query("SELECT c FROM Consulta c WHERE c.medico.idMedico = :medicoId ORDER BY c.fechaConsulta DESC")
    List<Consulta> historialMedico(@Param("medicoId") Long medicoId);

    // JPQL custom query: consultas de un médico en un rango de fechas
    @Query("SELECT c FROM Consulta c WHERE c.medico.idMedico = :medicoId AND c.fechaConsulta BETWEEN :start AND :end")
    List<Consulta> consultasPorMedicoEnPeriodo(@Param("medicoId") Long medicoId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Listar consultas con paciente y médico cargados ordenadas cronológicamente
    @Query("SELECT DISTINCT c FROM Consulta c JOIN FETCH c.paciente JOIN FETCH c.medico LEFT JOIN FETCH c.cita ORDER BY c.fechaConsulta DESC")
    List<Consulta> findAllWithPacienteAndMedico();

    // Listar consultas de un médico específico con paciente cargado
    @Query("SELECT DISTINCT c FROM Consulta c JOIN FETCH c.paciente JOIN FETCH c.medico LEFT JOIN FETCH c.cita WHERE c.medico.idMedico = :idMedico ORDER BY c.fechaConsulta DESC")
    List<Consulta> findByMedicoWithPaciente(@Param("idMedico") Long idMedico);

    // Consulta con detalles completos (paciente, médico, cita y fórmulas magistrales)
    @Query("SELECT DISTINCT c FROM Consulta c " +
           "JOIN FETCH c.paciente " +
           "JOIN FETCH c.medico " +
           "LEFT JOIN FETCH c.cita " +
           "LEFT JOIN FETCH c.formulas " +
           "WHERE c.idConsulta = :idConsulta")
    Optional<Consulta> findByIdWithDetails(@Param("idConsulta") Long idConsulta);

    long countByMedico_IdMedico(Long idMedico);

    long countByFechaConsultaBetween(LocalDateTime start, LocalDateTime end);

    long countByMedico_IdMedicoAndFechaConsultaBetween(Long idMedico, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT c.paciente.idPaciente) FROM Consulta c")
    long countDistinctPacientesAtendidosTotal();

    @Query("SELECT COUNT(DISTINCT c.paciente.idPaciente) FROM Consulta c WHERE c.fechaConsulta BETWEEN :start AND :end")
    long countDistinctPacientesAtendidosEnPeriodo(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT c.paciente.idPaciente) FROM Consulta c WHERE c.medico.idMedico = :idMedico")
    long countDistinctPacientesAtendidosPorMedico(@Param("idMedico") Long idMedico);

    @Query("SELECT COUNT(DISTINCT c.paciente.idPaciente) FROM Consulta c WHERE c.medico.idMedico = :idMedico AND c.fechaConsulta BETWEEN :start AND :end")
    long countDistinctPacientesAtendidosPorMedicoEnPeriodo(@Param("idMedico") Long idMedico, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Consulta> findByFechaConsultaBetweenOrderByFechaConsultaAsc(LocalDateTime start, LocalDateTime end);

    List<Consulta> findByMedico_IdMedicoAndFechaConsultaBetweenOrderByFechaConsultaAsc(Long idMedico, LocalDateTime start, LocalDateTime end);
}

