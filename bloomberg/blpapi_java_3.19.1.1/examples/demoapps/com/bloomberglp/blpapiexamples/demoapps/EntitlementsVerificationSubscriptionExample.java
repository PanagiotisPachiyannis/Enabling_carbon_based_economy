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

import com.bloomberglp.blpapi.AbstractSession;
import com.bloomberglp.blpapi.AuthOptions;
import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.SubscriptionList;
import com.bloomberglp.blpapi.TlsOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.ExampleState;
import com.bloomberglp.blpapiexamples.demoapps.util.SubscriptionOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import com.bloomberglp.blpapiexamples.demoapps.util.events.SessionRouter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class EntitlementsVerificationSubscriptionExample {
    private static final Name EID = Name.getName("EID");
    private static final Name ENTITLEMENT_CHANGED = Name.getName("EntitlementChanged");
    private static final Name SERVICE_NAME = Name.getName("serviceName");

    private final SessionRouter<Session> router = new SessionRouter<>();
    private final Map<CorrelationID, Identity> identitiesByCorrelationId = new HashMap<>();
    private final AtomicReference<ExampleState> exampleState =
            new AtomicReference<>(ExampleState.STARTING);
    private Session session;
    private SubscriptionOptions subscriptionOptions;
    private ConnectionAndAuthOptions connectionAndAuthOptions;

    public EntitlementsVerificationSubscriptionExample() {
        router.addExceptionHandler(this::handleException);

        router.addMessageHandler(Names.SESSION_STARTED, this::handleSessionStarted);
        router.addMessageHandler(Names.SESSION_STARTUP_FAILURE, this::handleSessionStartupFailure);
        router.addMessageHandler(Names.SESSION_TERMINATED, this::handleSessionTerminated);
        router.addMessageHandler(Names.SERVICE_OPENED, this::handleServiceOpened);
        router.addMessageHandler(Names.SERVICE_OPEN_FAILURE, this::handleServiceOpenFailure);
        router.addMessageHandler(ENTITLEMENT_CHANGED, this::handleEntitlementChanged);
    }

    public static void main(String[] args) throws Exception {
        EntitlementsVerificationSubscriptionExample example =
                new EntitlementsVerificationSubscriptionExample();
        if (!example.parseCommandLine(args)) {
            return;
        }

        example.run();

        // The main thread is not blocked and the example is running
        // asynchronously.
        while (example.exampleState.get() != ExampleState.TERMINATED) {
            Thread.sleep(100);
        }
    }

    private void run()
            throws IOException, InterruptedException, TlsOptions.TlsInitializationException {
        createAndStartSession();
    }

    private void stop() {
        // Cancel all the authorized identities
        session.cancel(new ArrayList<>(identitiesByCorrelationId.keySet()));

        try {
            session.stop(AbstractSession.StopOption.ASYNC);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void handleException(Session sess, Event event, Exception exception) {
        exception.printStackTrace();
        stop();
    }

    private void createAndStartSession()
            throws IOException, InterruptedException, TlsOptions.TlsInitializationException {
        // Use the specified application as the session identity to authorize.
        // This may cause the session to stop and the example to terminate if
        // the identity is revoked.
        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
        subscriptionOptions.setSessionOptions(sessionOptions);
        session = new Session(sessionOptions, router::processEvent);
        session.startAsync();
    }

    private void handleSessionStarted(Session sess, Event event, Message message)
            throws IOException {
        exampleState.set(ExampleState.STARTED);

        // Add the authorization messages handlers after the session
        // started to only react to the authorization messages of users,
        // i.e., avoid those of the session identity.
        router.addMessageHandler(Names.AUTHORIZATION_SUCCESS, this::handleAuthorizationSuccess);
        router.addMessageHandler(Names.AUTHORIZATION_FAILURE, this::handleAuthorizationFailure);
        router.addMessageHandler(Names.AUTHORIZATION_REVOKED, this::handleAuthorizationRevoked);

        authorizeUsers();
        openServices();
    }

    private void handleSessionStartupFailure(Session sess, Event event, Message message) {
        System.out.println("Failed to start session. Exiting...");

        exampleState.set(ExampleState.TERMINATED);
    }

    private void handleSessionTerminated(Session sess, Event event, Message message) {
        exampleState.set(ExampleState.TERMINATED);
    }

    private void openServices() throws IOException {
        session.openServiceAsync(subscriptionOptions.service);
    }

    private void handleServiceOpened(Session sess, Event event, Message message)
            throws IOException {
        String serviceName = message.getElementAsString(SERVICE_NAME);

        if (serviceName.equals(subscriptionOptions.service)) {
            subscribe();
        } else {
            System.out.println("A service was opened: " + serviceName);
        }
    }

    private void handleServiceOpenFailure(Session sess, Event event, Message message) {
        String serviceName = message.getElementAsString(SERVICE_NAME);

        if (serviceName.equals(subscriptionOptions.service)) {
            System.out.println(
                    "Failed to open service '" + serviceName + "', stopping application...");
            stop();
        } else {
            throw new RuntimeException("A service which is unknown failed to open: " + serviceName);
        }
    }

    private void authorizeUsers() {
        // Authorize each of the users
        Map<String, AuthOptions> authOptionsByIdentifier =
                connectionAndAuthOptions.createClientServerSetupAuthOptions();
        for (Map.Entry<String, AuthOptions> entry : authOptionsByIdentifier.entrySet()) {
            String userIdentifier = entry.getKey();
            AuthOptions authOptions = entry.getValue();
            CorrelationID correlationId = new CorrelationID(userIdentifier);
            session.generateAuthorizedIdentity(authOptions, correlationId);
        }
    }

    private void subscribe() throws IOException {
        router.addMessageHandler(Names.SUBSCRIPTION_FAILURE, this::handleSubscriptionFailure);
        router.addMessageHandler(Names.SUBSCRIPTION_TERMINATED, this::handleSubscriptionTerminated);
        router.addEventHandler(EventType.SUBSCRIPTION_DATA, this::handleSubscriptionData);

        SubscriptionList subscriptions =
                subscriptionOptions.createSubscriptionList(
                        (index, topic) -> new CorrelationID(topic));
        System.out.println("Subscribing...");
        session.subscribe(subscriptions);
    }

    private void handleSubscriptionFailure(Session sess, Event event, Message message) {
        String topic = (String) message.correlationID().object();
        System.out.println("Subscription failed: " + topic);
    }

    private void handleSubscriptionTerminated(Session sess, Event event, Message message) {
        String topic = (String) message.correlationID().object();
        System.out.println("Subscription terminated: " + topic);
    }

    private void handleSubscriptionData(Session sess, Event event) {
        for (Message message : event) {

            String topic = (String) message.correlationID().object();

            List<Integer> failedEntitlements = new ArrayList<>();
            Service service = message.service();

            if (message.hasElement(EID, true)) { // excludeNullElements
                Element entitlements = message.getElement(EID);

                for (Map.Entry<CorrelationID, Identity> entry :
                        identitiesByCorrelationId.entrySet()) {
                    String userIdentifier = (String) entry.getKey().object();
                    Identity identity = entry.getValue();

                    if (identity.hasEntitlements(entitlements, service, failedEntitlements)) {
                        System.out.println(
                                userIdentifier + " is entitled to get data for: " + topic);

                        // Now distribute message to the user.
                    } else {
                        System.out.println(
                                userIdentifier
                                        + " is NOT entitled to get data for: "
                                        + topic
                                        + " - Failed eids: "
                                        + failedEntitlements);
                    }

                    failedEntitlements.clear();
                }
            } else {
                System.out.println("No entitlements are required for: " + topic);

                // Now distribute message to the authorized users.
            }

            System.out.println();
        }
    }

    private void handleAuthorizationSuccess(Session sess, Event event, Message message) {
        CorrelationID correlationId = message.correlationID();
        String userIdentifier = (String) correlationId.object();
        System.out.println("Successfully authorized " + userIdentifier);
        Identity identity = session.getAuthorizedIdentity(correlationId);
        identitiesByCorrelationId.put(correlationId, identity);

        // Deliver init paint to the user. For the purpose of simplicity,
        // this example doesn't maintain an init paint cache.
    }

    private void handleAuthorizationFailure(Session sess, Event event, Message message) {
        CorrelationID correlationId = message.correlationID();
        router.removeMessageHandler(correlationId);

        String userIdentifier = (String) correlationId.object();
        System.out.println("Failed to authorize " + userIdentifier);
    }

    private void handleAuthorizationRevoked(Session sess, Event event, Message message) {
        CorrelationID correlationId = message.correlationID();
        router.removeMessageHandler(correlationId);

        String userIdentifier = (String) correlationId.object();
        System.out.println("Authorization revoked for " + userIdentifier);

        // Remove the identity
        identitiesByCorrelationId.remove(correlationId);
    }

    private void handleEntitlementChanged(Session sess, Event event, Message message) {
        // This is just informational. Continue to use existing identity.
        String userIdentifier = (String) message.correlationID().object();
        System.out.println("Entitlements updated for " + userIdentifier);
    }

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser =
                new ArgParser(
                        "Entitlements Verification Subscription Example",
                        EntitlementsVerificationSubscriptionExample.class);
        try {
            connectionAndAuthOptions = ConnectionAndAuthOptions.forClientServerSetup(argParser);
            subscriptionOptions = new SubscriptionOptions(argParser);

            argParser.parse(args);

            if (connectionAndAuthOptions.numClientServerSetupAuthOptions() == 0) {
                throw new IllegalArgumentException("No userId:IP or token specified.");
            }

            if (!subscriptionOptions.fields.contains(EID.toString())) {
                subscriptionOptions.fields.add(EID.toString());
            }
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        return true;
    }
}
