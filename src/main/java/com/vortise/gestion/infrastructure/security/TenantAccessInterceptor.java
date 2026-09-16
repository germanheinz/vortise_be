package com.vortise.gestion.infrastructure.security;

import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.repository.RegistroHorasRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.model.Proyecto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;

@Component
public class TenantAccessInterceptor implements HandlerInterceptor {
    private static final Pattern COMPANY_ROUTE = Pattern.compile("/api/empresas/(\\d+)(?:/.*)?");
    private static final Pattern PROJECT_ROUTE = Pattern.compile("/api/proyectos/(\\d+)(?:/.*)?");
    private static final Pattern RESTRICTION_ROUTE = Pattern.compile("/api/restricciones/(\\d+)(?:/.*)?");
    private static final Pattern HOURS_ROUTE = Pattern.compile("/api/registro-horas/(\\d+)");
    private static final Pattern TASK_ROUTE = Pattern.compile("/api/tareas/(\\d+)/status");
    private final CurrentTenantService currentTenant;
    private final ProyectoRepository proyectoRepository;
    private final RestriccionObraRepository restriccionRepository;
    private final RegistroHorasRepository registroHorasRepository;
    private final ObraTareaRepository obraTareaRepository;

    public TenantAccessInterceptor(CurrentTenantService currentTenant, ProyectoRepository proyectoRepository,
            RestriccionObraRepository restriccionRepository, RegistroHorasRepository registroHorasRepository,
            ObraTareaRepository obraTareaRepository) {
            this.currentTenant = currentTenant;
        this.proyectoRepository = proyectoRepository;
        this.restriccionRepository = restriccionRepository;
        this.registroHorasRepository = registroHorasRepository;
        this.obraTareaRepository = obraTareaRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String path = request.getRequestURI();
        Matcher company = COMPANY_ROUTE.matcher(path);
        if (company.matches()) {
            currentTenant.requireEmpresa(Long.valueOf(company.group(1)));
            return true;
        }

        Matcher project = PROJECT_ROUTE.matcher(path);
        if (project.matches()) {
            Long projectId = Long.valueOf(project.group(1));
            Long companyId = proyectoRepository.findById(projectId)
                .map(item -> item.getEmpresaRelacionada() == null ? null : item.getEmpresaRelacionada().getId())
                .orElseThrow(() -> new AccessDeniedException("Obra no encontrada o sin acceso"));
            if (companyId == null) throw new AccessDeniedException("La obra no tiene empresa asignada");
            currentTenant.requireEmpresa(companyId);
        }
        Matcher restriction = RESTRICTION_ROUTE.matcher(path);
        if (restriction.matches()) {
            restriccionRepository.findById(Long.valueOf(restriction.group(1)))
                .ifPresentOrElse(item -> requireOwnedProject(item.getProyecto()), () -> { throw new AccessDeniedException("Restricción no encontrada o sin acceso"); });
        }
        Matcher hours = HOURS_ROUTE.matcher(path);
        if (hours.matches()) {
            registroHorasRepository.findById(Long.valueOf(hours.group(1)))
                .ifPresentOrElse(item -> requireOwnedProject(item.getProyecto()), () -> { throw new AccessDeniedException("Registro no encontrado o sin acceso"); });
        }
        Matcher task = TASK_ROUTE.matcher(path);
        if (task.matches()) {
            obraTareaRepository.findById(Long.valueOf(task.group(1)))
                .ifPresentOrElse(item -> requireOwnedProject(item.getProyecto()), () -> { throw new AccessDeniedException("Tarea no encontrada o sin acceso"); });
        }
        return true;
    }

    private void requireOwnedProject(Proyecto project) {
        if (project == null || project.getEmpresaRelacionada() == null) {
            throw new AccessDeniedException("Recurso sin empresa asignada");
        }
        currentTenant.requireEmpresa(project.getEmpresaRelacionada().getId());
    }
}
