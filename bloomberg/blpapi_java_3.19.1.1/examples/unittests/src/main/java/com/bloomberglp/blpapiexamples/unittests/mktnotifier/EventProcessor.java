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

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Session;

public class EventProcessor implements EventHandler {
    private final INotifier notifier;
    private final IComputeEngine computeEngine;

    private static final Name LAST_PRICE = Name.getName(AppConfig.LAST_PRICE);

    public EventProcessor(INotifier notifier, IComputeEngine computeEngine) {
        this.notifier = notifier;
        this.computeEngine = computeEngine;
    }

    @Override
    public void processEvent(Event event, Session session) {
        for (Message msg : event) {
            EventType eventType = event.eventType();

            if (eventType == EventType.SESSION_STATUS) {
                notifier.logSessionState(msg);
            } else if (eventType == EventType.SUBSCRIPTION_STATUS) {
                notifier.logSubscriptionState(msg);
            } else if (eventType == EventType.SUBSCRIPTION_DATA) {
                if (msg.hasElement(LAST_PRICE)) {
                    double lastPrice = msg.getElementAsFloat64(LAST_PRICE);
                    double result = computeEngine.complexCompute(lastPrice);
                    notifier.sendToTerminal(result);
                }
            } else {
                break;
            }
        }
    }
}
