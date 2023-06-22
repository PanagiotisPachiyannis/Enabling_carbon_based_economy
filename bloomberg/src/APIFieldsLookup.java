//import com.bloomberglp.blpapi.CorrelationID;
//import com.bloomberglp.blpapi.Event;
//import com.bloomberglp.blpapi.Message;
//import com.bloomberglp.blpapi.Names;
//import com.bloomberglp.blpapi.Request;
//import com.bloomberglp.blpapi.Service;
//import com.bloomberglp.blpapi.Session;
//import com.bloomberglp.blpapi.SessionOptions;
//import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.apiflds.CategorizedFieldSearchRequests;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.apiflds.FieldInfoRequests;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.apiflds.FieldListRequests;
//import com.bloomberglp.blpapiexamples.demoapps.snippets.apiflds.FieldSearchRequests;
//import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
//import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
//import java.io.IOException;
//
//public class APIFieldsLookup {
//    private static final String APIFLDS_SVC = "//blp/apiflds";
//    private static final String CATEGORIZED_FIELD_SEARCH_REQUEST = "CategorizedFieldSearchRequest";
//    private static final String FIELD_INFO_REQUEST = "FieldInfoRequest";
//    private static final String FIELD_LIST_REQUEST = "FieldListRequest";
//    private static final String FIELD_SEARCH_REQUEST = "FieldSearchRequest";
//
//    private String requestType;
//
//    public static void main(String[] args) {
//        try {
//            new APIFieldsLookup().run(args);
//            System.out.println("Press ENTER to quit");
//            System.in.read();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    private void run(String[] args)
//            throws IOException, InterruptedException, TlsInitializationException {
//        ArgParser argParser = new ArgParser("Find API data fields", APIFieldsLookup.class);
//        ConnectionAndAuthOptions connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
//        try {
//            argParser
//                    .addArg("-r", "--request")
//                    .setMetaVar("requestType")
//                    .setDescription("Specify API fields request type")
//                    .setRequired(true)
//                    .setChoices(
//                            CATEGORIZED_FIELD_SEARCH_REQUEST,
//                            FIELD_INFO_REQUEST,
//                            FIELD_LIST_REQUEST,
//                            FIELD_SEARCH_REQUEST)
//                    .setDefaultValue("FieldInfoRequest")
//                    .setAction(value -> requestType = value);
//            argParser.parse(args);
//        } catch (Exception ex) {
//            System.err.println("Failed to parse arguments: " + ex.getMessage());
//            argParser.printHelp();
//            return;
//        }
//
//        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
//        Session session = new Session(sessionOptions);
//        try {
//            if (!session.start()) {
//                System.err.println("Failed to start session.");
//                return;
//            }
//
//            if (!session.openService(APIFLDS_SVC)) {
//                System.out.println("Failed to open service: " + APIFLDS_SVC);
//                return;
//            }
//
//            Request request = createRequest(session);
//            System.out.println("Sending request: " + request);
//            session.sendRequest(request, new CorrelationID());
//
//            boolean done = false;
//            while (!done) {
//                Event event = session.nextEvent();
//                Event.EventType eventType = event.eventType();
//
//                if (eventType == Event.EventType.REQUEST_STATUS) {
//                    for (Message msg : event) {
//                        if (msg.messageType().equals(Names.REQUEST_FAILURE)) {
//                            // Request has failed, exit.
//                            System.out.println(msg);
//                            done = true;
//                            break;
//                        }
//                    }
//                } else if (eventType == Event.EventType.RESPONSE
//                        || eventType == Event.EventType.PARTIAL_RESPONSE) {
//                    processResponse(event);
//                    System.out.println();
//
//                    if (eventType == Event.EventType.RESPONSE) {
//                        // Received the final response, no further
//                        // response events are expected.
//                        done = true;
//                    }
//                }
//            }
//        } finally {
//            session.stop();
//        }
//    }
//
//    Request createRequest(Session session) {
//        Service apifldsService = session.getService(APIFLDS_SVC);
//        switch (requestType) {
//            case CATEGORIZED_FIELD_SEARCH_REQUEST:
//                return CategorizedFieldSearchRequests.createRequest(apifldsService);
//            case FIELD_INFO_REQUEST:
//                return FieldInfoRequests.createRequest(apifldsService);
//            case FIELD_LIST_REQUEST:
//                return FieldListRequests.createRequest(apifldsService);
//            case FIELD_SEARCH_REQUEST:
//                return FieldSearchRequests.createRequest(apifldsService);
//            default:
//                throw new IllegalArgumentException("Unknown request type: " + requestType);
//        }
//    }
//
//    void processResponse(final Event event) {
//        switch (requestType) {
//            case CATEGORIZED_FIELD_SEARCH_REQUEST:
//                CategorizedFieldSearchRequests.processResponse(event);
//                break;
//            case FIELD_INFO_REQUEST:
//                FieldInfoRequests.processResponse(event);
//                break;
//            case FIELD_LIST_REQUEST:
//                FieldListRequests.processResponse(event);
//                break;
//            case FIELD_SEARCH_REQUEST:
//                FieldSearchRequests.processResponse(event);
//                break;
//            default:
//                throw new IllegalArgumentException("Unknown request type: " + requestType);
//        }
//    }
//}
