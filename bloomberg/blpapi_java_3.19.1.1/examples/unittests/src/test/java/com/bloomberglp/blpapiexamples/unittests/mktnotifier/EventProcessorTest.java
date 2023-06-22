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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.SchemaElementDefinition;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.test.MessageFormatter;
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
public class EventProcessorTest {

    @Mock private Session mockSession;
    @Mock private INotifier mockNotifier;
    @Mock private IComputeEngine mockComputeEngine;
    @Captor private ArgumentCaptor<Message> messageCaptor;

    private EventProcessor eventProcessor;

    @BeforeEach
    public void setup() {
        eventProcessor = new EventProcessor(mockNotifier, mockComputeEngine);
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(mockSession, mockNotifier, mockComputeEngine);
    }

    //
    // Concern: Verify that notifier receives 'SessionStarted' message.
    //
    // Plan:
    // 1. Create a SessionStatus admin event using TestUtil.createEvent().
    // 2. Obtain the message schema using TestUtil.getAdminMessageDefinition().
    // 3. Append a message of type 'SessionStarted' using TestUtil.appendMessage().
    // 4. Verify INotifier.logSessionState() is called and save the message
    //    which should be passed from EventProcessor to INotifier.
    // 5. Verify that the actual messages is correct.
    //
    @Test
    public void notifierReceivesSessionStarted() {
        final Name messageType = Names.SESSION_STARTED;

        Event event = TestUtil.createEvent(EventType.SESSION_STATUS);
        final SchemaElementDefinition elementDef = TestUtil.getAdminMessageDefinition(messageType);
        TestUtil.appendMessage(event, elementDef);

        eventProcessor.processEvent(event, mockSession);

        verify(mockNotifier).logSessionState(messageCaptor.capture());
        final Message message = messageCaptor.getValue();
        assertEquals(messageType, message.messageType());
    }

    //
    // Concern: Verify that notifier receives 'SubscriptionStarted' message.

    // Plan:
    // 1. Create a 'SubscriptionStatus' admin event using TestUtil.createEvent().
    // 2. Obtain the message schema using TestUtil.getAdminMessageDefinition().
    // 3. Append a message of type 'SubscriptionStarted' TestUtil.appendMessage().
    // 4. Verify INotifier.logSubscriptionState() is called and save the
    //    message which should be passed from EventProcessor to INotifier.
    // 5. Verify that the actual messages is correct.
    //
    @Test
    public void notifierReceivesSubscriptionStarted() {
        final Name messageType = Names.SUBSCRIPTION_STARTED;

        Event event = TestUtil.createEvent(EventType.SUBSCRIPTION_STATUS);
        final SchemaElementDefinition elementDef = TestUtil.getAdminMessageDefinition(messageType);
        TestUtil.appendMessage(event, elementDef);

        eventProcessor.processEvent(event, mockSession);

        verify(mockNotifier).logSubscriptionState(messageCaptor.capture());
        final Message message = messageCaptor.getValue();
        assertEquals(messageType, message.messageType());
    }

    //
    // Concern: Verify that:
    // IComputeEngine receives correct LAST_PRICE and INotifier sends correct
    // value to terminal.
    //
    // Plan:
    // 1. Obtain the service by deserializing its schema.
    // 2. Create a SubscriptionEvent using TestUtil.createEvent().
    // 3. Obtain the element schema definition from the service.
    // 4. Append a message of type 'MarketDataEvents' using TestUtil.appendMessage().
    // 5. Format the message using formatter returned by TestUtil.appendMessage().
    //    In this example the message is represented by XML
    //    <MarketDataEvents><LAST_PRICE>142.80</LAST_PRICE></MarketDataEvents>.
    // 6. Set up IComputeEngine.complexCompute() to return a pre-defined value.
    // 7. Verify that IComputeEngine gets correct value and INotifier sends
    //    correct value to terminal.
    //
    @Test
    public void notifierReceivesSubscriptionData() throws IOException {
        final double lastPrice = 142.80;
        final Event event = createSubscriptionDataWithLastPrice(lastPrice);
        final double expectedComputeResult = 200.0;
        when(mockComputeEngine.complexCompute(lastPrice)).thenReturn(expectedComputeResult);
        eventProcessor.processEvent(event, mockSession);

        verify(mockComputeEngine).complexCompute(lastPrice);
        verify(mockNotifier).sendToTerminal(expectedComputeResult);
    }

    private static Event createSubscriptionDataWithLastPrice(double lastPrice) throws IOException {
        final Service service;
        final String resourceName = "/mktdataSchema.xml";
        try (InputStream is = EventProcessorTest.class.getResourceAsStream(resourceName)) {
            service = TestUtil.deserializeService(is);
        }

        assertNotNull(service);
        Event event = TestUtil.createEvent(EventType.SUBSCRIPTION_DATA);
        final Name messageType = Name.getName("MarketDataEvents");
        final SchemaElementDefinition schemaDef = service.getEventDefinition(messageType);
        MessageFormatter formatter = TestUtil.appendMessage(event, schemaDef);

        String messageContent =
                "<MarketDataEvents>"
                        + "    <LAST_PRICE>"
                        + lastPrice
                        + "</LAST_PRICE>"
                        + "</MarketDataEvents>";
        formatter.formatMessageXml(messageContent);

        return event;
    }
}
