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

public class CategorizedFieldSearchRequests {
    private static final String CATEGORIZED_FIELD_SEARCH_REQUEST = "CategorizedFieldSearchRequest";

    private static final int CAT_NAME_LEN = 40;

    private static final Name SEARCH_SPEC = new Name("searchSpec");
    private static final Name EXCLUDE = new Name("exclude");
    private static final Name FIELD_TYPE = new Name("fieldType");
    private static final Name STATIC = new Name("Static");
    private static final Name CATEGORY = new Name("category");
    private static final Name CATEGORY_NAME = new Name("categoryName");
    private static final Name CATEGORY_ID = new Name("categoryId");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name RETURN_FIELD_DOCUMENTATION = new Name("returnFieldDocumentation");
    private static final Name FIELD_SEARCH_ERROR = new Name("fieldSearchError");

    public static Request createRequest(final Service apifldsService) {
        Request request = apifldsService.createRequest(CATEGORIZED_FIELD_SEARCH_REQUEST);
        request.set(SEARCH_SPEC, "last price");
        Element exclude = request.getElement(EXCLUDE);
        exclude.setElement(FIELD_TYPE, STATIC);
        request.set(RETURN_FIELD_DOCUMENTATION, false);

        return request;
    }

    public static void processResponse(final Event event) {
        for (Message msg : event) {
            if (msg.hasElement(FIELD_SEARCH_ERROR)) {
                System.out.println(msg);
                continue;
            }

            Element categories = msg.getElement(CATEGORY);
            int numCategories = categories.numValues();

            for (int catIdx = 0; catIdx < numCategories; ++catIdx) {
                Element category = categories.getValueAsElement(catIdx);
                String name = category.getElementAsString(CATEGORY_NAME);
                String id = category.getElementAsString(CATEGORY_ID);

                System.out.println(
                        "\nCategory Name:"
                                + ApiFieldsRequestUtils.padString(name, CAT_NAME_LEN)
                                + "\tId:"
                                + id);
                ApiFieldsRequestUtils.printHeader();

                Element fields = category.getElement(FIELD_DATA);
                for (int i = 0; i < fields.numValues(); ++i) {
                    ApiFieldsRequestUtils.printField(fields.getValueAsElement(i));
                }
            }
        }
    }
}
