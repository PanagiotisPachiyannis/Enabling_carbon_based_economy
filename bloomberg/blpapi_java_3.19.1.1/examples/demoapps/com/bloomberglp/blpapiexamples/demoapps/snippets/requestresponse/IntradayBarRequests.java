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
package com.bloomberglp.blpapiexamples.demoapps.snippets.requestresponse;

import com.bloomberglp.blpapi.Datetime;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapiexamples.demoapps.util.RequestOptions;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

public class IntradayBarRequests {
    private static final Name BAR_DATA = new Name("barData");
    private static final Name BAR_TICK_DATA = new Name("barTickData");
    private static final Name OPEN = new Name("open");
    private static final Name HIGH = new Name("high");
    private static final Name LOW = new Name("low");
    private static final Name CLOSE = new Name("close");
    private static final Name VOLUME = new Name("volume");
    private static final Name NUM_EVENTS = new Name("numEvents");
    private static final Name TIME = new Name("time");
    private static final Name RESPONSE_ERROR = new Name("responseError");
    private static final Name SECURITY = Name.getName("security");
    private static final Name EVENT_TYPE = Name.getName("eventType");
    private static final Name INTERVAL = Name.getName("interval");
    private static final Name START_DATE_TIME = Name.getName("startDateTime");
    private static final Name END_DATE_TIME = Name.getName("endDateTime");
    private static final Name GAP_FILL_INITIAL_BAR = Name.getName("gapFillInitialBar");

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy k:mm");
    private static final NumberFormat DECIMAL_FORMAT = new DecimalFormat();

    public static Request createRequest(Service service, RequestOptions options) {
        Request request = service.createRequest("IntradayBarRequest");

        // only one security/eventType per request
        request.set(SECURITY, options.securities.get(0));
        request.set(EVENT_TYPE, options.eventTypes.get(0));
        request.set(INTERVAL, options.barInterval);

        request.set(START_DATE_TIME, options.startDateTime);
        request.set(END_DATE_TIME, options.endDateTime);

        if (options.gapFillInitialBar) {
            request.set(GAP_FILL_INITIAL_BAR, options.gapFillInitialBar);
        }

        return request;
    }

    public static void processResponseEvent(Event response) {
        for (Message msg : response) {
            System.out.println("Received response to request " + msg.getRequestId());

            if (msg.hasElement(RESPONSE_ERROR)) {
                Element responseError = msg.getElement(RESPONSE_ERROR);
                System.out.println("REQUEST FAILED: " + responseError);
                continue;
            }

            Element data = msg.getElement(BAR_DATA).getElement(BAR_TICK_DATA);

            int numBars = data.numValues();
            System.out.println("Response contains " + numBars + " bars");

            String header =
                    String.join(
                            "\t\t\t",
                            "Datetime",
                            "Open",
                            "High",
                            "Low",
                            "Close",
                            "NumEvents",
                            "Volume");
            System.out.println(header);
            for (int i = 0; i < numBars; ++i) {
                Element bar = data.getValueAsElement(i);
                Datetime time = bar.getElementAsDate(TIME);
                double open = bar.getElementAsFloat64(OPEN);
                double high = bar.getElementAsFloat64(HIGH);
                double low = bar.getElementAsFloat64(LOW);
                double close = bar.getElementAsFloat64(CLOSE);
                int numEvents = bar.getElementAsInt32(NUM_EVENTS);
                long volume = bar.getElementAsInt64(VOLUME);

                String row =
                        String.join(
                                "\t\t\t",
                                DATE_FORMAT.format(time.calendar().getTime()),
                                DECIMAL_FORMAT.format(open),
                                DECIMAL_FORMAT.format(high),
                                DECIMAL_FORMAT.format(low),
                                DECIMAL_FORMAT.format(close),
                                DECIMAL_FORMAT.format(numEvents),
                                DECIMAL_FORMAT.format(volume));
                System.out.println(row);
            }
        }
    }
}
