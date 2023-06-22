import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Class to categorise matched bonds according to country, rating
public class CategoriseMatches {

  String AFRICAN_SHEET = "African Bonds";
  String SOUTH_AMERICAN_SHEET = "South American Bonds";
  String NORTH_AMERICAN_SHEET = "North American Bonds";
  String EUROPEAN_SHEET = "European Bonds";

  Map<String, Integer> rowNums = new HashMap<>();

  Set<String> african = new HashSet<>();
  Set<String> southAmerican = new HashSet<>();
  Set<String> northAmerican = new HashSet<>();
  Set<String> european = new HashSet<>();

  FileOutputStream out = getFileOutputStream();
  private final XSSFWorkbook workbook = new XSSFWorkbook();

  // Method which iterated over the matched bonds and categorises then depending on region of issuer
  public void categorise(String matchesFile, String africanFile, String southAmericanFile, String northAmericanFile, String europeanFile1, String europeanFile2) {
    // populate the sets of categorised corporations
    populateSets(africanFile, african);
    populateSets(southAmericanFile, southAmerican);
    populateSets(northAmericanFile, northAmerican);
    populateSets(europeanFile1, european);
    populateSets(europeanFile2, european);

    // init the row indexes for each sheet (used for output)
    rowNums.put(AFRICAN_SHEET, 0);
    rowNums.put(SOUTH_AMERICAN_SHEET, 0);
    rowNums.put(NORTH_AMERICAN_SHEET, 0);
    rowNums.put(EUROPEAN_SHEET, 0);

    FileInputStream file;
    try {
      file = new FileInputStream(matchesFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    XSSFWorkbook workbook;
    try {
      workbook = new XSSFWorkbook(file);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    byte[] lightBlue = new byte[]{91, -101, -43};
    byte[] darkRed = new byte[]{-64, 0, 0};
    XSSFSheet sheet = workbook.getSheetAt(0);
    for (Row row : sheet) {
      if (row.getFirstCellNum() != 0) { // avoid weird excel error
        break;
      }
      CellStyle cellStyle = row.getCell(0).getCellStyle();
      Color color = cellStyle.getFillForegroundColorColor();
      if (color != null && (Arrays.equals(((XSSFColor) color).getRGB(), lightBlue) || Arrays.equals(((XSSFColor) color).getRGB(), darkRed))) {
        continue;
      }
      String cell0 = row.getCell(0).getStringCellValue(); // get the green bond
      String cell1 = row.getCell(1).getStringCellValue(); // get the conventional bond
      double cell2 = row.getCell(10).getNumericCellValue(); // get the YTM percentage difference
      String regex = "issuer='(.*)', moodysRating"; // regex to extract the issuer from the green bond
      Pattern pattern = Pattern.compile(regex); // compile the pattern
      Matcher matcher = pattern.matcher(cell0); // match on the pattern
      if (matcher.find()) {
        String issuer = matcher.group(1); // get the issuer as a string

        // find in which region the issuer is in and write in the corresponding sheet
        if (african.contains(issuer)) {
          writeToExcel(AFRICAN_SHEET, cell0, cell1, cell2);
        } else if (southAmerican.contains(issuer)) {
          writeToExcel(SOUTH_AMERICAN_SHEET, cell0, cell1, cell2);
        } else if (northAmerican.contains(issuer)) {
          writeToExcel(NORTH_AMERICAN_SHEET, cell0, cell1, cell2);
        } else if (european.contains(issuer)) {
          writeToExcel(EUROPEAN_SHEET, cell0, cell1, cell2);
        } else {
          System.out.println("issuer not found: " + issuer);
        }
      } else {
        System.out.println("issuer not found for bond: " + cell0);
      }
    }
  }

  // Method which writes the given values (cell 0 - 3) to the given sheet in the output workbook
  private void writeToExcel(String sheetName, String cell0, String cell1, double cell2) {
    int rowNum = rowNums.get(sheetName);
    rowNums.put(sheetName, rowNum + 1);
    XSSFSheet spreadsheet;
    if (workbook.getSheet(sheetName) != null) {
      spreadsheet = workbook.getSheet(sheetName);
    } else {
     spreadsheet = workbook.createSheet(sheetName);
    }
    Row row = spreadsheet.createRow(rowNum);
    Cell cell0_ = row.createCell(0);
    Cell cell1_ = row.createCell(1);
    Cell cell2_ = row.createCell(2);
    cell0_.setCellValue(cell0);
    cell1_.setCellValue(cell1);
    cell2_.setCellValue(cell2);
  }

  // Method to write to the output file and then close the output stream and workbook
  private void writeAndClose() {
    try {// write and close the file & workbook
      workbook.write(out);
      assert out != null;
      out.close();
      workbook.close();
    } catch (IOException e) {
      System.out.println("cannot write to excel file");
    }
  }

  // Method to create the output file and return the output stream
  private static FileOutputStream getFileOutputStream() {
    File outputFile = new File("/Users/Panos/Desktop/FYP/categorised_matches.xlsx");
    try {
      outputFile.createNewFile(); // if file already exists will do nothing
    } catch (IOException e) {
      System.out.println("unable to create a new file");
    }
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(outputFile, false);
    } catch (FileNotFoundException e) {
      System.out.println("i swear i just created the file");
    }
    return out;
  }

  // Method to load the issuers from the given input file into the given set
  private void populateSets(String inputFile, Set<String> set) {
    FileInputStream file;
    try {
      file = new FileInputStream(inputFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    XSSFWorkbook workbook;
    try {
      workbook = new XSSFWorkbook(file);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    XSSFSheet sheet = workbook.getSheetAt(0);
    for (Row row : sheet) {

      if (row.getFirstCellNum() != 0) { // avoid weird excel error
        break;
      }
      String issuer = row.getCell(0).getStringCellValue();
      set.add(issuer);
    }
    try {
      file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    CategoriseMatches categoriseMatches = new CategoriseMatches();
    String africanFile = "/Users/Panos/Downloads/green_bonds_africa.xlsx";
    String southAmFile = "/Users/Panos/Downloads/green_bonds_central-south_america.xlsx";
    String northAmFile = "/Users/Panos/Downloads/green_bonds_north_america.xlsx";
    String europeanFile1 = "/Users/Panos/Downloads/green_bonds_europe_EUR.xlsx";
    String europeanFile2 = "/Users/Panos/Downloads/green_european_non-EUR.xlsx";
    String matchesFile = "/Users/Panos/Desktop/FYP/yield_matches.xlsx";
    categoriseMatches.categorise(matchesFile, africanFile, southAmFile, northAmFile, europeanFile1, europeanFile2);
    categoriseMatches.writeAndClose();
  }
}
