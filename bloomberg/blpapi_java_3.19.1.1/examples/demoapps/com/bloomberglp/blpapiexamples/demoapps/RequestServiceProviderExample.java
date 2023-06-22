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

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventFormatter;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.ProviderEventHandler;
import com.bloomberglp.blpapi.ProviderSession;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;

public class RequestServiceProviderExample {

    private static final String SERVICE = "//example/refdata";
    private static final Name REFERENCE_DATA_REQUEST = Name.getName("ReferenceDataRequest");
    private static final Name TIMESTAMP = Name.getName("timestamp");
    private static final Name FIELD_DATA = Name.getName("fieldData");
    private static final Name FIELD_ID = Name.getName("fieldId");
    private static final Name FIELDS = Name.getName("fields");

    private static final Name SECURITY = Name.getName("security");
    private static final Name SECURITIES = Name.getName("securities");
    private static final Name SECURITY_DATA = Name.getName("securityData");

    private static final Name DATA = Name.getName("data");
    private static final Name DOUBLE_VALUE = Name.getName("doubleValue");

    private static class ServerEventHandler implements ProviderEventHandler {

        @Override
        public void processEvent(Event event, ProviderSession session) {
            System.out.println("Received event " + event);
            EventType eventType = event.eventType();
            if (eventType == EventType.REQUEST) {
                for (Message msg : event) {
                    if (msg.messageType().equals(REFERENCE_DATA_REQUEST)) {
                        Service service = session.getService(SERVICE);
                        if (msg.hasElement(TIMESTAMP)) {
                            double requestTime = msg.getElementAsFloat64(TIMESTAMP);
                            double latency = getTimestamp() - requestTime;
                            System.out.format("Request latency = %.4f\n", latency);
                        }

                        // A response event must contain only one response
                        // message and attach the correlation ID of the request
                        // message.
                        Event response = service.createResponseEvent(msg.correlationID());
                        EventFormatter ef = new EventFormatter(response);

                        // The parameter of EventFormatter.appendResponse(Name)
                        // is the name of the operation instead of the response.
                        ef.appendResponse(REFERENCE_DATA_REQUEST);
                        Element securitiesElement = msg.getElement(SECURITIES);
                        Element fieldsElement = msg.getElement(FIELDS);
                        ef.setElement(TIMESTAMP, getTimestamp());

                        ef.pushElement(SECURITY_DATA);
                        for (int i = 0; i < securitiesElement.numValues(); ++i) {
                            ef.appendElement();
                            ef.setElement(SECURITY, securitiesElement.getValueAsString(i));
                            ef.pushElement(FIELD_DATA);
                            for (int j = 0; j < fieldsElement.numValues(); ++j) {
                                ef.appendElement();
                                ef.setElement(FIELD_ID, fieldsElement.getValueAsString(j));
                                ef.pushElement(DATA);
                                ef.setElement(DOUBLE_VALUE, getTimestamp());
                                ef.popElement();
                                ef.popElement();
                            }

                            ef.popElement();
                            ef.popElement();
                        }

                        ef.popElement();

                        System.out.println("Publishing Response: " + response);
                        session.sendResponse(response);
                    }
                }

                System.out.println("Waiting for requests..., Press ENTER to quit");
            }
        }
    }

    public static void run(String[] args)
            throws InterruptedException, TlsInitializationException, IOException {
        ArgParser argParser =
                new ArgParser(
                        "Request Service Provider Example, to be used in conjunction with "
                                + RequestServiceConsumerExample.class.getSimpleName(),
                        RequestServiceProviderExample.class);
        ConnectionAndAuthOptions connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        try {
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        ProviderSession providerSession =
                new ProviderSession(sessionOptions, new ServerEventHandler());
        try {
            if (!providerSession.start()) {
                System.err.println("Failed to start session");
                return;
            }

            if (!providerSession.registerService(SERVICE, providerSession.getSessionIdentity())) {
                System.err.println("Failed to register " + SERVICE);
                return;
            }

            System.out.println("Service is registered successfully");
            System.out.println("Waiting for requests..., Press ENTER to quit");
            System.in.read();
        } finally {
            providerSession.stop();
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
