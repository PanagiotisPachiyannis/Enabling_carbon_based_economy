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
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.EventFormatter;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.ProviderEventHandler;
import com.bloomberglp.blpapi.ProviderSession;
import com.bloomberglp.blpapi.ProviderSession.ResolveMode;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Topic;
import com.bloomberglp.blpapi.TopicList;
import com.bloomberglp.blpapiexamples.demoapps.util.ConnectionAndAuthOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.MaxEventsOption;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ContributionsExample {

    private static final String DEFAULT_MKT_DATA_TOPIC = "/ticker/AUDEUR Curncy";
    private static final String DEFAULT_PAGE_DATA_TOPIC = "/page/220/660/1";

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private static final Name MKT_DATA = Name.getName("MarketData");
    private static final Name BID = Name.getName("BID");
    private static final Name ASK = Name.getName("ASK");
    private static final Name BID_SIZE = Name.getName("BID_SIZE");
    private static final Name ASK_SIZE = Name.getName("ASK_SIZE");
    private static final Name PAGE_DATA = Name.getName("PageData");
    private static final Name ROW_UPDATE = Name.getName("rowUpdate");
    private static final Name ROW_NUM = Name.getName("rowNum");
    private static final Name SPAN_UPDATE = Name.getName("spanUpdate");
    private static final Name START_COL = Name.getName("startCol");
    private static final Name LENGTH = Name.getName("length");
    private static final Name TEXT = Name.getName("text");
    private static final Name ATTR = Name.getName("attr");
    private static final Name CONTRIBUTION_ID = Name.getName("contributorId");
    private static final Name PRODUCT_CODE = Name.getName("productCode");
    private static final Name PAGE_NUMBER = Name.getName("pageNumber");

    private String serviceName = "//blp/mpfbapi";
    private String topic = null;

    private volatile boolean running = true;
    private boolean isPageEnabled = false;
    private int contributionId = 8563;

    private ConnectionAndAuthOptions connectionAndAuthOptions;
    private MaxEventsOption maxEventsOption;

    private boolean parseCommandLine(String[] args) {
        ArgParser argParser =
                new ArgParser(
                        "Contribute market or page data to a topic", ContributionsExample.class);

        connectionAndAuthOptions = new ConnectionAndAuthOptions(argParser);
        maxEventsOption = new MaxEventsOption(argParser);

        ArgGroup argGroupContribution = new ArgGroup("Contribution Options");
        argGroupContribution
                .add("-s", "--service")
                .setDescription("service name")
                .setMetaVar("service")
                .setDefaultValue(serviceName)
                .setAction(value -> serviceName = value);

        argGroupContribution
                .add("-t", "--topic")
                .setDescription(
                        "topic to contribute (mktdata default: '"
                                + DEFAULT_MKT_DATA_TOPIC
                                + "', page default: '"
                                + DEFAULT_PAGE_DATA_TOPIC
                                + "')")
                .setMetaVar("topic")
                .setAction(value -> topic = value);

        argGroupContribution
                .add("-C", "--contribution-id")
                .setDescription("contributor id. Ignored unless page is enabled")
                .setMetaVar("contributionId")
                .setDefaultValue(String.valueOf(contributionId))
                .setAction(value -> contributionId = Integer.parseInt(value));

        argGroupContribution
                .add("-P", "--page")
                .setDescription("enable page contributions")
                .setMode(ArgMode.NO_VALUE)
                .setAction(value -> isPageEnabled = true);

        argParser.addGroup(argGroupContribution);
        argParser.parse(args);

        if (topic == null || topic.isEmpty()) {
            topic = isPageEnabled ? DEFAULT_PAGE_DATA_TOPIC : DEFAULT_MKT_DATA_TOPIC;
        } else if (topic.charAt(0) != '/') {
            topic = "/" + topic;
        }

        return true;
    }

    class MyEventHandler implements ProviderEventHandler {

        @Override
        public void processEvent(Event event, ProviderSession session) {
            for (Message msg : event) {
                System.out.println("Message = " + msg);

                Name messageType = msg.messageType();

                if (event.eventType() == EventType.SESSION_STATUS) {
                    if (messageType.equals(Names.SESSION_TERMINATED)
                            || messageType.equals(Names.SESSION_STARTUP_FAILURE)) {
                        running = false;
                    }
                }
            }
        }
    }

    private void run(String[] args) throws Exception {
        if (!parseCommandLine(args)) {
            return;
        }

        ProviderSession session =
                new ProviderSession(
                        connectionAndAuthOptions.createSessionOption(), new MyEventHandler());

        try {
            if (!session.start()) {
                System.err.println("Failed to start session");
                return;
            }

            TopicList topicList = new TopicList();
            topicList.add(serviceName + topic, new CorrelationID(topic));

            session.createTopics(topicList, ResolveMode.AUTO_REGISTER_SERVICES);

            Service service = session.getService(serviceName);
            if (service == null) {
                System.err.println("Open service failed: " + serviceName);
                return;
            }

            Topic topicObj;
            if (topicList.statusAt(0) == TopicList.Status.CREATED) {
                topicObj = session.getTopic(topicList.messageAt(0));
            } else {
                System.err.println("Topic " + serviceName + topic + " not created.");
                return;
            }

            int iteration = 0;
            while (iteration++ < maxEventsOption.getMaxEvents()) {
                if (!running) {
                    break;
                }
                Event event = service.createPublishEvent();
                EventFormatter eventFormatter = new EventFormatter(event);

                if (isPageEnabled) {
                    formatPageDataEvent(eventFormatter, topicObj);
                } else {
                    formatMktDataEvent(eventFormatter, topicObj, iteration);
                }

                System.out.println(TIME_FORMAT.format(new Date()) + " -");

                System.out.println("Publishing event: " + event);

                session.publish(event);
                Thread.sleep(10 * 1000);
            }
        } finally {
            session.stop();
        }
    }

    private void formatPageDataEvent(EventFormatter eventFormatter, Topic topic) {
        eventFormatter.appendMessage(PAGE_DATA, topic);
        eventFormatter.pushElement(ROW_UPDATE);

        eventFormatter.appendElement();
        eventFormatter.setElement(ROW_NUM, 1);
        eventFormatter.pushElement(SPAN_UPDATE);

        eventFormatter.appendElement();
        eventFormatter.setElement(START_COL, 20);
        eventFormatter.setElement(LENGTH, 4);
        eventFormatter.setElement(TEXT, "TEST");
        eventFormatter.setElement(ATTR, "INTENSIFY");
        eventFormatter.popElement();

        eventFormatter.appendElement();
        eventFormatter.setElement(START_COL, 25);
        eventFormatter.setElement(LENGTH, 4);
        eventFormatter.setElement(TEXT, "PAGE");
        eventFormatter.setElement(ATTR, "BLINK");
        eventFormatter.popElement();

        eventFormatter.appendElement();
        eventFormatter.setElement(START_COL, 30);
        String timestamp = TIME_FORMAT.format(new Date());
        eventFormatter.setElement(LENGTH, timestamp.length());
        eventFormatter.setElement(TEXT, timestamp);
        eventFormatter.setElement(ATTR, "REVERSE");
        eventFormatter.popElement();

        eventFormatter.popElement();
        eventFormatter.popElement();

        eventFormatter.appendElement();
        eventFormatter.setElement(ROW_NUM, 2);
        eventFormatter.pushElement(SPAN_UPDATE);
        eventFormatter.appendElement();
        eventFormatter.setElement(START_COL, 20);
        eventFormatter.setElement(LENGTH, 9);
        eventFormatter.setElement(TEXT, "---------");
        eventFormatter.setElement(ATTR, "UNDERLINE");
        eventFormatter.popElement();
        eventFormatter.popElement();
        eventFormatter.popElement();

        eventFormatter.appendElement();
        eventFormatter.setElement(ROW_NUM, 3);
        eventFormatter.pushElement(SPAN_UPDATE);
        eventFormatter.appendElement();
        eventFormatter.setElement(START_COL, 10);
        eventFormatter.setElement(LENGTH, 9);
        eventFormatter.setElement(TEXT, "TEST LINE");
        eventFormatter.popElement();
        eventFormatter.appendElement();
        eventFormatter.setElement(START_COL, 23);
        eventFormatter.setElement(LENGTH, 5);
        eventFormatter.setElement(TEXT, "THREE");
        eventFormatter.popElement();
        eventFormatter.popElement();
        eventFormatter.popElement();
        eventFormatter.popElement();

        eventFormatter.setElement(CONTRIBUTION_ID, contributionId);
        eventFormatter.setElement(PRODUCT_CODE, 1);
        eventFormatter.setElement(PAGE_NUMBER, 1);
    }

    private static void formatMktDataEvent(EventFormatter eventFormatter, Topic topic, int value) {
        eventFormatter.appendMessage(MKT_DATA, topic);
        eventFormatter.setElement(BID, 0.5 * value);
        eventFormatter.setElement(ASK, value);
        eventFormatter.setElement(BID_SIZE, 1200 + value);
        eventFormatter.setElement(ASK_SIZE, 1400 + value);
    }

    public static void main(String[] args) {
        ContributionsExample example = new ContributionsExample();
        try {
            example.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
