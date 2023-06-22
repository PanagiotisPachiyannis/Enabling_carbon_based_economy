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
package com.bloomberglp.blpapiexamples.demoapps.util.argparser;

/** Defines how many values can be present in a command line for an {@link Arg}. */
public enum ArgMode {
    /**
     * Indicates this is a boolean type argument that does not require a value, and can only be
     * specified once.
     */
    NO_VALUE,

    /** Indicates this is an argument that requires a value, and can only be specified once. */
    SINGLE_VALUE,

    /**
     * Indicates this is a boolean argument, can be specified multiple times. e.g.
     *
     * <pre>
     * -verbose
     * </pre>
     */
    MULTIPLE_NO_VALUE,

    /**
     * Indicates this is an argument that requires a value and can be specified multiple times, e.g.
     *
     * <pre>
     * -f BID -f ASK
     * </pre>
     */
    MULTIPLE_VALUES;

    public boolean allowMultipleValues() {
        return this == MULTIPLE_NO_VALUE || this == MULTIPLE_VALUES;
    }

    public boolean requiresValue() {
        return this == SINGLE_VALUE || this == MULTIPLE_VALUES;
    }
}
