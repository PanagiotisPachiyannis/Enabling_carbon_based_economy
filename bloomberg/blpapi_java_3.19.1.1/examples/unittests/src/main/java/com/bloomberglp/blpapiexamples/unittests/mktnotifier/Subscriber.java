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
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;
import java.io.IOException;
import java.util.List;

public class Subscriber implements ISubscriber {

    private final Session session;

    public Subscriber(Session session) {
        this.session = session;
    }

    @Override
    public void subscribe(
            String service,
            List<String> topics,
            List<String> fields,
            List<String> options,
            Identity identity)
            throws IOException {
        SubscriptionList subscriptions = new SubscriptionList();
        for (String topic : topics) {
            String fullTopic = service + topic;
            Subscription subscription =
                    new Subscription(fullTopic, fields, options, new CorrelationID(topic));
            subscriptions.add(subscription);
        }
        session.subscribe(subscriptions, identity);
    }
}
