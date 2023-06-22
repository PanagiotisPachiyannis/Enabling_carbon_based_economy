import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

// Class to categorise matched bonds according to country, rating
public class CategoriseRatingsIssuer {

  Map<String, Integer> rowNums = new HashMap<>();

  // Method which iterated over the matched bonds and categorises then depending on region of issuer
  public void categorise(String matchesFile) {
    rowNums.put("Prime", 0);
    rowNums.put("High grade", 0);
    rowNums.put("Upper medium grade", 0);
    rowNums.put("Lower medium grade", 0);
    rowNums.put("Junk", 0);

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

    File outputFile = new File("categorised_yields_by_issuer_rating2.xlsx");
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

    for (Row row : inputSheet) {
      if (row.getFirstCellNum() != 0) { // avoid weird excel error
        break;
      }
      if (row.getRowNum() == 0) {
        continue;
      }
      String cell0 = row.getCell(0).getStringCellValue(); // get the green bond
      String cell1 = row.getCell(1).getStringCellValue(); // get the conventional bond
      double cell2 = row.getCell(10).getNumericCellValue(); // get the green ytmBid
      String rating = row.getCell(15).getStringCellValue(); // get issuer rating

      switch (rating) {
        case "AAA":
          rating = "Prime";
          break;
        case "AA+":
        case "AA":
        case "AA-":
          rating = "High grade";
          break;
        case "A+":
        case "A":
        case "A-":
          rating = "Upper medium grade";
          break;
        case "BBB+":
        case "BBB":
        case "BBB-":
          rating = "Lower medium grade";
          break;
        case "BB+":
        case "BB":
        case "BB-":
        case "B+":
        case "B":
        case "B-":
        case "NR":
          rating = "Junk";
          break;
        default:
          System.out.println("rating not found " + rating);
      }
      int rowNum;
      try {
        rowNum = rowNums.get(rating);
      } catch (NullPointerException e) {
        System.out.println("wrong rating: " + rating);
        return;
      }
      rowNums.put(rating, rowNum + 1);
      XSSFSheet spreadsheet;
      if (outputWorkbook.getSheet(rating) != null) {
        spreadsheet = outputWorkbook.getSheet(rating);
      } else {
        spreadsheet = outputWorkbook.createSheet(rating);
      }
      Row outRow = spreadsheet.createRow(rowNum);
      Cell cell0_ = outRow.createCell(0);
      Cell cell1_ = outRow.createCell(1);
      Cell cell2_ = outRow.createCell(2);
      cell0_.setCellValue(cell0);
      cell1_.setCellValue(cell1);
      cell2_.setCellValue(cell2);
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


  public static void main(String[] args) {
    CategoriseRatingsIssuer categoriseRatings = new CategoriseRatingsIssuer();
    String matchesFile = "/Users/Panos/Desktop/FYP/yield_matches.xlsx";
    categoriseRatings.categorise(matchesFile);
  }
}
