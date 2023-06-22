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
package com.bloomberglp.blpapiexamples.demoapps.snippets.apiflds;

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;

public class FieldListRequests {
    private static final String FIELD_LIST_REQUEST = "FieldListRequest";

    private static final Name FIELD_TYPE = new Name("fieldType");
    private static final Name STATIC = new Name("Static");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name RETURN_FIELD_DOCUMENTATION = new Name("returnFieldDocumentation");

    public static Request createRequest(final Service apifldsService) {
        Request request = apifldsService.createRequest(FIELD_LIST_REQUEST);
        request.set(FIELD_TYPE, STATIC);
        request.set(RETURN_FIELD_DOCUMENTATION, false);
        return request;
    }

    public static void processResponse(final Event event) {
        for (Message msg : event) {
            ApiFieldsRequestUtils.printHeader();

            Element fields = msg.getElement(FIELD_DATA);
            for (int i = 0; i < fields.numValues(); i++) {
                ApiFieldsRequestUtils.printField(fields.getValueAsElement(i));
            }
        }
    }
}
