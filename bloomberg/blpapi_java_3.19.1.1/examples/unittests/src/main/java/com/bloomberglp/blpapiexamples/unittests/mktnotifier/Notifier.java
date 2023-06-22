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

import com.bloomberglp.blpapi.Message;

/**
 * {@code Notifier} is an approximation of a class that attempts to:
 *
 * <ul>
 *   <li>Log all session events, e.g. {@code SessionStartupFailure}.
 *   <li>Log all subscription events, e.g. {@code SubscriptionStarted}.
 *   <li>Deliver subscription data to terminal, e.g. {@code Bloomberg terminal}.
 * </ul>
 */
public class Notifier implements INotifier {

    @Override
    public void logSessionState(Message msg) {
        System.out.println("Logging Session state with: " + msg);
    }

    @Override
    public void logSubscriptionState(Message msg) {
        System.out.println("Logging Subscription state with: " + msg);
    }

    @Override
    public void sendToTerminal(double value) {
        System.out.println("VALUE = " + value);
    }
}