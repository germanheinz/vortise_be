package com.vortise.gestion.infrastructure.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real parser, synthetic in-memory workbooks only. No Spring, repositories or database. */
class ExcelParserSecurityTest {
    @Test
    void readsValidWorkbookWithTheParserUsedByExcelImport() throws Exception {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(validWorkbook()))) {
            var row = workbook.getSheet("RENDIMIENTOS").getRow(1);
            assertEquals("Hormigón", row.getCell(0).getStringCellValue());
            assertEquals(2.5, row.getCell(4).getNumericCellValue());
        }
    }

    @Test
    void rejectsDuplicateOoxmlZipEntriesInsteadOfChoosingAmbiguousContent() throws Exception {
        var bytes = new ByteArrayOutputStream();
        boolean duplicated = false;
        // Commons ZIP writer permits duplicate names, unlike java.util.zip.ZipOutputStream.
        try (var input = new ZipInputStream(new ByteArrayInputStream(validWorkbook()));
                var output = new ZipArchiveOutputStream(bytes)) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] content = input.readAllBytes();
                writeEntry(output, entry.getName(), content);
                if (entry.getName().equals("xl/workbook.xml")) {
                    writeEntry(output, entry.getName(), content);
                    duplicated = true;
                }
            }
        }
        assertTrue(duplicated, "Fixture must contain the duplicated workbook entry");
        var failure = assertThrows(org.apache.poi.openxml4j.opc.internal.InvalidZipException.class, () -> {
            try (var ignored = new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))) {
                fail("Ambiguous workbook must not be accepted");
            }
        });
        assertTrue(failure.getMessage().contains("more than 1 entry"));
        assertTrue(failure.getMessage().contains("xl/workbook.xml"));
    }

    private static byte[] validWorkbook() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("RENDIMIENTOS");
            sheet.createRow(0).createCell(0).setCellValue("Fase");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Hormigón");
            row.createCell(4).setCellValue(2.5);
            workbook.write(bytes);
        }
        return bytes.toByteArray();
    }

    private static void writeEntry(ZipArchiveOutputStream output, String name, byte[] content) throws Exception {
        output.putArchiveEntry(new ZipArchiveEntry(name));
        output.write(content);
        output.closeArchiveEntry();
    }
}
