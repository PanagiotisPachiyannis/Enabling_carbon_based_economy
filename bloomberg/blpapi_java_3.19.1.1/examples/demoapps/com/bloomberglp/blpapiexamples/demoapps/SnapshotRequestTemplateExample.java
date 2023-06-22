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
import com.bloomberglp.blpapi.DuplicateCorrelationIDException;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.RequestTemplate;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.SubscriptionOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class SnapshotRequestTemplateExample {

    /**
     * This correlation stores the topic of a snapshot in order to retrieve the topic when
     * processing a response message.
     *
     * <p>A snapshot requires a template and a request in order to receive data. Given the topic
     * string of a snapshot, if the {@link CorrelationID}s of both the template and the request were
     * created with the topic string, it would lead to {@link DuplicateCorrelationIDException}.
     *
     * <p>This class encapsulates the topic string, and every instance is unique (by reference)
     * regardless of the encapsulated topic string. Both the template and the request can create the
     * {@link CorrelationID} with an instance of this class so that when the response is received,
     * the topic string is extracted from it.
     */
    private static class MyCorrelation {
        public String topic;

        public MyCorrelation(String topic) {
            this.topic = topic;
        }

        @Override
        public String toString() {
            return topic;
        }
    }

    /** The {@link EventHandler} that mostly handles snapshot related events and messages. */
    private class MyEventHandler implements EventHandler {

        @Override
        public void processEvent(Event event, Session session) {
            for (Message msg : event) {
                Name messageType = msg.messageType();
                CorrelationID correlationId = msg.correlationID();
                if (messageType.equals(Names.REQUEST_TEMPLATE_AVAILABLE)) {
                    MyCorrelation myCorrelation = (MyCorrelation) correlationId.object();
                    System.out.println(
                            "Request template is successfully " + "created for " + myCorrelation);
                    System.out.println(msg);

                    templateSemaphore.release();
                    requestCountDown.countDown();
                } else if (event.eventType() == EventType.PARTIAL_RESPONSE) {
                    MyCorrelation myCorrelation = (MyCorrelation) correlationId.object();
                    System.out.println("Received partial response for " + myCorrelation);
                    System.out.println(msg);
                } else if (event.eventType() == EventType.RESPONSE) {
                    MyCorrelation myCorrelation = (MyCorrelation) correlationId.object();
                    System.out.println("Received response for " + myCorrelation);
                    System.out.println(msg);
                    requestCountDown.countDown();
                } else if (messageType.equals(Names.REQUEST_TEMPLATE_TERMINATED)) {

                    // Will also receive a 'RequestFailure' message preceding
                    // 'RequestTemplateTerminated' for every pending request.
                    MyCorrelation myCorrelation = (MyCorrelation) correlationId.object();
                    System.out.println("Request template terminated for " + myCorrelation);
                    System.out.println(msg);

                    // Remove this template from the map
                    synchronized (lock) {
                        snapshots.remove(correlationId);
                    }

                    templateSemaphore.release();
                    requestCountDown.countDown();
                } else if (messageType.equals(Names.SESSION_TERMINATED)) {
                    synchronized (lock) {
                        isRunning = false;
                    }

                    templateSemaphore.release();
                    while (requestCountDown.getCount() > 0) {
                        requestCountDown.countDown();
                    }

                    break;
                }
            }
        }
    }

    /**
     * Requests are throttled in the infrastructure. Snapshot {@link RequestTemplate}s send one
     * resolution request per topic (unlike normal subscriptions where multiple topics are resolved
     * at once), which is likely to cause request throttling. It is therefore recommended to send
     * {@link RequestTemplate}s in batches.
     */
    private static final int BATCH_SIZE = 50;

    private ConnectionAndAuthOptions connectionAndAuthOptions;
    private SubscriptionOptions subscriptionOptions;

    /** The semaphore used to help batch processing request templates. */
    private final Semaphore templateSemaphore = new Semaphore(BATCH_SIZE);

    private final Map<CorrelationID, RequestTemplate> snapshots = new HashMap<>();

    /**
     * The synchronize helper that is used to synchronize requests and responses, i.e., the next
     * requests are only sent after all the responses of the current requests have been received.
     */
    private CountDownLatch requestCountDown;

    private final Object lock = new Object();
    private boolean isRunning = true;

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser =
                new ArgParser("Snapshot Request Example", SnapshotRequestTemplateExample.class);
        try {
            connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
            subscriptionOptions = SubscriptionOptions.forSnapshot(argParser);
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        return true;
    }

    private void run(String[] args)
            throws IOException, InterruptedException, TlsInitializationException {
        if (!parseCommandLine(args)) {
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        subscriptionOptions.setSessionOptions(sessionOptions);
        Session session = new Session(sessionOptions, new MyEventHandler());
        try {
            if (!session.start()) {
                System.err.println("Failed to start session.");
                return;
            }

            if (!session.openService(subscriptionOptions.service)) {
                System.err.println("Failed to open service '" + subscriptionOptions.service + "'.");
                return;
            }

            createTemplates(session);

            synchronized (lock) {
                if (!isRunning) {
                    return;
                }

                if (snapshots.isEmpty()) {
                    System.out.println("All request templates failed to create.");
                    return;
                }

                System.out.println("All the request templates are finished");
            }

            sendRequests(session);
        } finally {
            session.stop();
        }
    }

    private void createTemplates(Session session)
            throws DuplicateCorrelationIDException, IllegalStateException, IllegalArgumentException,
                    IOException, InterruptedException {
        // NOTE: resources used by a snapshot request template are
        // released only when 'RequestTemplateTerminated' message
        // is received or when the session is destroyed. In order
        // to release resources when request template is not needed
        // anymore, user should call the 'Session.cancel' and pass
        // the correlation id used when creating the request template,
        // or call 'RequestTemplate.close'. If the 'Session.cancel'
        // is used, all outstanding requests are canceled and the
        // underlying subscription is closed immediately. If the
        // handle is closed with the 'RequestTemplate.close', the
        // underlying subscription is closed only when all outstanding
        // requests are served.

        Map<String, String> subscriptionStrings = subscriptionOptions.createSubscriptionStrings();
        requestCountDown = new CountDownLatch(subscriptionStrings.size());
        for (Entry<String, String> entry : subscriptionStrings.entrySet()) {

            // Acquire a permit first.
            templateSemaphore.acquire();

            synchronized (lock) {
                if (!isRunning) {
                    return;
                }

                // Create the template.
                String userTopic = entry.getKey();
                String subscriptionString = entry.getValue();
                System.out.println("Creating snapshot request template for " + userTopic);
                CorrelationID statusCid = new CorrelationID(new MyCorrelation(userTopic));

                // Lock ensures template is added to snapshots map
                // before response is processed.
                RequestTemplate requestTemplate =
                        session.createSnapshotRequestTemplate(subscriptionString, statusCid);
                snapshots.put(statusCid, requestTemplate);
            }
        }

        // Wait until all the request templates have finished, either success
        // or failure.
        requestCountDown.await();
    }

    private void sendRequests(Session session) throws IOException, InterruptedException {

        while (true) {
            synchronized (lock) {
                if (!isRunning) {
                    break;
                }
                System.out.println("Sending snapshot requests using " + "the request templates");
                requestCountDown = new CountDownLatch(snapshots.size());
                for (Entry<CorrelationID, RequestTemplate> entry : snapshots.entrySet()) {
                    MyCorrelation myCorrelation = (MyCorrelation) entry.getKey().object();
                    session.sendRequest(
                            entry.getValue(),
                            new CorrelationID(new MyCorrelation(myCorrelation.topic)));
                }
            }

            // Wait until all the requests have received responses.
            requestCountDown.await();

            System.out.println("Received all the responses..., Press [Ctrl-C] to exit");

            // Sleep 5 seconds before next snapshots.
            Thread.sleep(5000);
        }
    }

    public static void main(String[] args) {
        SnapshotRequestTemplateExample example = new SnapshotRequestTemplateExample();
        try {
            example.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
