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
package com.bloomberglp.blpapiexamples.demoapps.snippets.instruments;

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.InvalidConversionException;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.NotFoundException;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import java.util.List;

public class GovtListRequests {
    private static final Name NAME_QUERY = Name.getName("query");
    private static final Name NAME_RESULTS = Name.getName("results");
    private static final Name NAME_MAX_RESULTS = Name.getName("maxResults");

    private static final Name NAME_PARSEKY = Name.getName("parseky");
    private static final Name NAME_NAME = Name.getName("name");
    private static final Name NAME_TICKER = Name.getName("ticker");

    public static Request createRequest(
            Service instrumentsService,
            String query,
            int maxResults,
            List<InstrumentsFilter> filters) {
        Request request = instrumentsService.createRequest("govtListRequest");
        request.set(NAME_QUERY, query);
        request.set(NAME_MAX_RESULTS, maxResults);

        for (InstrumentsFilter filter : filters) {
            try {
                request.set(filter.name, filter.value);
            } catch (NotFoundException e) {
                throw new IllegalArgumentException("Filter not found: " + filter.name, e);
            } catch (InvalidConversionException e) {
                throw new IllegalArgumentException(
                        "Invalid value: " + filter.value + " for filter: " + filter.name, e);
            }
        }

        return request;
    }

    public static void processResponse(Message msg) {
        Element results = msg.getElement(NAME_RESULTS);
        int numResults = results.numValues();
        System.out.println("Processing " + numResults + " results:");
        for (int i = 0; i < numResults; ++i) {
            Element result = results.getValueAsElement(i);

            String parsekey = result.getElementAsString(NAME_PARSEKY);
            String name = result.getElementAsString(NAME_NAME);
            String ticker = result.getElementAsString(NAME_TICKER);
            System.out.println("    " + (i + 1) + " " + parsekey + ", " + name + " - " + ticker);
        }
    }
}
