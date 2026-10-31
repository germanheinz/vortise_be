package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.CategoriaManoObraCostoDto;
import com.vortise.gestion.application.dto.SubcontratistaObraDto;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.CatalogoEmpresa;
import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AplicarCambiosEmpresaObraUseCaseTest {
    private static final Long EMPRESA_ID = 7L;
    private CurrentIdentity identity;
    private EmpresaRepository empresas;
    private CatalogoEmpresaRepository catalogos;
    private CategoriaManoObraEmpresaRepository categorias;
    private Empresa empresa;
    private AplicarCambiosEmpresaObraUseCase useCase;

    @BeforeEach
    void setUp() {
        identity = mock(CurrentIdentity.class);
        empresas = mock(EmpresaRepository.class);
        catalogos = mock(CatalogoEmpresaRepository.class);
        categorias = mock(CategoriaManoObraEmpresaRepository.class);
        empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(EMPRESA_ID);
        when(identity.isAdministrator()).thenReturn(true);
        when(empresas.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        useCase = new AplicarCambiosEmpresaObraUseCase(identity, empresas, catalogos, categorias);
    }

    @Test
    void appliesOnlyTheRequestedCompanyCategoryCost() {
        Empresa owner = mock(Empresa.class);
        when(owner.getId()).thenReturn(EMPRESA_ID);
        CategoriaManoObraEmpresa category = mock(CategoriaManoObraEmpresa.class);
        when(category.getEmpresa()).thenReturn(owner);
        when(categorias.findById(3L)).thenReturn(Optional.of(category));

        assertEquals(List.of(), useCase.execute(EMPRESA_ID, List.of(new CategoriaManoObraCostoDto(3L, 27.5)), null));

        verify(category).setCostoHora(27.5);
        verify(categorias).save(category);
    }

    @Test
    void refusesConfigurationChangesFromNonAdministrators() {
        when(identity.isAdministrator()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> useCase.execute(EMPRESA_ID, null,
            List.of(new SubcontratistaObraDto(null, "Proveedor Uno"))));

        verifyNoInteractions(catalogos);
        verifyNoInteractions(categorias);
    }

    @Test
    void createsCompanyContractorAndReturnsItsIdForProjectSelection() {
        CatalogoEmpresa created = mock(CatalogoEmpresa.class);
        when(created.getId()).thenReturn(15L);
        when(catalogos.findByEmpresaIdAndTipo(EMPRESA_ID, "SUBCONTRATISTA")).thenReturn(List.of());
        when(catalogos.save(any(CatalogoEmpresa.class))).thenReturn(created);

        assertEquals(List.of(15L), useCase.execute(EMPRESA_ID, null,
            List.of(new SubcontratistaObraDto(null, "Proveedor Uno"))));

        verify(catalogos).save(any(CatalogoEmpresa.class));
    }

    @Test
    void reusesExistingContractorNameWithoutCreatingADuplicate() {
        CatalogoEmpresa existing = mock(CatalogoEmpresa.class);
        when(existing.getId()).thenReturn(15L);
        when(existing.getNombre()).thenReturn("Proveedor Uno");
        when(catalogos.findByEmpresaIdAndTipo(EMPRESA_ID, "SUBCONTRATISTA")).thenReturn(List.of(existing));

        assertEquals(List.of(15L), useCase.execute(EMPRESA_ID, null,
            List.of(new SubcontratistaObraDto(null, " proveedor uno "))));

        verify(catalogos, never()).save(any(CatalogoEmpresa.class));
    }

    @Test
    void refusesToRenameContractorOwnedByAnotherCompany() {
        Empresa otherCompany = mock(Empresa.class);
        when(otherCompany.getId()).thenReturn(99L);
        CatalogoEmpresa foreign = mock(CatalogoEmpresa.class);
        when(foreign.getId()).thenReturn(15L);
        when(foreign.getEmpresa()).thenReturn(otherCompany);
        when(foreign.getTipo()).thenReturn("SUBCONTRATISTA");
        when(catalogos.findByEmpresaIdAndTipo(EMPRESA_ID, "SUBCONTRATISTA")).thenReturn(List.of());
        when(catalogos.findById(15L)).thenReturn(Optional.of(foreign));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMPRESA_ID, null,
            List.of(new SubcontratistaObraDto(15L, "Nuevo nombre"))));

        verify(catalogos, never()).save(foreign);
    }
}
