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

import com.bloomberglp.blpapi.AuthOptions;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapiexamples.demoapps.snippets.requestresponse.HistoricalDataRequests;
import com.bloomberglp.blpapiexamples.demoapps.snippets.requestresponse.IntradayBarRequests;
import com.bloomberglp.blpapiexamples.demoapps.snippets.requestresponse.IntradayTickRequests;
import com.bloomberglp.blpapiexamples.demoapps.snippets.requestresponse.ReferenceDataRequests;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.RequestOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;

public class RequestResponseExample {

    private static final Name SERVICE_NAME = Name.getName("serviceName");
    private static final Name REASON = Name.getName("reason");

    private ConnectionAndAuthOptions connectionAndAuthOptions;
    private RequestOptions requestOptions;

    public static void main(String[] args) {
        RequestResponseExample example = new RequestResponseExample();

        try {
            example.run(args);

            System.out.println("Press ENTER to quit");
            System.in.read();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void run(String[] args)
            throws InterruptedException, IOException, TlsInitializationException {
        if (!parseCommandLine(args)) {
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        Session session = new Session(sessionOptions);
        try {
            if (!session.start()) {
                System.err.println("Failed to start session.");
                checkFailures(session);
                return;
            }

            if (!session.openService(requestOptions.service)) {
                checkFailures(session);
                return;
            }

            sendRequest(session);
            waitForResponse(session);
        } finally {
            session.stop();
        }
    }

    /**
     * Prints RequestId associated with a {@link Message}.
     *
     * <p>{@link Message} can have an associated RequestId that is used to identify the operation
     * through the network. When contacting support please provide the RequestId.
     */
    private static void printContactSupportMessage(Message message) {
        String requestId = message.getRequestId();
        if (requestId != null) {
            System.err.println(
                    "When contacting support, " + "please provide RequestId " + requestId + '.');
        }
    }

    /**
     * Prints the messages in the {@code event}.
     *
     * <p>When using a session identity, i.e. {@link
     * SessionOptions#setSessionIdentityOptions(AuthOptions)}, token generation failure,
     * authorization failure or revocation terminates the session, in which case, applications only
     * need to check session status messages. Applications don't need to handle token or
     * authorization messages, which are still printed.
     *
     * @return {@code true} if session has failed to start or terminated; {@code false} otherwise.
     */
    private static boolean processGenericEvent(Event event) {
        EventType eventType = event.eventType();
        for (Message msg : event) {
            System.out.println(msg);

            Name messageType = msg.messageType();
            if (eventType == EventType.SESSION_STATUS) {
                if (messageType.equals(Names.SESSION_TERMINATED)
                        || messageType.equals(Names.SESSION_STARTUP_FAILURE)) {
                    System.err.println("Session failed to start or terminated.");
                    printContactSupportMessage(msg);
                    return true;
                }
            } else if (eventType == EventType.SERVICE_STATUS) {
                if (messageType.equals(Names.SERVICE_OPEN_FAILURE)) {
                    String serviceName = msg.getElementAsString(SERVICE_NAME);
                    System.err.println("Failed to open " + serviceName + ".");

                    printContactSupportMessage(msg);
                }
            }
        }

        return false;
    }

    /**
     * Checks failure events published by the session.
     *
     * <p>Note that the loop uses {@link Session#tryNextEvent} as all events have been produced
     * before calling this function, but there could be no events at all in the queue if the OS
     * fails to allocate resources.
     */
    private static void checkFailures(Session session) {
        while (true) {
            Event event = session.tryNextEvent();
            if (event == null) {
                break;
            }

            if (processGenericEvent(event)) {
                break;
            }
        }
    }

    /**
     * Waits for response after sending the request.
     *
     * <p>Success response can come with a number of {@link EventType#PARTIAL_RESPONSE} events
     * followed by a {@link EventType#RESPONSE} event. Failures will be delivered in a {@link
     * EventType#REQUEST_STATUS} event holding a {@link Names#REQUEST_FAILURE} message.
     */
    private void waitForResponse(Session session) throws InterruptedException {
        boolean done = false;
        while (!done) {
            Event event = session.nextEvent();
            EventType eventType = event.eventType();
            if (eventType == EventType.PARTIAL_RESPONSE) {
                System.out.println("Processing Partial Response");
                processResponseEvent(event);
            } else if (eventType == EventType.RESPONSE) {
                System.out.println("Processing Response");
                processResponseEvent(event);
                done = true;
            } else if (eventType == EventType.REQUEST_STATUS) {
                for (Message msg : event) {
                    System.out.println(msg);

                    if (msg.messageType().equals(Names.REQUEST_FAILURE)) {
                        Element reason = msg.getElement(REASON);
                        System.err.println("Request failed: " + reason);
                        printContactSupportMessage(msg);
                        done = true;
                    }
                }
            } else {

                // SESSION_STATUS events can happen at any time and should be
                // handled as the session can be terminated, e.g.
                // session identity can be revoked at a later time, which
                // terminates the session.
                done = processGenericEvent(event);
            }
        }
    }

    /** Processes a response to the request. */
    private void processResponseEvent(Event event) {
        switch (requestOptions.requestType) {
            case RequestOptions.INTRADAY_BAR_REQUEST:
                IntradayBarRequests.processResponseEvent(event);
                break;
            case RequestOptions.INTRADAY_TICK_REQUEST:
                IntradayTickRequests.processResponseEvent(event);
                break;
            case RequestOptions.REFERENCE_DATA_REQUEST:
            case RequestOptions.REFERENCE_DATA_REQUEST_OVERRIDE:
            case RequestOptions.REFERENCE_DATA_REQUEST_TABLE_OVERRIDE:
                ReferenceDataRequests.processResponseEvent(event);
                break;
            case RequestOptions.HISTORICAL_DATA_REQUEST:
                HistoricalDataRequests.processResponseEvent(event);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown request type: " + requestOptions.requestType);
        }
    }

    /** Sends a request based on the request type. */
    private void sendRequest(Session session) throws IOException {
        Service service = session.getService(requestOptions.service);
        Request request;
        switch (requestOptions.requestType) {
            case RequestOptions.INTRADAY_BAR_REQUEST:
                request = IntradayBarRequests.createRequest(service, requestOptions);
                break;
            case RequestOptions.INTRADAY_TICK_REQUEST:
                request = IntradayTickRequests.createRequest(service, requestOptions);
                break;
            case RequestOptions.REFERENCE_DATA_REQUEST:
            case RequestOptions.REFERENCE_DATA_REQUEST_OVERRIDE:
                request = ReferenceDataRequests.createRequest(service, requestOptions);
                break;
            case RequestOptions.REFERENCE_DATA_REQUEST_TABLE_OVERRIDE:
                request = ReferenceDataRequests.createTableOverrideRequest(service, requestOptions);
                break;
            case RequestOptions.HISTORICAL_DATA_REQUEST:
                request = HistoricalDataRequests.createRequest(service, requestOptions);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown request type: " + requestOptions.requestType);
        }

        // Every request has a RequestId, which is automatically generated, and
        // used to identify the operation through the network and also present
        // in the response messages. The RequestId should be provided when
        // contacting support.
        System.out.println("Sending Request " + request.getRequestId() + ": " + request);
        session.sendRequest(request, null); // correlationId
    }

    private boolean parseCommandLine(String[] args) {

        ArgParser argParser =
                new ArgParser("Request/Response Example", RequestResponseExample.class);

        connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        requestOptions = new RequestOptions(argParser);

        try {
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        // handle default arguments
        requestOptions.setDefaultValues();

        return true;
    }
}
