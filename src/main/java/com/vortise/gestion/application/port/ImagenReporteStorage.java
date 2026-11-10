package com.vortise.gestion.application.port;

import com.vortise.gestion.application.port.FotoPerfilStorage.TipoImagen;

/** Hosting for the images attached to reports. */
public interface ImagenReporteStorage {
    boolean disponible();

    Subida subir(Long proyectoId, byte[] datos, TipoImagen tipo);

    void eliminar(String publicId);

    record Subida(String publicId, String url, int ancho, int alto, long tamanoBytes) { }
}
