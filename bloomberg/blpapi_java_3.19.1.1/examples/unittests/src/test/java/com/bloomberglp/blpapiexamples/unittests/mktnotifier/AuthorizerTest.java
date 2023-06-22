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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventQueue;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.SchemaElementDefinition;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.test.TestUtil;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorizerTest {

    private static final String TEST_TOKEN = "testToken";

    @Mock private Session mockSession;
    @Mock private ITokenGenerator mockTokenGenerator;
    @Mock private EventQueue mockEventQueue;
    @Mock private Identity mockIdentity;
    @Captor private ArgumentCaptor<Request> requestCaptor;

    private Authorizer authorizer;
    private Service apiauthService;

    @BeforeEach
    public void setup() throws InterruptedException, IOException {
        apiauthService = loadApiauthService();
        authorizer = new Authorizer(mockSession, mockTokenGenerator);

        when(mockSession.createIdentity()).thenReturn(mockIdentity);
        when(mockSession.openService(AppConfig.DEFAULT_AUTH_SERVICE)).thenReturn(true);
        when(mockSession.getService(AppConfig.DEFAULT_AUTH_SERVICE)).thenReturn(apiauthService);

        when(mockTokenGenerator.generate()).thenReturn(TEST_TOKEN);
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(mockSession, mockTokenGenerator, mockEventQueue);
    }

    //
    // Concern:
    // Verify that for a valid identity the authorization returns true.
    //
    // Plan:
    // 1. Create admin event to represent the authorization success.
    // 2. Set up EventQueue.nextEvent() to return the event.
    // 3. Verify the authorize is success and identity is returned as expected.
    //
    @Test
    public void authorizationSuccess() throws IOException, InterruptedException {
        Event event = TestUtil.createEvent(EventType.AUTHORIZATION_STATUS);
        final SchemaElementDefinition elementDef =
                TestUtil.getAdminMessageDefinition(Names.AUTHORIZATION_SUCCESS);
        TestUtil.appendMessage(event, elementDef);

        when(mockEventQueue.nextEvent(anyLong())).thenReturn(event);

        final Identity identity =
                authorizer.authorize(
                        AppConfig.DEFAULT_AUTH_SERVICE, "auth_options", mockEventQueue);
        assertEquals(mockIdentity, identity);

        verifyMockInteractions();
    }

    //
    // Concern: Verify that the authorization throws exception for an invalid
    // identity .
    //
    // Plan:
    // 1. Create admin event to represent the authorization failure.
    // 2. Set up EventQueue.nextEvent() to return the event.
    // 3. Verify that RuntimeException is thrown.
    //
    @Test
    public void authorizationFailure() throws IOException, InterruptedException {
        Event event = TestUtil.createEvent(EventType.AUTHORIZATION_STATUS);
        final SchemaElementDefinition elementDef =
                TestUtil.getAdminMessageDefinition(Names.REQUEST_FAILURE);
        TestUtil.appendMessage(event, elementDef);

        when(mockEventQueue.nextEvent(anyLong())).thenReturn(event);

        try {
            authorizer.authorize(AppConfig.DEFAULT_AUTH_SERVICE, "auth_options", mockEventQueue);
            fail("Authorization should fail");
        } catch (RuntimeException ex) {
        }

        verifyMockInteractions();
    }

    private static Service loadApiauthService() {
        final Service service;
        final String resourceName = "/apiauthSchema.xml";
        try (InputStream is = EventProcessorTest.class.getResourceAsStream(resourceName)) {
            service = TestUtil.deserializeService(is);
        } catch (IOException ex) {
            fail();
            return null;
        }

        assertNotNull(service);
        return service;
    }

    private void verifyMockInteractions() throws InterruptedException, IOException {
        verify(mockSession).createIdentity();
        verify(mockSession).openService(AppConfig.DEFAULT_AUTH_SERVICE);
        verify(mockSession).getService(AppConfig.DEFAULT_AUTH_SERVICE);

        // Verify AuthorizationRequest has correct token
        verify(mockSession)
                .sendAuthorizationRequest(
                        requestCaptor.capture(),
                        eq(mockIdentity),
                        eq(mockEventQueue),
                        any(CorrelationID.class));
        final Request request = requestCaptor.getValue();
        assertTrue(request.hasElement(Authorizer.TOKEN));
        final Element element = request.getElement(Authorizer.TOKEN);
        assertEquals(TEST_TOKEN, element.getValueAsString());

        verify(mockTokenGenerator).generate();
        verify(mockEventQueue).nextEvent(anyLong());
    }
}
