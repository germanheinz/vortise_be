package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.FotoPerfilStorage.TipoImagen;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.security.CurrentTenantService;
import com.vortise.gestion.infrastructure.storage.FileSystemFotoPerfilStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class PerfilFotoTest {
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 4, 5};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 9};

    @TempDir Path dir;
    private FileSystemFotoPerfilStorage storage;
    private final UsuarioEmpresaRepository usuarios = mock(UsuarioEmpresaRepository.class);
    private final CurrentTenantService tenant = mock(CurrentTenantService.class);
    private PerfilUseCase useCase;
    private final UsuarioEmpresa usuario = new UsuarioEmpresa(new Empresa("E", null), "u@example.invalid", "U", "h", RolEmpresa.CONSULTA);

    @BeforeEach
    void setUp() {
        storage = new FileSystemFotoPerfilStorage(dir.toString());
        useCase = new PerfilUseCase(usuarios, tenant, mock(SuscripcionConsultaService.class), storage);
        ReflectionTestUtils.setField(usuario, "id", 7L);
        when(tenant.usuarioId()).thenReturn(7L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
    }

    @Test
    void detectsTypeFromContentOnly() {
        assertEquals(Optional.of(TipoImagen.PNG), TipoImagen.detectar(PNG));
        assertEquals(Optional.of(TipoImagen.JPEG), TipoImagen.detectar(JPEG));
        assertEquals(Optional.of(TipoImagen.WEBP), TipoImagen.detectar(WEBP));
        assertEquals(Optional.empty(), TipoImagen.detectar("<svg onload=alert(1)>".getBytes()));
        assertEquals(Optional.empty(), TipoImagen.detectar(new byte[] {(byte) 0xFF}));
    }

    @Test
    void uploadedPhotoIsStoredForCurrentUserAndRetrievable() throws Exception {
        useCase.subirFoto(PNG);
        assertTrue(Files.isRegularFile(dir.resolve("7.png")));
        var foto = useCase.obtenerFoto().orElseThrow();
        assertEquals(TipoImagen.PNG, foto.tipo());
        assertArrayEquals(PNG, foto.datos());
        assertTrue(storage.existe(7L));
    }

    @Test
    void replacingWithAnotherFormatRemovesThePreviousFile() {
        useCase.subirFoto(PNG);
        useCase.subirFoto(JPEG);
        assertFalse(Files.exists(dir.resolve("7.png")));
        assertEquals(TipoImagen.JPEG, useCase.obtenerFoto().orElseThrow().tipo());
    }

    @Test
    void deleteRemovesPhoto() {
        useCase.subirFoto(WEBP);
        useCase.eliminarFoto();
        assertTrue(useCase.obtenerFoto().isEmpty());
        assertFalse(storage.existe(7L));
    }

    @Test
    void rejectsEmptyOversizedAndNonImageContent() {
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirFoto(new byte[0]));
        byte[] grande = Arrays.copyOf(PNG, PerfilUseCase.MAX_FOTO_BYTES + 1);
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirFoto(grande));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.subirFoto("GIF89a....".getBytes()));
        assertFalse(storage.existe(7L));
    }

    @Test
    void inactiveUserCannotUpload() {
        usuario.setActivo(false);
        assertThrows(AccessDeniedException.class, () -> useCase.subirFoto(PNG));
    }

    @Test
    void storageRejectsInvalidUserIds() {
        assertThrows(IllegalArgumentException.class, () -> storage.existe(0L));
        assertThrows(IllegalArgumentException.class, () -> storage.existe(-1L));
    }
}
