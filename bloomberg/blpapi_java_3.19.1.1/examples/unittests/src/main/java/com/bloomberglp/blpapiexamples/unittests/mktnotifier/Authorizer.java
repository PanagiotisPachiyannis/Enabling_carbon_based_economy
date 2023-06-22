/*
 * Copyright 2019. Bloomberg Finance L.P.
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
package com.bloomberglp.blpapiexamples.unittests.mktnotifier;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventQueue;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import java.io.IOException;

public class Authorizer implements IAuthorizer {
    private static final int WAIT_TIME_MS = 10000; // 10 seconds

    protected static final Name TOKEN = Name.getName("token");

    private final Session session;
    private final ITokenGenerator tokenGenerator;

    public Authorizer(Session session, ITokenGenerator tokenGenerator) {
        this.session = session;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Identity authorize(String authService, String authOptions, EventQueue eventQueue)
            throws IOException, InterruptedException {

        if (authOptions != null && !authOptions.isEmpty()) {
            session.openService(authService);
            Service service = session.getService(authService);
            return authorize(service, eventQueue);
        }

        return null;
    }

    private Identity authorize(Service authService, EventQueue eventQueue)
            throws IOException, InterruptedException {
        String token = tokenGenerator.generate();
        if (token == null) {
            throw new RuntimeException("Failed to generate token.");
        }

        Request authRequest = authService.createAuthorizationRequest();
        authRequest.set(TOKEN, token);
        EventQueue authEventQueue = eventQueue == null ? new EventQueue() : eventQueue;
        Identity identity = session.createIdentity();
        session.sendAuthorizationRequest(
                authRequest, identity, authEventQueue, new CorrelationID("auth"));

        while (true) {
            Event event = authEventQueue.nextEvent(WAIT_TIME_MS);

            EventType eventType = event.eventType();
            if (eventType == EventType.RESPONSE
                    || eventType == EventType.REQUEST_STATUS
                    || eventType == EventType.PARTIAL_RESPONSE
                    || eventType == EventType.AUTHORIZATION_STATUS) {
                for (Message msg : event) {
                    if (msg.messageType() == Names.AUTHORIZATION_SUCCESS) {
                        return identity;
                    }

                    throw new RuntimeException("Failed to authorize: " + msg);
                }
            }

            if (eventType == EventType.TIMEOUT) {
                throw new RuntimeException("Authorization timed out");
            }
        }
    }
}
