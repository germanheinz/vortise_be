package com.vortise.gestion.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.port.FotoPerfilStorage.TipoImagen;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CloudinaryImagenReporteStorageTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void parsesTheCloudinaryUrlAndRejectsMalformedValues() {
        assertArrayEquals(new String[] {"demo", "key123", "s3cr3t"}, CloudinaryImagenReporteStorage.parse("cloudinary://key123:s3cr3t@demo"));
        for (String bad : new String[] {null, "", "  ", "https://key:secret@demo", "cloudinary://onlykey@demo", "cloudinary://:secret@demo", "cloudinary://key:@demo", "not a url"}) {
            assertArrayEquals(new String[3], CloudinaryImagenReporteStorage.parse(bad), String.valueOf(bad));
        }
    }

    @Test void isUnavailableWithoutCredentialsAndRefusesToUpload() {
        var storage = new CloudinaryImagenReporteStorage("", "folder", mapper);
        assertFalse(storage.disponible());
        assertThrows(FeatureUnavailableException.class, () -> storage.subir(1L, new byte[] {1}, TipoImagen.PNG));
        assertThrows(FeatureUnavailableException.class, () -> storage.eliminar("a/b"));
        assertTrue(new CloudinaryImagenReporteStorage("cloudinary://k:s@demo", "folder", mapper).disponible());
    }

    @Test void signsWithSha1() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", CloudinaryImagenReporteStorage.sha1Hex("abc"));
    }
}
