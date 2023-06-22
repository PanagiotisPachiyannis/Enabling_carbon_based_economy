import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Class to categorise matched bonds according to country, rating
public class CategoriseGreenActivity {

  Map<String, Integer> rowNums = new HashMap<>();

  // Method which iterated over the matched bonds and categorises then depending on region of issuer
  public void categorise(String matchesFile) {
    rowNums.put("undisclosed", 0);
    rowNums.put("Energy smart technologies and energy efficiency", 0);
    rowNums.put("Green buildings and infrastructure", 0);
    rowNums.put("Clean transportation", 0);
    rowNums.put("Sustainable water management", 0);
    rowNums.put("Renewable energy", 0);
    rowNums.put("Terrestrial and aquatic biodiversity conservation", 0);
    rowNums.put("Pollution prevention and control", 0);
    rowNums.put("Agriculture and forestry", 0);
    rowNums.put("Climate change adaptation", 0);
    rowNums.put("Eco-efficient products, production technologies and processes", 0);


    FileInputStream inputFile;
    try {
      inputFile = new FileInputStream(matchesFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    XSSFWorkbook inputWorkbook;
    try {
      inputWorkbook = new XSSFWorkbook(inputFile);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    XSSFSheet inputSheet = inputWorkbook.getSheetAt(0);

    File outputFile = new File("categorised_yields_by_green_activity.xlsx");
    try {
      outputFile.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    FileOutputStream out;
    try {
      out = new FileOutputStream(outputFile);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    XSSFWorkbook outputWorkbook = new XSSFWorkbook();

    byte[] darkRed = new byte[]{-64, 0, 0};
    byte[] lightBlue = new byte[]{91, -101, -43};
    for (Row row : inputSheet) {
      if (row.getFirstCellNum() != 0) { // avoid weird excel error
        break;
      }
      if (row.getRowNum() == 0) {
        continue;
      }
      CellStyle cellStyle = row.getCell(0).getCellStyle();
      Color color = cellStyle.getFillForegroundColorColor();
      if (color != null && (Arrays.equals(((XSSFColor) color).getRGB(), darkRed) || Arrays.equals(((XSSFColor) color).getRGB(), lightBlue))) { // dark red rows have no conventional bonds issued by that issuer, so skip them
        continue;
      }
      String cell0 = row.getCell(0).getStringCellValue(); // get the green bond
      String cell1 = row.getCell(1).getStringCellValue(); // get the conventional bond
      double cell2 = row.getCell(8).getNumericCellValue(); // get the ytm % diff
//      double cell3 = row.getCell(3).getNumericCellValue(); // get the conventional bond
//      double cell4 = row.getCell(4).getNumericCellValue(); // get the conventional bond
//      double cell5 = row.getCell(5).getNumericCellValue(); // get the conventional bond
//      double cell8 = row.getCell(8).getNumericCellValue(); // get the conventional bond
//      double cell9 = row.getCell(9).getNumericCellValue(); // get the conventional bond
      String cell12 = row.getCell(13).getStringCellValue(); // get the green activity

      String regex = "([^/]+)"; // regex to extract the issuer from the green bond
      Pattern pattern = Pattern.compile(regex); // compile the pattern
      Matcher matcher = pattern.matcher(cell12); // match on the pattern
      while (matcher.find()) {
        String activity = matcher.group(0);
        populateSheet(outputWorkbook, cell0, cell1, cell2, activity);
      }

    }
    try {
      outputWorkbook.write(out);
      inputWorkbook.close();
      outputWorkbook.close();
    } catch (
        IOException e) {
      throw new RuntimeException(e);
    }

  }

  private void populateSheet(XSSFWorkbook outputWorkbook, String cell0, String cell1, double cell2, String activity) {
    int rowNum;
    try {
      rowNum = rowNums.get(activity);
    } catch (NullPointerException e) {
      System.out.println("unknown activity: " + activity);
      return;
    }
    rowNums.put(activity, rowNum + 1);
    XSSFSheet spreadsheet;
    if (activity.length() > 31) {
      activity = activity.substring(0, 31);
    }
    if (outputWorkbook.getSheet(activity) != null) {
      spreadsheet = outputWorkbook.getSheet(activity);
    } else {
      spreadsheet = outputWorkbook.createSheet(activity);
    }
    Row outRow = spreadsheet.createRow(rowNum);
    Cell cell0_ = outRow.createCell(0);
    Cell cell1_ = outRow.createCell(1);
    Cell cell2_ = outRow.createCell(2);
//    Cell cell3_ = outRow.createCell(3);
//    Cell cell4_ = outRow.createCell(4);
//    Cell cell5_ = outRow.createCell(5);
//    Cell cell6_ = outRow.createCell(6);
//    Cell cell7_ = outRow.createCell(7);
//    Cell cell8_ = outRow.createCell(8);
//    Cell cell9_ = outRow.createCell(9);
//    Cell cell10_ = outRow.createCell(10);
    cell0_.setCellValue(cell0);
    cell1_.setCellValue(cell1);
    cell2_.setCellValue(cell2);
//    cell3_.setCellValue(cell3);
//    cell4_.setCellValue(cell4);
//    cell5_.setCellValue(cell5);
//    cell6_.setCellFormula("C" + (rowNum + 1) + "-E" + (rowNum + 1));
//    cell7_.setCellFormula("D" + (rowNum + 1) + "-F" + (rowNum + 1));
//    cell8_.setCellValue(cell8);
//    cell9_.setCellValue(cell9);
//    cell10_.setCellFormula("ABS(I" + (rowNum + 1) + "-J" + (rowNum + 1) + ")");

//    XSSFFormulaEvaluator formulaEvaluator = outputWorkbook.getCreationHelper().createFormulaEvaluator();
//    formulaEvaluator.evaluateFormulaCell(cell6_);
//    formulaEvaluator.evaluateFormulaCell(cell7_);
//    formulaEvaluator.evaluateFormulaCell(cell10_);
  }


  public static void main(String[] args) {
    CategoriseGreenActivity categoriseRatings = new CategoriseGreenActivity();
    String matchesFile = "/Users/Panos/Desktop/FYP/green_activity_yield_matches.xlsx";
    categoriseRatings.categorise(matchesFile);
  }
}
