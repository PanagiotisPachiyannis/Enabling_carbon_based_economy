/*
 * Copyright 2021, Bloomberg Finance L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:  The above
 * copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.bloomberglp.blpapiexamples.demoapps;

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.CurveListRequests;
import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.GovtListRequests;
import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.InstrumentListRequests;
import com.bloomberglp.blpapiexamples.demoapps.snippets.instruments.InstrumentsFilter;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecurityLookupExample {
    private static final Name ERROR_RESPONSE = Name.getName("ErrorResponse");
    private static final Name INSTRUMENT_LIST_RESPONSE = Name.getName("InstrumentListResponse");
    private static final Name CURVE_LIST_RESPONSE = Name.getName("CurveListResponse");
    private static final Name GOVT_LIST_RESPONSE = Name.getName("GovtListResponse");

    private static final String INSTRUMENT_LIST_REQUEST = "instrumentListRequest";
    private static final String CURVE_LIST_REQUEST = "curveListRequest";
    private static final String GOVT_LIST_REQUEST = "govtListRequest";

    private static final String INSTRUMENT_SERVICE = "//blp/instruments";

    private static final String[] FILTERS_INSTRUMENTS = {"yellowKeyFilter", "languageOverride"};

    private static final String[] FILTERS_GOVT = {"ticker", "partialMatch"};

    private static final String[] FILTERS_CURVE = {
        "countryCode", "currencyCode", "type", "subtype", "curveid", "bbgid"
    };

    private ConnectionAndAuthOptions connectionAndAuthOptions;
    private String query;
    private String requestType;
    private int maxResults;
    private List<InstrumentsFilter> filters = new ArrayList<>();

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser = new ArgParser("Security Lookup Example", SecurityLookupExample.class);
        connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        try {
            ArgGroup argGroupLookup = new ArgGroup("Security Lookup Options");
            argGroupLookup
                    .add("-r", "--request")
                    .setMetaVar("requestType")
                    .setDescription("specify the request type")
                    .setDefaultValue(INSTRUMENT_LIST_REQUEST)
                    .setChoices(INSTRUMENT_LIST_REQUEST, CURVE_LIST_REQUEST, GOVT_LIST_REQUEST)
                    .setAction(value -> requestType = value);
            argGroupLookup
                    .add("-S", "--security")
                    .setMetaVar("security")
                    .setDescription("security query string")
                    .setDefaultValue("Thames Water Utilities Ltd")
                    .setAction(value -> query = value);
            argGroupLookup
                    .add("--max-results")
                    .setMetaVar("maxResults")
                    .setDescription("max results returned in the response")
                    .setDefaultValue("10")
                    .setAction(value -> maxResults = Integer.parseInt(value));

            String eol = System.lineSeparator();
            argGroupLookup
                    .add("-F", "--filter")
                    .setMetaVar("<filter>=<value>")
                    .setMode(ArgMode.MULTIPLE_VALUES)
                    .setDescription(
                            "filter and value separated by '=', e.g., countryCode=US"
                                    + eol
                                    + "The applicable filters for each request:"
                                    + eol
                                    + INSTRUMENT_LIST_REQUEST
                                    + ": "
                                    + Arrays.toString(FILTERS_INSTRUMENTS)
                                    + eol
                                    + CURVE_LIST_REQUEST
                                    + ": "
                                    + Arrays.toString(FILTERS_CURVE)
                                    + eol
                                    + GOVT_LIST_REQUEST
                                    + ": "
                                    + Arrays.toString(FILTERS_GOVT))
                    .setAction(
                            value -> {
                                String[] tokens = value.split("=");
                                if (tokens.length != 2) {
                                    throw new IllegalArgumentException(
                                            "Invalid filter option " + value);
                                }

                                filters.add(
                                        new InstrumentsFilter(Name.getName(tokens[0]), tokens[1]));
                            });

            argParser.addGroup(argGroupLookup);
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        return true;
    }

    private static void processResponseEvent(Event event) {
        for (Message msg : event) {
            Name msgType = msg.messageType();
            if (msgType.equals(ERROR_RESPONSE)) {
                System.out.println("Received error: " + msg);
            } else if (msgType.equals(INSTRUMENT_LIST_RESPONSE)) {
                InstrumentListRequests.processResponse(msg);
            } else if (msgType.equals(CURVE_LIST_RESPONSE)) {
                CurveListRequests.processResponse(msg);
            } else if (msgType.equals(GOVT_LIST_RESPONSE)) {
                GovtListRequests.processResponse(msg);
            } else {
                System.err.println("Unknown message received: " + msgType);
            }
        }
    }

    private static void waitForResponse(Session session) throws InterruptedException {
        boolean done = false;
        while (!done) {
            Event event = session.nextEvent();
            EventType eventType = event.eventType();
            if (eventType == Event.EventType.PARTIAL_RESPONSE) {
                System.out.println("Processing Partial Response");
                processResponseEvent(event);
            } else if (eventType == Event.EventType.RESPONSE) {
                System.out.println("Processing Response");
                processResponseEvent(event);
                done = true;
            } else {
                for (Message msg : event) {
                    System.out.println(msg);

                    if (eventType == Event.EventType.SESSION_STATUS) {
                        Name msgType = msg.messageType();
                        if (msgType == Names.SESSION_TERMINATED
                                || msgType == Names.SESSION_STARTUP_FAILURE) {
                            done = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private void sendRequest(Session session) throws IOException {
        Service instrumentsService = session.getService(INSTRUMENT_SERVICE);
        Request request;
        switch (requestType) {
            case INSTRUMENT_LIST_REQUEST:
                request =
                        InstrumentListRequests.createRequest(
                                instrumentsService, query, maxResults, filters);
                break;

            case CURVE_LIST_REQUEST:
                request =
                        CurveListRequests.createRequest(
                                instrumentsService, query, maxResults, filters);
                break;

            case GOVT_LIST_REQUEST:
                request =
                        GovtListRequests.createRequest(
                                instrumentsService, query, maxResults, filters);
                break;

            default:
                throw new IllegalArgumentException("Unknown request " + requestType);
        }

        System.out.println("Sending request: " + request);
        session.sendRequest(request, null /* correlationId */);
    }

    private void run(String[] args) throws Exception {
        if (!parseCommandLine(args)) {
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        Session session = new Session(sessionOptions);
        try {
            if (!session.start()) {
                System.err.println("Failed to start session.");
                return;
            }

            if (!session.openService(INSTRUMENT_SERVICE)) {
                System.err.println("Failed to open " + INSTRUMENT_SERVICE);
                return;
            }

            sendRequest(session);
            waitForResponse(session);
        } finally {
            session.stop();
        }
    }

    public static void main(String[] args) {
        SecurityLookupExample example = new SecurityLookupExample();
        try {
            example.run(args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
