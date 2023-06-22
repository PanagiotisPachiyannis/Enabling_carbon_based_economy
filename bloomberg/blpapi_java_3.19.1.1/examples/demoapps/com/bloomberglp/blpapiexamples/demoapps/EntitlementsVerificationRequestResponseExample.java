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

import com.bloomberglp.blpapi.AbstractSession.StopOption;
import com.bloomberglp.blpapi.AuthOptions;
import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.ExampleState;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import com.bloomberglp.blpapiexamples.demoapps.util.events.SessionRouter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

public class EntitlementsVerificationRequestResponseExample {
    private static final Name RESPONSE_ERROR = Name.getName("responseError");
    private static final Name SECURITY_DATA = Name.getName("securityData");
    private static final Name SECURITY = Name.getName("security");
    private static final Name EID_DATA = Name.getName("eidData");
    private static final Name ENTITLEMENT_CHANGED = Name.getName("EntitlementChanged");
    private static final Name SERVICE_NAME = Name.getName("serviceName");
    private static final Name SECURITIES = Name.getName("securities");
    private static final Name FIELDS = Name.getName("fields");
    private static final Name RETURN_EIDS = Name.getName("returnEids");

    private static final String REF_DATA_SVC_NAME = "//blp/refdata";

    private final SessionRouter<Session> router = new SessionRouter<>();
    private final List<String> securities = new ArrayList<>();
    private ConnectionAndAuthOptions connectionAndAuthOptions;

    private Session session;
    private Service blpRefDataSvc;

    private final AtomicReference<ExampleState> exampleState =
            new AtomicReference<>(ExampleState.STARTING);
    private final Map<CorrelationID, Identity> identitiesByCorrelationId = new HashMap<>();
    private final List<Event> responses = new ArrayList<>();
    private boolean finalResponseReceived = false;

    public EntitlementsVerificationRequestResponseExample() {
        router.addExceptionHandler(this::handleException);

        router.addMessageHandler(Names.SESSION_STARTED, this::handleSessionStarted);
        router.addMessageHandler(Names.SESSION_STARTUP_FAILURE, this::handleSessionStartupFailure);
        router.addMessageHandler(Names.SESSION_TERMINATED, this::handleSessionTerminated);
        router.addMessageHandler(Names.SERVICE_OPENED, this::handleServiceOpened);
        router.addMessageHandler(Names.SERVICE_OPEN_FAILURE, this::handleServiceOpenFailure);
        router.addMessageHandler(ENTITLEMENT_CHANGED, this::handleEntitlementChanged);
    }

    public static void main(String[] args) throws Exception {
        EntitlementsVerificationRequestResponseExample example =
                new EntitlementsVerificationRequestResponseExample();
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

    private void run() throws IOException, InterruptedException, TlsInitializationException {
        createAndStartSession();
    }

    private void stop() {
        // Cancel all the authorized identities
        session.cancel(new ArrayList<>(identitiesByCorrelationId.keySet()));

        try {
            session.stop(StopOption.ASYNC);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void handleException(Session sess, Event event, Exception exception) {
        exception.printStackTrace();
        stop();
    }

    private void createAndStartSession()
            throws IOException, InterruptedException, TlsInitializationException {
        // Use the specified application as the session identity to authorize.
        // This may cause the session to stop and the example to terminate if
        // the identity is revoked.
        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();
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
        session.openServiceAsync(REF_DATA_SVC_NAME);
    }

    private void handleServiceOpened(Session sess, Event event, Message message)
            throws IOException {
        String serviceName = message.getElementAsString(SERVICE_NAME);
        Service service = session.getService(serviceName);

        switch (serviceName) {
            case REF_DATA_SVC_NAME:
                blpRefDataSvc = service;
                sendRefDataRequest();
                break;
            default:
                System.out.println("A service was opened: " + serviceName);
                break;
        }
    }

    private void handleServiceOpenFailure(Session sess, Event event, Message message) {
        String serviceName = message.getElementAsString(SERVICE_NAME);
        switch (serviceName) {
            case REF_DATA_SVC_NAME:
                System.out.println(
                        "Failed to open service '" + serviceName + "', stopping application...");
                stop();
                break;
            default:
                throw new RuntimeException(
                        "A service which is unknown failed to open: " + serviceName);
        }
    }

    private void authorizeUsers() {
        // Authorize each of the users
        Map<String, AuthOptions> authOptionsByIdentifier =
                connectionAndAuthOptions.createClientServerSetupAuthOptions();
        for (Entry<String, AuthOptions> entry : authOptionsByIdentifier.entrySet()) {
            String userIdentifier = entry.getKey();
            AuthOptions authOptions = entry.getValue();
            CorrelationID correlationId = new CorrelationID(userIdentifier);
            session.generateAuthorizedIdentity(authOptions, correlationId);
        }
    }

    private void sendRefDataRequest() throws IOException {
        Request request = blpRefDataSvc.createRequest("ReferenceDataRequest");

        // Add securities.
        Element securitiesElement = request.getElement(SECURITIES);
        for (String security : securities) {
            securitiesElement.appendValue(security);
        }

        // Add fields
        Element fields = request.getElement(FIELDS);
        fields.appendValue("PX_LAST");
        fields.appendValue("DS002");

        request.set(RETURN_EIDS, true);

        router.addEventHandler(EventType.REQUEST_STATUS, this::processRequestStatus);
        router.addEventHandler(EventType.PARTIAL_RESPONSE, this::processPartialResponseStatus);
        router.addEventHandler(EventType.RESPONSE, this::processResponseStatus);
        System.out.println("Sending RefDataRequest ...");
        session.sendRequest(request, new CorrelationID());
    }

    private void processRequestStatus(Session sess, Event event) {
        for (Message message : event) {
            if (message.messageType().equals(Names.REQUEST_FAILURE)) {
                System.out.println("Request failed, stopping application...");
                stop();
                return;
            }
        }
    }

    private void processPartialResponseStatus(Session sess, Event event) {
        System.out.println("Received partial response");

        // Save the response
        responses.add(event);
    }

    private void processResponseStatus(Session sess, Event event) {

        System.out.println("Received final response");
        finalResponseReceived = true;

        // Save the response
        responses.add(event);

        // Distributes all the cached responses to the identities that have
        // been authorized so far.
        for (Entry<CorrelationID, Identity> entry : identitiesByCorrelationId.entrySet()) {
            String userIdentifier = (String) entry.getKey().object();
            distributeResponses(userIdentifier, entry.getValue());
        }
    }

    private void handleAuthorizationSuccess(Session sess, Event event, Message message) {
        CorrelationID correlationId = message.correlationID();
        String userIdentifier = (String) correlationId.object();
        System.out.println("Successfully authorized " + userIdentifier);
        Identity identity = session.getAuthorizedIdentity(correlationId);
        identitiesByCorrelationId.put(correlationId, identity);

        if (finalResponseReceived) {
            distributeResponses(userIdentifier, identity);
        }
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

    /** Distributes all the cached responses to the identity specified by {@code identity}. */
    private void distributeResponses(String userIdentifier, Identity identity) {
        for (Event event : responses) {
            distributeResponse(event, userIdentifier, identity);
        }
    }

    private static void distributeResponse(Event event, String userIdentifier, Identity identity) {
        List<Integer> failedEntitlements = new ArrayList<>();
        for (Message msg : event) {
            if (msg.hasElement(RESPONSE_ERROR)) {
                continue;
            }

            Service service = msg.service();
            Element securitiesElement = msg.getElement(SECURITY_DATA);
            int numSecurities = securitiesElement.numValues();

            System.out.println("Processing " + numSecurities + " securities:");
            for (int i = 0; i < numSecurities; ++i) {
                Element security = securitiesElement.getValueAsElement(i);
                String ticker = security.getElementAsString(SECURITY);

                if (security.hasElement(EID_DATA, true)) { // excludeNullElements
                    // Entitlements are required to access this data
                    Element entitlements = security.getElement(EID_DATA);
                    failedEntitlements.clear();
                    if (identity.hasEntitlements(entitlements, service, failedEntitlements)) {
                        System.out.println(
                                userIdentifier + " is entitled to get data for: " + ticker);

                        // Now Distribute message to the user.
                    } else {
                        System.out.println(
                                userIdentifier
                                        + " is NOT entitled to get data for: "
                                        + ticker
                                        + " - Failed EIDs: "
                                        + failedEntitlements);
                    }
                } else {
                    System.out.println("No entitlements are required for: " + ticker);

                    // Now Distribute message to the user.
                }
            }
        }
    }

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser =
                new ArgParser(
                        "Entitlements Verification Request/Response Example",
                        EntitlementsVerificationRequestResponseExample.class);
        try {
            connectionAndAuthOptions = ConnectionAndAuthOptions.forClientServerSetup(argParser);

            argParser
                    .addArg("-S", "--security")
                    .setMetaVar("security")
                    .setDefaultValue("IBM US Equity")
                    .setDescription("security used in ReferenceDataRequest")
                    .setMode(ArgMode.MULTIPLE_VALUES)
                    .setAction(securities::add);

            argParser.parse(args);

            if (connectionAndAuthOptions.numClientServerSetupAuthOptions() == 0) {
                throw new IllegalArgumentException("No userId:IP or token specified.");
            }
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        return true;
    }
}
