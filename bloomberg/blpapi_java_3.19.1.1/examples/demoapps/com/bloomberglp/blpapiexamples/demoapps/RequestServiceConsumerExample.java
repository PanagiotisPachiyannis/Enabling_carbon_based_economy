/*
 * Copyright 2021, Bloomberg Finance L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright
 * notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
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

import com.bloomberglp.blpapi.CorrelationID;
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
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;

public class RequestServiceConsumerExample {

    private static final String SERVICE = "//example/refdata";
    private static final Name TIMESTAMP = Name.getName("timestamp");
    private static final Name FIELDS = Name.getName("fields");
    private static final Name SECURITIES = Name.getName("securities");

    public static void run(String[] args) throws Exception {
        ArgParser argParser =
                new ArgParser(
                        "Request Service Consumer Example, to be used in conjunction with "
                                + RequestServiceProviderExample.class.getSimpleName(),
                        RequestServiceConsumerExample.class);
        ConnectionAndAuthOptions connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        try {
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        Session session = new Session(sessionOptions);
        try {
            if (!session.start()) {
                System.err.println("Failed to start session");
                return;
            }

            if (!session.openService(SERVICE)) {
                System.err.println("Failed to open " + SERVICE);
                return;
            }

            // Send a request
            Service service = session.getService(SERVICE);
            Request request = service.createRequest("ReferenceDataRequest");

            // Add securities to request
            String[] securities = {"IBM US Equity", "MSFT US Equity"};
            Element securitiesElement = request.getElement(SECURITIES);
            for (String security : securities) {
                securitiesElement.appendValue(security);
            }

            // Add fields to request
            String[] fields = {"PX_LAST", "DS002"};
            Element fieldsElement = request.getElement(FIELDS);
            for (String field : fields) {
                fieldsElement.appendValue(field);
            }

            // Set time stamp
            request.set(TIMESTAMP, getTimestamp());

            System.out.println("Sending Request: " + request);
            session.sendRequest(request, new CorrelationID());

            // Wait for response for the request, expect a final response
            // either failure or success.
            boolean done = false;
            while (!done) {
                Event event = session.nextEvent();
                System.out.println("Received an event: " + event);

                EventType eventType = event.eventType();
                if (eventType == EventType.REQUEST_STATUS) {
                    for (Message msg : event) {
                        if (msg.messageType().equals(Names.REQUEST_FAILURE)) {
                            System.err.println("Request failed!");
                            done = true;
                        }
                    }
                } else if (eventType == EventType.RESPONSE) {
                    for (Message msg : event) {
                        if (msg.hasElement(TIMESTAMP)) {
                            double responseTime = msg.getElementAsFloat64(TIMESTAMP);
                            double latency = getTimestamp() - responseTime;
                            System.out.format("Response latency = %.4f\n", latency);
                        }
                    }

                    done = true;
                }
            }
        } finally {
            session.stop();
        }
    }

    private static double getTimestamp() {
        return ((double) System.nanoTime()) / 1000000000;
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
