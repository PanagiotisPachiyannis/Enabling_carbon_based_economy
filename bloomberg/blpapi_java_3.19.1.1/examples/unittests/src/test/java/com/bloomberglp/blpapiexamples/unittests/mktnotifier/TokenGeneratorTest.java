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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventQueue;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.SchemaElementDefinition;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.test.MessageFormatter;
import com.bloomberglp.blpapi.test.TestUtil;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TokenGeneratorTest {

    @Mock private Session mockSession;
    @Mock private EventQueue mockEventQueue;

    private TokenGenerator tokenGenerator;

    @BeforeEach
    public void setup() {
        tokenGenerator = new TokenGenerator(mockSession);
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(mockSession, mockEventQueue);
    }

    //
    // Concern: Verify that in case of successful token generation, a valid
    // token
    // is received by the application.
    //
    // Plan:
    // 1. Create a 'TOKEN_STATUS' event using TestUtil.createEvent().
    // 2. Obtain schema for 'TokenGenerationSuccess' using TestUtil.getAdminMessageDefinition().
    // 3. Append a 'TokenGenerationSuccess' message using TestUtil.appendMessage().
    // 4. Using the returned formatter, format the message. In this example
    //    the message is represented by XML
    //   <TokenGenerationSuccess><token>dummyToken</token></TokenGenerationSuccess>.
    // 5. Verify the actual token is correct.
    //
    @Test
    public void tokenGenerationSuccess() throws InterruptedException, IOException {
        Event event = TestUtil.createEvent(EventType.TOKEN_STATUS);
        SchemaElementDefinition elementDef =
                TestUtil.getAdminMessageDefinition(Names.TOKEN_GENERATION_SUCCESS);
        MessageFormatter formatter = TestUtil.appendMessage(event, elementDef);

        final String expectedToken = "dummyToken";
        final String messageContent =
                "<TokenGenerationSuccess>"
                        + "    <token>"
                        + expectedToken
                        + "</token>"
                        + "</TokenGenerationSuccess>";
        formatter.formatMessageXml(messageContent);

        when(mockSession.generateToken(any(CorrelationID.class), eq(mockEventQueue)))
                .thenReturn(new CorrelationID());
        when(mockEventQueue.nextEvent()).thenReturn(event);

        String token = tokenGenerator.generate(mockEventQueue);
        assertEquals(expectedToken, token);

        verify(mockSession).generateToken(any(CorrelationID.class), eq(mockEventQueue));
        verify(mockEventQueue).nextEvent();
    }

    //
    // Concern:
    // Verify that in case of failure in token generation, an empty
    // token is received by the application.
    //
    // Plan:
    // 1. Create a 'TOKEN_STATUS' event using TestUtil.createEvent().
    // 2. Obtain schema for 'TokenGenerationFailure' using TestUtil.getAdminMessageDefinition().
    // 3. Append 'TokenGenerationFailure' message using TestUtil.appendMessage().
    // 4. Using the returned formatter, format the message. In this example
    //    the message is represented by XML containing the reason of failure.
    // 5. Verify that the actual token is null.
    //
    @Test
    public void tokenGenerationFailure() throws InterruptedException, IOException {
        Event event = TestUtil.createEvent(EventType.TOKEN_STATUS);
        SchemaElementDefinition elementDef =
                TestUtil.getAdminMessageDefinition(Names.TOKEN_GENERATION_FAILURE);
        MessageFormatter formatter = TestUtil.appendMessage(event, elementDef);

        final String messageContent =
                "<TokenGenerationFailure>"
                        + "    <reason>"
                        + "        <source>apitkns (apiauth) on n795</source>"
                        + "        <errorCode>3</errorCode>"
                        + "        <category>NO_AUTH</category>"
                        + "        <description>App not in emrs.</description>"
                        + "        <subcategory>INVALID_APP</subcategory>"
                        + "    </reason>"
                        + "</TokenGenerationFailure>";
        formatter.formatMessageXml(messageContent);

        when(mockSession.generateToken(any(CorrelationID.class), eq(mockEventQueue)))
                .thenReturn(new CorrelationID());
        when(mockEventQueue.nextEvent()).thenReturn(event);

        String token = tokenGenerator.generate(mockEventQueue);
        assertNull(token);

        verify(mockSession).generateToken(any(CorrelationID.class), eq(mockEventQueue));
        verify(mockEventQueue).nextEvent();
    }
}
