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

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapiexamples.demoapps.util.RequestOptions;

public class HistoricalDataRequests {
    private static final Name SECURITIES = Name.getName("securities");
    private static final Name PERIODICITY_ADJUSTMENT = Name.getName("periodicityAdjustment");
    private static final Name PERIODICITY_SELECTION = Name.getName("periodicitySelection");
    private static final Name START_DATE = Name.getName("startDate");
    private static final Name END_DATE = Name.getName("endDate");
    private static final Name MAX_DATA_POINTS = Name.getName("maxDataPoints");
    private static final Name RETURN_EIDS = Name.getName("returnEids");
    private static final Name FIELDS = Name.getName("fields");

    public static Request createRequest(Service service, RequestOptions options) {
        Request request = service.createRequest("HistoricalDataRequest");

        Element securitiesElement = request.getElement(SECURITIES);
        for (String security : options.securities) {
            securitiesElement.appendValue(security);
        }

        Element fieldsElement = request.getElement(FIELDS);
        for (String field : options.fields) {
            fieldsElement.appendValue(field);
        }

        request.set(PERIODICITY_ADJUSTMENT, "ACTUAL");
        request.set(PERIODICITY_SELECTION, "MONTHLY");
        request.set(START_DATE, "20200101");
        request.set(END_DATE, "20201231");
        request.set(MAX_DATA_POINTS, 100);
        request.set(RETURN_EIDS, true);

        return request;
    }

    public static void processResponseEvent(Event event) {
        for (Message msg : event) {
            System.out.println("Received response to request " + msg.getRequestId());
            System.out.println(msg);
        }
    }
}
