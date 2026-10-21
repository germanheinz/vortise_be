package com.vortise.gestion.application.port;

import java.util.Arrays;
import java.util.Optional;

public interface FotoPerfilStorage {
    void guardar(Long usuarioId, byte[] datos, TipoImagen tipo);

    Optional<FotoPerfil> cargar(Long usuarioId);

    boolean existe(Long usuarioId);

    void eliminar(Long usuarioId);

    record FotoPerfil(byte[] datos, TipoImagen tipo) { }

    enum TipoImagen {
        JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG("image/png", "png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
        WEBP("image/webp", "webp", null);

        private final String contentType;
        private final String extension;
        private final byte[] firma;

        TipoImagen(String contentType, String extension, byte[] firma) {
            this.contentType = contentType; this.extension = extension; this.firma = firma;
        }

        public String contentType() { return contentType; }
        public String extension() { return extension; }

        /** Detects the type from file content, never from the client-provided name or header. */
        public static Optional<TipoImagen> detectar(byte[] datos) {
            if (datos == null) return Optional.empty();
            if (datos.length >= 12 && ascii(datos, 0, "RIFF") && ascii(datos, 8, "WEBP")) return Optional.of(WEBP);
            return Arrays.stream(values()).filter(tipo -> tipo.firma != null && empiezaCon(datos, tipo.firma)).findFirst();
        }

        private static boolean empiezaCon(byte[] datos, byte[] prefijo) {
            if (datos.length < prefijo.length) return false;
            for (int i = 0; i < prefijo.length; i++) if (datos[i] != prefijo[i]) return false;
            return true;
        }

        private static boolean ascii(byte[] datos, int offset, String texto) {
            for (int i = 0; i < texto.length(); i++) if (datos[offset + i] != (byte) texto.charAt(i)) return false;
            return true;
        }
    }
}
