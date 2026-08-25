/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.multipart.MultipartFile
 */
package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.infrastructure.excel.ExcelImportService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/import"})
public class ImportController {
    private final ExcelImportService excelImportService;

    @PostMapping(value={"/excel"})
    public ResponseEntity<Map<String, String>> importExcel(@RequestParam(value="file") MultipartFile file, @RequestParam(value="nombre", defaultValue="Proyecto Importado") String nombre) {
        try {
            String resultado = this.excelImportService.importar(file, nombre);
            return ResponseEntity.ok(Map.of("message", resultado));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public ImportController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }
}
