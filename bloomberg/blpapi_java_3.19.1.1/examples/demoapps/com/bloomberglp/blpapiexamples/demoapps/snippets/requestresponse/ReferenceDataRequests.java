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
import com.bloomberglp.blpapiexamples.demoapps.util.RequestOptions.Override;

public class ReferenceDataRequests {

    private static final Name SECURITY_DATA = new Name("securityData");
    private static final Name SECURITY = new Name("security");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name RESPONSE_ERROR = new Name("responseError");
    private static final Name SECURITY_ERROR = new Name("securityError");
    private static final Name FIELD_EXCEPTIONS = new Name("fieldExceptions");
    private static final Name FIELD_ID = new Name("fieldId");
    private static final Name ERROR_INFO = new Name("errorInfo");
    private static final Name SECURITIES = new Name("securities");
    private static final Name FIELDS = new Name("fields");
    private static final Name OVERRIDES = new Name("overrides");
    private static final Name VALUE = new Name("value");
    private static final Name TABLE_OVERRIDES = new Name("tableOverrides");
    private static final Name ROW = new Name("row");

    private static class RateVector {
        public final float rate;
        public final int duration;
        public final char transition;

        public RateVector(float rate, int duration, char transition) {
            this.rate = rate;
            this.duration = duration;
            this.transition = transition;
        }
    }

    public static Request createRequest(Service service, RequestOptions options) {
        Request request = service.createRequest("ReferenceDataRequest");

        // Add securities to request
        Element securitiesElement = request.getElement(SECURITIES);
        for (String security : options.securities) {
            securitiesElement.appendValue(security);
        }

        // Add fields to request
        Element fieldsElement = request.getElement(FIELDS);
        for (String field : options.fields) {
            fieldsElement.appendValue(field);
        }

        if (!options.overrides.isEmpty()) {
            // add overrides
            Element overridesElement = request.getElement(OVERRIDES);
            for (Override override : options.overrides) {
                Element overrideElement = overridesElement.appendElement();
                overrideElement.setElement(FIELD_ID, override.fieldId);
                overrideElement.setElement(VALUE, override.value);
            }
        }

        return request;
    }

    public static Request createTableOverrideRequest(Service service, RequestOptions options) {
        Request request = service.createRequest("ReferenceDataRequest");

        // Add securities to request
        Element securitiesElement = request.getElement(SECURITIES);
        for (String security : options.securities) {
            securitiesElement.appendValue(security);
        }

        // Add fields to request
        Element fieldsElement = request.getElement(FIELDS);
        for (String field : options.fields) {
            fieldsElement.appendValue(field);
        }

        // Add scalar overrides to request.
        Element overrides = request.getElement(OVERRIDES);
        Element override1 = overrides.appendElement();
        override1.setElement(FIELD_ID, "ALLOW_DYNAMIC_CASHFLOW_CALCS");
        override1.setElement(VALUE, "Y");
        Element override2 = overrides.appendElement();
        override2.setElement(FIELD_ID, "LOSS_SEVERITY");
        override2.setElement(VALUE, 31);

        // Add table overrides to request.
        Element tableOverrides = request.getElement(TABLE_OVERRIDES);
        Element tableOverride = tableOverrides.appendElement();
        tableOverride.setElement(FIELD_ID, "DEFAULT_VECTOR");
        Element rows = tableOverride.getElement(ROW);

        // Layout of input table is specified by the definition of
        // 'DEFAULT_VECTOR'. Attributes are specified in the first rows.
        // Subsequent rows include rate, duration, and transition.
        Element row = rows.appendElement();
        Element cols = row.getElement(VALUE);
        cols.appendValue("Anchor"); // Anchor type
        cols.appendValue("PROJ"); // PROJ = Projected
        row = rows.appendElement();
        cols = row.getElement(VALUE);
        cols.appendValue("Type"); // Type of default
        cols.appendValue("CDR"); // CDR = Conditional Default Rate

        RateVector[] vectors = {
            new RateVector(1.0f, 12, 'S'), // S = Step
            new RateVector(2.0f, 12, 'R') // R = Ramp
        };

        for (RateVector rateVector : vectors) {
            row = rows.appendElement();
            cols = row.getElement(VALUE);
            cols.appendValue(rateVector.rate);
            cols.appendValue(rateVector.duration);
            cols.appendValue(rateVector.transition);
        }

        return request;
    }

    public static void processResponseEvent(Event event) {
        for (Message msg : event) {
            System.out.println("Received response to request " + msg.getRequestId());

            if (msg.hasElement(RESPONSE_ERROR)) {
                System.out.println("REQUEST FAILED: " + msg.getElement(RESPONSE_ERROR));
                continue;
            }

            Element securities = msg.getElement(SECURITY_DATA);
            int numSecurities = securities.numValues();
            System.out.println("Processing " + numSecurities + " securities:");
            for (int i = 0; i < numSecurities; ++i) {
                Element security = securities.getValueAsElement(i);
                String ticker = security.getElementAsString(SECURITY);
                System.out.println();
                System.out.println("Ticker: " + ticker);
                if (security.hasElement(SECURITY_ERROR)) {
                    System.out.println("SECURITY FAILED: " + security.getElement(SECURITY_ERROR));
                    continue;
                }

                if (security.hasElement(FIELD_DATA)) {
                    Element fields = security.getElement(FIELD_DATA);
                    if (fields.numElements() > 0) {
                        System.out.println("FIELD\t\tVALUE");
                        System.out.println("-----\t\t-----");
                        int numElements = fields.numElements();
                        for (int j = 0; j < numElements; ++j) {
                            Element field = fields.getElement(j);
                            System.out.println(field.name() + "\t\t" + field);
                        }
                    }
                }
                System.out.println();
                Element fieldExceptions = security.getElement(FIELD_EXCEPTIONS);
                if (fieldExceptions.numValues() > 0) {
                    System.out.println("FIELD\t\tEXCEPTION");
                    System.out.println("-----\t\t---------");
                    for (int k = 0; k < fieldExceptions.numValues(); ++k) {
                        Element fieldException = fieldExceptions.getValueAsElement(k);
                        System.out.println(
                                fieldException.getElementAsString(FIELD_ID)
                                        + "\t\t"
                                        + fieldException.getElement(ERROR_INFO));
                    }
                }
            }
        }
    }
}
