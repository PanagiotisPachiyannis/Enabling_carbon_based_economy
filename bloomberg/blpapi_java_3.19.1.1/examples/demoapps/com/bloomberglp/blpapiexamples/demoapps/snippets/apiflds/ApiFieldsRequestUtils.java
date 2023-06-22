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
import com.bloomberglp.blpapi.Name;

public class ApiFieldsRequestUtils {
    private static final int ID_LEN = 13;
    private static final int MNEMONIC_LEN = 36;
    private static final int DESC_LEN = 40;
    private static final String PADDING = "                                            ";
    private static final Name FIELD_ID = new Name("id");
    private static final Name FIELD_MNEMONIC = new Name("mnemonic");
    private static final Name FIELD_DESC = new Name("description");
    private static final Name FIELD_INFO = new Name("fieldInfo");
    private static final Name FIELD_ERROR = new Name("fieldError");
    private static final Name FIELD_MSG = new Name("message");

    public static String padString(String str, int width) {
        if (str.length() >= width || str.length() >= PADDING.length()) {
            return str;
        }

        return str + PADDING.substring(0, width - str.length());
    }

    public static void printHeader() {
        System.out.println(
                padString("FIELD ID", ID_LEN)
                        + padString("MNEMONIC", MNEMONIC_LEN)
                        + padString("DESCRIPTION", DESC_LEN));
        System.out.println(
                padString("-----------", ID_LEN)
                        + padString("-----------", MNEMONIC_LEN)
                        + padString("-----------", DESC_LEN));
    }

    public static void printField(Element field) {
        String fieldId = field.getElementAsString(FIELD_ID);
        if (field.hasElement(FIELD_INFO)) {
            Element fldInfo = field.getElement(FIELD_INFO);
            String fieldMnemonic = fldInfo.getElementAsString(FIELD_MNEMONIC);
            String fieldDesc = fldInfo.getElementAsString(FIELD_DESC);

            System.out.println(
                    padString(fieldId, ID_LEN)
                            + padString(fieldMnemonic, MNEMONIC_LEN)
                            + padString(fieldDesc, DESC_LEN));
        } else {
            Element fieldError = field.getElement(FIELD_ERROR);
            String fieldDesc = fieldError.getElementAsString(FIELD_MSG);

            System.out.println("\n ERROR: " + fieldId + " - " + fieldDesc);
        }
    }
}
