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
package com.bloomberglp.blpapiexamples.unittests.snippets.resolver;

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventFormatter;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.ProviderSession;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.ServiceRegistrationOptions;
import com.bloomberglp.blpapi.ServiceRegistrationOptions.RegistrationParts;

/**
 * A utility class that shows interactions that are particular to resolvers, i.e. register a
 * resolver service and handle a permission request.
 */
public class ResolverUtils {
    // 'Name' objects are more expensive to construct than String,
    // but are more efficient on use through the interface. By creating the
    // 'Name' objects in advance we can take advantage of the efficiency
    // without paying the cost of constructing them when needed.
    protected static final Name PERMISSION_REQUEST = Name.getName("PermissionRequest");
    protected static final Name PERMISSION_RESPONSE = Name.getName("PermissionResponse");
    protected static final Name TOPIC_PERMISSIONS = Name.getName("topicPermissions");
    protected static final Name RESULT = Name.getName("result");
    protected static final Name REASON = Name.getName("reason");
    protected static final Name CATEGORY = Name.getName("category");
    protected static final String NOT_AUTHORIZED = "NOT_AUTHORIZED";
    protected static final int ALLOWED_APP_ID = 1234;

    private static final Name TOPIC = Name.getName("topic");
    private static final Name TOPICS = Name.getName("topics");
    private static final Name SOURCE = Name.getName("source");
    private static final Name SUBCATEGORY = Name.getName("subcategory");
    private static final Name DESCRIPTION = Name.getName("description");

    // This can be any string, but it's helpful to provide information on the
    // instance of the resolver that responded to debug failures in production.
    private static final String RESOLVER_ID = "service:hostname";
    private static final Name APPLICATION_ID = Name.getName("applicationId");

    /**
     * Demonstrates how to register a resolver service.
     *
     * <p>This method assumes the following:
     * <li><code>session</code> is already started;
     * <li><code>providerIdentity</code> is already authorized if auth is needed or null if
     *     authorization is not required.
     */
    public static boolean resolutionServiceRegistration(
            ProviderSession session, Identity providerIdentity, String serviceName)
            throws InterruptedException {
        // Prepare registration options
        ServiceRegistrationOptions serviceOptions = new ServiceRegistrationOptions();

        final int dummyPriority = 123;
        serviceOptions.setServicePriority(dummyPriority);

        serviceOptions.setPartsToRegister(RegistrationParts.PART_SUBSCRIBER_RESOLUTION);

        if (!session.registerService(serviceName, providerIdentity, serviceOptions)) {
            System.err.println("Failed to register " + serviceName);
            return false;
        }

        return true;
    }

    public static void handlePermissionRequest(
            ProviderSession session, Service service, Message request) {
        assert request.messageType().equals(PERMISSION_REQUEST)
                : "request must be a permission request";

        boolean allowed =
                request.hasElement(APPLICATION_ID)
                        && request.getElementAsInt32(APPLICATION_ID) == ALLOWED_APP_ID;

        Event response = service.createResponseEvent(request.correlationID());
        EventFormatter formatter = new EventFormatter(response);
        formatter.appendResponse(PERMISSION_RESPONSE);

        formatter.pushElement(TOPIC_PERMISSIONS);

        Element topics = request.getElement(TOPICS);
        for (int i = 0; i < topics.numValues(); ++i) {
            formatter.appendElement();
            formatter.setElement(TOPIC, topics.getValueAsString(i));
            // ALLOWED: 0, DENIED: 1
            formatter.setElement(RESULT, allowed ? 0 : 1);

            if (!allowed) {
                formatter.pushElement(REASON);
                formatter.setElement(SOURCE, RESOLVER_ID);
                formatter.setElement(CATEGORY, NOT_AUTHORIZED);
                formatter.setElement(SUBCATEGORY, "");
                formatter.setElement(
                        DESCRIPTION, "Only app " + ResolverUtils.ALLOWED_APP_ID + " allowed");
                formatter.popElement();
            }

            formatter.popElement();
        }

        formatter.popElement();

        session.sendResponse(response);
    }
}
