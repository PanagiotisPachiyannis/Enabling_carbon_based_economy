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
import com.bloomberglp.blpapi.EventQueue;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Session;
import java.io.IOException;

public class TokenGenerator implements ITokenGenerator {

    private static final Name TOKEN = Name.getName("token");

    private final Session session;

    public TokenGenerator(Session session) {
        this.session = session;
    }

    @Override
    public String generate(EventQueue eventQueue) throws IOException, InterruptedException {
        EventQueue tokenEventQueue = eventQueue == null ? new EventQueue() : eventQueue;
        session.generateToken(new CorrelationID(), tokenEventQueue);

        Event event = tokenEventQueue.nextEvent();
        String token = null;
        for (Message msg : event) {
            Name messageType = msg.messageType();

            if (messageType.equals(Names.TOKEN_GENERATION_SUCCESS)) {
                token = msg.getElementAsString(TOKEN);
                break;
            }

            if (messageType.equals(Names.TOKEN_GENERATION_FAILURE)) {
                break;
            }
        }

        return token;
    }
}
