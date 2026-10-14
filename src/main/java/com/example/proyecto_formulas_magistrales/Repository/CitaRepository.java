package com.example.proyecto_formulas_magistrales.Repository;

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
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Buscar citas por estado
    List<Cita> findByEstado(Cita.Estado estado);

    // Buscar citas por fecha y hora exactas
    List<Cita> findByFechaHora(LocalDateTime fechaHora);

    // Buscar citas de un paciente
    List<Cita> findByPaciente(Paciente paciente);

    // Buscar citas de un médico
    List<Cita> findByMedico(Medico medico);

    // Buscar citas de un paciente ordenadas por fecha y hora
    List<Cita> findByPacienteOrderByFechaHoraAsc(Paciente paciente);

    // Buscar citas de un médico ordenadas por fecha y hora
    List<Cita> findByMedicoOrderByFechaHoraAsc(Medico medico);

    // JPQL custom query: citas de un médico en un periodo de tiempo
    @Query("SELECT c FROM Cita c WHERE c.medico.idMedico = :medicoId AND c.fechaHora BETWEEN :start AND :end")
    List<Cita> encontrarCitasPorMedicoEnPeriodo(@Param("medicoId") Long medicoId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    // Cita con detalles (paciente, médico y consulta)
    @Query("SELECT c FROM Cita c JOIN FETCH c.paciente JOIN FETCH c.medico LEFT JOIN FETCH c.consulta WHERE c.idCita = :idCita")
    Optional<Cita> findByIdWithDetails(@Param("idCita") Long idCita);

    // Detección de conflicto de horario (nueva cita)
    @Query("SELECT COUNT(c) FROM Cita c WHERE c.medico.idMedico = :idMedico " +
           "AND c.fechaHora BETWEEN :inicio AND :fin " +
           "AND c.estado != com.example.proyecto_formulas_magistrales.Model.Cita.Estado.CANCELADA")
    long countConflictosHorarioNuevo(@Param("idMedico") Long idMedico,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fin") LocalDateTime fin);

    // Detección de conflicto de horario excluyendo la cita actual (edición o reprogramación)
    @Query("SELECT COUNT(c) FROM Cita c WHERE c.medico.idMedico = :idMedico " +
           "AND c.fechaHora BETWEEN :inicio AND :fin " +
           "AND c.estado != com.example.proyecto_formulas_magistrales.Model.Cita.Estado.CANCELADA " +
           "AND c.idCita != :idCitaExcluida")
    long countConflictosHorarioExcluyendo(@Param("idMedico") Long idMedico,
                                          @Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin,
                                          @Param("idCitaExcluida") Long idCitaExcluida);

    // Citas disponibles para consulta (todas)
    @Query("SELECT DISTINCT c FROM Cita c JOIN FETCH c.paciente JOIN FETCH c.medico WHERE " +
           "c.estado IN (com.example.proyecto_formulas_magistrales.Model.Cita.Estado.PROGRAMADA, com.example.proyecto_formulas_magistrales.Model.Cita.Estado.CONFIRMADA) " +
           "AND c.consulta IS NULL " +
           "ORDER BY c.fechaHora ASC")
    List<Cita> findCitasDisponiblesParaConsultaTodas();

    // Citas disponibles para consulta (filtradas por médico)
    @Query("SELECT DISTINCT c FROM Cita c JOIN FETCH c.paciente JOIN FETCH c.medico WHERE " +
           "c.estado IN (com.example.proyecto_formulas_magistrales.Model.Cita.Estado.PROGRAMADA, com.example.proyecto_formulas_magistrales.Model.Cita.Estado.CONFIRMADA) " +
           "AND c.consulta IS NULL " +
           "AND c.medico.idMedico = :idMedico " +
           "ORDER BY c.fechaHora ASC")
    List<Cita> findCitasDisponiblesParaConsultaPorMedico(@Param("idMedico") Long idMedico);

    // Contadores estadísticos
    long countByEstado(Cita.Estado estado);

    long countByMedico_IdMedicoAndEstado(Long idMedico, Cita.Estado estado);

    long countByMedico_IdMedico(Long idMedico);

    long countByFechaHoraBetween(LocalDateTime start, LocalDateTime end);

    long countByMedico_IdMedicoAndFechaHoraBetween(Long idMedico, LocalDateTime start, LocalDateTime end);

    long countByEstadoAndFechaHoraBetween(Cita.Estado estado, LocalDateTime start, LocalDateTime end);

    long countByMedico_IdMedicoAndEstadoAndFechaHoraBetween(Long idMedico, Cita.Estado estado, LocalDateTime start, LocalDateTime end);

    List<Cita> findByFechaHoraBetweenOrderByFechaHoraAsc(LocalDateTime start, LocalDateTime end);

    List<Cita> findByMedico_IdMedicoAndFechaHoraBetweenOrderByFechaHoraAsc(Long idMedico, LocalDateTime start, LocalDateTime end);

    List<Cita> findTop10ByFechaHoraGreaterThanEqualAndEstadoInOrderByFechaHoraAsc(LocalDateTime desde, List<Cita.Estado> estados);

    List<Cita> findTop10ByMedico_IdMedicoAndFechaHoraGreaterThanEqualAndEstadoInOrderByFechaHoraAsc(Long idMedico, LocalDateTime desde, List<Cita.Estado> estados);

    long countByMedico_IdMedicoAndEstadoIn(Long idMedico, List<Cita.Estado> estados);
}


