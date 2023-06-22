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
package com.bloomberglp.blpapiexamples.demoapps;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
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
import com.bloomberglp.blpapi.Topic;
import com.bloomberglp.blpapi.TopicList;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.MaxEventsOption;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.util.ArrayList;
import java.util.List;

public class BroadcastPublisherExample {
    private static final Name HIGH = Name.getName("HIGH");
    private static final Name LENGTH = Name.getName("length");
    private static final Name LOW = Name.getName("LOW");
    private static final Name MARKET_DATA_EVENTS = Name.getName("MarketDataEvents");
    private static final Name ROW_NUM = Name.getName("rowNum");
    private static final Name ROW_UPDATE = Name.getName("RowUpdate");
    private static final Name SPAN_UPDATE = Name.getName("spanUpdate");
    private static final Name START_COL = Name.getName("startCol");
    private static final Name TEXT = Name.getName("text");

    private static final String DEFAULT_MKTDATA_TOPIC = "IBM US Equity";
    private static final String DEFAULT_PAGE_TOPIC = "178/1/1";

    private ConnectionAndAuthOptions connectionAndAuthOptions;
    private MaxEventsOption maxEventsOption;

    private String serviceName;
    private final List<String> topics = new ArrayList<>();

    private String groupId = null;
    private int priority;

    private volatile boolean isRunning = true;
    private boolean isPageEnabled = false;

    private static class MyStream {
        final String id;
        Topic topic;

        public MyStream(String id) {
            this.id = id;
        }
    }

    private class MyEventHandler implements ProviderEventHandler {

        @Override
        public void processEvent(Event event, ProviderSession session) {
            System.out.println("Received event " + event);

            if (event.eventType() != Event.EventType.SESSION_STATUS) {
                return;
            }

            for (Message msg : event) {
                if (msg.messageType() == Names.SESSION_TERMINATED) {
                    System.out.println("Session terminated, stopping application...");
                    isRunning = false;
                    break;
                }
            }
        }
    }

    private boolean parseCmdLine(String[] args) {
        ArgParser argParser =
                new ArgParser("Broadcast publisher example", BroadcastPublisherExample.class);
        connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        maxEventsOption = new MaxEventsOption(argParser);

        try {
            ArgGroup argGroupPublisher = new ArgGroup("Broadcast Publisher Options");
            argGroupPublisher
                    .add("-s", "--service")
                    .setDescription("the service name")
                    .setMetaVar("service")
                    .setRequired(true)
                    .setAction(value -> serviceName = value);

            argGroupPublisher
                    .add("-t", "--topic")
                    .setDescription(
                            "topic to publish (default: mktdata \""
                                    + DEFAULT_MKTDATA_TOPIC
                                    + "\", page \""
                                    + DEFAULT_PAGE_TOPIC
                                    + "\")")
                    .setMode(ArgMode.MULTIPLE_VALUES)
                    .setMetaVar("topic")
                    .setAction(topics::add);

            argGroupPublisher
                    .add("-g", "--group-id")
                    .setDescription(
                            "publisher's group ID, default to an "
                                    + "automatically generated unique value")
                    .setMetaVar("groupId")
                    .setAction(value -> groupId = value);

            argGroupPublisher
                    .add("-p", "--priority")
                    .setDescription("publisher's priority")
                    .setMetaVar("priority")
                    .setDefaultValue(String.valueOf(ServiceRegistrationPriority.PRIORITY_HIGH))
                    .setAction(value -> priority = Integer.parseInt(value));

            argGroupPublisher
                    .add("-P", "--page")
                    .setDescription("enable publish as page")
                    .setMode(ArgMode.NO_VALUE)
                    .setAction(value -> isPageEnabled = true);

            argParser.addGroup(argGroupPublisher);
            argParser.parse(args);

        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return false;
        }

        // Assign the default topic
        if (topics.isEmpty()) {
            topics.add(isPageEnabled ? DEFAULT_PAGE_TOPIC : DEFAULT_MKTDATA_TOPIC);
        }

        return true;
    }

    public void run(String[] args) throws Exception {
        if (!parseCmdLine(args)) {
            return;
        }

        SessionOptions sessionOptions = connectionAndAuthOptions.createSessionOption();

        ProviderSession session = new ProviderSession(sessionOptions, new MyEventHandler());
        try {
            if (!session.start()) {
                System.err.println("Failed to start session");
                return;
            }

            if (groupId != null) {
                // NOTE: perform explicit service registration here, instead of
                // letting createTopics() do it, as the latter approach doesn't
                // allow for custom ServiceRegistrationOptions
                ServiceRegistrationOptions serviceRegistrationOptions =
                        new ServiceRegistrationOptions();
                serviceRegistrationOptions.setGroupId(groupId);
                serviceRegistrationOptions.setServicePriority(priority);

                if (!session.registerService(
                        serviceName, session.getSessionIdentity(), serviceRegistrationOptions)) {
                    System.out.print("Failed to register " + serviceName);
                    return;
                }
            }

            TopicList topicList = new TopicList();
            for (String topic : topics) {

                // Prefix user topic with '/' if it's not there.
                String userTopic = topic;
                if (!userTopic.isEmpty() && userTopic.charAt(0) != '/') {
                    userTopic = "/" + userTopic;
                }

                topicList.add(serviceName + userTopic, new CorrelationID(new MyStream(topic)));
            }

            // createTopics() is synchronous, topicList will be updated
            // with the results of topic creation (resolution will happen
            // under the covers)
            session.createTopics(topicList, ProviderSession.ResolveMode.AUTO_REGISTER_SERVICES);

            List<MyStream> myStreams = new ArrayList<MyStream>();

            for (int i = 0; i < topicList.size(); ++i) {
                MyStream stream = (MyStream) topicList.correlationIdAt(i).object();
                if (topicList.statusAt(i) == TopicList.Status.CREATED) {
                    Message msg = topicList.messageAt(i);
                    stream.topic = session.getTopic(msg);
                    myStreams.add(stream);
                    System.out.println("Topic created: " + topicList.topicStringAt(i));
                } else {
                    System.out.println(
                            "Stream '"
                                    + stream.id
                                    + "': topic not resolved, status = "
                                    + topicList.statusAt(i));
                }
            }
            Service service = session.getService(serviceName);
            if (service == null) {
                System.err.println("Service registration failed: " + serviceName);
                return;
            }
            if (myStreams.isEmpty()) {
                System.err.println("No topics created for publishing");
                return;
            }

            // Now we will start publishing
            int maxEvents = maxEventsOption.getMaxEvents();
            for (int eventCount = 0; eventCount < maxEvents; ++eventCount) {
                if (!isRunning) {
                    break;
                }

                Event event = service.createPublishEvent();
                EventFormatter eventFormatter = new EventFormatter(event);

                for (MyStream myStream : myStreams) {
                    Topic topic = myStream.topic;
                    if (!topic.isActive()) {
                        System.out.println(
                                "[WARNING] Publishing on an inactive topic: " + myStream.id + ".");
                    }

                    if (isPageEnabled) {
                        formatPageEvent(eventFormatter, topic);
                    } else {
                        formatMarketDataEvent(eventFormatter, topic);
                    }
                }

                System.out.println("Publishing event: " + event);
                session.publish(event);
                Thread.sleep(2 * 1000);
            }
        } finally {
            session.stop();
        }
    }

    private static void formatMarketDataEvent(EventFormatter eventFormatter, Topic topic) {
        eventFormatter.appendMessage(MARKET_DATA_EVENTS, topic);
        eventFormatter.setElement(HIGH, 1.0);
        eventFormatter.setElement(LOW, 0.5);
    }

    private static void formatPageEvent(EventFormatter eventFormatter, Topic topic) {
        int numRows = 5;
        for (int i = 1; i <= numRows; ++i) {
            eventFormatter.appendMessage(ROW_UPDATE, topic);
            eventFormatter.setElement(ROW_NUM, i);
            eventFormatter.pushElement(SPAN_UPDATE);

            eventFormatter.appendElement();
            eventFormatter.setElement(START_COL, 1);
            eventFormatter.setElement(LENGTH, "100");
            eventFormatter.setElement(TEXT, "row " + i);
            eventFormatter.popElement();

            eventFormatter.popElement();
        }
    }

    public static void main(String[] args) throws Exception {
        BroadcastPublisherExample example = new BroadcastPublisherExample();
        example.run(args);
        System.out.println("Press ENTER to quit");
        System.in.read();
    }
}
