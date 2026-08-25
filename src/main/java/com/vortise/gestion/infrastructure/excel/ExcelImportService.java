/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.poi.ss.usermodel.Cell
 *  org.apache.poi.ss.usermodel.CellType
 *  org.apache.poi.ss.usermodel.Row
 *  org.apache.poi.ss.usermodel.Sheet
 *  org.apache.poi.xssf.usermodel.XSSFWorkbook
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.web.multipart.MultipartFile
 */
package com.vortise.gestion.infrastructure.excel;

import com.vortise.gestion.domain.model.Entrega;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.Rendimiento;
import com.vortise.gestion.domain.model.StatusTarea;
import com.vortise.gestion.domain.model.Tarea;
import com.vortise.gestion.domain.repository.EntregaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RendimientoRepository;
import com.vortise.gestion.domain.repository.TareaRepository;
import java.io.IOException;
import java.util.HashMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelImportService {
    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);
    private final ProyectoRepository proyectoRepository;
    private final EntregaRepository entregaRepository;
    private final TareaRepository tareaRepository;
    private final RendimientoRepository rendimientoRepository;

    public String importar(MultipartFile file, String nombreProyecto) throws IOException {
        int tareasImportadas = 0;
        int rendimientosImportados = 0;
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream());){
            Sheet precioSheet;
            Proyecto proyecto = this.proyectoRepository.save(Proyecto.builder().nombre(nombreProyecto).descripcion("Importado desde Excel").build());
            Sheet rendSheet = wb.getSheet("RENDIMIENTOS");
            if (rendSheet != null) {
                for (int i = 1; i <= rendSheet.getLastRowNum(); ++i) {
                    Row row = rendSheet.getRow(i);
                    if (row == null) continue;
                    String fase = this.getCellString(row, 0);
                    String etapa = this.getCellString(row, 1);
                    String sistema = this.getCellString(row, 2);
                    String rol = this.getCellString(row, 3);
                    Double hh = this.getCellDouble(row, 4);
                    Double precio = this.getCellDouble(row, 5);
                    if (fase == null || hh == null || precio == null) continue;
                    this.rendimientoRepository.save(Rendimiento.builder().fase(fase).etapa(etapa).sistema(sistema).rol(rol).hhPorM2(hh).precioPorHh(precio).build());
                    ++rendimientosImportados;
                }
            }
            if ((precioSheet = wb.getSheet("01-PRECIO DETALLADO-AVANCE DOC.")) == null) {
                precioSheet = wb.getSheet("PRECIO DETALLADO");
            }
            if (precioSheet != null) {
                HashMap<String, Entrega> entregasCache = new HashMap<String, Entrega>();
                String currentEntrega = "ENTREGA 1";
                for (int i = 1; i <= precioSheet.getLastRowNum(); ++i) {
                    Row row = precioSheet.getRow(i);
                    if (row == null) continue;
                    String col0 = this.getCellString(row, 0);
                    if (col0 != null && col0.startsWith("ENTREGA")) {
                        currentEntrega = col0;
                        continue;
                    }
                    String nombre = this.getCellString(row, 2);
                    if (nombre == null || nombre.isBlank()) continue;
                    Entrega entrega = entregasCache.computeIfAbsent(currentEntrega, k -> {
                        int orden = entregasCache.size() + 1;
                        return this.entregaRepository.save(Entrega.builder().proyecto(proyecto).nombre((String)k).orden(orden).build());
                    });
                    String statusStr = this.getCellString(row, 7);
                    StatusTarea status = this.parseStatus(statusStr);
                    Double avance = this.getCellDouble(row, 8);
                    Double costo = this.getCellDouble(row, 9);
                    this.tareaRepository.save(Tarea.builder().entrega(entrega).fase(this.getCellString(row, 0)).etapa(this.getCellString(row, 1)).sistema(this.getCellString(row, 3)).nombre(nombre).status(status).avancePct(avance != null ? avance.intValue() : 0).costo(costo).build());
                    ++tareasImportadas;
                }
            }
        }
        return String.format("Importaci\u00f3n completada: %d tareas, %d rendimientos", tareasImportadas, rendimientosImportados);
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int)cell.getNumericCellValue());
            default -> null;
        };
    }

    private Double getCellDouble(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue());
                }
                catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private StatusTarea parseStatus(String s) {
        if (s == null) {
            return StatusTarea.PENDIENTE;
        }
        return switch (s.toUpperCase().trim()) {
            case "EJECUTADO", "EJECUTADA" -> StatusTarea.EJECUTADO;
            case "EN PROCESO", "EN_PROCESO" -> StatusTarea.EN_PROCESO;
            default -> StatusTarea.PENDIENTE;
        };
    }

    public ExcelImportService(ProyectoRepository proyectoRepository, EntregaRepository entregaRepository, TareaRepository tareaRepository, RendimientoRepository rendimientoRepository) {
        this.proyectoRepository = proyectoRepository;
        this.entregaRepository = entregaRepository;
        this.tareaRepository = tareaRepository;
        this.rendimientoRepository = rendimientoRepository;
    }
}
