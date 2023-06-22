/*
 * Copyright 2021, Bloomberg Finance L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright
 * notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.bloomberglp.blpapiexamples.demoapps.util.events;

import com.bloomberglp.blpapi.AbstractSession;
import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionRouter<T extends AbstractSession> {
    private final Map<EventType, RouterEventHandler<T>> eventHandlersByEventType = new HashMap<>();

    private final Map<EventType, RouterMessageHandler<T>> messageHandlersByEventType =
            new HashMap<>();

    private final Map<Name, RouterMessageHandler<T>> messageHandlersByMessageType = new HashMap<>();

    private final Map<CorrelationID, RouterMessageHandler<T>> messageHandlersByCorrelationId =
            new HashMap<>();

    private final List<ExceptionHandler<T>> exceptionHandlers = new ArrayList<>();

    public void addEventHandler(EventType eventType, RouterEventHandler<T> handler) {
        eventHandlersByEventType.put(eventType, handler);
    }

    public void addMessageHandler(EventType eventType, RouterMessageHandler<T> handler) {
        messageHandlersByEventType.put(eventType, handler);
    }

    public void addMessageHandler(Name messageType, RouterMessageHandler<T> handler) {
        messageHandlersByMessageType.put(messageType, handler);
    }

    public void addMessageHandler(CorrelationID correlationId, RouterMessageHandler<T> handler) {
        messageHandlersByCorrelationId.put(correlationId, handler);
    }

    public void removeMessageHandler(CorrelationID correlationId) {
        messageHandlersByCorrelationId.remove(correlationId);
    }

    public void addExceptionHandler(ExceptionHandler<T> handler) {
        exceptionHandlers.add(handler);
    }

    public void processEvent(Event event, T session) {
        try {
            printEvent(event);

            RouterEventHandler<T> eventTypeEventHandler =
                    eventHandlersByEventType.get(event.eventType());

            if (eventTypeEventHandler != null) {
                eventTypeEventHandler.accept(session, event);
            }

            for (Message message : event) {
                for (int i = 0; i < message.numCorrelationIds(); ++i) {
                    CorrelationID cid = message.correlationID(i);
                    if (cid == null) {
                        break;
                    }

                    RouterMessageHandler<T> correlationIdMessageHandler =
                            messageHandlersByCorrelationId.get(cid);
                    if (correlationIdMessageHandler != null) {
                        correlationIdMessageHandler.accept(session, event, message);
                    }
                }

                RouterMessageHandler<T> eventTypeMessageHandler =
                        messageHandlersByEventType.get(event.eventType());
                if (eventTypeMessageHandler != null) {
                    eventTypeMessageHandler.accept(session, event, message);
                }

                RouterMessageHandler<T> messageTypeMessageHandler =
                        messageHandlersByMessageType.get(message.messageType());
                if (messageTypeMessageHandler != null) {
                    messageTypeMessageHandler.accept(session, event, message);
                }
            }
        } catch (Exception exception) {
            for (ExceptionHandler<T> handler : exceptionHandlers) {
                handler.accept(session, event, exception);
            }
        }
    }

    private static void printEvent(Event event) {
        for (Message message : event) {
            Service service = message.service();
            if (service != null) {
                System.out.println("Service: " + service.name());
            }

            System.out.println(message);
        }
    }
}
