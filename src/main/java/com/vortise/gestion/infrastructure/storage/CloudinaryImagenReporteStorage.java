package com.vortise.gestion.infrastructure.storage;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.port.FotoPerfilStorage.TipoImagen;
import com.vortise.gestion.application.port.ImagenReporteStorage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cloudinary adapter using its signed REST API, so no extra dependency is needed.
 * Configure it with {@code CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME}; without it uploads are unavailable.
 */
@Component
public class CloudinaryImagenReporteStorage implements ImagenReporteStorage {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryImagenReporteStorage.class);
    private static final String API = "https://api.cloudinary.com/v1_1/";
    private static final String DELIVERY_PREFIX = "https://res.cloudinary.com/";

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String carpeta;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public CloudinaryImagenReporteStorage(@Value("${app.cloudinary.url:}") String cloudinaryUrl,
            @Value("${app.cloudinary.folder:gestion/reportes}") String carpeta, ObjectMapper mapper) {
        String[] credentials = parse(cloudinaryUrl);
        this.cloudName = credentials[0]; this.apiKey = credentials[1]; this.apiSecret = credentials[2];
        this.carpeta = carpeta == null || carpeta.isBlank() ? "gestion/reportes" : carpeta.trim().replaceAll("^/+|/+$", "");
        this.mapper = mapper;
    }

    /** Returns {cloud, key, secret}, all null when the URL is missing or malformed (the value is never logged). */
    static String[] parse(String cloudinaryUrl) {
        String[] empty = new String[3];
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) return empty;
        try {
            URI uri = URI.create(cloudinaryUrl.trim());
            String info = uri.getUserInfo();
            if (!"cloudinary".equals(uri.getScheme()) || info == null || uri.getHost() == null || !info.contains(":")) return empty;
            int split = info.indexOf(':');
            String key = info.substring(0, split), secret = info.substring(split + 1);
            return key.isBlank() || secret.isBlank() ? empty : new String[] {uri.getHost(), key, secret};
        } catch (IllegalArgumentException ex) {
            return empty;
        }
    }

    @Override
    public boolean disponible() {
        return cloudName != null && apiKey != null && apiSecret != null;
    }

    @Override
    public Subida subir(Long proyectoId, byte[] datos, TipoImagen tipo) {
        requireAvailable();
        String publicId = carpeta + "/" + proyectoId + "/" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        String boundary = "----gestion" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = multipart(boundary, publicId, timestamp, datos, tipo);
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + cloudName + "/image/upload"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        JsonNode response = send(request, "subir la imagen");
        String url = text(response, "secure_url");
        if (url == null || !url.startsWith(DELIVERY_PREFIX) || text(response, "public_id") == null) {
            log.warn("Cloudinary returned an unexpected upload response");
            throw new FeatureUnavailableException("No se pudo subir la imagen. Intentá nuevamente.");
        }
        return new Subida(text(response, "public_id"), url, number(response, "width"), number(response, "height"), number(response, "bytes"));
    }

    @Override
    public void eliminar(String publicId) {
        requireAvailable();
        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1Hex("public_id=" + publicId + "&timestamp=" + timestamp + apiSecret);
        String form = "public_id=" + enc(publicId) + "&timestamp=" + timestamp + "&api_key=" + enc(apiKey) + "&signature=" + signature;
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + cloudName + "/image/destroy"))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        send(request, "eliminar la imagen");
    }

    private void requireAvailable() {
        if (!disponible()) throw new FeatureUnavailableException("La subida de imágenes no está configurada.");
    }

    private JsonNode send(HttpRequest request, String action) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                log.warn("Cloudinary request to {} failed with status {}", action, response.statusCode());
                throw new FeatureUnavailableException("No se pudo " + action + ". Intentá nuevamente.");
            }
            return mapper.readTree(response.body());
        } catch (java.io.IOException ex) {
            log.warn("Cloudinary request to {} failed: {}", action, ex.getClass().getSimpleName());
            throw new FeatureUnavailableException("No se pudo " + action + ". Intentá nuevamente.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FeatureUnavailableException("No se pudo " + action + ". Intentá nuevamente.");
        }
    }

    private byte[] multipart(String boundary, String publicId, long timestamp, byte[] datos, TipoImagen tipo) {
        String signature = sha1Hex("public_id=" + publicId + "&timestamp=" + timestamp + apiSecret);
        ByteArrayOutputStream out = new ByteArrayOutputStream(datos.length + 1024);
        field(out, boundary, "api_key", apiKey);
        field(out, boundary, "timestamp", String.valueOf(timestamp));
        field(out, boundary, "public_id", publicId);
        field(out, boundary, "signature", signature);
        write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"imagen." + tipo.extension()
            + "\"\r\nContent-Type: " + tipo.contentType() + "\r\n\r\n");
        out.writeBytes(datos);
        write(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void field(ByteArrayOutputStream out, String boundary, String name, String value) {
        write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n");
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    static String sha1Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static int number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? 0 : value.asInt();
    }
}
