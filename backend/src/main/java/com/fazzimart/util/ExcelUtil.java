package com.fazzimart.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Low-level Excel (.xlsx) I/O using Apache POI - same approach as DhanyaMart.
 *
 * Data files live in:  <user home>/fazzimart-data/
 *   users.xlsx  products.xlsx  cart_items.xlsx  orders.xlsx  order_items.xlsx
 *
 * To change the location, set the Java system property  fazzi.data.dir
 * or the environment variable  FAZZIMART_DATA_DIR  to your preferred folder.
 *
 * Every file has a header row (row 0). All values are stored as text so the
 * same code reads them back safely whatever Excel may do with the cells.
 */
public final class ExcelUtil implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Shared lock so concurrent requests never corrupt an Excel file. */
    public static final Object LOCK = new Object();

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ExcelUtil() {
    }

    /** Returns the folder that holds all FAZZI MART .xlsx files. */
    public static File dataDir() {
        String dir = System.getProperty("fazzi.data.dir");
        if (dir == null || dir.trim().isEmpty()) {
            dir = System.getenv("FAZZIMART_DATA_DIR");
        }
        if (dir == null || dir.trim().isEmpty()) {
            dir = System.getProperty("user.home") + File.separator + "fazzimart-data";
        }
        return new File(dir);
    }

    /** Returns the File object for a named data file inside the data folder. */
    public static File dataFile(String name) {
        return new File(dataDir(), name);
    }

    /**
     * Reads every data row (skipping the header) as String arrays,
     * in file order. Creates the file with the header row if missing.
     */
    public static List<String[]> readRows(File file, String sheetName, String[] headers) throws IOException {
        try (Workbook wb = openOrCreate(file, sheetName, headers)) {
            Sheet sheet = wb.getSheet(sheetName);
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                String[] values = new String[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    values[i] = cell(row, i);
                }
                rows.add(values);
            }
            return rows;
        }
    }

    /**
     * Writes a complete workbook: header row + one row per String array.
     * Used by the DAOs for every write (add / update / delete).
     */
    public static void writeRows(File file, String sheetName, String[] headers,
                                 List<String[]> rows) throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(sheetName);

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            sheet.setColumnWidth(i, 22 * 256);
        }

        int r = 1;
        for (String[] values : rows) {
            Row row = sheet.createRow(r++);
            for (int i = 0; i < values.length && i < headers.length; i++) {
                row.createCell(i).setCellValue(values[i] == null ? "" : values[i]);
            }
        }

        save(wb, file);
    }

    /** Opens an existing workbook, or creates it (with headers) on first use. */
    public static Workbook openOrCreate(File file, String sheetName, String[] headers) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create data directory: " + parent);
        }

        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                return WorkbookFactory.create(in);
            }
        }

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(sheetName);
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            sheet.setColumnWidth(i, 22 * 256);
        }
        save(wb, file);
        return wb;
    }

    /** Writes the workbook to disk. */
    public static void save(Workbook wb, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }
    }

    /** Safely reads a cell as text whatever its stored type is. */
    public static String cell(Row row, int col) {
        if (row.getCell(col) == null) {
            return "";
        }
        switch (row.getCell(col).getCellType()) {
            case STRING:
                return row.getCell(col).getStringCellValue();
            case NUMERIC:
                double d = row.getCell(col).getNumericCellValue();
                if (d == Math.rint(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(row.getCell(col).getBooleanCellValue());
            default:
                return "";
        }
    }

    /** Next id = max existing id + 1 (or startAt if there is no data). */
    public static long nextId(List<String[]> rows, int idCol, long startAt) {
        long max = startAt - 1;
        for (String[] row : rows) {
            try {
                long v = Long.parseLong(row[idCol].trim());
                if (v > max) {
                    max = v;
                }
            } catch (Exception ignored) {
                // ignore bad cells
            }
        }
        return max + 1;
    }

    /** Formats a LocalDateTime the way timestamps are stored. */
    public static String format(LocalDateTime dt) {
        return dt.format(TIMESTAMP);
    }

    /** Parses a stored timestamp back to LocalDateTime. */
    public static LocalDateTime parse(String value) {
        try {
            return LocalDateTime.parse(value.trim(), TIMESTAMP);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}