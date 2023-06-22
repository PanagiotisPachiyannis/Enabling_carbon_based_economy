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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Session;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationTest {

    @Mock private Session mockSession;
    @Mock private IAuthorizer mockAuthorizer;
    @Mock private ISubscriber mockSubscriber;
    @Mock private Identity mockIdentity;

    private AppConfig appConfig;
    private Application application;

    @BeforeEach
    public void setup() {
        appConfig = new AppConfig();
        application = new Application(mockSession, mockAuthorizer, mockSubscriber, appConfig);
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(mockSession, mockAuthorizer, mockSubscriber);
    }

    //
    // Concern:
    // Verify that if Session fails to start, no authorization and
    // subscriptions are made.
    //
    // Plan:
    // Set up Session.start() to return false.
    //
    @Test
    public void sessionStartFail() throws IOException, InterruptedException {
        when(mockSession.start()).thenReturn(false);
        application.run();
        verify(mockSession).start();
    }

    //
    // Concern:
    // Verify that if authorization fails, no subscriptions are made.
    //
    // Plan:
    // 1. Set up Session.start() to return true.
    // 2. Set up IAuthorizer.authorize() to throw RuntimeException.
    //
    @Test
    public void sessionAuthorizeFail() throws IOException, InterruptedException {
        when(mockSession.start()).thenReturn(true);

        final RuntimeException authException = new RuntimeException();
        doThrow(authException).when(mockAuthorizer).authorize(anyString(), isNull());

        try {
            application.run();
            fail("Authorization should fail");
        } catch (RuntimeException ex) {
            assertEquals(authException, ex);
        }

        verify(mockSession).start();
        verify(mockAuthorizer).authorize(anyString(), isNull());
    }

    //
    // Concern:
    // Verify correct auth service and auth options are used for authorization.
    //
    @Test
    public void authorizeWithConfig() throws IOException, InterruptedException {
        when(mockSession.start()).thenReturn(true);

        appConfig.authService = "authService";
        appConfig.authOptions = "app=test:app";

        when(mockAuthorizer.authorize(anyString(), anyString())).thenReturn(mockIdentity);

        application.run();

        verify(mockSession).start();
        verify(mockAuthorizer).authorize(eq(appConfig.authService), eq(appConfig.authOptions));
        verify(mockSubscriber)
                .subscribe(anyString(), anyList(), anyList(), anyList(), eq(mockIdentity));
    }

    //
    // Concern:
    // Verify that correct service, topics, fields and options are used when
    // subscribing.
    //
    @Test
    public void subscribeWithConfig() throws IOException, InterruptedException {
        when(mockSession.start()).thenReturn(true);

        appConfig.service = "mktdataService";
        appConfig.topics = Arrays.asList("IBM US Equity", "MSFT US Equity");
        appConfig.fields = Arrays.asList("LAST_PRICE", "BID", "ASK");
        appConfig.options = Arrays.asList("option1", "option2");

        when(mockAuthorizer.authorize(anyString(), isNull())).thenReturn(mockIdentity);

        application.run();

        verify(mockSession).start();
        verify(mockAuthorizer).authorize(anyString(), isNull());
        verify(mockSubscriber)
                .subscribe(
                        eq(appConfig.service),
                        eq(appConfig.topics),
                        eq(appConfig.fields),
                        eq(appConfig.options),
                        eq(mockIdentity));
    }
}
