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
package com.bloomberglp.blpapiexamples.demoapps;

import com.bloomberglp.blpapi.AuthOptions;
import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Message.Recap;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.SubscriptionList;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.MaxEventsOption;
import com.bloomberglp.blpapiexamples.demoapps.util.SubscriptionOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;

public class SubscriptionWithEventPollingExample {

    private static final Name SERVICE_NAME = Name.getName("serviceName");

    /**
     * Prints contact support message.
     *
     * <p>{@link Message} can have an associated RequestId that is used to identify the operation
     * through the network. When contacting support please provide the RequestId.
     */
    private static void printContactSupportMessage(Message message) {
        String requestId = message.getRequestId();
        if (requestId != null) {
            System.err.println(
                    "When contacting support, " + "please provide RequestId " + requestId + '.');
        }
    }

    /**
     * Prints the error if the {@code message} is a failure message.
     *
     * <p>When using a session identity, i.e. {@link
     * SessionOptions#setSessionIdentityOptions(AuthOptions)}, token generation failure,
     * authorization failure or revocation terminates the session, in which case, applications only
     * need to check session status messages. Applications don't need to handle token or
     * authorization messages.
     *
     * @return {@code true} if session has failed to start or terminated; {@code false} otherwise.
     */
    private static boolean processGenericMessage(EventType eventType, Message message) {
        Name messageType = message.messageType();
        if (eventType == EventType.SESSION_STATUS) {
            if (messageType.equals(Names.SESSION_TERMINATED)
                    || messageType.equals(Names.SESSION_STARTUP_FAILURE)) {
                System.err.println("Session failed to start or terminated.");
                printContactSupportMessage(message);
                return true;
            }
        } else if (eventType == EventType.SERVICE_STATUS) {
            if (messageType.equals(Names.SERVICE_OPEN_FAILURE)) {
                String serviceName = message.getElementAsString(SERVICE_NAME);
                System.err.println("Failed to open " + serviceName + ".");

                printContactSupportMessage(message);
            }
        }

        return false;
    }

    /**
     * Checks failure events published by the session.
     *
     * <p>Note that the loop uses {@link Session#tryNextEvent} as all events have been produced
     * before calling this function, but there could be no events at all in the queue if the OS
     * fails to allocate resources.
     */
    private static void checkFailures(Session session) {
        while (true) {
            Event event = session.tryNextEvent();
            if (event == null) {
                return;
            }

            for (Message msg : event) {
                System.out.println(msg);

                if (processGenericMessage(event.eventType(), msg)) {
                    return;
                }
            }
        }
    }

    private static void run(String[] args)
            throws IOException, InterruptedException, TlsInitializationException {
        ArgParser argParser =
                new ArgParser(
                        "Subscription example with event polling",
                        SubscriptionWithEventPollingExample.class);
        ConnectionAndAuthOptions connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        SubscriptionOptions subscriptionOptions = new SubscriptionOptions(argParser);
        MaxEventsOption maxEventsConfig = new MaxEventsOption(argParser);
        try {
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        subscriptionOptions.setSessionOptions(sessionOptions);
        Session session = new Session(sessionOptions);
        try {
            if (!session.start()) {
                System.err.println("Failed to start session.");
                checkFailures(session);
                return;
            }

            if (!session.openService(subscriptionOptions.service)) {
                checkFailures(session);
                return;
            }

            SubscriptionList subscriptions =
                    subscriptionOptions.createSubscriptionList(
                            (index, topic) -> new CorrelationID(topic));
            session.subscribe(subscriptions);

            int maxEvents = maxEventsConfig.getMaxEvents();
            int eventCount = 0;
            boolean done = false;
            while (!done) {
                Event event = session.nextEvent();
                EventType eventType = event.eventType();
                for (Message msg : event) {
                    System.out.println(msg);

                    Name messageType = msg.messageType();
                    CorrelationID msgCorrelationId = msg.correlationID();
                    if (eventType == EventType.SUBSCRIPTION_STATUS) {
                        if (messageType.equals(Names.SUBSCRIPTION_FAILURE)
                                || messageType.equals(Names.SUBSCRIPTION_TERMINATED)) {
                            String topic = (String) msgCorrelationId.object();
                            System.err.println("Subscription failed for topic " + topic);
                            printContactSupportMessage(msg);
                        }
                    } else if (eventType == EventType.SUBSCRIPTION_DATA) {
                        String topic = (String) msgCorrelationId.object();
                        System.out.println("Received subscription data for topic " + topic);

                        if (msg.recapType() == Recap.SOLICITED) {
                            if (msg.getRequestId() != null) {

                                // An init paint tick can have an associated
                                // RequestId that is used to identify the
                                // source of the data and can be used when
                                // contacting support.
                                System.out.println(
                                        "Received init paint with RequestId " + msg.getRequestId());
                            }
                        }
                    } else {

                        // SESSION_STATUS events can happen at any time and
                        // should be handled as the session can be terminated,
                        // e.g. session identity can be revoked at a later
                        // time, which terminates the session.
                        done = processGenericMessage(eventType, msg);
                    }
                }

                if (eventType == EventType.SUBSCRIPTION_DATA) {
                    if (++eventCount >= maxEvents) {
                        break;
                    }
                }
            }
        } finally {
            session.stop();
        }
    }

    public static void main(String[] args) {
        try {
            SubscriptionWithEventPollingExample.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
