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

import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

/**
 * A data structure that holds options for creating a request. When constructed with {@link
 * ArgParser}, it adds the options for creating a request to the argument parser.
 */
public class RequestOptions {

    public static class Override {
        public String fieldId;
        public String value;

        public Override(String fieldId, String value) {
            this.fieldId = fieldId;
            this.value = value;
        }
    }

    public static final String REFDATA_SERVICE = "//blp/refdata";
    public static final String INTRADAY_BAR_REQUEST = "IntradayBarRequest";
    public static final String INTRADAY_TICK_REQUEST = "IntradayTickRequest";
    public static final String REFERENCE_DATA_REQUEST = "ReferenceDataRequest";
    public static final String REFERENCE_DATA_REQUEST_OVERRIDE = "ReferenceDataRequestOverride";
    public static final String REFERENCE_DATA_REQUEST_TABLE_OVERRIDE =
            "ReferenceDataRequestTableOverride";
    public static final String HISTORICAL_DATA_REQUEST = "HistoricalDataRequest";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public String service = REFDATA_SERVICE;
    public final List<String> securities = new ArrayList<>();
    public final List<String> fields = new ArrayList<>();
    public final List<String> eventTypes = new ArrayList<>();
    public final List<Override> overrides = new ArrayList<>();

    public int barInterval = 60;
    public boolean gapFillInitialBar = false;
    public String startDateTime;
    public String endDateTime;
    public boolean includeConditionCodes = false;
    public String requestType = REFERENCE_DATA_REQUEST;

    private String defaultIntradayBarEndDateTime;
    private String defaultIntradayTickEndDateTime;

    public RequestOptions() {
        setDefaultStartAndEndTime();
    }

    public RequestOptions(ArgParser argParser) {
        Objects.requireNonNull(argParser, "argParser must not be null");

        setDefaultStartAndEndTime();

        ArgGroup argGroupRequest = new ArgGroup("Request Options");
        int defaultBarInterval = 5;
        argGroupRequest
                .add("-s", "--service")
                .setDescription("The service name")
                .setMetaVar("service")
                .setDefaultValue(REFDATA_SERVICE)
                .setAction(value -> service = value);

        argGroupRequest
                .add("-S", "--security")
                .setDescription("Security to request")
                .setMetaVar("security")
                .setMode(ArgMode.MULTIPLE_VALUES)
                .setAction(securities::add);

        argGroupRequest
                .add("-f", "--field")
                .setDescription("Field to request")
                .setMetaVar("field")
                .setMode(ArgMode.MULTIPLE_VALUES)
                .setAction(fields::add);

        argGroupRequest
                .add("-e", "--event")
                .setDescription("Event type")
                .setMetaVar("eventType")
                .setMode(ArgMode.MULTIPLE_VALUES)
                .setDefaultValue("TRADE")
                .setAction(eventTypes::add);

        argGroupRequest
                .add("-i", "--interval")
                .setDescription("Bar interval in minutes")
                .setMetaVar("barInterval")
                .setDefaultValue(String.valueOf(defaultBarInterval))
                .setAction(value -> barInterval = Integer.parseInt(value));

        argGroupRequest
                .add("-I", "--include-condition-codes")
                .setDescription("Include condition codes")
                .setMode(ArgMode.NO_VALUE)
                .setAction(value -> includeConditionCodes = true);

        argGroupRequest
                .add("-G", "--gap-fill-initial-bar")
                .setDescription("Gap fill initial bar")
                .setMode(ArgMode.NO_VALUE)
                .setAction(value -> gapFillInitialBar = true);

        argGroupRequest
                .add("--start-date")
                .setDescription("Start datetime in the format of " + DATE_FORMAT)
                .setMetaVar("startDateTime")
                .setAction(value -> startDateTime = value);

        argGroupRequest
                .add("--end-date")
                .setDescription("End datetime in the format of " + DATE_FORMAT)
                .setMetaVar("endDateTime")
                .setAction(value -> endDateTime = value);

        argGroupRequest
                .add("-O", "--override")
                .setDescription("Field to override")
                .setMetaVar("<fieldId>=<value>")
                .setMode(ArgMode.MULTIPLE_VALUES)
                .setAction(this::appendOverride);

        String eol = System.lineSeparator();
        argGroupRequest
                .add("-r", "--request")
                .setDescription(
                        "Request type."
                                + eol
                                + "To retrieve reference data: "
                                + eol
                                + "\t-r, --request "
                                + REFERENCE_DATA_REQUEST
                                + eol
                                + "\t[-S, --security <security = {IBM US Equity, MSFT US Equity}>]"
                                + eol
                                + "\t[-f, --field <field = PX_LAST>]"
                                + eol
                                + "To retrieve reference data with overrides: "
                                + eol
                                + "\t-r, --request "
                                + REFERENCE_DATA_REQUEST_OVERRIDE
                                + eol
                                + "\t[-S, --security <security = {IBM US Equity, MSFT US Equity}>]"
                                + eol
                                + "\t[-f, --field <field = {PX_LAST, DS002, EQY_WEIGHTED_AVG_PX}>]"
                                + eol
                                + "\t[-O, --override <<fieldId>=<value> = "
                                + "{VWAP_START_TIME=9:30, VWAP_END_TIME=11:30}]"
                                + eol
                                + "To retrieve reference data with table overrides: "
                                + eol
                                + "\t-r, --request "
                                + REFERENCE_DATA_REQUEST_TABLE_OVERRIDE
                                + eol
                                + "\t[-S, --security <security = FHR 3709 FA Mtge>]"
                                + eol
                                + "\t[-f, --field <field = {MTG_CASH_FLOW, SETTLE_DT}>]"
                                + eol
                                + "To retrieve intraday bars: "
                                + eol
                                + "\t-r, --request "
                                + INTRADAY_BAR_REQUEST
                                + eol
                                + "\t[-S, --security <security = IBM US Equity>]"
                                + eol
                                + "\t[-e, --event <event = TRADE>]"
                                + eol
                                + "\t[-i, --interval <barInterval = "
                                + defaultBarInterval
                                + ">]"
                                + eol
                                + "\t[--start-date <startDateTime = "
                                + startDateTime
                                + ">]"
                                + eol
                                + "\t[--end-date <endDateTime = "
                                + defaultIntradayBarEndDateTime
                                + ">]"
                                + eol
                                + "\t[-G, --gap-fill-initial-bar]"
                                + eol
                                + "\t\t1) All times are in GMT."
                                + eol
                                + "\t\t2) Only one security can be specified."
                                + eol
                                + "\t\t3) Only one event can be specified."
                                + eol
                                + "To retrieve intraday raw ticks: "
                                + eol
                                + "\t-r, --request "
                                + INTRADAY_TICK_REQUEST
                                + eol
                                + "\t[-S, --security <security = IBM US Equity>]"
                                + eol
                                + "\t[-e, --event <event = TRADE>]"
                                + eol
                                + "\t[--start-date <startDateTime = "
                                + startDateTime
                                + ">]"
                                + eol
                                + "\t[--end-date <endDateTime = "
                                + defaultIntradayTickEndDateTime
                                + ">]"
                                + eol
                                + "\t[-I, --include-condition-codes]"
                                + eol
                                + "\t\t1) All times are in GMT."
                                + eol
                                + "\t\t2) Only one security can be specified."
                                + eol
                                + "To retrieve historical data: "
                                + eol
                                + "\t-r, --request "
                                + HISTORICAL_DATA_REQUEST
                                + eol
                                + "\t[-S, --security <security = {IBM US Equity, MSFT US Equity}>]"
                                + eol
                                + "\t[-f, --field <field = PX_LAST>]"
                                + eol)
                .setMetaVar("requestType")
                .setDefaultValue(REFERENCE_DATA_REQUEST)
                .setChoices(
                        REFERENCE_DATA_REQUEST,
                        REFERENCE_DATA_REQUEST_OVERRIDE,
                        REFERENCE_DATA_REQUEST_TABLE_OVERRIDE,
                        INTRADAY_BAR_REQUEST,
                        INTRADAY_TICK_REQUEST,
                        HISTORICAL_DATA_REQUEST)
                .setAction(value -> requestType = value);

        argParser.addGroup(argGroupRequest);
    }

    private void setDefaultStartAndEndTime() {
        Calendar calendar = getPreviousTradingDate();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        startDateTime = sdf.format(calendar.getTime());

        calendar.add(Calendar.MINUTE, 5);
        defaultIntradayTickEndDateTime = sdf.format(calendar.getTime());

        // Make the end time for IntradayBarRequest 1 hour from the start time.
        // The default bar interval is 5 minute, by default there are 12 bars.
        calendar.add(Calendar.MINUTE, 55);
        defaultIntradayBarEndDateTime = sdf.format(calendar.getTime());
    }

    private static Calendar getPreviousTradingDate() {
        Calendar previousTradingDate = Calendar.getInstance();
        previousTradingDate.roll(Calendar.DAY_OF_MONTH, -1);
        int dayOfWeek = previousTradingDate.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY) {
            previousTradingDate.roll(Calendar.DAY_OF_MONTH, -2);
        } else if (dayOfWeek == Calendar.SATURDAY) {
            previousTradingDate.roll(Calendar.DAY_OF_MONTH, -1);
        }

        // Return the market open time (GMT) on previous trading day.
        previousTradingDate.set(Calendar.HOUR_OF_DAY, 14);
        previousTradingDate.set(Calendar.MINUTE, 30);
        previousTradingDate.set(Calendar.SECOND, 0);

        return previousTradingDate;
    }

    private void appendOverride(String overrideStr) {
        String[] fieldIdAndValue = overrideStr.split("=");

        if (fieldIdAndValue.length != 2) {
            throw new IllegalArgumentException("Invalid override " + overrideStr);
        }

        overrides.add(new Override(fieldIdAndValue[0], fieldIdAndValue[1]));
    }

    public void setDefaultValues() {
        if (securities.isEmpty()) {
            if (requestType.equals(REFERENCE_DATA_REQUEST_TABLE_OVERRIDE)) {
                securities.add("FHR 3709 FA Mtge");
            } else {
                securities.add("IBM US Equity");
                securities.add("MSFT US Equity");
            }
        }

        if (fields.isEmpty()) {
            if (requestType.equals(REFERENCE_DATA_REQUEST_TABLE_OVERRIDE)) {
                fields.add("MTG_CASH_FLOW");
                fields.add("SETTLE_DT");
            } else {
                fields.add("PX_LAST");
                if (requestType.equals(REFERENCE_DATA_REQUEST_OVERRIDE)) {
                    fields.add("DS002");
                    fields.add("EQY_WEIGHTED_AVG_PX");
                }
            }
        }

        if (overrides.isEmpty() && requestType.equals(REFERENCE_DATA_REQUEST_OVERRIDE)) {
            overrides.addAll(
                    Arrays.asList(
                            new Override("VWAP_START_TIME", "9:30"),
                            new Override("VWAP_END_TIME", "11:30")));
        }

        if (endDateTime == null) {
            endDateTime =
                    requestType.equals(INTRADAY_BAR_REQUEST)
                            ? defaultIntradayBarEndDateTime
                            : defaultIntradayTickEndDateTime;
        }
    }
}
