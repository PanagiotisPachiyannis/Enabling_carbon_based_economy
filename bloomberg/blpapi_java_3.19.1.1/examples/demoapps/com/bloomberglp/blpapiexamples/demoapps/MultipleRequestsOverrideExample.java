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

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapiexamples.demoapps.snippets.requestresponse.ReferenceDataRequests;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.RequestOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.RequestOptions.Override;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;
import java.util.Arrays;

public class MultipleRequestsOverrideExample {
    public static void main(String[] args) {
        try {
            run(args);
            System.out.println("Press ENTER to quit");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void run(String[] args)
            throws IOException, InterruptedException, TlsInitializationException {
        ArgParser argParser =
                new ArgParser(
                        "Multiple requests with override example",
                        MultipleRequestsOverrideExample.class);
        ConnectionAndAuthOptions connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        try {
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return;
        }

        Session session = new Session(connectionAndAuthOptions.createSessionOption());
        try {
            if (!session.start()) {
                System.err.println("Failed to connect!");
                return;
            }

            if (!session.openService(RequestOptions.REFDATA_SERVICE)) {
                System.err.println("Failed to open " + RequestOptions.REFDATA_SERVICE);
                return;
            }

            final Service refDataService = session.getService(RequestOptions.REFDATA_SERVICE);
            RequestOptions options = new RequestOptions();
            options.securities.addAll(Arrays.asList("IBM US Equity", "MSFT US Equity"));
            options.fields.addAll(Arrays.asList("PX_LAST", "DS002"));
            String fieldIdVwapStartTime = "VWAP_START_TIME";
            String fieldIdVwapEndTime = "VWAP_END_TIME";

            // Request 1
            String startTime1 = "9:30";
            String endTime1 = "11:30";

            options.overrides.addAll(
                    Arrays.asList(
                            new Override(fieldIdVwapStartTime, startTime1),
                            new Override(fieldIdVwapEndTime, endTime1)));

            Request request1 = ReferenceDataRequests.createRequest(refDataService, options);

            System.out.println("Sending request 1: " + request1);
            CorrelationID correlationId1 = new CorrelationID("request 1");
            session.sendRequest(request1, correlationId1);

            // Request 2
            String startTime2 = "11:30";
            String endTime2 = "13:30";

            options.overrides.clear();
            options.overrides.addAll(
                    Arrays.asList(
                            new Override(fieldIdVwapStartTime, startTime2),
                            new Override(fieldIdVwapEndTime, endTime2)));

            Request request2 = ReferenceDataRequests.createRequest(refDataService, options);

            System.out.println("Sending request 2: " + request2);
            CorrelationID correlationId2 = new CorrelationID("request 2");
            session.sendRequest(request2, correlationId2);

            // Wait for responses for both requests, expect 2 final responses
            // either failure or success.
            int finalResponseCount = 0;
            while (finalResponseCount < 2) {
                Event event = session.nextEvent();
                EventType eventType = event.eventType();
                for (Message msg : event) {
                    CorrelationID msgCorrelationId = msg.correlationID();
                    if (eventType == EventType.REQUEST_STATUS) {
                        if (msg.messageType().equals(Names.REQUEST_FAILURE)) {
                            if (correlationId1.equals(msgCorrelationId)) {
                                System.out.println("Request 1 failed.");
                            } else if (correlationId2.equals(msgCorrelationId)) {
                                System.out.println("Request 2 failed.");
                            }

                            ++finalResponseCount;
                        }
                    } else if (eventType == EventType.RESPONSE
                            || eventType == EventType.PARTIAL_RESPONSE) {

                        if (correlationId1.equals(msgCorrelationId)) {
                            System.out.println("Received response for request 1");
                        } else if (correlationId2.equals(msgCorrelationId)) {
                            System.out.println("Received response for request 2");
                        }

                        if (eventType == Event.EventType.RESPONSE) {
                            ++finalResponseCount;
                        }
                    }

                    System.out.println(msg);
                }
            }
        } finally {
            session.stop();
        }
    }
}
