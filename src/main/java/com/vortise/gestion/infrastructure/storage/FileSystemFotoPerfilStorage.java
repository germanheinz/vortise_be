package com.vortise.gestion.infrastructure.storage;

import com.vortise.gestion.application.port.FotoPerfilStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Temporary local storage until a storage service exists; ephemeral on hosts like Render. */
@Component
public class FileSystemFotoPerfilStorage implements FotoPerfilStorage {
    private final Path base;

    public FileSystemFotoPerfilStorage(@Value("${app.uploads.dir:./data/uploads/avatars}") String directorio) {
        this.base = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public void guardar(Long usuarioId, byte[] datos, TipoImagen tipo) {
        try {
            Files.createDirectories(base);
            Path destino = archivo(usuarioId, tipo);
            Path temporal = Files.createTempFile(base, "upload-", ".tmp");
            try {
                Files.write(temporal, datos);
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporal);
            }
            for (TipoImagen otro : TipoImagen.values()) {
                if (otro != tipo) Files.deleteIfExists(archivo(usuarioId, otro));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la foto de perfil", ex);
        }
    }

    @Override
    public Optional<FotoPerfil> cargar(Long usuarioId) {
        return Arrays.stream(TipoImagen.values())
            .filter(tipo -> Files.isRegularFile(archivo(usuarioId, tipo)))
            .findFirst()
            .map(tipo -> {
                try {
                    return new FotoPerfil(Files.readAllBytes(archivo(usuarioId, tipo)), tipo);
                } catch (IOException ex) {
                    throw new UncheckedIOException("No se pudo leer la foto de perfil", ex);
                }
            });
    }

    @Override
    public boolean existe(Long usuarioId) {
        return Arrays.stream(TipoImagen.values()).anyMatch(tipo -> Files.isRegularFile(archivo(usuarioId, tipo)));
    }

    @Override
    public void eliminar(Long usuarioId) {
        try {
            for (TipoImagen tipo : TipoImagen.values()) Files.deleteIfExists(archivo(usuarioId, tipo));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo eliminar la foto de perfil", ex);
        }
    }

    private Path archivo(Long usuarioId, TipoImagen tipo) {
        if (usuarioId == null || usuarioId <= 0) throw new IllegalArgumentException("Usuario inválido");
        Path path = base.resolve(usuarioId + "." + tipo.extension()).normalize();
        if (!path.getParent().equals(base)) throw new IllegalArgumentException("Ruta de archivo inválida");
        return path;
    }
}
