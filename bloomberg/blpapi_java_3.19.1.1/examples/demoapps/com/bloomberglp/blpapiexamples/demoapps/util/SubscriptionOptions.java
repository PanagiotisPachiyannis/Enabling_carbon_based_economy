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

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.Arg;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A data structure that holds the option to make subscriptions or snapshot requests. It also adds
 * the options to the {@link ArgParser}.
 */
public class SubscriptionOptions {
    static final String DEFAULT_SERVICE = "//blp/mktdata";
    static final String DEFAULT_TOPIC_PREFIX = "/ticker/";

    private static final String DEFAULT_TOPIC = "IBM US Equity";

    public String service;
    public final List<String> fields = new ArrayList<>();
    private final List<String> topics = new ArrayList<>();
    private final List<String> options = new ArrayList<>();

    private String topicPrefix;

    private SubscriptionOptions(ArgParser argParser, boolean isSnapshot) {
        String eol = System.lineSeparator();
        Arg argService =
                new Arg("-s", "--service")
                        .setMetaVar("service")
                        .setDescription("service name")
                        .setDefaultValue(DEFAULT_SERVICE)
                        .setAction(value -> service = value);
        Arg argTopic =
                new Arg("-t", "--topic")
                        .setMetaVar("topic")
                        .setDescription(
                                "Topic to subscribe. "
                                        + eol
                                        + "Can be one of the following:"
                                        + eol
                                        + "* Instrument"
                                        + eol
                                        + "* Instrument qualified with a prefix"
                                        + eol
                                        + "* Instrument qualified with a service and a prefix")
                        .setDefaultValue(DEFAULT_TOPIC)
                        .setMode(ArgMode.MULTIPLE_VALUES) // allow multiple
                        .setAction(topics::add);
        Arg argField =
                new Arg("-f", "--field")
                        .setMetaVar("field")
                        .setDescription("field to subscribe")
                        .setMode(ArgMode.MULTIPLE_VALUES) // allow multiple
                        .setAction(fields::add);
        Arg argOption =
                new Arg("-o", "--option")
                        .setMetaVar("option")
                        .setDescription("subscription options")
                        .setMode(ArgMode.MULTIPLE_VALUES) // allow multiple
                        .setAction(options::add);
        Arg argTopicPrefix =
                new Arg("-x", "--topic-prefix")
                        .setMetaVar("prefix")
                        .setDescription("The topic prefix to be used for subscriptions")
                        .setDefaultValue(DEFAULT_TOPIC_PREFIX)
                        .setAction(value -> topicPrefix = value);
        ArgGroup groupSubscription =
                new ArgGroup(
                        "Subscriptions", argService, argTopic, argField, argOption, argTopicPrefix);

        if (!isSnapshot) {
            Arg argInterval =
                    new Arg("-i", "--interval")
                            .setDescription(
                                    "subscription option that specifies a time"
                                            + " in seconds to intervalize the subscriptions")
                            .setAction(this::parseInterval);
            groupSubscription.add(argInterval);
        }

        argParser.addGroup(groupSubscription);
    }

    public SubscriptionOptions(ArgParser argParser) {
        this(argParser, false);
    }

    /** Creates an instance for snapshot requests, which does not include interval option. */
    public static SubscriptionOptions forSnapshot(ArgParser argParser) {
        return new SubscriptionOptions(argParser, true);
    }

    private void parseInterval(String value) {
        double interval;
        try {
            interval = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid interval " + value, ex);
        }

        options.add("interval=" + interval);
    }

    public SubscriptionList createSubscriptionList(
            BiFunction<Integer, String, CorrelationID> correlationIdGenerator) {
        SubscriptionList subscriptions = new SubscriptionList();
        for (int i = 0; i < topics.size(); ++i) {
            String topic = topics.get(i);
            Subscription subscription =
                    new Subscription(
                            topic, fields, options, correlationIdGenerator.apply(i, topic));
            subscriptions.add(subscription);
        }

        return subscriptions;
    }

    /**
     * Creates a map from topics provided on the command line to their corresponding subscription
     * strings.
     *
     * <p>This method uses {@link Subscription} to help construct the subscription string.
     */
    public Map<String, String> createSubscriptionStrings() {
        Map<String, String> subStrings = new HashMap<>();
        for (String userTopic : topics) {
            Subscription sub = new Subscription(userTopic, fields, options);
            subStrings.put(userTopic, sub.subscriptionString());
        }

        return subStrings;
    }

    public void setSessionOptions(SessionOptions sessionOptions) {
        sessionOptions.setDefaultSubscriptionService(service);
        sessionOptions.setDefaultTopicPrefix(topicPrefix);
    }
}
