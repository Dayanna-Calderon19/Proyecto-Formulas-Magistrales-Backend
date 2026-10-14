package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.*;
import com.example.proyecto_formulas_magistrales.Model.*;
import com.example.proyecto_formulas_magistrales.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final CitaRepository citaRepository;
    private final ConsultaRepository consultaRepository;
    private final FormulaMagistralRepository formulaMagistralRepository;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter DIA_MES_FORMATTER = DateTimeFormatter.ofPattern("dd MMM", new Locale("es", "ES"));

    @Transactional(readOnly = true)
    public DashboardResumenDTO obtenerResumen(String periodo, LocalDate fechaDesde, LocalDate fechaHasta, Authentication auth) {
        String username = auth != null ? auth.getName() : "anonimo";
        boolean esSoloMedico = esSoloMedico(auth);
        String rol = esSoloMedico ? "MEDICO" : "ADMIN";

        Usuario usuario = null;
        Medico medico = null;
        if (auth != null && auth.getName() != null) {
            usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
            if (usuario != null) {
                medico = medicoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario()).orElse(null);
            }
        }

        String nombreUsuario = usuario != null && usuario.getNombreCompleto() != null
                ? usuario.getNombreCompleto()
                : username;

        // Si es rol MEDICO pero no tiene perfil médico asociado, devolver respuesta controlada
        if (esSoloMedico && medico == null) {
            return DashboardResumenDTO.builder()
                    .rol(rol)
                    .username(username)
                    .nombreUsuario(nombreUsuario)
                    .tienePerfilMedico(false)
                    .periodo(periodo)
                    .fechaDesde(LocalDate.now())
                    .fechaHasta(LocalDate.now())
                    .kpis(Collections.emptyList())
                    .citasPorEstado(Collections.emptyList())
                    .tendenciaPeriodo(Collections.emptyList())
                    .formulasPorEstado(Collections.emptyList())
                    .proximasCitas(Collections.emptyList())
                    .build();
        }

        DashboardMedicoInfoDTO medicoInfo = null;
        if (medico != null) {
            medicoInfo = DashboardMedicoInfoDTO.builder()
                    .idMedico(medico.getIdMedico())
                    .nombres(medico.getNombres())
                    .apellidos(medico.getApellidos())
                    .nombreCompleto((medico.getNombres() + " " + medico.getApellidos()).trim())
                    .especialidad(medico.getEspecialidad())
                    .colegiatura(medico.getColegiatura())
                    .build();
        }

        // Resolver rango de fechas
        LocalDate[] rango = calcularRangoFechas(periodo, fechaDesde, fechaHasta);
        LocalDate start = rango[0];
        LocalDate end = rango[1];
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.atTime(LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();
        Long medicoId = medico != null ? medico.getIdMedico() : null;

        // 1. KPIs
        List<DashboardKpiDTO> kpis = esSoloMedico
                ? calcularKpisMedico(medicoId, start, end, startDT, endDT, now)
                : calcularKpisAdmin(start, end, startDT, endDT);

        // 2. Citas por Estado (con porcentajes reales y paleta del sistema)
        List<DashboardEstadoCountDTO> citasPorEstado = calcularCitasPorEstado(esSoloMedico, medicoId, startDT, endDT);

        // 3. Tendencia temporal de Citas y Consultas por día en el periodo
        List<DashboardSeriePuntoDTO> tendenciaPeriodo = calcularTendenciaPeriodo(esSoloMedico, medicoId, start, end, startDT, endDT);

        // 4. Fórmulas Magistrales por Estado en el periodo
        List<DashboardEstadoCountDTO> formulasPorEstado = calcularFormulasPorEstado(esSoloMedico, medicoId, start, end);

        // 5. Próximas Citas ordenadas cronológicamente
        List<DashboardProximaCitaDTO> proximasCitas = obtenerProximasCitas(esSoloMedico, medicoId, now);

        return DashboardResumenDTO.builder()
                .rol(rol)
                .username(username)
                .nombreUsuario(nombreUsuario)
                .tienePerfilMedico(true)
                .medicoInfo(medicoInfo)
                .periodo(periodo != null ? periodo : "30_DIAS")
                .fechaDesde(start)
                .fechaHasta(end)
                .kpis(kpis)
                .citasPorEstado(citasPorEstado)
                .tendenciaPeriodo(tendenciaPeriodo)
                .formulasPorEstado(formulasPorEstado)
                .proximasCitas(proximasCitas)
                .build();
    }

    private LocalDate[] calcularRangoFechas(String periodo, LocalDate fechaDesde, LocalDate fechaHasta) {
        LocalDate hoy = LocalDate.now();
        if (periodo == null || periodo.isBlank()) {
            periodo = "30_DIAS";
        }

        switch (periodo.toUpperCase()) {
            case "HOY":
                return new LocalDate[]{hoy, hoy};
            case "7_DIAS":
                return new LocalDate[]{hoy.minusDays(6), hoy};
            case "MES_ACTUAL":
                return new LocalDate[]{hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth())};
            case "PERSONALIZADO":
                if (fechaDesde != null && fechaHasta != null && !fechaDesde.isAfter(fechaHasta)) {
                    return new LocalDate[]{fechaDesde, fechaHasta};
                }
                return new LocalDate[]{hoy.minusDays(29), hoy};
            case "30_DIAS":
            default:
                return new LocalDate[]{hoy.minusDays(29), hoy};
        }
    }

    private List<DashboardKpiDTO> calcularKpisAdmin(LocalDate start, LocalDate end, LocalDateTime startDT, LocalDateTime endDT) {
        long totalPacientes = pacienteRepository.count();
        long pacientesActivos = pacienteRepository.countByEstado(true);

        long medicosActivos = medicoRepository.countByEstado(true);
        long medicosTotales = medicoRepository.count();

        long citasPeriodo = citaRepository.countByFechaHoraBetween(startDT, endDT);
        long citasPendientes = citaRepository.countByEstado(Cita.Estado.PROGRAMADA)
                + citaRepository.countByEstado(Cita.Estado.CONFIRMADA);

        long consultasPeriodo = consultaRepository.countByFechaConsultaBetween(startDT, endDT);
        long consultasTotales = consultaRepository.count();

        long formulasPeriodo = formulaMagistralRepository.countByFechaPreparacionBetween(start, end);
        long formulasTotales = formulaMagistralRepository.count();

        long usuariosActivos = usuarioRepository.countByActivoTrue();

        List<DashboardKpiDTO> kpis = new ArrayList<>();

        kpis.add(DashboardKpiDTO.builder()
                .id("pacientes_totales")
                .titulo("Total Pacientes")
                .valor(totalPacientes)
                .subtitulo(pacientesActivos + " activos")
                .icono("users")
                .ruta("/pacientes")
                .color("primary")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("medicos_activos")
                .titulo("Médicos Activos")
                .valor(medicosActivos)
                .subtitulo("De " + medicosTotales + " registrados")
                .icono("stethoscope")
                .ruta("/medicos")
                .color("secondary")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("citas_periodo")
                .titulo("Citas en Periodo")
                .valor(citasPeriodo)
                .subtitulo("Agendadas en el rango")
                .icono("calendar")
                .ruta("/citas")
                .color("info")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("citas_pendientes")
                .titulo("Citas por Atender")
                .valor(citasPendientes)
                .subtitulo("Programadas / Confirmadas")
                .icono("clock")
                .ruta("/citas")
                .color("warning")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("consultas_periodo")
                .titulo("Consultas Clínicas")
                .valor(consultasPeriodo)
                .subtitulo("En periodo (" + consultasTotales + " totales)")
                .icono("activity")
                .ruta("/consultas")
                .color("success")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("formulas_periodo")
                .titulo("Fórmulas Magistrales")
                .valor(formulasPeriodo)
                .subtitulo("En periodo (" + formulasTotales + " totales)")
                .icono("flask-conical")
                .ruta("/formulas-magistrales")
                .color("primary")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("usuarios_activos")
                .titulo("Usuarios Activos")
                .valor(usuariosActivos)
                .subtitulo("Cuentas de sistema activas")
                .icono("shield-check")
                .ruta("/usuarios")
                .color("secondary")
                .build());

        return kpis;
    }

    private List<DashboardKpiDTO> calcularKpisMedico(Long medicoId, LocalDate start, LocalDate end,
                                                     LocalDateTime startDT, LocalDateTime endDT, LocalDateTime now) {
        LocalDateTime hoyInicio = LocalDate.now().atStartOfDay();
        LocalDateTime hoyFin = LocalDate.now().atTime(LocalTime.MAX);

        long citasHoy = citaRepository.countByMedico_IdMedicoAndFechaHoraBetween(medicoId, hoyInicio, hoyFin);

        long citasPendientes = citaRepository.countByMedico_IdMedicoAndEstado(medicoId, Cita.Estado.PROGRAMADA)
                + citaRepository.countByMedico_IdMedicoAndEstado(medicoId, Cita.Estado.CONFIRMADA);

        long citasPeriodo = citaRepository.countByMedico_IdMedicoAndFechaHoraBetween(medicoId, startDT, endDT);

        long consultasPeriodo = consultaRepository.countByMedico_IdMedicoAndFechaConsultaBetween(medicoId, startDT, endDT);
        long consultasTotales = consultaRepository.countByMedico_IdMedico(medicoId);

        long pacientesAtendidosPeriodo = consultaRepository.countDistinctPacientesAtendidosPorMedicoEnPeriodo(medicoId, startDT, endDT);
        long pacientesAtendidosTotales = consultaRepository.countDistinctPacientesAtendidosPorMedico(medicoId);

        long formulasPeriodo = formulaMagistralRepository.countByConsulta_Medico_IdMedicoAndFechaPreparacionBetween(medicoId, start, end);
        long formulasTotales = formulaMagistralRepository.countByConsulta_Medico_IdMedico(medicoId);

        List<DashboardKpiDTO> kpis = new ArrayList<>();

        kpis.add(DashboardKpiDTO.builder()
                .id("citas_hoy")
                .titulo("Citas para Hoy")
                .valor(citasHoy)
                .subtitulo("Agenda del día")
                .icono("calendar-check")
                .ruta("/citas")
                .color("primary")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("citas_pendientes_medico")
                .titulo("Próximas por Atender")
                .valor(citasPendientes)
                .subtitulo("En tu agenda general")
                .icono("clock")
                .ruta("/citas")
                .color("warning")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("citas_periodo_medico")
                .titulo("Citas en Periodo")
                .valor(citasPeriodo)
                .subtitulo("En el rango seleccionado")
                .icono("calendar")
                .ruta("/citas")
                .color("info")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("consultas_periodo_medico")
                .titulo("Consultas Atendidas")
                .valor(consultasPeriodo)
                .subtitulo("En periodo (" + consultasTotales + " totales)")
                .icono("activity")
                .ruta("/consultas")
                .color("success")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("pacientes_atendidos_medico")
                .titulo("Pacientes Atendidos")
                .valor(pacientesAtendidosPeriodo)
                .subtitulo("Únicos en periodo (" + pacientesAtendidosTotales + " totales)")
                .icono("users")
                .ruta("/pacientes")
                .color("secondary")
                .build());

        kpis.add(DashboardKpiDTO.builder()
                .id("formulas_periodo_medico")
                .titulo("Fórmulas Prescritas")
                .valor(formulasPeriodo)
                .subtitulo("En periodo (" + formulasTotales + " totales)")
                .icono("flask-conical")
                .ruta("/formulas-magistrales")
                .color("primary")
                .build());

        return kpis;
    }

    private List<DashboardEstadoCountDTO> calcularCitasPorEstado(boolean esSoloMedico, Long medicoId, LocalDateTime startDT, LocalDateTime endDT) {
        Map<Cita.Estado, Long> counts = new EnumMap<>(Cita.Estado.class);
        long total = 0;

        for (Cita.Estado estado : Cita.Estado.values()) {
            long count = esSoloMedico
                    ? citaRepository.countByMedico_IdMedicoAndEstadoAndFechaHoraBetween(medicoId, estado, startDT, endDT)
                    : citaRepository.countByEstadoAndFechaHoraBetween(estado, startDT, endDT);
            counts.put(estado, count);
            total += count;
        }

        // Mapeo de colores coherente con la paleta:
        // PROGRAMADA: #4E8C80 (verde salvia)
        // CONFIRMADA: #173F4F (azul petróleo)
        // ATENDIDA:   #2E7D32 (verde éxito)
        // CANCELADA:  #C0392B (rojo discreto)
        Map<Cita.Estado, String> colorMap = Map.of(
                Cita.Estado.PROGRAMADA, "#4E8C80",
                Cita.Estado.CONFIRMADA, "#173F4F",
                Cita.Estado.ATENDIDA, "#2E7D32",
                Cita.Estado.CANCELADA, "#C0392B"
        );

        List<DashboardEstadoCountDTO> lista = new ArrayList<>();
        for (Cita.Estado estado : Cita.Estado.values()) {
            long cant = counts.getOrDefault(estado, 0L);
            double pct = total > 0 ? Math.round((cant * 100.0 / total) * 10.0) / 10.0 : 0.0;
            lista.add(DashboardEstadoCountDTO.builder()
                    .estado(estado.name())
                    .cantidad(cant)
                    .porcentaje(pct)
                    .color(colorMap.getOrDefault(estado, "#263238"))
                    .build());
        }

        return lista;
    }

    private List<DashboardSeriePuntoDTO> calcularTendenciaPeriodo(boolean esSoloMedico, Long medicoId,
                                                                 LocalDate start, LocalDate end,
                                                                 LocalDateTime startDT, LocalDateTime endDT) {
        List<Cita> citas = esSoloMedico
                ? citaRepository.findByMedico_IdMedicoAndFechaHoraBetweenOrderByFechaHoraAsc(medicoId, startDT, endDT)
                : citaRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(startDT, endDT);

        List<Consulta> consultas = esSoloMedico
                ? consultaRepository.findByMedico_IdMedicoAndFechaConsultaBetweenOrderByFechaConsultaAsc(medicoId, startDT, endDT)
                : consultaRepository.findByFechaConsultaBetweenOrderByFechaConsultaAsc(startDT, endDT);

        List<FormulaMagistral> formulas = esSoloMedico
                ? formulaMagistralRepository.findByConsulta_Medico_IdMedicoAndFechaPreparacionBetween(medicoId, start, end)
                : formulaMagistralRepository.findByFechaPreparacionBetween(start, end);

        Map<LocalDate, Long> citasPorDia = citas.stream()
                .collect(Collectors.groupingBy(c -> c.getFechaHora().toLocalDate(), Collectors.counting()));

        Map<LocalDate, Long> consultasPorDia = consultas.stream()
                .collect(Collectors.groupingBy(c -> c.getFechaConsulta().toLocalDate(), Collectors.counting()));

        Map<LocalDate, Long> formulasPorDia = formulas.stream()
                .collect(Collectors.groupingBy(FormulaMagistral::getFechaPreparacion, Collectors.counting()));

        List<DashboardSeriePuntoDTO> serie = new ArrayList<>();
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            serie.add(DashboardSeriePuntoDTO.builder()
                    .etiqueta(curr.format(DIA_MES_FORMATTER))
                    .fecha(curr)
                    .citas(citasPorDia.getOrDefault(curr, 0L))
                    .consultas(consultasPorDia.getOrDefault(curr, 0L))
                    .formulas(formulasPorDia.getOrDefault(curr, 0L))
                    .build());
            curr = curr.plusDays(1);
        }

        return serie;
    }

    private List<DashboardEstadoCountDTO> calcularFormulasPorEstado(boolean esSoloMedico, Long medicoId, LocalDate start, LocalDate end) {
        List<FormulaMagistral> formulas = esSoloMedico
                ? formulaMagistralRepository.findByConsulta_Medico_IdMedicoAndFechaPreparacionBetween(medicoId, start, end)
                : formulaMagistralRepository.findByFechaPreparacionBetween(start, end);

        if (formulas.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Long> agrupado = formulas.stream()
                .collect(Collectors.groupingBy(f -> f.getEstado() != null ? f.getEstado().trim().toUpperCase() : "DESCONOCIDO", Collectors.counting()));

        long total = formulas.size();
        Map<String, String> colorMap = Map.of(
                "ACTIVO", "#4E8C80",
                "PREPARADA", "#173F4F",
                "DISPENSADA", "#2E7D32",
                "BORRADOR", "#8E44AD",
                "RECETADA", "#2980B9",
                "ANULADA", "#C0392B"
        );

        List<DashboardEstadoCountDTO> resultado = new ArrayList<>();
        for (Map.Entry<String, Long> entry : agrupado.entrySet()) {
            long cant = entry.getValue();
            double pct = total > 0 ? Math.round((cant * 100.0 / total) * 10.0) / 10.0 : 0.0;
            resultado.add(DashboardEstadoCountDTO.builder()
                    .estado(entry.getKey())
                    .cantidad(cant)
                    .porcentaje(pct)
                    .color(colorMap.getOrDefault(entry.getKey(), "#263238"))
                    .build());
        }

        return resultado;
    }

    private List<DashboardProximaCitaDTO> obtenerProximasCitas(boolean esSoloMedico, Long medicoId, LocalDateTime now) {
        List<Cita.Estado> estadosPendientes = List.of(Cita.Estado.PROGRAMADA, Cita.Estado.CONFIRMADA);
        List<Cita> proximas = esSoloMedico
                ? citaRepository.findTop10ByMedico_IdMedicoAndFechaHoraGreaterThanEqualAndEstadoInOrderByFechaHoraAsc(medicoId, now, estadosPendientes)
                : citaRepository.findTop10ByFechaHoraGreaterThanEqualAndEstadoInOrderByFechaHoraAsc(now, estadosPendientes);

        return proximas.stream().map(c -> {
            Paciente p = c.getPaciente();
            Medico m = c.getMedico();
            return DashboardProximaCitaDTO.builder()
                    .idCita(c.getIdCita())
                    .fechaHora(c.getFechaHora())
                    .idPaciente(p != null ? p.getIdPaciente() : null)
                    .nombrePaciente(p != null ? (p.getNombres() + " " + p.getApellidos()).trim() : "Sin asignar")
                    .dniPaciente(p != null ? p.getDni() : null)
                    .idMedico(m != null ? m.getIdMedico() : null)
                    .nombreMedico(m != null ? (m.getNombres() + " " + m.getApellidos()).trim() : "Sin asignar")
                    .especialidadMedico(m != null ? m.getEspecialidad() : null)
                    .motivo(c.getMotivo())
                    .estado(c.getEstado().name())
                    .build();
        }).collect(Collectors.toList());
    }

    private boolean esSoloMedico(Authentication auth) {
        if (auth == null) return false;
        boolean esAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));
        if (esAdmin) return false;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_MEDICO") || a.equals("MEDICO"));
    }
}
