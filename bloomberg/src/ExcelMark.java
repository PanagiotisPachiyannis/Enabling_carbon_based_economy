import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelMark {
    public static void main(String[] args) {
        String fileAPath = "\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\bonds_FIGI_test.xlsx"; // Path of File A
        String fileBPath = "\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\issuersProcessed_test.xlsx"; // Path of File B

        try {
            FileInputStream fileAInputStream = new FileInputStream(fileAPath);
            FileInputStream fileBInputStream = new FileInputStream(fileBPath);

            // Load File A and File B
            XSSFWorkbook fileAWorkbook = new XSSFWorkbook(fileAInputStream);
            XSSFWorkbook fileBWorkbook = new XSSFWorkbook(fileBInputStream);

            XSSFSheet fileASheet = fileAWorkbook.getSheetAt(0); // Assuming File A has only one sheet
            XSSFSheet fileBSheet = fileBWorkbook.getSheetAt(0); // Assuming File B has only one sheet

            byte[] darkRed = new byte[]{-64, 0, 0};

            for (int i = 0; i <= fileASheet.getLastRowNum(); i++) {
                XSSFRow fileARow = fileASheet.getRow(i);
                Cell fileACell = fileARow.getCell(0); // Assuming File A has only one column

                String fileAValue = fileACell.getStringCellValue();

                for (int j = 0; j <= fileBSheet.getLastRowNum(); j++) {
                    XSSFRow fileBRow = fileBSheet.getRow(j);
                    Cell fileBCell = fileBRow.getCell(0); // Assuming File B has only one column

                    String fileBValue = fileBCell.getStringCellValue();

                    if (fileAValue.equals(fileBValue)) {
                        // Create red fill style
                        XSSFCellStyle redCellStyle = fileAWorkbook.createCellStyle();
                        redCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        redCellStyle.setFillForegroundColor(new XSSFColor(darkRed));

                        // Apply red fill style to file A cell
                        fileACell.setCellStyle(redCellStyle);
                    }
                }
            }

            // Write the updated File A
            FileOutputStream fileAOutputStream = new FileOutputStream(fileAPath);
            fileAWorkbook.write(fileAOutputStream);
            fileAOutputStream.close();

            System.out.println("Comparison and highlighting completed successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
