import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;

// Class to categorise matched bonds according to country, rating
public class PrepareData {

    public static void main(String[] args) throws IOException {
        String inputFilePath = "/Users/Panos/Desktop/FYP/yield_matches.xlsx";
        String outputFilePath = "prepped_data.xlsx";

        FileInputStream fis = new FileInputStream(inputFilePath);
        Workbook inputWorkbook = new XSSFWorkbook(fis);
        Sheet inputSheet = inputWorkbook.getSheetAt(0);

        Workbook outputWorkbook = new XSSFWorkbook();
        Sheet outputSheet = outputWorkbook.createSheet("Processed Data");
        int outputRowIndex = 0;
        byte[] lightBlue = new byte[]{91, -101, -43};
        byte[] darkRed = new byte[]{-64, 0, 0};
        // Iterate through each row in the input sheet
        for (Row inputRow : inputSheet) {
            if (inputRow.getFirstCellNum() != 0) { // avoid weird excel error
                break;
            }
            Row outputRow = outputSheet.createRow(outputRowIndex++);
            CellStyle cellStyle = inputRow.getCell(0).getCellStyle();
            Color color = cellStyle.getFillForegroundColorColor();
            if (color != null && (Arrays.equals(((XSSFColor) color).getRGB(), lightBlue) || Arrays.equals(((XSSFColor) color).getRGB(), darkRed))) {
                continue;
            }
            int lastIdx = 0;
            // Iterate through each cell in the input row
            for (int i = 0; i < 2; i++) {
                String cellValue = inputRow.getCell(i).getStringCellValue();
                List<Object> splitData = extractData(cellValue);

                // Write the split data to the output row
                for (int j = 0; j < splitData.size(); j++) {
                    Cell outputCell = outputRow.createCell(j + lastIdx);
                    Object value = splitData.get(j);
                    if (value instanceof String) {
                        outputCell.setCellValue((String) value);
                    } else if (value instanceof Double) {
                        outputCell.setCellValue((Double) value);
                    } else if (value instanceof Date) {
                        outputCell.setCellValue((Date) value);
                    }
                }
                lastIdx = splitData.size();
            }
            String issuerRating = inputRow.getCell(15).getStringCellValue();
            Cell cell = outputRow.createCell(outputRow.getLastCellNum());
            cell.setCellValue(issuerRating);
        }

        // Write the output workbook to a file
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        outputWorkbook.write(fos);
        fos.close();

        inputWorkbook.close();
        outputWorkbook.close();
    }

    private static List<Object> extractData(String input) {
        // Regular expression pattern to extract data between single quotes
        Pattern pattern = Pattern.compile("='*([^',}]+)'*");
        Matcher matcher = pattern.matcher(input);

        // Extract the data and store it in a list
        List<String> extractedData = new ArrayList<>();
        while (matcher.find()) {
            extractedData.add(matcher.group(1));
        }

        // Select only the required fields and parse numbers and dates
        List<Object> result = new ArrayList<>(9);
        result.add(extractedData.get(0)); // issuer name
        result.add(extractedData.get(1)); // moody's rating
        result.add(extractedData.get(2)); // S&P rating
        result.add(extractedData.get(7)); // issuance date
        result.add(extractedData.get(4)); // maturity date
        result.add(extractedData.get(8)); // currency
        result.add(extractedData.get(9)); // green (true/false)
        result.add(parseDouble(extractedData.get(11))); // ytmBid
        result.add(parseDouble(extractedData.get(12))); // ytmAsk

        return result;
    }

//    private static LocalDate parseDate(String dateStr) {
//        String dateFormat = new DateFormatter.("yyyy-MM-dd");
//        try {
//            return dateFormat.parse(dateStr);
//        } catch (ParseException e) {
//            return null;
//        }
//    }
}
