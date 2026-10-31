package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ProyectoCreateDto;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateProyectoSubcontratistaTest {
    private static final Long EMPRESA_ID = 7L;
    private ProyectoRepository proyectos;
    private CatalogoEmpresaRepository catalogos;
    private Empresa empresa;
    private CreateProyectoUseCase useCase;

    @BeforeEach
    void setUp() {
        proyectos = mock(ProyectoRepository.class);
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        catalogos = mock(CatalogoEmpresaRepository.class);
        empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(EMPRESA_ID);
        when(empresas.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        CurrentTenantService tenant = mock(CurrentTenantService.class);
        AplicarCambiosEmpresaObraUseCase cambiosEmpresa = mock(AplicarCambiosEmpresaObraUseCase.class);
        when(cambiosEmpresa.execute(any(), any(), any())).thenReturn(List.of());
        useCase = new CreateProyectoUseCase(proyectos, empresas, catalogos, tenant, cambiosEmpresa);
    }

    @Test
    void rejectsSelectedSubcontractorOwnedByAnotherCompany() {
        Empresa otherEmpresa = mock(Empresa.class);
        when(otherEmpresa.getId()).thenReturn(99L);
        CatalogoEmpresa foreign = subcontractor(otherEmpresa, "SUBCONTRATISTA");
        when(catalogos.findById(81L)).thenReturn(Optional.of(foreign));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request(List.of(81L))));
        verify(proyectos, never()).save(any(Proyecto.class));
    }

    @Test
    void rejectsSelectedCatalogEntryThatIsNotASubcontractor() {
        CatalogoEmpresa wrongType = subcontractor(empresa, "CLIENTE");
        when(catalogos.findById(82L)).thenReturn(Optional.of(wrongType));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request(List.of(82L))));
        verify(proyectos, never()).save(any(Proyecto.class));
    }

    @Test
    void rejectsDuplicateSelectedIdsBeforeLookingUpCatalogEntries() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request(List.of(83L, 83L))));

        verifyNoInteractions(catalogos);
        verify(proyectos, never()).save(any(Proyecto.class));
    }

    private CatalogoEmpresa subcontractor(Empresa owner, String type) {
        CatalogoEmpresa item = mock(CatalogoEmpresa.class);
        when(item.getEmpresa()).thenReturn(owner);
        when(item.getTipo()).thenReturn(type);
        return item;
    }

    private ProyectoCreateDto request(List<Long> subcontractorIds) {
        return new ProyectoCreateDto("Obra de prueba", null, null, "Dirección", "OB-1",
            null, null, null, null, null, EMPRESA_ID, null, null, null, null, null, subcontractorIds);
    }
}