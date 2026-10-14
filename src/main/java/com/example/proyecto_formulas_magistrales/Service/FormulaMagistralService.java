package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaResumenDTO;
import com.example.proyecto_formulas_magistrales.DTO.FormulaMagistralRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.FormulaMagistralResponseDTO;
import com.example.proyecto_formulas_magistrales.Model.Consulta;
import com.example.proyecto_formulas_magistrales.Model.FormulaMagistral;
import com.example.proyecto_formulas_magistrales.Model.Medico;
import com.example.proyecto_formulas_magistrales.Repository.ConsultaRepository;
import com.example.proyecto_formulas_magistrales.Repository.FormulaMagistralRepository;
import com.example.proyecto_formulas_magistrales.Repository.MedicoRepository;
import com.example.proyecto_formulas_magistrales.Repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormulaMagistralService {

    private final FormulaMagistralRepository formulaRepository;
    private final ConsultaRepository consultaRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;

    // Listar fórmulas magistrales con filtros de búsqueda y estado
    @Transactional(readOnly = true)
    public List<FormulaMagistralResponseDTO> listarTodas(String search, String estado, String username, boolean isAdmin) {
        List<FormulaMagistral> lista = formulaRepository.buscarConFiltros(
                (search != null && !search.isBlank()) ? search.trim() : null,
                (estado != null && !estado.isBlank()) ? estado.trim() : null,
                null
        );

        return lista.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Obtener fórmula DTO por ID
    @Transactional(readOnly = true)
    public FormulaMagistralResponseDTO obtenerFormulaDTO(Long id) {
        FormulaMagistral formula = formulaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Fórmula magistral con ID " + id + " no encontrada"));
        return mapToResponseDTO(formula);
    }

    // Obtener entidad por ID (compatibilidad con tests unitarios existentes)
    @Transactional(readOnly = true)
    public FormulaMagistral obtenerFormula(Long id) {
        return formulaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("La fórmula no ha sido encontrada"));
    }

    // Registrar una nueva fórmula magistral desde DTO
    @Transactional
    public FormulaMagistralResponseDTO crearFormula(FormulaMagistralRequestDTO dto, String username, boolean isAdmin) {
        String cleanNombre = dto.getNombre().trim();

        if (formulaRepository.existsByNombreIgnoreCase(cleanNombre)) {
            throw new IllegalStateException("Ya existe una fórmula magistral registrada con el nombre '" + cleanNombre + "'");
        }

        if (dto.getFechaVencimiento().isBefore(dto.getFechaPreparacion())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de preparación");
        }

        if (dto.getCantidad() == null || dto.getCantidad().compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("La cantidad debe ser igual o superior a 1.00");
        }

        FormulaMagistral formula = new FormulaMagistral();
        formula.setNombre(cleanNombre);
        formula.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null);
        formula.setComposicion(dto.getComposicion().trim());
        formula.setCantidad(dto.getCantidad());
        formula.setUnidad(dto.getUnidad().trim());
        formula.setFechaPreparacion(dto.getFechaPreparacion());
        formula.setFechaVencimiento(dto.getFechaVencimiento());
        formula.setEstado(dto.getEstado() != null ? dto.getEstado().trim().toUpperCase() : "ACTIVO");

        if (dto.getIdConsulta() != null && consultaRepository != null) {
            Consulta consulta = consultaRepository.findById(dto.getIdConsulta())
                    .orElseThrow(() -> new EntityNotFoundException("La consulta médica seleccionada con ID " + dto.getIdConsulta() + " no existe"));

            if (!isAdmin && username != null) {
                validarPermisoMedicoSobreConsulta(consulta, username);
            }

            formula.setConsulta(consulta);
        }

        FormulaMagistral guardada = formulaRepository.save(formula);
        return mapToResponseDTO(guardada);
    }

    // Crear fórmula desde entidad (para compatibilidad con tests unitarios existentes)
    @Transactional
    public FormulaMagistral crearFormula(FormulaMagistral formula) {
        if (formulaRepository.existsByNombreIgnoreCase(formula.getNombre())) {
            throw new IllegalArgumentException("Ya existe una fórmula con ese nombre");
        }
        return formulaRepository.save(formula);
    }

    // Actualizar fórmula magistral desde DTO
    @Transactional
    public FormulaMagistralResponseDTO actualizarFormula(Long id, FormulaMagistralRequestDTO dto, String username, boolean isAdmin) {
        FormulaMagistral formula = formulaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Fórmula magistral con ID " + id + " no encontrada"));

        if (!isAdmin && ("ANULADA".equalsIgnoreCase(formula.getEstado()) || "DISPENSADA".equalsIgnoreCase(formula.getEstado()))) {
            throw new IllegalStateException("No es posible editar una fórmula en estado " + formula.getEstado());
        }

        String cleanNombre = dto.getNombre().trim();
        if (!formula.getNombre().equalsIgnoreCase(cleanNombre) && formulaRepository.existsByNombreIgnoreCase(cleanNombre)) {
            throw new IllegalStateException("Ya existe otra fórmula registrada con el nombre '" + cleanNombre + "'");
        }

        if (dto.getFechaVencimiento().isBefore(dto.getFechaPreparacion())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de preparación");
        }

        if (dto.getCantidad() == null || dto.getCantidad().compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("La cantidad debe ser igual o superior a 1.00");
        }

        formula.setNombre(cleanNombre);
        formula.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null);
        formula.setComposicion(dto.getComposicion().trim());
        formula.setCantidad(dto.getCantidad());
        formula.setUnidad(dto.getUnidad().trim());
        formula.setFechaPreparacion(dto.getFechaPreparacion());
        formula.setFechaVencimiento(dto.getFechaVencimiento());
        if (dto.getEstado() != null) {
            formula.setEstado(dto.getEstado().trim().toUpperCase());
        }

        if (dto.getIdConsulta() != null && consultaRepository != null) {
            Long idActual = (formula.getConsulta() != null) ? formula.getConsulta().getIdConsulta() : null;
            if (!dto.getIdConsulta().equals(idActual)) {
                Consulta nuevaConsulta = consultaRepository.findById(dto.getIdConsulta())
                        .orElseThrow(() -> new EntityNotFoundException("La consulta médica con ID " + dto.getIdConsulta() + " no existe"));

                if (!isAdmin && username != null) {
                    validarPermisoMedicoSobreConsulta(nuevaConsulta, username);
                }
                formula.setConsulta(nuevaConsulta);
            }
        }

        FormulaMagistral actualizada = formulaRepository.save(formula);
        return mapToResponseDTO(actualizada);
    }

    // Actualizar fórmula desde entidad (para compatibilidad con tests unitarios existentes)
    @Transactional
    public FormulaMagistral actualizarFormula(Long id, FormulaMagistral formulaActualizada) {
        FormulaMagistral formula = obtenerFormula(id);

        if (!formula.getNombre().equalsIgnoreCase(formulaActualizada.getNombre())
                && formulaRepository.existsByNombreIgnoreCase(formulaActualizada.getNombre())) {
            throw new IllegalArgumentException("Ya existe una fórmula con ese nombre");
        }

        formula.setNombre(formulaActualizada.getNombre());
        formula.setDescripcion(formulaActualizada.getDescripcion());
        formula.setComposicion(formulaActualizada.getComposicion());
        formula.setCantidad(formulaActualizada.getCantidad());
        formula.setUnidad(formulaActualizada.getUnidad());
        formula.setFechaPreparacion(formulaActualizada.getFechaPreparacion());
        formula.setFechaVencimiento(formulaActualizada.getFechaVencimiento());
        formula.setEstado(formulaActualizada.getEstado());

        return formulaRepository.save(formula);
    }

    // Cambiar estado de la fórmula
    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        FormulaMagistral formula = obtenerFormula(id);
        formula.setEstado(nuevoEstado.trim().toUpperCase());
        formulaRepository.save(formula);
    }

    // Desactivar o anular fórmula
    @Transactional
    public void desactivarFormula(Long id) {
        FormulaMagistral formula = obtenerFormula(id);
        formula.setEstado("INACTIVO");
        formulaRepository.save(formula);
    }

    // Eliminar fórmula si no tiene historial crítico
    @Transactional
    public void eliminarFormula(Long id) {
        FormulaMagistral formula = obtenerFormula(id);
        if (formula.getConsulta() != null) {
            formula.setEstado("ANULADA");
            formulaRepository.save(formula);
        } else {
            formulaRepository.delete(formula);
        }
    }

    // Listar consultas disponibles para asociar en el formulario
    @Transactional(readOnly = true)
    public List<ConsultaResumenDTO> listarConsultasParaAsociar(String username, boolean isAdmin) {
        List<Consulta> consultas;
        if (isAdmin || username == null) {
            consultas = consultaRepository.findAllWithPacienteAndMedico();
        } else {
            Optional<Medico> medicoOpt = (usuarioRepository != null && medicoRepository != null)
                    ? usuarioRepository.findByUsername(username)
                        .flatMap(u -> medicoRepository.findByUsuario_IdUsuario(u.getIdUsuario()))
                    : Optional.empty();

            if (medicoOpt.isPresent()) {
                consultas = consultaRepository.findByMedicoWithPaciente(medicoOpt.get().getIdMedico());
            } else {
                consultas = consultaRepository.findAllWithPacienteAndMedico();
            }
        }

        return consultas.stream().map(c -> ConsultaResumenDTO.builder()
                .idConsulta(c.getIdConsulta())
                .fechaConsulta(c.getFechaConsulta())
                .motivo(c.getMotivo())
                .diagnostico(c.getDiagnostico())
                .idPaciente(c.getPaciente() != null ? c.getPaciente().getIdPaciente() : null)
                .nombrePaciente(c.getPaciente() != null ? (c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos()) : "Desconocido")
                .dniPaciente(c.getPaciente() != null ? c.getPaciente().getDni() : "")
                .idMedico(c.getMedico() != null ? c.getMedico().getIdMedico() : null)
                .nombreMedico(c.getMedico() != null ? (c.getMedico().getNombres() + " " + c.getMedico().getApellidos()) : "Desconocido")
                .especialidadMedico(c.getMedico() != null ? c.getMedico().getEspecialidad() : "")
                .build()
        ).collect(Collectors.toList());
    }

    // Estadísticas
    @Transactional(readOnly = true)
    public long totalFormulas() {
        return formulaRepository.count();
    }

    @Transactional(readOnly = true)
    public long totalFormulasActivas() {
        return formulaRepository.countByEstadoIgnoreCase("ACTIVO") +
               formulaRepository.countByEstadoIgnoreCase("DISPONIBLE");
    }

    private void validarPermisoMedicoSobreConsulta(Consulta consulta, String username) {
        if (consulta.getMedico() == null || consulta.getMedico().getUsuario() == null) {
            return;
        }
        String usuarioMedico = consulta.getMedico().getUsuario().getUsername();
        if (!usuarioMedico.equalsIgnoreCase(username)) {
            throw new AccessDeniedException("No tiene autorización para asociar una fórmula a una consulta de otro médico");
        }
    }

    public FormulaMagistralResponseDTO mapToResponseDTO(FormulaMagistral formula) {
        Long idConsulta = null;
        java.time.LocalDateTime fechaConsulta = null;
        String motivoConsulta = null;
        String diagnosticoConsulta = null;

        Long idPaciente = null;
        String nombrePaciente = null;
        String dniPaciente = null;

        Long idMedico = null;
        String nombreMedico = null;
        String especialidadMedico = null;
        String colegiaturaMedico = null;

        if (formula.getConsulta() != null) {
            Consulta c = formula.getConsulta();
            idConsulta = c.getIdConsulta();
            fechaConsulta = c.getFechaConsulta();
            motivoConsulta = c.getMotivo();
            diagnosticoConsulta = c.getDiagnostico();

            if (c.getPaciente() != null) {
                idPaciente = c.getPaciente().getIdPaciente();
                nombrePaciente = c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos();
                dniPaciente = c.getPaciente().getDni();
            }

            if (c.getMedico() != null) {
                idMedico = c.getMedico().getIdMedico();
                nombreMedico = c.getMedico().getNombres() + " " + c.getMedico().getApellidos();
                especialidadMedico = c.getMedico().getEspecialidad();
                colegiaturaMedico = c.getMedico().getColegiatura();
            }
        }

        return FormulaMagistralResponseDTO.builder()
                .id(formula.getId())
                .nombre(formula.getNombre())
                .descripcion(formula.getDescripcion())
                .composicion(formula.getComposicion())
                .cantidad(formula.getCantidad())
                .unidad(formula.getUnidad())
                .fechaPreparacion(formula.getFechaPreparacion())
                .fechaVencimiento(formula.getFechaVencimiento())
                .estado(formula.getEstado())
                .idConsulta(idConsulta)
                .fechaConsulta(fechaConsulta)
                .motivoConsulta(motivoConsulta)
                .diagnosticoConsulta(diagnosticoConsulta)
                .idPaciente(idPaciente)
                .nombrePaciente(nombrePaciente)
                .dniPaciente(dniPaciente)
                .idMedico(idMedico)
                .nombreMedico(nombreMedico)
                .especialidadMedico(especialidadMedico)
                .colegiaturaMedico(colegiaturaMedico)
                .build();
    }
}
