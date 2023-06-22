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
import java.util.concurrent.atomic.AtomicReference;

public class UserModeExample {

    private static final Name SERVICE_NAME = Name.getName("serviceName");
    private static final Name SECURITIES = Name.getName("securities");
    private static final Name FIELDS = Name.getName("fields");
    private static final Name RETURN_EIDS = Name.getName("returnEids");

    private static final String REF_DATA_SVC_NAME = "//blp/refdata";

    private final List<String> securities = new ArrayList<>();
    private final SessionRouter<Session> router = new SessionRouter<>();
    private final AtomicReference<ExampleState> exampleState =
            new AtomicReference<>(ExampleState.STARTING);
    private final Map<CorrelationID, Identity> identitiesByCorrelationId = new HashMap<>();

    private Service blpRefDataSvc;
    private Session session;
    private ConnectionAndAuthOptions connectionAndAuthOptions;

    public UserModeExample() {
        router.addExceptionHandler(this::handleException);

        router.addMessageHandler(Names.SESSION_STARTED, this::handleSessionStarted);
        router.addMessageHandler(Names.SESSION_STARTUP_FAILURE, this::handleSessionStartupFailure);
        router.addMessageHandler(Names.SESSION_TERMINATED, this::handleSessionTerminated);

        router.addMessageHandler(Names.SERVICE_OPENED, this::handleServiceOpened);
        router.addMessageHandler(Names.SERVICE_OPEN_FAILURE, this::handleServiceOpenFailure);
    }

    public static void main(String[] args) throws Exception {
        UserModeExample example = new UserModeExample();

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

    private void run() throws InterruptedException, TlsInitializationException, IOException {
        createAndStartSession();
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

    private void stop() {
        // Cancel all the authorized identities
        session.cancel(new ArrayList<>(identitiesByCorrelationId.keySet()));

        try {
            session.stop(AbstractSession.StopOption.ASYNC);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void handleSessionStarted(Session session, Event event, Message message)
            throws IOException {
        // Add the authorization messages handlers after the session
        // started to only react to the authorization messages of users,
        // i.e., avoid those of the session identity.
        router.addMessageHandler(Names.AUTHORIZATION_SUCCESS, this::handleAuthorizationSuccess);
        router.addMessageHandler(Names.AUTHORIZATION_FAILURE, this::handleAuthorizationFailure);
        router.addMessageHandler(Names.AUTHORIZATION_REVOKED, this::handleAuthorizationRevoked);

        authorizeUsers();
        openServices();
    }

    private void handleSessionStartupFailure(Session session, Event event, Message message) {

        System.out.println("Failed to start session. Exiting...");

        exampleState.set(ExampleState.TERMINATED);
    }

    private void handleSessionTerminated(Session session, Event event, Message message) {
        exampleState.set(ExampleState.TERMINATED);
    }

    private void handleServiceOpened(Session session, Event event, Message message) {
        String serviceName = message.getElementAsString(SERVICE_NAME);
        Service service = session.getService(serviceName);

        if (serviceName.equals(REF_DATA_SVC_NAME)) {
            blpRefDataSvc = service;
            identitiesByCorrelationId.forEach(
                    (corId, identity) -> {
                        try {
                            sendRefDataRequest(identity, (String) corId.object());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } else {
            System.out.println("A service was opened: " + serviceName);
        }
    }

    private void handleServiceOpenFailure(Session sess, Event event, Message message) {
        String serviceName = message.getElementAsString(SERVICE_NAME);

        if (serviceName.equals(REF_DATA_SVC_NAME)) {
            stop();
        } else {
            System.out.println("A service which is unknown failed to open: " + serviceName);
        }
    }

    private void handleAuthorizationSuccess(Session session, Event event, Message message) {
        CorrelationID correlationId = message.correlationID();
        String userIdentifier = (String) correlationId.object();
        System.out.println("Successfully authorized " + userIdentifier);

        Identity identity = session.getAuthorizedIdentity(correlationId);
        identitiesByCorrelationId.put(correlationId, identity);

        if (blpRefDataSvc != null) {
            try {
                sendRefDataRequest(identity, userIdentifier);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAuthorizationFailure(Session session, Event event, Message message) {
        CorrelationID correlationId = message.correlationID();
        router.removeMessageHandler(correlationId);

        String userIdentifier = (String) correlationId.object();
        System.out.println("Failed to authorize " + userIdentifier);
    }

    private void handleAuthorizationRevoked(Session session, Event event, Message message) {
        CorrelationID correlationId = message.correlationID();
        router.removeMessageHandler(correlationId);

        String userIdentifier = (String) correlationId.object();
        System.out.println("Authorization revoked for " + userIdentifier);

        // Remove the identity
        identitiesByCorrelationId.remove(correlationId);
    }

    private void handleException(Session session, Event event, Exception exception) {
        exception.printStackTrace();
        stop();
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

    private void openServices() throws IOException {
        session.openServiceAsync(REF_DATA_SVC_NAME);
    }

    private void sendRefDataRequest(Identity identity, String userIdentifier) throws IOException {
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

        System.out.println("Sending RefDataRequest on behalf of " + userIdentifier + " ...");

        CorrelationID correlationId = new CorrelationID();
        router.addMessageHandler(
                correlationId,
                (session, event, msg) -> {
                    if (msg.messageType().equals(Names.REQUEST_FAILURE)) {
                        System.out.println("Request for " + userIdentifier + " failed.");
                        return;
                    }
                    System.out.println("Received response for " + userIdentifier + ".");
                });
        session.sendRequest(request, identity, correlationId);
    }

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser = new ArgParser("User Mode Example", UserModeExample.class);
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
