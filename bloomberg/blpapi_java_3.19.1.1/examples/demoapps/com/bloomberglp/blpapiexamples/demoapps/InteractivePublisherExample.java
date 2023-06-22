/*
 * Copyright 2021, Bloomberg Finance L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:  The above
 * copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.bloomberglp.blpapiexamples.demoapps;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventFormatter;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.ProviderEventHandler;
import com.bloomberglp.blpapi.ProviderSession;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.ServiceRegistrationOptions;
import com.bloomberglp.blpapi.ServiceRegistrationOptions.ServiceRegistrationPriority;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapi.Topic;
import com.bloomberglp.blpapi.TopicList;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InteractivePublisherExample {
    private static final Name APPLICATION_ID = Name.getName("applicationId");
    private static final Name ATTR = Name.getName("attr");
    private static final Name CATEGORY = Name.getName("category");
    private static final Name DESCRIPTION = Name.getName("description");
    private static final Name EIDS = Name.getName("eids");
    private static final Name FG_COLOR = Name.getName("fgColor");
    private static final Name HIGH = Name.getName("HIGH");
    private static final Name LENGTH = Name.getName("length");
    private static final Name LOW = Name.getName("LOW");
    private static final Name MARKET_DATA_EVENTS = Name.getName("MarketDataEvents");
    private static final Name NUM_COLS = Name.getName("numCols");
    private static final Name NUM_ROWS = Name.getName("numRows");
    private static final Name OPEN = Name.getName("OPEN");
    private static final Name PERMISSIONS = Name.getName("permissions");
    private static final Name PERMISSION_SERVICE = Name.getName("permissionService");
    private static final Name RESOLVED_TOPIC = Name.getName("resolvedTopic");
    private static final Name RESULT = Name.getName("result");
    private static final Name REASON = Name.getName("reason");
    private static final Name ROW_NUM = Name.getName("rowNum");
    private static final Name ROW_UPDATE = Name.getName("rowUpdate");
    private static final Name MESSAGE_TYPE_ROW_UPDATE = Name.getName("RowUpdate");
    private static final Name SPAN_UPDATE = Name.getName("spanUpdate");
    private static final Name SOURCE = Name.getName("source");
    private static final Name SUB_CATEGORY = Name.getName("subcategory");
    private static final Name START_COL = Name.getName("startCol");
    private static final Name SUB_SERVICE_CODE = Name.getName("subServiceCode");
    private static final Name TEXT = Name.getName("text");
    private static final Name TOPIC = Name.getName("topic");
    private static final Name TOPICS = Name.getName("topics");
    private static final Name TOPIC_PERMISSIONS = Name.getName("topicPermissions");
    private static final Name UUID = Name.getName("uuid");

    private String serviceName;
    private final List<Integer> eids = new ArrayList<>();

    private final Set<Topic> activeTopics = new HashSet<>();

    private String groupId = null;
    private int priority = ServiceRegistrationPriority.PRIORITY_HIGH;

    private int clearInterval = 0;

    private boolean useSsc = false;
    private int sscBegin;
    private int sscEnd;
    private int sscPriority;

    private Integer resolveSubServiceCode = null;
    private boolean running = true;
    private boolean pageEnabled = false;
    private final Object lock = new Object();

    private ConnectionAndAuthOptions connectionAndAuthOptions;

    private class MyEventHandler implements ProviderEventHandler {

        @Override
        public void processEvent(Event event, ProviderSession session) {
            try {
                doProcessEvent(event, session);
            } catch (Exception e) {
                // don't let exceptions thrown by the library go back
                // into the library unnoticed
                e.printStackTrace();
            }
        }

        private void doProcessEvent(Event event, ProviderSession session) {
            System.out.println("Received event: " + event);

            EventType eventType = event.eventType();
            if (eventType == EventType.SESSION_STATUS) {
                for (Message msg : event) {
                    if (msg.messageType() == Names.SESSION_TERMINATED) {
                        System.out.println("Session terminated, stopping application...");
                        synchronized (lock) {
                            running = false;
                        }

                        break;
                    }
                }
            } else if (eventType == EventType.TOPIC_STATUS) {
                processTopicStatusEvent(event, session);
            } else if (eventType == EventType.RESOLUTION_STATUS) {
                for (Message msg : event) {
                    if (msg.messageType() == Names.RESOLUTION_SUCCESS) {
                        String resolvedTopic = msg.getElementAsString(RESOLVED_TOPIC);
                        System.out.println("ResolvedTopic: " + resolvedTopic);
                    } else if (msg.messageType() == Names.RESOLUTION_FAILURE) {
                        System.out.println(
                                "Topic resolution failed (cid = " + msg.correlationID() + ")");
                    }
                }
            } else if (eventType == EventType.REQUEST) {
                for (Message msg : event) {
                    if (msg.messageType() == Names.PERMISSION_REQUEST) {
                        processPermissionRequest(session, msg);
                    } else {
                        System.out.println("Received unknown request: " + msg);
                    }
                }
            }
        }

        private void processPermissionRequest(ProviderSession session, Message msg) {
            Service service = session.getService(serviceName);

            // Similar to createPublishEvent. We assume just one service -
            // d_service. A responseEvent can only be for single request so we
            // can specify the correlationId - which establishes context -
            // when we create the Event.
            Event response = service.createResponseEvent(msg.correlationID());
            EventFormatter ef = new EventFormatter(response);
            int permission = 1; // ALLOWED: 0, DENIED: 1
            if (msg.hasElement(UUID)) {
                int uuid = msg.getElementAsInt32(UUID);
                System.out.println("UUID = " + uuid);
                permission = 0;
            }

            if (msg.hasElement(APPLICATION_ID)) {
                int applicationId = msg.getElementAsInt32(APPLICATION_ID);
                System.out.println("APPID = " + applicationId);
                permission = 0;
            }

            // In appendResponse the string is the name of the operation, the
            // correlationId indicates which request we are responding to.
            ef.appendResponse(Names.PERMISSION_RESPONSE);
            ef.pushElement(TOPIC_PERMISSIONS);
            // For each of the topics in the request, add an entry to the
            // response
            Element topicsElement = msg.getElement(TOPICS);
            for (int i = 0; i < topicsElement.numValues(); ++i) {
                ef.appendElement();
                ef.setElement(TOPIC, topicsElement.getValueAsString(i));
                ef.setElement(RESULT, permission); // ALLOWED: 0, DENIED: 1

                if (permission == 1) { // DENIED
                    ef.pushElement(REASON);
                    ef.setElement(SOURCE, "My Publisher Name");
                    ef.setElement(CATEGORY, "NOT_AUTHORIZED");
                    // or BAD_TOPIC, or custom

                    ef.setElement(SUB_CATEGORY, "Publisher Controlled");
                    ef.setElement(DESCRIPTION, "Permission denied by My Publisher Name");
                    ef.popElement();
                } else { // ALLOWED
                    if (resolveSubServiceCode != null) {
                        ef.setElement(SUB_SERVICE_CODE, resolveSubServiceCode);
                        System.err.format(
                                "Mapping topic %1$s to subserviceCode %2$d",
                                topicsElement.getValueAsString(i), resolveSubServiceCode);
                    }
                    if (!eids.isEmpty()) {
                        ef.pushElement(PERMISSIONS);
                        ef.appendElement();
                        ef.setElement(PERMISSION_SERVICE, "//blp/blpperm");

                        ef.pushElement(EIDS);
                        for (int eid : eids) {
                            ef.appendValue(eid);
                        }

                        ef.popElement();
                        ef.popElement();
                        ef.popElement();
                    }
                }

                ef.popElement();
            }

            ef.popElement();

            // Service is implicit in the Event. sendResponse has a second
            // parameter - partialResponse - that defaults to false.
            session.sendResponse(response);
        }

        private void processTopicStatusEvent(Event event, ProviderSession session) {
            TopicList topicsToCreate = new TopicList();
            List<Topic> unsubscribedTopics = new ArrayList<>();
            for (Message msg : event) {
                Name messageType = msg.messageType();
                Topic topic = session.getTopic(msg);

                if (messageType == Names.TOPIC_SUBSCRIBED) {
                    if (topic == null) {
                        // Add the topic contained in the message to TopicList
                        String topicString = msg.getElementAsString(TOPIC);
                        CorrelationID cid = new CorrelationID(topicString);
                        topicsToCreate.add(msg, cid);
                    }
                } else if (messageType == Names.TOPIC_UNSUBSCRIBED) {
                    unsubscribedTopics.add(topic);
                    synchronized (lock) {
                        activeTopics.remove(topic);
                        lock.notifyAll();
                    }
                } else if (messageType == Names.TOPIC_ACTIVATED) {
                    synchronized (lock) {
                        activeTopics.add(topic);
                        lock.notifyAll();
                    }
                } else if (messageType == Names.TOPIC_RECAP) {
                    // Here we send a recap in response to a Recap Request.
                    Service service = topic.service();
                    Event recapEvent = service.createPublishEvent();
                    EventFormatter eventFormatter = new EventFormatter(recapEvent);
                    CorrelationID correlationId = msg.correlationID();
                    eventFormatter.appendRecapMessage(topic, correlationId);
                    if (pageEnabled) {
                        formatPageRecapEvent(eventFormatter);
                    } else {
                        formatMarketDataRecapEvent(eventFormatter);
                    }

                    session.publish(recapEvent);
                    System.out.println("Publishing Recap: " + recapEvent);
                }
            }

            // createTopicsAsync will result in RESOLUTION_STATUS,
            // TOPIC_STATUS events.
            if (topicsToCreate.size() > 0) {
                session.createTopicsAsync(topicsToCreate);
            }

            // Delete all the unsubscribed topics.
            if (!unsubscribedTopics.isEmpty()) {
                session.deleteTopics(unsubscribedTopics);
            }
        }
    }

    private void activate(ProviderSession session) {
        if (useSsc) {
            System.out.format(
                    "Activating sub service code range [%1$d, %2$d] " + "@ priority: %3$d",
                    sscBegin, sscEnd, sscPriority);
            session.activateSubServiceCodeRange(serviceName, sscBegin, sscEnd, sscPriority);
        }
    }

    private void deactivate(ProviderSession session) {
        if (useSsc) {
            System.out.format(
                    "Deactivating sub service code range [%1$d, %2$d] " + "@ priority: %3$d",
                    sscBegin, sscEnd, sscPriority);
            session.deactivateSubServiceCodeRange(serviceName, sscBegin, sscEnd);
        }
    }

    private void parseSubServiceCodeRangeAndPriority(String value) {
        String[] splitRange = value.split(",");
        if (splitRange.length != 3) {
            throw new IllegalArgumentException("Invalid sub-service code range: " + value);
        }

        useSsc = true;
        sscBegin = Integer.parseInt(splitRange[0]);
        sscEnd = Integer.parseInt(splitRange[1]);
        sscPriority = Integer.parseInt(splitRange[2]);
    }

    public void run(String[] args)
            throws InterruptedException, TlsInitializationException, IOException {
        if (!parseCommandLine(args)) {
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();

        ProviderSession session = new ProviderSession(sessionOptions, new MyEventHandler());

        if (!session.start()) {
            System.err.println("Failed to start session");
            return;
        }

        try {
            ServiceRegistrationOptions serviceRegistrationOptions =
                    new ServiceRegistrationOptions();
            serviceRegistrationOptions.setGroupId(groupId);
            serviceRegistrationOptions.setServicePriority(priority);

            if (useSsc) {
                System.out.format(
                        "Activating sub service code range [%1$d, %2$d] " + "@ priority: %3$d",
                        sscBegin, sscEnd, sscPriority);
                try {
                    serviceRegistrationOptions.addActiveSubServiceCodeRange(
                            sscBegin, sscEnd, sscPriority);
                } catch (Exception e) {
                    System.err.println("FAILED to add active sub service codes.");
                    e.printStackTrace();
                }
            }

            // Service can also be registered asynchronously with
            // session.registerServiceAsync(...)
            boolean serviceRegistered =
                    session.registerService(
                            serviceName, session.getSessionIdentity(), serviceRegistrationOptions);
            if (!serviceRegistered) {
                System.err.println("Service registration failed: " + serviceName);
                return;
            }

            Service service = session.getService(serviceName);
            if (service == null) {
                System.err.println("Failed to get service: " + serviceName);
                return;
            }

            System.out.println("Service registered: " + serviceName);

            // Now we will start publishing
            int eventCount = 0;
            while (true) {
                Event event;
                synchronized (lock) {
                    while (activeTopics.isEmpty()) {
                        if (!running) {
                            return;
                        }

                        try {
                            lock.wait(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }

                    event = service.createPublishEvent();
                    EventFormatter eventFormatter = new EventFormatter(event);
                    boolean publishNull = false;
                    if (clearInterval > 0 && eventCount == clearInterval) {
                        eventCount = 0;
                        publishNull = true;
                    }

                    for (Topic topic : activeTopics) {
                        if (pageEnabled) {
                            formatPageEvent(eventFormatter, topic, publishNull);

                        } else {
                            formatMarketDataEvent(eventFormatter, topic, publishNull);
                        }
                    }

                    ++eventCount;
                }

                System.out.println("Publishing event: " + event);
                session.publish(event);
                Thread.sleep(2 * 1000);

                if (eventCount % 3 == 0) {
                    deactivate(session);
                    Thread.sleep(10 * 1000);
                    activate(session);
                }
            }
        } finally {
            session.stop();
        }
    }

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser =
                new ArgParser("Interactive publisher example", InteractivePublisherExample.class);
        try {
            connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);

            ArgGroup argGroupPublisher = new ArgGroup("Publisher Options");
            argGroupPublisher
                    .add("-s", "--service")
                    .setDescription("the service name")
                    .setRequired(true)
                    .setMetaVar("service")
                    .setAction(value -> serviceName = value);

            argGroupPublisher
                    .add("-g", "--group-id")
                    .setDescription(
                            "the group ID of the service,"
                                    + " default to an automatically generated unique value")
                    .setMetaVar("groupId")
                    .setAction(value -> groupId = value);

            argGroupPublisher
                    .add("-p", "--priority")
                    .setDescription("the service priority")
                    .setMetaVar("priority")
                    .setDefaultValue(String.valueOf(priority))
                    .setAction(value -> priority = Integer.parseInt(value));

            argGroupPublisher
                    .add("--register-ssc")
                    .setMetaVar("begin,end,priority")
                    .setDescription(
                            "specify active sub-service code range and"
                                    + " priority separated by ','")
                    .setAction(this::parseSubServiceCodeRangeAndPriority);

            argGroupPublisher
                    .add("--clear-cache")
                    .setMetaVar("eventCount")
                    .setDescription(
                            "number of events after which cache will "
                                    + "be cleared (default: 0, i.e cache never cleared)")
                    .setAction(value -> clearInterval = Integer.parseInt(value));

            argGroupPublisher
                    .add("--resolve-ssc")
                    .setMetaVar("subServiceCode")
                    .setDescription("sub-service code to be used in permission response")
                    .setAction(value -> resolveSubServiceCode = Integer.valueOf(value));

            argGroupPublisher
                    .add("-E", "--eid")
                    .setMetaVar("eid")
                    .setDescription("EIDs that are used in permission response")
                    .setMode(ArgMode.MULTIPLE_VALUES)
                    .setAction(value -> eids.add(Integer.valueOf(value)));

            argGroupPublisher
                    .add("-P", "--page")
                    .setDescription("enable publish as page")
                    .setMode(ArgMode.NO_VALUE)
                    .setAction(value -> pageEnabled = true);

            argParser.addGroup(argGroupPublisher);
            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        return true;
    }

    private static void formatMarketDataRecapEvent(EventFormatter eventFormatter) {
        eventFormatter.setElement(OPEN, 100.0);
    }

    private static void formatPageRecapEvent(EventFormatter eventFormatter) {
        int numRows = 5;

        eventFormatter.setElement(NUM_ROWS, numRows);
        eventFormatter.setElement(NUM_COLS, 80);
        eventFormatter.pushElement(ROW_UPDATE);
        for (int i = 1; i <= numRows; ++i) {
            eventFormatter.appendElement();
            eventFormatter.setElement(ROW_NUM, i);
            eventFormatter.pushElement(SPAN_UPDATE);
            eventFormatter.appendElement();
            eventFormatter.setElement(START_COL, 1);
            eventFormatter.setElement(LENGTH, 10);
            eventFormatter.setElement(TEXT, "INTIAL");
            eventFormatter.setElement(FG_COLOR, "RED");
            eventFormatter.pushElement(ATTR);
            eventFormatter.appendValue("UNDERLINE");
            eventFormatter.appendValue("BLINK");
            eventFormatter.popElement();
            eventFormatter.popElement();
            eventFormatter.popElement();
            eventFormatter.popElement();
        }

        eventFormatter.popElement();
    }

    private static void formatMarketDataEvent(
            EventFormatter eventFormatter, Topic topic, boolean publishNull) {
        eventFormatter.appendMessage(MARKET_DATA_EVENTS, topic);
        if (publishNull) {
            eventFormatter.setElementNull(HIGH);
            eventFormatter.setElementNull(LOW);
        } else {
            eventFormatter.setElement(HIGH, 100);
            eventFormatter.setElement(LOW, 99);
        }
    }

    private static void formatPageEvent(
            EventFormatter eventFormatter, Topic topic, boolean publishNull) {
        int numRows = 5;
        for (int i = 1; i <= numRows; ++i) {
            eventFormatter.appendMessage(MESSAGE_TYPE_ROW_UPDATE, topic);
            eventFormatter.setElement(ROW_NUM, i);
            if (publishNull) {
                eventFormatter.setElementNull(SPAN_UPDATE);
            } else {
                eventFormatter.pushElement(SPAN_UPDATE);

                eventFormatter.appendElement();
                eventFormatter.setElement(START_COL, 1);
                eventFormatter.setElement(LENGTH, 100);
                eventFormatter.setElement(TEXT, "row " + i);
                eventFormatter.popElement();

                eventFormatter.popElement();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        InteractivePublisherExample example = new InteractivePublisherExample();
        example.run(args);
        System.out.println("Press ENTER to quit");
        System.in.read();
    }
}
