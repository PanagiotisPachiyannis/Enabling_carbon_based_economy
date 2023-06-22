import com.bloomberglp.blpapi.*;
import com.bloomberglp.blpapi.Name;
import javafx.util.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.*;

public class BloombergMain {

    private static final Name SECURITIES = new Name("securities");
    private static final Name FIELDS = new Name("fields");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name SECURITY_DATA = new Name("securityData");
    private static final Name RESPONSE_ERROR = new Name("responseError");

    private String field;
    private Session session;
    private int apiCallsMade = 0;

    private final Map<String, Bond> conventionalBonds = new HashMap<>(); // map of conventional bond ids to bond object; used to lookup details of potential matches easily
    private final Map<String, List<String>> conventionalBondsByIssuer = new HashMap<>(); // map of conventional bonds (issuerName -> bondID)
    private final Map<String, List<Bond>> greenBondsByIssuer = new HashMap<>(); // map of green bond issuers to list of bond objects
    private final List<Pair<Bond, Bond>> matchedBonds = new ArrayList<>(); // list of pairs of matched bonds
    public static final Map<Bond, String> matchedBondIds = new HashMap<>(); // conventional bond to id, only the matched bonds should be here, used for exporting data
    private final List<String> processedIssuers = new ArrayList<>();

    /** Tries to match the green bonds to conventional ones
     * PRE: the hashmaps are populated **/
    public void match() {
        int num = 0;
        for (String issuer : greenBondsByIssuer.keySet()) {
            if (!conventionalBondsByIssuer.containsKey(issuer) || conventionalBondsByIssuer.get(issuer).isEmpty()) {
                System.out.println("issuer not found");
                continue; //skip to next issuer
            } else {
                processedIssuers.add(issuer);
            }
            List<Bond> greenBonds = greenBondsByIssuer.get(issuer);
            List<String> cBonds = conventionalBondsByIssuer.get(issuer);
            for (Bond greenBond : greenBonds) {
                num++;
                boolean match = false;
                for (String cBond : cBonds) { // try and match the green bond with the information already given to us
                    Bond bond = conventionalBonds.get(cBond);
                    if (greenBond.greenAndConventionalEquals(bond)) {
                        matchedBonds.add(new Pair<>(greenBond, bond));
                        matchedBondIds.put(bond, cBond);
                        System.out.println("found a match locally");
                        match = true;
                    }
                }
                if (match) { // if a match was found locally, skip to the next green bond
                    continue;
                }
                // no match from the bonds map, we have to lookup
                for (String cBond : cBonds) {
                    if (conventionalBonds.containsKey(cBond)) { // should always be true, sanity check
                        // we already have the bond stored, but it either doesn't match or we are missing fields
                        Bond bond = conventionalBonds.get(cBond);
                        List<String> missingFields = bond.getMissingFields();
                        if (missingFields.isEmpty()) { // we have no missing fields, so it's a mismatch
                            break; // skip to next conventional bond and keep checking
                        } else {
                            // check if the fields we have are equal
                            boolean fieldsEqual = greenBond.areAllPresentFieldsEqual(bond);
                            // if they are, perform lookup
                            if (fieldsEqual) {
                                // bool to keep track of mismatches, if true we break out the loop to save api calls
                                boolean mismatch = false;
                                for (String missingField : missingFields) {
                                    // perform lookup, one field at a time
                                    String lookup = lookup(cBond, missingField);
                                    if (lookup == null) {
                                        return;// some error was thrown, return to terminate
                                    }
                                    // based on the missing field, set the value in the bond object for later checks
                                    switch (missingField) {
                                        case Bond.ISSUER: // this should not be the case ever
                                            bond.issuer = lookup;
                                            mismatch = !greenBond.issuer.equals(lookup);
                                            break;
                                        case Bond.CURRENCY:
                                            bond.ccy = lookup;
                                            mismatch = !greenBond.ccy.equals(lookup);
                                            break;
                                        case Bond.ISSUE_DATE:
                                            bond.setIssuance(lookup);
                                            mismatch = !greenBond.issuanceYear.equals(bond.issuanceYear) || !greenBond.issuanceTerm.equals(bond.issuanceTerm);
                                            break;
                                        case Bond.MATURITY:
                                            if (lookup.equals("#N/A N/A")) {
                                                lookup = "#N/A Field Not Applicable";
                                            }
                                            bond.setMaturity(lookup);
                                            mismatch = !greenBond.maturity.equals(bond.maturity);
                                            break;
                                        case Bond.MOODYS_RATING:
                                            if (lookup.equals("#N/A N/A")) {
                                                lookup = "NR"; // treat NR and empty responses here equally
                                            }
                                            bond.moodysRating = lookup;
                                            mismatch = !greenBond.moodysRating.equals(lookup);
                                            break;
                                        case Bond.SNP_RATING:
                                            if (lookup.equals("#N/A N/A")) {
                                                lookup = "NR"; // treat NR and empty responses here equally
                                            }
                                            bond.snpRating = lookup;
                                            mismatch = !greenBond.snpRating.equals(lookup);
                                            break;
                                        default:
                                            System.out.println("unexpected field id");
                                            break;
                                    }
                                    // stop looking up other fields from this bond if one is mismatched
                                    if (mismatch) {
                                        break;
                                    }
                                } // here we have either matched the bond or have exited the loop early
                                if (greenBond.greenAndConventionalEquals(bond)) {
                                    matchedBonds.add(new Pair<>(greenBond, bond));
                                    matchedBondIds.put(bond, cBond);
                                    System.out.println("Matched a bond!");
                                    break;
                                }
                            }  // we have some fields, but they are not equal so continue to next conventional bond

                        }
                    } else {
                        System.out.println("wth isn't this in the map?");
                    }
                }
                if (num % 500 == 0) {
                    System.out.println("500 green bonds done");
                }
            }
        }
        writeToExcel();
    }

    /** Reads the excel file provided and populates the bondsByIssuer map
     * PRE: the excel file provided must have two columns 'issuer name':'isin/cusip/bbid' **/
    public void readFromExcelConventional(String fileName) {
        FileInputStream file;
        try {
            file = new FileInputStream(fileName);
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
        byte[] lightBlue = new byte[]{91, -101, -43};
        byte[] darkRed = new byte[]{-64, 0, 0};
        boolean isin = false; // one file has isins, all others have figis
        for (Row row : sheet) {
            CellStyle cellStyle = row.getCell(0).getCellStyle();
            Color color = cellStyle.getFillForegroundColorColor();
            if (color != null && Arrays.equals(((XSSFColor) color).getRGB(), lightBlue)) {
                isin = row.getCell(1).getStringCellValue().equals("ISIN");
                continue;
            }
            if (color != null && Arrays.equals(((XSSFColor) color).getRGB(), darkRed)) { // dark red rows have been already processed
                continue;
            }

            String issuer = row.getCell(0).getStringCellValue();
            String id = row.getCell(1).getStringCellValue();

            List<String> bonds = conventionalBondsByIssuer.get(issuer);

            if (id.equals("#N/A Field Not Applicable")) {
                continue;
            }

            if (bonds == null) { // we found a new issuer, initialise the list and add the id
                ArrayList<String> list = new ArrayList<>();
                list.add(id);
                conventionalBondsByIssuer.put(issuer, list);
            } else {
                bonds.add(id);
                conventionalBondsByIssuer.put(issuer, bonds);
            }
            // add id with null bond in the bondMap
            if (conventionalBonds.containsKey(id)) { // sanity check
                System.out.println("bond id already in the map!!?");
            } else {
                conventionalBonds.put(id, new Bond(issuer, isin));
            }
        }

        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Reads the excel files provided and populates the greenBondsByIssuer map
     * PRE: the excel file provided must have 16 columns 'issuer name'...'ccy'
     * In the excels for green bonds we do not have issue date (needed)
     * */
    public void readFromExcelGreen(String... fileNames) {
        for (String fileName : fileNames) {
            FileInputStream file;
            try {
                file = new FileInputStream(fileName);
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
            int issuerIdx = 0, maturityDateIdx = 0, moodysIdx = 0, snpIdx = 0, ccyIdx = 0, issueDateIdx = 0;
            for (Row row : sheet) {
                byte[] darkRed = new byte[]{-64, 0, 0};
                byte[] lightBlue = new byte[]{91, -101, -43};

                if (row.getFirstCellNum() != 0) { // avoid weird excel error
                    break;
                }
                CellStyle cellStyle = row.getCell(0).getCellStyle();
                Color color = cellStyle.getFillForegroundColorColor();
                if (color != null && Arrays.equals(((XSSFColor) color).getRGB(), darkRed)) { // dark red rows have no conventional bonds issued by that issuer, so skip them
                    continue;
                }

                if (color != null && Arrays.equals(((XSSFColor) color).getRGB(), lightBlue)) { // light blue is the title row, get the indexes for the fields
                    for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                        String cellValue = new DataFormatter().formatCellValue(row.getCell(i));
                        switch (cellValue) {
                            case "Issuer Name":
                                issuerIdx = i;
                                break;
                            case "Maturity":
                                maturityDateIdx = i;
                                break;
                            case "Moody Rtg":
                                moodysIdx = i;
                                break;
                            case "S&P Rating":
                                snpIdx = i;
                                break;
                            case "Currency":
                                ccyIdx = i;
                                break;
                            case "Issue Date":
                                issueDateIdx = i;
                                break;
                            default:
                                System.out.println("ignoring field " + cellValue);
                                break;
                        }
                    }
                    continue;
                }

                String issuer = row.getCell(issuerIdx).getStringCellValue();
                String maturityDate = new DataFormatter().formatCellValue(row.getCell(maturityDateIdx));
                String issuanceDate = new DataFormatter().formatCellValue(row.getCell(issueDateIdx));
                String moodysRating = row.getCell(moodysIdx).getStringCellValue().equals("#N/A N/A") ? "NR" : row.getCell(moodysIdx).getStringCellValue();
                String snpRating = row.getCell(snpIdx).getStringCellValue().equals("#N/A N/A") ? "NR" : row.getCell(snpIdx).getStringCellValue();
                String ccy = ccyIdx == 0 ? "EUR" : row.getCell(ccyIdx).getStringCellValue(); // if the ccy index is 0 (never the case if present) we are missing ccy field meaning we have eur bonds

                Bond bond = new Bond(issuer, maturityDate, moodysRating, snpRating, ccy, issuanceDate, true);

                List<Bond> bonds = greenBondsByIssuer.get(issuer);

                if (bonds == null) { // we found a new issuer, initialise the list and add the isin
                    ArrayList<Bond> list = new ArrayList<>();
                    list.add(bond);
                    greenBondsByIssuer.put(issuer, list);
                } else {
                    bonds.add(bond);
                    greenBondsByIssuer.put(issuer, bonds);
                }
            }
            try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Looks up the specified field for the given ID
     * Uses 1 unit of monthly limit per call - avoid if possible
     * Assumes the session is open and running correctly and that the field to lookup is the field tag returned by FLDS<GO> **/
    public String lookup(String securityID, String field) {
        if (session == null) {
            System.out.println("session no longer open");
            return null;
        }
        this.field = field; // set the field so that you know what to expect in response method

        Service refDataService = session.getService("//blp/refdata");

        if (refDataService == null) {
            writeToExcel();
            return null;
        }

        Request request = refDataService.createRequest("ReferenceDataRequest");

        boolean isin = conventionalBonds.get(securityID).isin; // get the bond's isin bool to shape the request accordingly

        if (isin) {
            request.getElement(SECURITIES).appendValue("/isin/" + securityID); // isin requires this prefix
        } else {
            request.getElement(SECURITIES).appendValue(securityID); // figi case; no prefix required
        }

        request.getElement(FIELDS).appendValue(this.field);

        try {
            session.sendRequest(request, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        apiCallsMade++;
        String ret = null;
        try {
            ret = processResponse();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    /** Process the response from Bloomberg **/
    private String processResponse() throws InterruptedException {
        while (true) {
            Event event = session.nextEvent();
            for (Message msg : event) {
                if (msg.hasElement(RESPONSE_ERROR)) {
                    System.out.println("REQUEST FAILED: " + msg.getElement(RESPONSE_ERROR));
                    writeToExcel();
                    break;
                }
                if (event.eventType() == Event.EventType.SESSION_STATUS) {
                    if (msg.messageType().equals(Names.SESSION_TERMINATED)
                            || msg.messageType().equals(Names.SESSION_STARTUP_FAILURE)) {
                        writeToExcel();
                        System.err.println("Session failed to start or terminated.");
                    }
                    continue;
                } else if (event.eventType() == Event.EventType.SERVICE_STATUS) {
                    if (msg.messageType().equals(Names.SERVICE_OPEN_FAILURE)) {
                        String serviceName = msg.getElementAsString(Name.getName("serviceName"));
                        System.err.println("Failed to open " + serviceName + ".");
                        writeToExcel();
                    }
                    continue;
                }
                //msg.getElement(SECURITY_DATA).getValueAsElement(0).getElement(FIELD_DATA).getElementAsString(new Name(field))
                Element securities = msg.getElement(SECURITY_DATA);
                Element element = securities.getValueAsElement(0); // the response should always have only one element
                Element fieldData = element.getElement(FIELD_DATA);
                if (fieldData.hasElement(Name.getName(field))) {
                    return fieldData.getElementAsString(Name.getName(field));
                } else return "#N/A N/A"; // some securities are missing some fields (e.g. rating), so put N/A to avoid infinite loops with null
            }
            if (event.eventType() == Event.EventType.RESPONSE) {
                break;
            }
        }
        return null; //?
    }

    /** Writes to the output excel file all the matched bonds so far **/
    public void writeToExcel() {
        File outputFile = new File("matches.xlsx");
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
        int rownum = 0;
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet spreadsheet = workbook.createSheet("Matched Green Bonds");
        for (Pair<Bond, Bond> match : matchedBonds) {
            Row row = spreadsheet.createRow(rownum++);
            Cell greenBondCell = row.createCell(0);
            Cell convBondCell = row.createCell(1);
            greenBondCell.setCellValue(match.getKey().toString());
            convBondCell.setCellValue(match.getValue().toString());
        }
        try {// write and close the file & workbook
            workbook.write(out);
            assert out != null;
            out.close();
            workbook.close();
        } catch (IOException e) {
            System.out.println("cannot write to excel file");
        }

        File outputCheckedIssuers = new File("issuersProcessed.xlsx");
        try {
            outputCheckedIssuers.createNewFile(); // if file already exists will do nothing
        } catch (IOException e) {
            System.out.println("unable to create a new file");
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputCheckedIssuers, false);
        } catch (FileNotFoundException e) {
            System.out.println("i swear i just created the file");
        }
        int rownum2 = 0;
        XSSFWorkbook sheets = new XSSFWorkbook();
        XSSFSheet issuersProcessed = sheets.createSheet("Checked Issuers");
        for (String s : processedIssuers) {
            Row row = issuersProcessed.createRow(rownum2++);
            Cell issuerCell = row.createCell(0);
            issuerCell.setCellValue(s);
        }
        try {// write and close the file & sheets
            sheets.write(outputStream);
            assert outputStream != null;
            outputStream.close();
            sheets.close();
        } catch (IOException e) {
            System.out.println("cannot write to excel file");
        }
        System.out.println(apiCallsMade);
    }

    /** Set up connection to Bloomberg for //blp/refdata service **/
    private void startSession() throws IOException, InterruptedException {
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setServerHost("localhost");
        sessionOptions.setServerPort(8194);

        this.session = new Session(sessionOptions);
        if (!session.start()) {
            System.err.println("Failed to start session.");
            return;
        }
        if (!session.openService("//blp/refdata")) {
            System.err.println("Failed to open //blp/refdata");
        }
    }

    /** Class to represent Bonds **/
    private static class Bond {
        public static final String ISSUER = "DS134";
        public static final String MOODYS_RATING = "RA001";
        public static final String SNP_RATING = "RA002";
        public static final String MATURITY = "DS035";
        public static final String CURRENCY = "DS004";
        public static final String ISSUE_DATE = "DS031";

        public String issuer; // issuer name // Field ID = DS134
        public String moodysRating; // Moody's rating // Field ID = RA001
        public String snpRating; // S&P rating // Field ID = RA002
        public Maturities maturity; // maturity length
        public LocalDate maturityDate; // maturity date // Field ID = DS035
        public Issuance issuanceTerm; // yearly quarter the bond got issued
        public String issuanceYear; // year it got issued
        public LocalDate issuanceDate; // date it got issued // Field ID (issuance date) = DS031
        public String ccy; // currency // Field ID = DS004
        public boolean green; // differentiate between green and non-green bonds
        public boolean isin; // if true the bond's id is the ISIN, if false, it's the FIGI number

        public Bond(String issuer, String maturityDate, String moodysRating, String snpRating, String ccy, String issuanceDate, boolean green) {
            this.issuer = issuer;
            this.moodysRating = moodysRating;
            this.snpRating = snpRating;
            setIssuance(issuanceDate);
            setMaturity(maturityDate);
            this.ccy = ccy;
            this.green = green;
        }

        public Bond(String issuer, boolean isin) {
            this.issuer = issuer;
            this.isin = isin;
            this.green = false;
        }


        /** Method to compare green and non-green bonds **/
        public boolean greenAndConventionalEquals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bond bond = (Bond) o;
            return Objects.equals(issuer, bond.issuer) &&
                    Objects.equals(moodysRating, bond.moodysRating) &&
                    Objects.equals(snpRating, bond.snpRating) &&
                    maturity == bond.maturity &&
                    issuanceTerm == bond.issuanceTerm &&
                    Objects.equals(issuanceYear, bond.issuanceYear) &&
                    Objects.equals(ccy, bond.ccy);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bond bond = (Bond) o;
            return green == bond.green &&
                    Objects.equals(issuer, bond.issuer) &&
                    Objects.equals(moodysRating, bond.moodysRating) &&
                    Objects.equals(snpRating, bond.snpRating) &&
                    maturity == bond.maturity &&
                    Objects.equals(maturityDate, bond.maturityDate) &&
                    issuanceTerm == bond.issuanceTerm &&
                    Objects.equals(issuanceYear, bond.issuanceYear) &&
                    Objects.equals(issuanceDate, bond.issuanceDate) &&
                    Objects.equals(ccy, bond.ccy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(issuer, moodysRating, snpRating, maturity, maturityDate, issuanceTerm, issuanceYear, issuanceDate, ccy, green);
        }

        @Override
        public String toString() {
            String isin = matchedBondIds.getOrDefault(this, "N/A");
            return "Bond{" +
                    "issuer='" + issuer + '\'' +
                    ", moodysRating='" + moodysRating + '\'' +
                    ", snpRating='" + snpRating + '\'' +
                    ", maturity=" + maturity +
                    ", maturityDate=" + maturityDate +
                    ", issuanceTerm=" + issuanceTerm +
                    ", issuanceYear='" + issuanceYear + '\'' +
                    ", issuanceDate=" + issuanceDate +
                    ", ccy='" + ccy + '\'' +
                    ", green?='" + green + '\'' +
                    ", isin='" + isin + '\'' +
                    '}';
        }

        public List<String> getMissingFields() {
            List<String> missing = new ArrayList<>();
            if (issuer == null) {
                missing.add(ISSUER);
            }
            if (issuanceDate == null) { // no need to check term as well, they are set together
                missing.add(ISSUE_DATE);
            } // issuance NEEDS to be before maturity, cause maturity depends on it
            if (maturityDate == null && maturity == null) { // in the case of perpetual bonds maturityDate will be null, but maturity will not
                missing.add(MATURITY);
            }
            if (ccy == null) {
                missing.add(CURRENCY);
            }
            if (moodysRating == null) {
                missing.add(MOODYS_RATING);
            }
            if (snpRating == null) {
                missing.add(SNP_RATING);
            }
            return missing;
        }

        /** PRE: this is called on a green bond instance (with all fields present) and compares it to a conventional bond **/
        public boolean areAllPresentFieldsEqual(Bond bond) {
            boolean a = bond.issuer == null || issuer.equals(bond.issuer);
            boolean b = bond.moodysRating == null || moodysRating.equals(bond.moodysRating);
            boolean c = bond.snpRating == null || snpRating.equals(bond.snpRating);
            boolean d = bond.maturity == null || maturity.equals(bond.maturity);
            boolean e = bond.ccy == null || ccy.equals(bond.ccy);
            boolean f = bond.issuanceYear == null || issuanceYear.equals(bond.issuanceYear);
            boolean g = bond.issuanceTerm == null || issuanceTerm.equals(bond.issuanceTerm);

            return a && b && c && d && e && f && g;
        }

        /** Takes in a string of the maturity date and converts it in LocalDate
         * Also sets the field 'maturity' for this bond
         * PRE: the issuance date is already populated (will log error otherwise) **/
        public void setMaturity(String maturityDate) {
            if (maturityDate.equals("#N/A Field Not Applicable") || maturityDate.equals("#N/A N/A")) {
                this.maturity = Maturities.PERPETUAL;
                this.maturityDate = null;
                return;
            }
            LocalDate date;
            date = getLocalDate(maturityDate);
            this.maturityDate = date;
            // subtract the maturity date from the issuance date and mod 5 to find & set the enum
            long years = ChronoUnit.YEARS.between(issuanceDate, date);
            if (years < 5) {
                this.maturity = Maturities.LESS_THAN_FIVE;
            } else if (years < 10) {
                this.maturity = Maturities.FIVE_YEARS;
            } else if (years < 20) {
                this.maturity = Maturities.TEN_YEARS;
            } else {
                this.maturity = Maturities.TWENTY_YEARS;
            }
        }

        public void setIssuance(String issuanceDate) {
            if (issuanceDate.equals("#N/A N/A")) {
                return; // error case; used for debugging
            }

            LocalDate date = getLocalDate(issuanceDate);
            //set the issuanceDate field
            this.issuanceDate = date;
            // get the year and set the issuanceYear field
            this.issuanceYear = String.valueOf(date.getYear());
            // get the term of year and set the issuanceTerm field
            int term = date.get(IsoFields.QUARTER_OF_YEAR);
            switch (term) {
                case 1:
                    this.issuanceTerm = Issuance.FIRST_Q;
                    break;
                case 2:
                    this.issuanceTerm = Issuance.SECOND_Q;
                    break;
                case 3:
                    this.issuanceTerm = Issuance.THIRD_Q;
                    break;
                case 4:
                    this.issuanceTerm = Issuance.FORTH_Q;
                    break;
                default:
                    System.out.println("Wrong issuance term");
                    break;
            }
        }

        private LocalDate getLocalDate(String dateStr) {
            LocalDate date;
            if (dateStr.length() > 8) {
                if (dateStr.indexOf('/') != -1) {
                    // case of dd/MM/yyyy
                    date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d/M/y"));
                } else {
                    // we have an API response, format is yyyy-MM-dd
                    date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("y-M-d"));
                }
            } else {
                // case of MM/dd/yy
                date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yy"));
            }
            return date;
        }
    }

    /** Maturities and issuance quarter enums **/
    public enum Maturities {
        LESS_THAN_FIVE,
        FIVE_YEARS,
        TEN_YEARS,
        TWENTY_YEARS,
        PERPETUAL
    }
    public enum Issuance {
        FIRST_Q,
        SECOND_Q,
        THIRD_Q,
        FORTH_Q
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        BloombergMain bloombergMain = new BloombergMain();
        bloombergMain.readFromExcelGreen("\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\green_bonds_central-south_america.xlsx", "\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\green_bonds_europe_EUR.xlsx", "\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\green_bonds_north_america.xlsx", "\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\green_bonds_africa.xlsx", "\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\green_european_non-EUR.xlsx");
        bloombergMain.readFromExcelConventional("\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\bonds_isin_1.xlsx");
        bloombergMain.readFromExcelConventional("\\\\icnas3.cc.ic.ac.uk\\pp419\\downloads\\conventional_bonds_FIGI.xlsx");
        bloombergMain.startSession();
        System.out.println("started matching");
        bloombergMain.match();
        System.out.println("API calls made = " + bloombergMain.apiCallsMade);
        bloombergMain.session.stop();
    }
}
