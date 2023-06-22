import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Class to categorise matched bonds according to country, rating
public class CategoriseRatings {

  Map<String, Integer> rowNums = new HashMap<>();
  Map<String, String> snpToMoodys = new HashMap<>();
  Set<String> issuers = new HashSet<>();

  // Method which iterated over the matched bonds and categorises then depending on region of issuer
  public void categorise(String matchesFile) {
    rowNums.put("Prime", 0);
    rowNums.put("High grade", 0);
    rowNums.put("Upper medium grade", 0);
    rowNums.put("Lower medium grade", 0);
    rowNums.put("Junk", 0);

    snpToMoodys.put("AAA", "Aaa");
    snpToMoodys.put("AA+", "Aa1");
    snpToMoodys.put("AA", "Aa2");
    snpToMoodys.put("AA-", "Aa3");
    snpToMoodys.put("A+", "A1");
    snpToMoodys.put("A", "A2");
    snpToMoodys.put("A-", "A3");
    snpToMoodys.put("BBB+", "Baa1");
    snpToMoodys.put("BBB", "Baa2");
    snpToMoodys.put("BBB-", "Baa3");
    snpToMoodys.put("BB+", "Ba1");
    snpToMoodys.put("BB", "Ba2");
    snpToMoodys.put("BB-", "Ba3");
    snpToMoodys.put("B+", "B1");
    snpToMoodys.put("B", "B2");
    snpToMoodys.put("B-", "B3");
    snpToMoodys.put("NR", "NR");

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

    File outputFile = new File("categorised_rating_yields.xlsx");
    try {
      outputFile.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(outputFile);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    XSSFWorkbook outputWorkbook = new XSSFWorkbook();
    byte[] lightBlue = new byte[]{91, -101, -43};
    byte[] darkRed = new byte[]{-64, 0, 0};
    for (Row row : inputSheet) {
      if (row.getFirstCellNum() != 0) { // avoid weird excel error
        break;
      }
      CellStyle cellStyle = row.getCell(0).getCellStyle();
      Color color = cellStyle.getFillForegroundColorColor();
      if (color != null && (Arrays.equals(((XSSFColor) color).getRGB(), lightBlue) || Arrays.equals(((XSSFColor) color).getRGB(), darkRed))) {
        continue;
      }
      String issuerRegex = "issuer='(.*?)'";
      Pattern issuerPattern = Pattern.compile(issuerRegex); // compile the pattern
      String cell0 = row.getCell(0).getStringCellValue(); // get the green bond
      Matcher issuerMatcher = issuerPattern.matcher(cell0);
      if (issuerMatcher.find()) {
        issuers.add(issuerMatcher.group(1));
      } else {
        System.out.println("error: " + cell0);
      }
      String cell1 = row.getCell(1).getStringCellValue(); // get the conventional bond
      double cell2 = row.getCell(10).getNumericCellValue(); // get the green ytmBid
//      double cell3 = row.getCell(3).getNumericCellValue(); // get the green ytmAsk
//      double cell4 = row.getCell(4).getNumericCellValue(); // get the green ytmBid
//      double cell5 = row.getCell(5).getNumericCellValue(); // get the green ytmAsk
//      double cell8 = row.getCell(8).getNumericCellValue(); // get gb cpn
//      double cell9 = row.getCell(9).getNumericCellValue(); // get cb cpn
//      String cell10 = row.getCell(12).getStringCellValue(); // get the green activity
      String regex = "moodysRating='(.*?)', snpRating='(.*?)', maturity";
      Pattern pattern = Pattern.compile(regex); // compile the pattern
      Matcher matcher = pattern.matcher(cell0); // match on the pattern
      if (matcher.find()) {
        String moodys = matcher.group(1); // get the moodys rating as a string
        String snp = matcher.group(2); // get the snp rating as a string

        String rating = getRating(moodys, snp);
        switch (rating) {
          case "Aaa":
            rating =  "Prime";
          break;
          case "Aa1":
          case "Aa2":
          case "Aa3":
            rating =  "High grade";
          break;
          case "A1":
          case "A2":
          case "A3":
            rating =  "Upper medium grade";
          break;
          case "Baa1":
          case "Baa2":
          case "Baa3":
            rating =  "Lower medium grade";
          break;
          case "Ba1":
          case "Ba2":
          case "Ba3":
          case "B1":
          case "B2":
          case "B3":
          case "NR":
            rating =  "Junk";
          break;
          default:
            System.out.println("rating not found " + moodys);
        }
        int rowNum;
        try {
          rowNum = rowNums.get(rating);
        } catch (NullPointerException e) {
          System.out.println("'" + moodys + "'" + "   " + "'" + snp + "'" );
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
//        Cell cell3_ = outRow.createCell(3);
//        Cell cell4_ = outRow.createCell(4);
//        Cell cell5_ = outRow.createCell(5);
//        Cell cell6_ = outRow.createCell(6);
//        Cell cell7_ = outRow.createCell(7);
//        Cell cell8_ = outRow.createCell(8);
//        Cell cell9_ = outRow.createCell(9);
//        Cell cell10_ = outRow.createCell(10);
        cell0_.setCellValue(cell0);
        cell1_.setCellValue(cell1);
        cell2_.setCellValue(cell2);
//        cell3_.setCellValue(cell3);
//        cell4_.setCellValue(cell4);
//        cell5_.setCellValue(cell5);
//        cell6_.setCellFormula("C"+(rowNum+1)+"-E"+(rowNum+1));
//        cell7_.setCellFormula("D"+(rowNum+1)+"-F"+(rowNum+1));
//        XSSFFormulaEvaluator formulaEvaluator = outputWorkbook.getCreationHelper().createFormulaEvaluator();
//        formulaEvaluator.evaluateFormulaCell(cell6_);
//        formulaEvaluator.evaluateFormulaCell(cell7_);
//        cell8_.setCellValue(cell8);
//        cell9_.setCellValue(cell9);
//        cell10_.setCellValue(cell10);
      } else {
        System.out.println("ratings not found for bond: " + cell0);
      }
    }
    try {
      outputWorkbook.write(out);
      inputWorkbook.close();
      outputWorkbook.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getRating(String moodys, String snp) {
    if (moodys != null & !Objects.equals(moodys, "NR")) {
      return moodys;
    } else {
      return snpToMoodys.get(snp);
    }
  }


  public static void main(String[] args) {
    CategoriseRatings categoriseRatings = new CategoriseRatings();
    String matchesFile = "/Users/Panos/Desktop/FYP/yield_matches.xlsx";
    categoriseRatings.categorise(matchesFile);
    System.out.println(categoriseRatings.issuers.size());
  }
}
