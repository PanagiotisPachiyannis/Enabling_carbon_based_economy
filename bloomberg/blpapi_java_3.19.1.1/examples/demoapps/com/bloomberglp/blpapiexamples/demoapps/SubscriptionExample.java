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

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.SubscriptionList;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.SubscriptionOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;
import java.time.LocalDateTime;

public class SubscriptionExample {

    private ConnectionAndAuthOptions connectionAndAuthOptions;
    private SubscriptionOptions subscriptionOptions;
    private SubscriptionList subscriptions;

    private int eventQueueSize = 10000;

    public static void main(String[] args) {
        SubscriptionExample example = new SubscriptionExample();

        try {
            example.run(args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void run(String[] args)
            throws InterruptedException, TlsInitializationException, IOException {
        if (!parseCommandLine(args)) {
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        subscriptionOptions.setSessionOptions(sessionOptions);

        sessionOptions.setMaxEventQueueSize(eventQueueSize);

        Session session = new Session(sessionOptions, new SubscriptionEventHandler());
        try {
            if (!session.start()) {
                System.err.println("Failed to start session.");
                return;
            }

            System.out.println("Session started successfully.");

            if (!session.openService(subscriptionOptions.service)) {
                System.err.println("Failed to open service " + subscriptionOptions.service);
                return;
            }

            System.out.println("Subscribing...");

            subscriptions =
                    subscriptionOptions.createSubscriptionList(
                            (index, topic) -> new CorrelationID(topic));

            session.subscribe(subscriptions);

            System.out.println("Press ENTER to quit");
            System.in.read();
        } finally {
            session.stop();
        }
    }

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser =
                new ArgParser(
                        "Asynchronous subscription with event handler", SubscriptionExample.class);
        connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        subscriptionOptions = new SubscriptionOptions(argParser);

        try {
            argParser
                    .addArg("-q", "--event-queue-size")
                    .setMetaVar("eventQueueSize")
                    .setDescription("The maximum number of events that is buffered by the session")
                    .setDefaultValue(Integer.toString(eventQueueSize))
                    .setAction(value -> eventQueueSize = Integer.parseInt(value));
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        return true;
    }

    class SubscriptionEventHandler implements EventHandler {

        @Override
        public void processEvent(Event event, Session session) {
            try {
                switch (event.eventType().intValue()) {
                    case EventType.Constants.SUBSCRIPTION_DATA:
                        processSubscriptionDataEvent(event);
                        break;
                    case EventType.Constants.SUBSCRIPTION_STATUS:
                        processSubscriptionStatus(event);
                        break;
                    default:
                        processGenericEvent(event);
                        break;
                }
            } catch (Exception e) {
                System.err.println("Failed to process event " + event + " " + e);
            }
        }

        private void processSubscriptionStatus(Event event) {
            for (Message msg : event) {
                CorrelationID cid = msg.correlationID();
                String topic = (String) cid.object();
                System.out.println(LocalDateTime.now() + ": " + topic);
                System.out.println("MESSAGE: " + msg);

                if (Names.SUBSCRIPTION_FAILURE.equals(msg.messageType())) {
                    System.err.println("Subscription for " + topic + " failed.");
                } else if (Names.SUBSCRIPTION_TERMINATED.equals(msg.messageType())) {
                    // Subscription can be terminated if the session identity
                    // is revoked.
                    System.err.println("Subscription for " + topic + " terminated.");
                }
            }
        }

        private void processSubscriptionDataEvent(Event event) {
            for (Message msg : event) {
                String topic = (String) msg.correlationID().object();
                System.out.println(LocalDateTime.now() + ": " + topic);
                System.out.println(msg);
            }
            return;
        }

        private void processGenericEvent(Event event) {
            for (Message msg : event) {
                if (Names.SLOW_CONSUMER_WARNING.equals(msg.messageType())) {
                    System.out.println(
                            Names.SLOW_CONSUMER_WARNING
                                    + " - The event queue is "
                                    + "beginning to approach its maximum capacity and "
                                    + "the application is not processing the data fast "
                                    + "enough. This could lead to ticks being dropped "
                                    + "(DataLoss).");
                    System.out.println();
                } else if (Names.SLOW_CONSUMER_WARNING_CLEARED.equals(msg.messageType())) {
                    System.out.println(
                            Names.SLOW_CONSUMER_WARNING_CLEARED
                                    + " - the event "
                                    + "queue has shrunk enough that there is no "
                                    + "longer any immediate danger of overflowing the "
                                    + "queue. If any precautionary actions were taken "
                                    + "when SlowConsumerWarning message was delivered, "
                                    + "it is now safe to continue as normal.");
                    System.out.println();
                } else if (Names.DATA_LOSS.equals(msg.messageType())) {
                    System.out.println(msg);

                    CorrelationID cid = msg.correlationID();
                    String topic = (String) cid.object();
                    System.out.println(
                            Names.DATA_LOSS
                                    + " - The application is too slow to "
                                    + "process events and the event queue is overflowing. "
                                    + "Data is lost for topic "
                                    + topic
                                    + ".");
                    System.out.println();
                } else if (EventType.SESSION_STATUS == event.eventType()) {
                    // SESSION_STATUS events can happen at any time and
                    // should be handled as the session can be terminated,
                    // e.g. session identity can be revoked at a later
                    // time, which terminates the session.
                    if (Names.SESSION_TERMINATED.equals(msg.messageType())) {
                        System.out.println("Session terminated.");
                        return;
                    }
                }
            }
        }
    }
}
