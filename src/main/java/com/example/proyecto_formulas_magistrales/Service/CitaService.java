package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.CitaReprogramarDTO;
import com.example.proyecto_formulas_magistrales.DTO.CitaRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.CitaResponseDTO;
import com.example.proyecto_formulas_magistrales.Model.Cita;
import com.example.proyecto_formulas_magistrales.Model.Medico;
import com.example.proyecto_formulas_magistrales.Model.Paciente;
import com.example.proyecto_formulas_magistrales.Model.Usuario;
import com.example.proyecto_formulas_magistrales.Repository.CitaRepository;
import com.example.proyecto_formulas_magistrales.Repository.MedicoRepository;
import com.example.proyecto_formulas_magistrales.Repository.PacienteRepository;
import com.example.proyecto_formulas_magistrales.Repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    // Intervalo de cita en minutos para detección de conflictos
    private static final int INTERVALO_MINUTOS = 20;

    // ==========================================
    // MÉTODOS ENTITY-BASED (COMPATIBILIDAD)
    // ==========================================

    @Transactional
    public Cita registrarCita(Cita cita) {
        Paciente paciente = pacienteRepository.findById(cita.getPaciente().getIdPaciente())
                .orElseThrow(() -> new IllegalArgumentException("Paciente no existe"));
        Medico medico = medicoRepository.findById(cita.getMedico().getIdMedico())
                .orElseThrow(() -> new IllegalArgumentException("Médico no existe"));

        if (cita.getFechaHora() == null || cita.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Fecha y hora deben ser futuras y no nulas");
        }
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        return citaRepository.save(cita);
    }

    @Transactional(readOnly = true)
    public Cita obtenerCita(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica con ID " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Cita> listarTodas() {
        return citaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cita> buscarPorEstado(Cita.Estado estado) {
        return citaRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<Cita> buscarPorPaciente(Paciente paciente) {
        return citaRepository.findByPaciente(paciente);
    }

    @Transactional(readOnly = true)
    public List<Cita> buscarPorMedico(Medico medico) {
        return citaRepository.findByMedico(medico);
    }

    @Transactional(readOnly = true)
    public List<Cita> buscarPorFechaHora(LocalDateTime fechaHora) {
        return citaRepository.findByFechaHora(fechaHora);
    }

    @Transactional
    public Cita actualizarCita(Long id, Cita datos) {
        Cita existente = obtenerCita(id);
        if (!existente.getPaciente().getIdPaciente().equals(datos.getPaciente().getIdPaciente())) {
            Paciente nuevoPaciente = pacienteRepository.findById(datos.getPaciente().getIdPaciente())
                    .orElseThrow(() -> new IllegalArgumentException("Paciente nuevo no existe"));
            existente.setPaciente(nuevoPaciente);
        }
        if (!existente.getMedico().getIdMedico().equals(datos.getMedico().getIdMedico())) {
            Medico nuevoMedico = medicoRepository.findById(datos.getMedico().getIdMedico())
                    .orElseThrow(() -> new IllegalArgumentException("Médico nuevo no existe"));
            existente.setMedico(nuevoMedico);
        }
        existente.setFechaHora(datos.getFechaHora());
        existente.setMotivo(datos.getMotivo());
        existente.setObservaciones(datos.getObservaciones());
        return citaRepository.save(existente);
    }

    @Transactional
    public void cambiarEstado(Long id, Cita.Estado nuevoEstado) {
        Cita cita = obtenerCita(id);
        cita.setEstado(nuevoEstado);
        citaRepository.save(cita);
    }

    @Transactional
    public void cancelarCita(Long id) {
        cambiarEstado(id, Cita.Estado.CANCELADA);
    }

    @Transactional
    public void eliminarCita(Long id) {
        Cita cita = obtenerCita(id);
        if (cita.getConsulta() != null) {
            throw new IllegalArgumentException("No se puede eliminar la cita porque está vinculada a una consulta médica");
        }
        citaRepository.delete(cita);
    }

    @Transactional(readOnly = true)
    public long totalCitas() {
        return citaRepository.count();
    }

    // ==========================================
    // MÉTODOS DTO-BASED Y REGLAS DE NEGOCIO
    // ==========================================

    @Transactional(readOnly = true)
    public List<CitaResponseDTO> listarCitasDTO(String search, String estadoStr, Long idMedico, Long idPaciente,
                                                LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                                Authentication auth) {
        Long medicoIdFiltro = idMedico;
        if (esSoloMedico(auth)) {
            Medico medicoAutenticado = resolverMedicoAutenticado(auth);
            medicoIdFiltro = medicoAutenticado.getIdMedico();
        }

        Cita.Estado estadoEnum = parsearEstado(estadoStr);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Cita> cq = cb.createQuery(Cita.class);
        Root<Cita> root = cq.from(Cita.class);
        root.fetch("paciente", JoinType.INNER);
        root.fetch("medico", JoinType.INNER);
        root.fetch("consulta", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        if (medicoIdFiltro != null) {
            predicates.add(cb.equal(root.get("medico").get("idMedico"), medicoIdFiltro));
        }
        if (idPaciente != null) {
            predicates.add(cb.equal(root.get("paciente").get("idPaciente"), idPaciente));
        }
        if (estadoEnum != null) {
            predicates.add(cb.equal(root.get("estado"), estadoEnum));
        }
        if (fechaDesde != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), fechaDesde));
        }
        if (fechaHasta != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fechaHora"), fechaHasta));
        }
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            Predicate pNombre = cb.like(cb.lower(root.get("paciente").get("nombres")), pattern);
            Predicate pApellido = cb.like(cb.lower(root.get("paciente").get("apellidos")), pattern);
            Predicate pDni = cb.like(root.get("paciente").get("dni"), pattern);
            Predicate mNombre = cb.like(cb.lower(root.get("medico").get("nombres")), pattern);
            Predicate mApellido = cb.like(cb.lower(root.get("medico").get("apellidos")), pattern);
            Predicate cMotivo = cb.like(cb.lower(root.get("motivo")), pattern);
            predicates.add(cb.or(pNombre, pApellido, pDni, mNombre, mApellido, cMotivo));
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        cq.orderBy(cb.desc(root.get("fechaHora")));
        cq.distinct(true);

        List<Cita> citas = em.createQuery(cq).getResultList();
        return citas.stream().map(this::convertirAResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CitaResponseDTO obtenerCitaDTO(Long id, Authentication auth) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica con ID " + id + " no encontrada"));

        validarAccesoCita(cita, auth);
        return convertirAResponseDTO(cita);
    }

    @Transactional
    public CitaResponseDTO registrarCitaDTO(CitaRequestDTO dto, Authentication auth) {
        Long idMedicoFinal = dto.getIdMedico();
        if (esSoloMedico(auth)) {
            Medico medicoAut = resolverMedicoAutenticado(auth);
            idMedicoFinal = medicoAut.getIdMedico();
        } else if (idMedicoFinal == null) {
            throw new IllegalArgumentException("Debe seleccionar un médico responsable para la cita");
        }

        Medico medico = medicoRepository.findById(idMedicoFinal)
                .orElseThrow(() -> new IllegalArgumentException("El médico seleccionado no existe"));

        if (Boolean.FALSE.equals(medico.getEstado())) {
            throw new IllegalStateException("El médico seleccionado no se encuentra activo para atender citas");
        }

        Paciente paciente = pacienteRepository.findById(dto.getIdPaciente())
                .orElseThrow(() -> new IllegalArgumentException("El paciente seleccionado no existe"));

        if (Boolean.FALSE.equals(paciente.getEstado())) {
            throw new IllegalStateException("El paciente seleccionado no se encuentra activo");
        }

        if (dto.getFechaHora() == null || dto.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora de la cita deben ser futuras y válidas");
        }

        verificarDisponibilidadHorario(medico.getIdMedico(), dto.getFechaHora(), null);

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaHora(dto.getFechaHora());
        cita.setMotivo(dto.getMotivo().trim());
        cita.setObservaciones(dto.getObservaciones() != null ? dto.getObservaciones().trim() : null);

        Cita.Estado estadoInicial = parsearEstado(dto.getEstado());
        cita.setEstado(estadoInicial != null ? estadoInicial : Cita.Estado.PROGRAMADA);

        Cita guardada = citaRepository.save(cita);
        return convertirAResponseDTO(guardada);
    }

    @Transactional
    public CitaResponseDTO actualizarCitaDTO(Long id, CitaRequestDTO dto, Authentication auth) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica con ID " + id + " no encontrada"));

        validarAccesoCita(cita, auth);

        if (cita.getEstado() == Cita.Estado.CANCELADA) {
            throw new IllegalStateException("No se puede editar una cita médica que ya ha sido cancelada");
        }
        if (cita.getEstado() == Cita.Estado.ATENDIDA) {
            throw new IllegalStateException("No se puede editar una cita médica que ya fue atendida en consulta");
        }

        if (dto.getFechaHora() != null && !dto.getFechaHora().isEqual(cita.getFechaHora())) {
            if (dto.getFechaHora().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("La nueva fecha y hora deben ser futuras");
            }
            verificarDisponibilidadHorario(cita.getMedico().getIdMedico(), dto.getFechaHora(), cita.getIdCita());
            cita.setFechaHora(dto.getFechaHora());
        }

        if (dto.getIdPaciente() != null && !dto.getIdPaciente().equals(cita.getPaciente().getIdPaciente())) {
            if (esSoloMedico(auth)) {
                throw new AccessDeniedException("Un médico no puede reasignar el paciente de una cita");
            }
            Paciente nuevoPaciente = pacienteRepository.findById(dto.getIdPaciente())
                    .orElseThrow(() -> new IllegalArgumentException("El nuevo paciente seleccionado no existe"));
            cita.setPaciente(nuevoPaciente);
        }

        if (dto.getMotivo() != null && !dto.getMotivo().trim().isEmpty()) {
            cita.setMotivo(dto.getMotivo().trim());
        }

        if (dto.getObservaciones() != null) {
            cita.setObservaciones(dto.getObservaciones().trim());
        }

        if (dto.getEstado() != null) {
            Cita.Estado nuevoEstado = parsearEstado(dto.getEstado());
            if (nuevoEstado != null) {
                cita.setEstado(nuevoEstado);
            }
        }

        Cita actualizada = citaRepository.save(cita);
        return convertirAResponseDTO(actualizada);
    }

    @Transactional
    public CitaResponseDTO reprogramarCitaDTO(Long id, CitaReprogramarDTO dto, Authentication auth) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica con ID " + id + " no encontrada"));

        validarAccesoCita(cita, auth);

        if (cita.getEstado() == Cita.Estado.CANCELADA) {
            throw new IllegalStateException("No se puede reprogramar una cita cancelada");
        }
        if (cita.getEstado() == Cita.Estado.ATENDIDA) {
            throw new IllegalStateException("No se puede reprogramar una cita ya atendida");
        }

        if (dto.getNuevaFechaHora() == null || dto.getNuevaFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La nueva fecha y hora de la cita deben ser futuras");
        }

        verificarDisponibilidadHorario(cita.getMedico().getIdMedico(), dto.getNuevaFechaHora(), cita.getIdCita());

        LocalDateTime fechaAnterior = cita.getFechaHora();
        cita.setFechaHora(dto.getNuevaFechaHora());
        cita.setEstado(Cita.Estado.PROGRAMADA);

        String notaReprogramacion = "[Reprogramada de " + fechaAnterior.toString().replace('T', ' ') + " a " +
                dto.getNuevaFechaHora().toString().replace('T', ' ') +
                (dto.getMotivoReprogramacion() != null && !dto.getMotivoReprogramacion().trim().isEmpty()
                        ? " por motivo: " + dto.getMotivoReprogramacion().trim() : "") + "]";

        if (cita.getObservaciones() != null && !cita.getObservaciones().isEmpty()) {
            cita.setObservaciones(cita.getObservaciones() + "\n" + notaReprogramacion);
        } else {
            cita.setObservaciones(notaReprogramacion);
        }

        Cita guardada = citaRepository.save(cita);
        return convertirAResponseDTO(guardada);
    }

    @Transactional
    public CitaResponseDTO cancelarCitaDTO(Long id, String motivoCancelacion, Authentication auth) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica con ID " + id + " no encontrada"));

        validarAccesoCita(cita, auth);

        if (cita.getEstado() == Cita.Estado.ATENDIDA) {
            throw new IllegalStateException("No se puede cancelar una cita médica que ya ha sido atendida en consulta");
        }

        cita.setEstado(Cita.Estado.CANCELADA);

        if (motivoCancelacion != null && !motivoCancelacion.trim().isEmpty()) {
            String notaCancelacion = "[Cancelada el " + LocalDateTime.now().toString().replace('T', ' ') +
                    ": " + motivoCancelacion.trim() + "]";
            if (cita.getObservaciones() != null && !cita.getObservaciones().isEmpty()) {
                cita.setObservaciones(cita.getObservaciones() + "\n" + notaCancelacion);
            } else {
                cita.setObservaciones(notaCancelacion);
            }
        }

        Cita guardada = citaRepository.save(cita);
        return convertirAResponseDTO(guardada);
    }

    @Transactional
    public CitaResponseDTO cambiarEstadoDTO(Long id, String nuevoEstadoStr, Authentication auth) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita médica con ID " + id + " no encontrada"));

        validarAccesoCita(cita, auth);

        Cita.Estado nuevoEstado = parsearEstado(nuevoEstadoStr);
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstadoStr);
        }

        if (cita.getEstado() == Cita.Estado.CANCELADA && nuevoEstado != Cita.Estado.CANCELADA) {
            throw new IllegalStateException("Una cita cancelada requiere una reprogramación explícita para reactivarse");
        }

        cita.setEstado(nuevoEstado);
        Cita guardada = citaRepository.save(cita);
        return convertirAResponseDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<CitaResponseDTO> listarCitasDisponiblesParaConsulta(Authentication auth) {
        List<Cita> disponibles;
        if (esSoloMedico(auth)) {
            Medico medicoAut = resolverMedicoAutenticado(auth);
            disponibles = citaRepository.findCitasDisponiblesParaConsultaPorMedico(medicoAut.getIdMedico());
        } else {
            disponibles = citaRepository.findCitasDisponiblesParaConsultaTodas();
        }
        return disponibles.stream().map(this::convertirAResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Authentication auth) {
        Map<String, Object> stats = new HashMap<>();
        if (esSoloMedico(auth)) {
            Medico medicoAut = resolverMedicoAutenticado(auth);
            Long idMed = medicoAut.getIdMedico();
            stats.put("total", citaRepository.countByMedico_IdMedico(idMed));
            stats.put("programadas", citaRepository.countByMedico_IdMedicoAndEstado(idMed, Cita.Estado.PROGRAMADA));
            stats.put("confirmadas", citaRepository.countByMedico_IdMedicoAndEstado(idMed, Cita.Estado.CONFIRMADA));
            stats.put("atendidas", citaRepository.countByMedico_IdMedicoAndEstado(idMed, Cita.Estado.ATENDIDA));
            stats.put("canceladas", citaRepository.countByMedico_IdMedicoAndEstado(idMed, Cita.Estado.CANCELADA));
        } else {
            stats.put("total", citaRepository.count());
            stats.put("programadas", citaRepository.countByEstado(Cita.Estado.PROGRAMADA));
            stats.put("confirmadas", citaRepository.countByEstado(Cita.Estado.CONFIRMADA));
            stats.put("atendidas", citaRepository.countByEstado(Cita.Estado.ATENDIDA));
            stats.put("canceladas", citaRepository.countByEstado(Cita.Estado.CANCELADA));
        }
        return stats;
    }

    // ==========================================
    // MÉTODOS AUXILIARES DE SEGURIDAD Y VALIDACIÓN
    // ==========================================

    private void verificarDisponibilidadHorario(Long idMedico, LocalDateTime fechaHora, Long idCitaExcluida) {
        LocalDateTime inicioIntervalo = fechaHora.minusMinutes(INTERVALO_MINUTOS);
        LocalDateTime finIntervalo = fechaHora.plusMinutes(INTERVALO_MINUTOS);

        long conflictos;
        if (idCitaExcluida != null) {
            conflictos = citaRepository.countConflictosHorarioExcluyendo(idMedico, inicioIntervalo, finIntervalo, idCitaExcluida);
        } else {
            conflictos = citaRepository.countConflictosHorarioNuevo(idMedico, inicioIntervalo, finIntervalo);
        }

        if (conflictos > 0) {
            throw new IllegalStateException("El médico ya tiene una cita médica programada en ese horario (margen de " +
                    INTERVALO_MINUTOS + " minutos). Por favor seleccione otro horario.");
        }
    }

    public boolean esSoloMedico(Authentication auth) {
        if (auth == null) return false;
        boolean esAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));
        if (esAdmin) return false;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_MEDICO") || a.equals("MEDICO"));
    }

    public Medico resolverMedicoAutenticado(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No se encuentra información de autenticación válida");
        }
        Usuario usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

        return medicoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado (" + auth.getName() + ") no tiene un perfil médico asociado"));
    }

    private void validarAccesoCita(Cita cita, Authentication auth) {
        if (esSoloMedico(auth)) {
            Medico medicoAut = resolverMedicoAutenticado(auth);
            if (!cita.getMedico().getIdMedico().equals(medicoAut.getIdMedico())) {
                throw new AccessDeniedException("No tiene permisos para consultar o modificar una cita médica de otro profesional");
            }
        }
    }

    private Cita.Estado parsearEstado(String estadoStr) {
        if (estadoStr == null || estadoStr.trim().isEmpty() || estadoStr.equalsIgnoreCase("TODOS")) {
            return null;
        }
        try {
            return Cita.Estado.valueOf(estadoStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public CitaResponseDTO convertirAResponseDTO(Cita cita) {
        Paciente p = cita.getPaciente();
        Medico m = cita.getMedico();

        return CitaResponseDTO.builder()
                .idCita(cita.getIdCita())
                .fechaHora(cita.getFechaHora())
                .motivo(cita.getMotivo())
                .estado(cita.getEstado().name())
                .observaciones(cita.getObservaciones())
                .idPaciente(p != null ? p.getIdPaciente() : null)
                .nombrePaciente(p != null ? (p.getNombres() + " " + p.getApellidos()).trim() : null)
                .dniPaciente(p != null ? p.getDni() : null)
                .telefonoPaciente(p != null ? p.getTelefono() : null)
                .idMedico(m != null ? m.getIdMedico() : null)
                .nombreMedico(m != null ? (m.getNombres() + " " + m.getApellidos()).trim() : null)
                .especialidadMedico(m != null ? m.getEspecialidad() : null)
                .colegiaturaMedico(m != null ? m.getColegiatura() : null)
                .idConsulta(cita.getConsulta() != null ? cita.getConsulta().getIdConsulta() : null)
                .build();
    }
}
