package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaCreateDto;
import com.vortise.gestion.application.dto.EmpresaDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmpresaTenantScopeTest {
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final CurrentTenantService tenant = mock(CurrentTenantService.class);

    @Test
    void superAdminListsAllCompanies() {
        when(tenant.isSuperAdmin()).thenReturn(true);
        when(empresas.findAll()).thenReturn(List.of(company(1L, "A"), company(2L, "B"), company(3L, "C")));
        assertEquals(List.of("A", "B", "C"), names(new GetEmpresasUseCase(empresas, tenant).execute()));
    }

    @Test
    void adminListsOwnAndCreatedCompaniesOnly() {
        when(tenant.isAdministrator()).thenReturn(true);
        when(tenant.empresaId()).thenReturn(1L);
        when(tenant.usuarioId()).thenReturn(7L);
        Empresa own = company(1L, "Own");
        when(empresas.findById(1L)).thenReturn(Optional.of(own));
        when(empresas.findByCreadoPorUsuarioId(7L)).thenReturn(List.of(own, company(4L, "Created")));
        assertEquals(List.of("Own", "Created"), names(new GetEmpresasUseCase(empresas, tenant).execute()));
        verify(empresas, never()).findAll();
    }

    @Test
    void nonAdminListsOwnCompanyOnly() {
        when(tenant.empresaId()).thenReturn(1L);
        when(empresas.findById(1L)).thenReturn(Optional.of(company(1L, "Own")));
        assertEquals(List.of("Own"), names(new GetEmpresasUseCase(empresas, tenant).execute()));
        verify(empresas, never()).findByCreadoPorUsuarioId(any());
        verify(empresas, never()).findAll();
    }

    @Test
    void createdCompanyRecordsCreator() {
        when(tenant.usuarioId()).thenReturn(7L);
        when(empresas.findByNombreIgnoreCase("Nueva")).thenReturn(Optional.empty());
        when(empresas.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        new SaveEmpresaUseCase(empresas, tenant, mock(InicializarEmpresaUseCase.class)).create(new EmpresaCreateDto("Nueva", null));
        verify(empresas).save(org.mockito.ArgumentMatchers.argThat(empresa -> Long.valueOf(7L).equals(empresa.getCreadoPorUsuarioId())));
    }

    private static Empresa company(Long id, String name) {
        Empresa empresa = new Empresa(name, null);
        ReflectionTestUtils.setField(empresa, "id", id);
        return empresa;
    }

    private static List<String> names(List<EmpresaDto> dtos) {
        return dtos.stream().map(EmpresaDto::nombre).toList();
    }
}
