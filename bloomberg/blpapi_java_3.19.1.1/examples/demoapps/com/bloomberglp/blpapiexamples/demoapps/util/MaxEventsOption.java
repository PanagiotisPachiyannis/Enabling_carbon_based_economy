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
package com.bloomberglp.blpapiexamples.demoapps.util;

import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;

/**
 * Helper class that adds the option for the maximum number of events before stopping to {@link
 * ArgParser} and parse the command line argument to an integer as the maximum number of events.
 */
public class MaxEventsOption {

    private int maxEvents = Integer.MAX_VALUE;

    public MaxEventsOption(ArgParser argParser) {
        argParser
                .addArg("--max-events")
                .setMetaVar("maxEvents")
                .setDescription("number of events to process before stopping")
                .setDefaultValue(String.valueOf(maxEvents))
                .setAction(value -> maxEvents = Integer.parseInt(value));
    }

    public int getMaxEvents() {
        return maxEvents;
    }
}
