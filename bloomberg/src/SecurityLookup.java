//import com.bloomberglp.blpapi.CorrelationID;
//import com.bloomberglp.blpapi.Event;
//import com.bloomberglp.blpapi.Message;
//import com.bloomberglp.blpapi.Name;
//import com.bloomberglp.blpapi.Names;
//import com.bloomberglp.blpapi.Request;
//import com.bloomberglp.blpapi.Service;
//import com.bloomberglp.blpapi.Session;
//import com.bloomberglp.blpapi.SessionOptions;
//import com.bloomberglp.blpapi.Event.EventType;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.CurveListRequests;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.GovtListRequests;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.InstrumentListRequests;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.InstrumentsFilter;
//import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
//import com.bloomberglp.blpapiexamples.demoapps.util.argparser.Arg;
//import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
//import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
//import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//
//public class SecurityLookup {
//    private static final Name ERROR_RESPONSE = Name.getName("ErrorResponse");
//    private static final Name INSTRUMENT_LIST_RESPONSE = Name.getName("InstrumentListResponse");
//    private static final Name CURVE_LIST_RESPONSE = Name.getName("CurveListResponse");
//    private static final Name GOVT_LIST_RESPONSE = Name.getName("GovtListResponse");
//    private static final String INSTRUMENT_LIST_REQUEST = "instrumentListRequest";
//    private static final String CURVE_LIST_REQUEST = "curveListRequest";
//    private static final String GOVT_LIST_REQUEST = "govtListRequest";
//    private static final String INSTRUMENT_SERVICE = "//blp/instruments";
//    private static final String[] FILTERS_INSTRUMENTS = new String[]{"yellowKeyFilter", "languageOverride"};
//    private static final String[] FILTERS_GOVT = new String[]{"ticker", "partialMatch"};
//    private static final String[] FILTERS_CURVE = new String[]{"countryCode", "currencyCode", "type", "subtype", "curveid", "bbgid"};
//    private ConnectionAndAuthOptions connectionAndAuthOptions;
//    private String query;
//    private String requestType;
//    private int maxResults;
//    private List<InstrumentsFilter> filters = new ArrayList();
//
//    public SecurityLookup() {
//    }
//
//    private boolean parseCommandLine(String[] args) {
//        ArgParser argParser = new ArgParser("Security Lookup Example", SecurityLookup.class);
//        this.connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
//
//        try {
//            ArgGroup argGroupLookup = new ArgGroup("Security Lookup Options", new Arg[0]);
//            argGroupLookup.add(new String[]{"-r", "--request"}).setMetaVar("requestType").setDescription("specify the request type").setDefaultValue("instrumentListRequest").setChoices(new String[]{"instrumentListRequest", "curveListRequest", "govtListRequest"}).setAction((value) -> {
//                this.requestType = value;
//            });
//            argGroupLookup.add(new String[]{"-S", "--security"}).setMetaVar("security").setDescription("security query string").setDefaultValue("US458140CA64").setAction((value) -> {
//                this.query = value;
//            });
//            argGroupLookup.add(new String[]{"--max-results"}).setMetaVar("maxResults").setDescription("max results returned in the response").setDefaultValue("10").setAction((value) -> {
//                this.maxResults = Integer.parseInt(value);
//            });
//            String eol = System.lineSeparator();
//            argGroupLookup.add(new String[]{"-F", "--filter"}).setMetaVar("<filter>=<value>").setMode(ArgMode.MULTIPLE_VALUES).setDescription("filter and value separated by '=', e.g., countryCode=US" + eol + "The applicable filters for each request:" + eol + "instrumentListRequest" + ": " + Arrays.toString(FILTERS_INSTRUMENTS) + eol + "curveListRequest" + ": " + Arrays.toString(FILTERS_CURVE) + eol + "govtListRequest" + ": " + Arrays.toString(FILTERS_GOVT)).setDefaultValue("yellowKeyFilter=YK_FILTER_CORP").setAction((value) -> {
//                String[] tokens = value.split("=");
//                if (tokens.length != 2) {
//                    throw new IllegalArgumentException("Invalid filter option " + value);
//                } else {
//                    this.filters.add(new InstrumentsFilter(Name.getName(tokens[0]), tokens[1]));
//                }
//            });
//            argParser.addGroup(argGroupLookup);
//            argParser.parse(args);
//            return true;
//        } catch (Exception var5) {
//            System.err.println("Failed to parse arguments: " + var5.getMessage());
//            argParser.printHelp();
//            return false;
//        }
//    }
//
//    private static void processResponseEvent(Event event) {
//        Iterator var1 = event.iterator();
//
//        while(var1.hasNext()) {
//            Message msg = (Message)var1.next();
//            Name msgType = msg.messageType();
//            if (msgType.equals(ERROR_RESPONSE)) {
//                System.out.println("Received error: " + msg);
//            } else if (msgType.equals(INSTRUMENT_LIST_RESPONSE)) {
//                InstrumentListRequests.processResponse(msg);
//            } else if (msgType.equals(CURVE_LIST_RESPONSE)) {
//                CurveListRequests.processResponse(msg);
//            } else if (msgType.equals(GOVT_LIST_RESPONSE)) {
//                GovtListRequests.processResponse(msg);
//            } else {
//                System.err.println("Unknown message received: " + msgType);
//            }
//        }
//
//    }
//
//    private static void waitForResponse(Session session) throws InterruptedException {
//        boolean done = false;
//
//        while(true) {
//            label39:
//            while(!done) {
//                Event event = session.nextEvent();
//                EventType eventType = event.eventType();
//                if (eventType == EventType.PARTIAL_RESPONSE) {
//                    System.out.println("Processing Partial Response");
//                    processResponseEvent(event);
//                } else if (eventType == EventType.RESPONSE) {
//                    System.out.println("Processing Response");
//                    processResponseEvent(event);
//                    done = true;
//                } else {
//                    Iterator var4 = event.iterator();
//
//                    Name msgType;
//                    do {
//                        Message msg;
//                        do {
//                            if (!var4.hasNext()) {
//                                continue label39;
//                            }
//
//                            msg = (Message)var4.next();
//                            System.out.println(msg);
//                        } while(eventType != EventType.SESSION_STATUS);
//
//                        msgType = msg.messageType();
//                    } while(msgType != Names.SESSION_TERMINATED && msgType != Names.SESSION_STARTUP_FAILURE);
//
//                    done = true;
//                }
//            }
//
//            return;
//        }
//    }
//
//    private void sendRequest(Session session) throws IOException {
//        Service instrumentsService = session.getService("//blp/instruments");
//        String var4 = this.requestType;
//        byte var5 = -1;
//        switch(var4.hashCode()) {
//            case -1841195102:
//                if (var4.equals("curveListRequest")) {
//                    var5 = 1;
//                }
//                break;
//            case 977611178:
//                if (var4.equals("instrumentListRequest")) {
//                    var5 = 0;
//                }
//                break;
//            case 984108683:
//                if (var4.equals("govtListRequest")) {
//                    var5 = 2;
//                }
//        }
//
//        Request request;
//        switch(var5) {
//            case 0:
//                request = InstrumentListRequests.createRequest(instrumentsService, this.query, this.maxResults, this.filters);
//                break;
//            case 1:
//                request = CurveListRequests.createRequest(instrumentsService, this.query, this.maxResults, this.filters);
//                break;
//            case 2:
//                request = GovtListRequests.createRequest(instrumentsService, this.query, this.maxResults, this.filters);
//                break;
//            default:
//                throw new IllegalArgumentException("Unknown request " + this.requestType);
//        }
//
//        System.out.println("Sending request: " + request);
//        session.sendRequest(request, (CorrelationID)null);
//    }
//
//    private void run(String[] args) throws Exception {
//        if (this.parseCommandLine(args)) {
//            SessionOptions sessionOptions = this.connectionAndAuthOptions.createSessionOption();
//            Session session = new Session(sessionOptions);
//
//            try {
//                if (session.start()) {
//                    if (!session.openService("//blp/instruments")) {
//                        System.err.println("Failed to open //blp/instruments");
//                        return;
//                    }
//
//                    this.sendRequest(session);
//                    waitForResponse(session);
//                    return;
//                }
//
//                System.err.println("Failed to start session.");
//            } finally {
//                session.stop();
//            }
//
//        }
//    }
//
//    public static void main(String[] args) {
//        SecurityLookup example = new SecurityLookup();
//        try {
//            example.run(args);
//        } catch (Exception var3) {
//            var3.printStackTrace();
//        }
//
//    }
//}
//
