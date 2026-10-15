package vn.edu.qlhs.student.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StudentServiceBirthDateTest {

    @Test
    void readsYearAndVietnameseDates() throws Exception {
        assertEquals(LocalDate.of(2018, 1, 1), StudentService.parseBirthDate("2018"));
        assertEquals(LocalDate.of(2018, 10, 15), StudentService.parseBirthDate("15/10/2018"));
        assertEquals(LocalDate.of(2018, 3, 5), StudentService.parseBirthDate("5-3-2018"));
        assertEquals(LocalDate.of(2018, 3, 5), StudentService.parseBirthDate("2018-03-05"));
        assertNull(StudentService.parseBirthDate(""));
        assertNull(StudentService.parseBirthDate("abc"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet("HocSinh").createRow(0);
            row.createCell(0).setCellValue(2018);
            Cell excelDate = row.createCell(1);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            excelDate.setCellValue(LocalDate.of(2017, 9, 5));
            excelDate.setCellStyle(dateStyle);
            assertEquals(LocalDate.of(2018, 1, 1), StudentService.readBirthDate(row.getCell(0)));
            assertEquals(LocalDate.of(2017, 9, 5), StudentService.readBirthDate(excelDate));
        }
    }
}
