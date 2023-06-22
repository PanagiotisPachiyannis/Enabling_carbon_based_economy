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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.ProviderSession;
import com.bloomberglp.blpapi.SchemaElementDefinition;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.ServiceRegistrationOptions;
import com.bloomberglp.blpapi.ServiceRegistrationOptions.RegistrationParts;
import com.bloomberglp.blpapi.test.MessageFormatter;
import com.bloomberglp.blpapi.test.MessageProperties;
import com.bloomberglp.blpapi.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResolverUtilsTest {
    private static final int INVALID_APP_ID = 4321;
    @Mock private ProviderSession mockSession;
    @Mock private Identity mockIdentity;

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(mockSession, mockIdentity);
    }

    // This test demonstrates how to mock interactions with ProviderSession.
    // This test sets up the return value of ProviderSession.registerService
    // and verifies the input arguments.
    @Test
    public void resolutionServiceRegistration() throws InterruptedException {
        String serviceName = "//blp/mytestservice";
        when(mockSession.registerService(
                        eq(serviceName), eq(mockIdentity), any(ServiceRegistrationOptions.class)))
                .thenReturn(true);

        boolean success =
                ResolverUtils.resolutionServiceRegistration(mockSession, mockIdentity, serviceName);
        assertTrue(success);

        ArgumentCaptor<ServiceRegistrationOptions> optionsCaptor =
                ArgumentCaptor.forClass(ServiceRegistrationOptions.class);
        verify(mockSession)
                .registerService(eq(serviceName), eq(mockIdentity), optionsCaptor.capture());

        // Verify that the service is registered with the expected options.
        ServiceRegistrationOptions actualOptions = optionsCaptor.getValue();
        final int expectedPriority = 123;
        assertEquals(expectedPriority, actualOptions.getServicePriority());
        assertEquals(
                RegistrationParts.PART_SUBSCRIBER_RESOLUTION,
                actualOptions.getPartsToRegister() & RegistrationParts.PART_SUBSCRIBER_RESOLUTION);
    }

    // This test demonstrates how to create a successful permission response
    // and verifies its content is as expected.
    @Test
    public void successfulResolution() {
        Service service = getService();

        CorrelationID cid = new CorrelationID();
        Event permissionEvent = createPermissionEvent(cid, ResolverUtils.ALLOWED_APP_ID);
        Message permissionRequest = getFirstMessage(permissionEvent);

        ResolverUtils.handlePermissionRequest(mockSession, service, permissionRequest);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockSession).sendResponse(eventCaptor.capture());
        Event response = eventCaptor.getValue();

        assertEquals(EventType.RESPONSE, response.eventType());
        Message permissionResponse = getFirstMessage(response);
        assertEquals(cid, permissionResponse.correlationID());
        assertEquals(ResolverUtils.PERMISSION_RESPONSE, permissionResponse.messageType());

        assertTrue(permissionResponse.hasElement(ResolverUtils.TOPIC_PERMISSIONS));
        Element topicPermissions = permissionResponse.getElement(ResolverUtils.TOPIC_PERMISSIONS);
        int numTopics = 2;
        assertEquals(numTopics, topicPermissions.numValues());
        for (int i = 0; i < numTopics; ++i) {
            Element topicPermission = topicPermissions.getValueAsElement(i);
            assertTrue(topicPermission.hasElement(ResolverUtils.RESULT));
            assertEquals(0, topicPermission.getElementAsInt32(ResolverUtils.RESULT));
        }
    }

    // This test demonstrates how to create a failure permission response and
    // verify its content is as expected.
    @Test
    public void failedResolution() {
        Service service = getService();

        CorrelationID cid = new CorrelationID();
        Event permissionEvent = createPermissionEvent(cid, INVALID_APP_ID);
        Message permissionRequest = getFirstMessage(permissionEvent);

        ResolverUtils.handlePermissionRequest(mockSession, service, permissionRequest);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockSession).sendResponse(eventCaptor.capture());
        Event response = eventCaptor.getValue();

        assertEquals(EventType.RESPONSE, response.eventType());
        Message permissionResponse = getFirstMessage(response);
        assertEquals(cid, permissionResponse.correlationID());
        assertEquals(ResolverUtils.PERMISSION_RESPONSE, permissionResponse.messageType());

        assertTrue(permissionResponse.hasElement(ResolverUtils.TOPIC_PERMISSIONS));
        Element topicPermissions = permissionResponse.getElement(ResolverUtils.TOPIC_PERMISSIONS);
        int numTopics = 2;
        assertEquals(numTopics, topicPermissions.numValues());

        for (int i = 0; i < numTopics; ++i) {
            Element topicPermission = topicPermissions.getValueAsElement(i);
            assertTrue(topicPermission.hasElement(ResolverUtils.RESULT));
            assertEquals(1, topicPermission.getElementAsInt32(ResolverUtils.RESULT));

            assertTrue(topicPermission.hasElement(ResolverUtils.REASON));
            Element reason = topicPermission.getElement(ResolverUtils.REASON);
            assertTrue(reason.hasElement(ResolverUtils.CATEGORY));
            assertEquals(
                    ResolverUtils.NOT_AUTHORIZED,
                    reason.getElementAsString(ResolverUtils.CATEGORY));
        }
    }

    private static Message getFirstMessage(Event event) {
        for (Message msg : event) {
            return msg;
        }

        throw new RuntimeException("No messages in event");
    }

    private static Service getService() {
        String schema =
                "<ServiceDefinition xsi:schemaLocation=\"http://bloomberg.com/schemas/apidd apidd.xsd\""
                        + "                   name=\"test-svc\""
                        + "                   version=\"1.0.0.0\""
                        + "                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "  <service name=\"//blp-test/test-svc\" version=\"1.0.0.0\">"
                        + "    <event name=\"Events\" eventType=\"EventType\">"
                        + "        <eventId>1</eventId>"
                        + "    </event>"
                        + "    <defaultServiceId>12345</defaultServiceId>"
                        + "    <publisherSupportsRecap>false</publisherSupportsRecap>"
                        + "    <authoritativeSourceSupportsRecap>false</authoritativeSourceSupportsRecap>"
                        + "    <SubscriberResolutionServiceId>12346</SubscriberResolutionServiceId>"
                        + "  </service>"
                        + "  <schema>"
                        + "      <sequenceType name=\"EventType\">"
                        + "         <element name=\"price\" type=\"Float64\" minOccurs=\"0\" maxOccurs=\"1\"/>"
                        + "      </sequenceType>"
                        + "   </schema>"
                        + "</ServiceDefinition>";

        InputStream stream = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
        Service service = TestUtil.deserializeService(stream);

        return service;
    }

    private static Event createPermissionEvent(CorrelationID cid, int applicationId) {
        MessageProperties props = new MessageProperties();
        props.setCorrelationId(cid);

        // Create sample blpapi event
        Event request = TestUtil.createEvent(EventType.REQUEST);

        SchemaElementDefinition schemaDef =
                TestUtil.getAdminMessageDefinition(ResolverUtils.PERMISSION_REQUEST);

        String content =
                "<PermissionRequest>"
                        + "    <topics>topic1</topics>"
                        + "    <topics>topic2</topics>"
                        + "    <serviceName>//blp/mytestservice</serviceName>"
                        + "    <applicationId>"
                        + applicationId
                        + "</applicationId>"
                        + "</PermissionRequest>";

        MessageFormatter formatter = TestUtil.appendMessage(request, schemaDef, props);
        formatter.formatMessageXml(content);

        return request;
    }
}
