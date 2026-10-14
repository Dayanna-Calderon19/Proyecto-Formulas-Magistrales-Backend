package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.ConsultaResponseDTO;
import com.example.proyecto_formulas_magistrales.DTO.FormulaResumenDTO;
import com.example.proyecto_formulas_magistrales.Model.*;
import com.example.proyecto_formulas_magistrales.Repository.*;
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
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    // ==========================================
    // MÉTODOS ENTITY-BASED (COMPATIBILIDAD)
    // ==========================================

    @Transactional
    public Consulta registrarConsulta(Consulta consulta) {
        Paciente paciente = pacienteRepository.findById(consulta.getPaciente().getIdPaciente())
                .orElseThrow(() -> new IllegalArgumentException("Paciente no existe"));
        Medico medico = medicoRepository.findById(consulta.getMedico().getIdMedico())
                .orElseThrow(() -> new IllegalArgumentException("Médico no existe"));

        if (consulta.getFechaConsulta() == null) {
            throw new IllegalArgumentException("Fecha de consulta no puede ser nula");
        }
        if (consulta.getFechaConsulta().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Fecha de consulta no puede ser futura");
        }
        consulta.setPaciente(paciente);
        consulta.setMedico(medico);
        return consultaRepository.save(consulta);
    }

    @Transactional(readOnly = true)
    public Consulta obtenerConsulta(Long id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta médica con ID " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarTodas() {
        return consultaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Consulta> buscarPorPaciente(Paciente paciente) {
        return consultaRepository.findByPaciente(paciente);
    }

    @Transactional(readOnly = true)
    public List<Consulta> buscarPorMedico(Medico medico) {
        return consultaRepository.findByMedico(medico);
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarOrdenadasPorFecha() {
        return consultaRepository.findAllByOrderByFechaConsultaAsc();
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarPorPacienteOrdenado(Paciente paciente) {
        return consultaRepository.findByPacienteOrderByFechaConsultaAsc(paciente);
    }

    @Transactional(readOnly = true)
    public List<Consulta> listarPorMedicoOrdenado(Medico medico) {
        return consultaRepository.findByMedicoOrderByFechaConsultaAsc(medico);
    }

    @Transactional(readOnly = true)
    public List<Consulta> buscarPorDiagnostico(String diagnostico) {
        return consultaRepository.findByDiagnosticoContainingIgnoreCase(diagnostico);
    }

    @Transactional
    public Consulta actualizarConsulta(Long id, Consulta datos) {
        Consulta existente = obtenerConsulta(id);
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
        existente.setFechaConsulta(datos.getFechaConsulta());
        existente.setMotivo(datos.getMotivo());
        existente.setDiagnostico(datos.getDiagnostico());
        existente.setTratamiento(datos.getTratamiento());
        existente.setObservaciones(datos.getObservaciones());
        return consultaRepository.save(existente);
    }

    @Transactional
    public void eliminarConsulta(Long id) {
        Consulta consulta = obtenerConsulta(id);
        consultaRepository.delete(consulta);
    }

    @Transactional(readOnly = true)
    public long totalConsultas() {
        return consultaRepository.count();
    }

    // ==========================================
    // MÉTODOS DTO-BASED Y REGLAS DE NEGOCIO
    // ==========================================

    @Transactional(readOnly = true)
    public List<ConsultaResponseDTO> listarConsultasDTO(String search, Long idMedico, Long idPaciente,
                                                        LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                                        Authentication auth) {
        Long medicoIdFiltro = idMedico;
        if (esSoloMedico(auth)) {
            Medico medicoAut = resolverMedicoAutenticado(auth);
            medicoIdFiltro = medicoAut.getIdMedico();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Consulta> cq = cb.createQuery(Consulta.class);
        Root<Consulta> root = cq.from(Consulta.class);
        root.fetch("paciente", JoinType.INNER);
        root.fetch("medico", JoinType.INNER);
        root.fetch("cita", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        if (medicoIdFiltro != null) {
            predicates.add(cb.equal(root.get("medico").get("idMedico"), medicoIdFiltro));
        }
        if (idPaciente != null) {
            predicates.add(cb.equal(root.get("paciente").get("idPaciente"), idPaciente));
        }
        if (fechaDesde != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fechaConsulta"), fechaDesde));
        }
        if (fechaHasta != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fechaConsulta"), fechaHasta));
        }
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            Predicate pNombre = cb.like(cb.lower(root.get("paciente").get("nombres")), pattern);
            Predicate pApellido = cb.like(cb.lower(root.get("paciente").get("apellidos")), pattern);
            Predicate pDni = cb.like(root.get("paciente").get("dni"), pattern);
            Predicate mNombre = cb.like(cb.lower(root.get("medico").get("nombres")), pattern);
            Predicate mApellido = cb.like(cb.lower(root.get("medico").get("apellidos")), pattern);
            Predicate cMotivo = cb.like(cb.lower(root.get("motivo")), pattern);
            Predicate cDiag = cb.like(cb.lower(root.get("diagnostico")), pattern);
            predicates.add(cb.or(pNombre, pApellido, pDni, mNombre, mApellido, cMotivo, cDiag));
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        cq.orderBy(cb.desc(root.get("fechaConsulta")));
        cq.distinct(true);

        List<Consulta> consultas = em.createQuery(cq).getResultList();
        return consultas.stream().map(this::convertirAResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsultaResponseDTO obtenerConsultaDTO(Long id, Authentication auth) {
        Consulta consulta = consultaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta médica con ID " + id + " no encontrada"));

        validarAccesoConsulta(consulta, auth);
        return convertirAResponseDTO(consulta);
    }

    @Transactional
    public ConsultaResponseDTO registrarConsultaDTO(ConsultaRequestDTO dto, Authentication auth) {
        Consulta consulta = new Consulta();
        Cita citaAsociada = null;
        Paciente paciente;
        Medico medico;

        // 1. Caso atención con Cita previa
        if (dto.getIdCita() != null) {
            citaAsociada = citaRepository.findByIdWithDetails(dto.getIdCita())
                    .orElseThrow(() -> new IllegalArgumentException("La cita médica seleccionada no existe"));

            if (consultaRepository.existsByCita_IdCita(dto.getIdCita())) {
                throw new IllegalStateException("La cita seleccionada ya tiene una consulta médica registrada");
            }

            if (citaAsociada.getEstado() == Cita.Estado.CANCELADA) {
                throw new IllegalStateException("No se puede iniciar una consulta médica para una cita cancelada");
            }

            if (esSoloMedico(auth)) {
                Medico medicoAut = resolverMedicoAutenticado(auth);
                if (!citaAsociada.getMedico().getIdMedico().equals(medicoAut.getIdMedico())) {
                    throw new AccessDeniedException("No puede iniciar la consulta de una cita asignada a otro médico");
                }
            }

            paciente = citaAsociada.getPaciente();
            medico = citaAsociada.getMedico();

            citaAsociada.setEstado(Cita.Estado.ATENDIDA);
            citaRepository.save(citaAsociada);

            consulta.setCita(citaAsociada);
        } else {
            // 2. Caso atención directa
            if (dto.getIdPaciente() == null) {
                throw new IllegalArgumentException("Debe seleccionar un paciente para la consulta médica");
            }

            paciente = pacienteRepository.findById(dto.getIdPaciente())
                    .orElseThrow(() -> new IllegalArgumentException("El paciente seleccionado no existe"));

            if (esSoloMedico(auth)) {
                medico = resolverMedicoAutenticado(auth);
            } else {
                if (dto.getIdMedico() == null) {
                    throw new IllegalArgumentException("Debe seleccionar un médico responsable para la consulta");
                }
                medico = medicoRepository.findById(dto.getIdMedico())
                        .orElseThrow(() -> new IllegalArgumentException("El médico seleccionado no existe"));
            }
        }

        if (Boolean.FALSE.equals(paciente.getEstado())) {
            throw new IllegalStateException("El paciente seleccionado no se encuentra activo");
        }
        if (Boolean.FALSE.equals(medico.getEstado())) {
            throw new IllegalStateException("El médico responsable no se encuentra activo");
        }

        consulta.setPaciente(paciente);
        consulta.setMedico(medico);
        consulta.setFechaConsulta(dto.getFechaConsulta() != null ? dto.getFechaConsulta() : LocalDateTime.now());
        consulta.setMotivo(dto.getMotivo().trim());
        consulta.setDiagnostico(dto.getDiagnostico().trim());
        consulta.setTratamiento(dto.getTratamiento() != null ? dto.getTratamiento().trim() : null);
        consulta.setObservaciones(dto.getObservaciones() != null ? dto.getObservaciones().trim() : null);

        Consulta guardada = consultaRepository.save(consulta);
        return convertirAResponseDTO(guardada);
    }

    @Transactional
    public ConsultaResponseDTO actualizarConsultaDTO(Long id, ConsultaRequestDTO dto, Authentication auth) {
        Consulta consulta = consultaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta médica con ID " + id + " no encontrada"));

        validarAccesoConsulta(consulta, auth);

        if (dto.getMotivo() != null && !dto.getMotivo().trim().isEmpty()) {
            consulta.setMotivo(dto.getMotivo().trim());
        }
        if (dto.getDiagnostico() != null && !dto.getDiagnostico().trim().isEmpty()) {
            consulta.setDiagnostico(dto.getDiagnostico().trim());
        }
        if (dto.getTratamiento() != null) {
            consulta.setTratamiento(dto.getTratamiento().trim());
        }
        if (dto.getObservaciones() != null) {
            consulta.setObservaciones(dto.getObservaciones().trim());
        }
        if (dto.getFechaConsulta() != null) {
            consulta.setFechaConsulta(dto.getFechaConsulta());
        }

        Consulta actualizada = consultaRepository.save(consulta);
        return convertirAResponseDTO(actualizada);
    }

    @Transactional
    public void eliminarConsultaDTO(Long id, Authentication auth) {
        Consulta consulta = consultaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta médica con ID " + id + " no encontrada"));

        validarAccesoConsulta(consulta, auth);

        if (consulta.getFormulas() != null && !consulta.getFormulas().isEmpty()) {
            throw new IllegalStateException("No se puede eliminar la consulta médica porque tiene " +
                    consulta.getFormulas().size() + " fórmula(s) magistral(es) vinculada(s). " +
                    "Debe anular o desvincular las fórmulas previamente para preservar la trazabilidad.");
        }

        if (consulta.getCita() != null) {
            Cita cita = consulta.getCita();
            cita.setEstado(Cita.Estado.PROGRAMADA);
            citaRepository.save(cita);
        }

        consultaRepository.delete(consulta);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Authentication auth) {
        Map<String, Object> stats = new HashMap<>();
        if (esSoloMedico(auth)) {
            Medico medicoAut = resolverMedicoAutenticado(auth);
            stats.put("total", consultaRepository.countByMedico_IdMedico(medicoAut.getIdMedico()));
        } else {
            stats.put("total", consultaRepository.count());
        }
        return stats;
    }

    // ==========================================
    // MÉTODOS AUXILIARES DE SEGURIDAD Y MAPEO
    // ==========================================

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

    private void validarAccesoConsulta(Consulta consulta, Authentication auth) {
        if (esSoloMedico(auth)) {
            Medico medicoAut = resolverMedicoAutenticado(auth);
            if (!consulta.getMedico().getIdMedico().equals(medicoAut.getIdMedico())) {
                throw new AccessDeniedException("No tiene permisos para consultar o modificar atenciones clínicas de otro médico");
            }
        }
    }

    public ConsultaResponseDTO convertirAResponseDTO(Consulta c) {
        Paciente p = c.getPaciente();
        Medico m = c.getMedico();
        Cita cita = c.getCita();

        List<FormulaResumenDTO> formulasResumen = null;
        if (c.getFormulas() != null) {
            formulasResumen = c.getFormulas().stream()
                    .map(f -> FormulaResumenDTO.builder()
                            .idFormula(f.getId())
                            .nombre(f.getNombre())
                            .composicion(f.getComposicion())
                            .cantidad(f.getCantidad())
                            .unidad(f.getUnidad())
                            .estado(f.getEstado())
                            .build())
                    .collect(Collectors.toList());
        }

        return ConsultaResponseDTO.builder()
                .idConsulta(c.getIdConsulta())
                .fechaConsulta(c.getFechaConsulta())
                .motivo(c.getMotivo())
                .diagnostico(c.getDiagnostico())
                .tratamiento(c.getTratamiento())
                .observaciones(c.getObservaciones())
                .idCita(cita != null ? cita.getIdCita() : null)
                .fechaHoraCita(cita != null ? cita.getFechaHora() : null)
                .idPaciente(p != null ? p.getIdPaciente() : null)
                .nombrePaciente(p != null ? (p.getNombres() + " " + p.getApellidos()).trim() : null)
                .dniPaciente(p != null ? p.getDni() : null)
                .telefonoPaciente(p != null ? p.getTelefono() : null)
                .correoPaciente(p != null ? p.getCorreo() : null)
                .idMedico(m != null ? m.getIdMedico() : null)
                .nombreMedico(m != null ? (m.getNombres() + " " + m.getApellidos()).trim() : null)
                .especialidadMedico(m != null ? m.getEspecialidad() : null)
                .colegiaturaMedico(m != null ? m.getColegiatura() : null)
                .totalFormulas(formulasResumen != null ? formulasResumen.size() : 0)
                .formulas(formulasResumen != null ? formulasResumen : List.of())
                .build();
    }
}
