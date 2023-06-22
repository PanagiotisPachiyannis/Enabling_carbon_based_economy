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
package com.bloomberglp.blpapiexamples.unittests.snippets.eventglossarytests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Event.EventType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.SchemaElementDefinition;
import com.bloomberglp.blpapi.test.MessageFormatter;
import com.bloomberglp.blpapi.test.TestUtil;
import org.junit.jupiter.api.Test;

/**
 * The following test cases provide examples on how to mock different events/messages supported by
 * BLPAPI Java SDK. The code to set up expectation and verification of values is omitted from
 * examples tests.
 */
public class EventGlossaryTest {
    private static final Name REASON = Name.getName("reason");
    private static final Name SOURCE = Name.getName("source");
    private static final Name ERROR_CODE = Name.getName("errorCode");
    private static final Name CATEGORY = Name.getName("category");
    private static final Name DESCRIPTION = Name.getName("description");
    private static final Name SUB_CATEGORY = Name.getName("subcategory");
    private static final Name INITIAL_ENDPOINTS = Name.getName("initialEndpoints");
    private static final Name ADDRESS = Name.getName("address");
    private static final Name SERVER = Name.getName("server");
    private static final Name SERVER_ID = Name.getName("serverId");
    private static final Name ENCRYPTION_STATUS = Name.getName("encryptionStatus");
    private static final Name COMPRESSION_STATUS = Name.getName("compressionStatus");
    private static final Name NAME = Name.getName("name");
    private static final Name ENDPOINTS = Name.getName("endpoints");
    private static final Name ENDPOINTS_ADDED = Name.getName("endpointsAdded");
    private static final Name ENDPOINTS_REMOVED = Name.getName("endpointsRemoved");
    private static final Name EVENTS_DROPPED = Name.getName("eventsDropped");
    private static final Name ID = Name.getName("id");
    private static final Name NUM_MESSAGES_DROPPED = Name.getName("numMessagesDropped");
    private static final Name SERVICE_NAME = Name.getName("serviceName");
    private static final Name ENDPOINT = Name.getName("endpoint");
    private static final Name SERVERS = Name.getName("servers");
    private static final Name SERVER_ADDED = Name.getName("serverAdded");
    private static final Name SERVER_REMOVED = Name.getName("serverRemoved");
    private static final Name RESOLVED_TOPIC = Name.getName("resolvedTopic");
    private static final Name TOPIC = Name.getName("topic");
    private static final Name TOPICS = Name.getName("topics");
    private static final Name TOPIC_WITH_OPTIONS = Name.getName("topicWithOptions");
    private static final Name UUID = Name.getName("uuid");
    private static final Name SEAT_TYPE = Name.getName("seatType");
    private static final Name APPLICATION_ID = Name.getName("applicationId");
    private static final Name USER_NAME = Name.getName("userName");
    private static final Name APP_NAME = Name.getName("appName");
    private static final Name DEVICE_ADDRESS = Name.getName("deviceAddress");
    private static final Name IS_SOLICITED = Name.getName("isSolicited");
    private static final Name EXCEPTIONS = Name.getName("exceptions");
    private static final Name RESUBSCRIPTION_ID = Name.getName("resubscriptionId");
    private static final Name STREAM_IDS = Name.getName("streamIds");
    private static final Name RECEIVED_FROM = Name.getName("receivedFrom");
    private static final Name FAILURE_DETAILS = Name.getName("failureDetails");
    private static final Name FIELD_ID = Name.getName("fieldId");
    private static final Name STREAMS = Name.getName("streams");
    private static final Name BOUND_TO = Name.getName("boundTo");
    private static final Name DATA_CONNECTION = Name.getName("dataConnection");
    private static final Name TOKEN = Name.getName("token");

    // Helper method to validate common element 'reason'
    private static void validateReasonElement(Element root) {
        Element reason = root.getElement(REASON);

        assertEquals("TestUtil", reason.getElementAsString(SOURCE));
        assertEquals(-1, reason.getElementAsInt32(ERROR_CODE));
        assertEquals("CATEGORY", reason.getElementAsString(CATEGORY));
        assertEquals("for testing", reason.getElementAsString(DESCRIPTION));
        assertEquals("SUBCATEGORY", reason.getElementAsString(SUB_CATEGORY));
    }

    @Test
    public void exampleEvent_SessionStarted() {
        EventType eventType = EventType.SESSION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SESSION_STARTED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SessionStarted>"
                        + "    <initialEndpoints>"
                        + "        <address>12.34.56.78:8194</address>"
                        + "     </initialEndpoints>"
                        + "    <initialEndpoints>"
                        + "        <address>98.76.54.32:8194</address>"
                        + "     </initialEndpoints>"
                        + "</SessionStarted>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            Element endpoints = msg.getElement(INITIAL_ENDPOINTS);
            assertEquals(
                    "12.34.56.78:8194", endpoints.getValueAsElement(0).getElementAsString(ADDRESS));
            assertEquals(
                    "98.76.54.32:8194", endpoints.getValueAsElement(1).getElementAsString(ADDRESS));
        }
    }

    @Test
    public void exampleEvent_SessionStartupFailure() {
        EventType eventType = EventType.SESSION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SESSION_STARTUP_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SessionStartupFailure>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "     </reason>"
                        + "</SessionStartupFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_SessionTerminated() {
        EventType eventType = EventType.SESSION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SESSION_TERMINATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SessionTerminated>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "     </reason>"
                        + "</SessionTerminated>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_SessionConnectionUp() {
        EventType eventType = EventType.SESSION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SESSION_CONNECTION_UP;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SessionConnectionUp>"
                        + "    <server>12.34.56.78:8194</server>"
                        + "    <serverId>ny-hostname</serverId>"
                        + "    <encryptionStatus>Clear</encryptionStatus>"
                        + "    <compressionStatus>Uncompressed</compressionStatus>"
                        + "</SessionConnectionUp>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("12.34.56.78:8194", msg.getElementAsString(SERVER));
            assertEquals("ny-hostname", msg.getElementAsString(SERVER_ID));
            assertEquals("Clear", msg.getElementAsString(ENCRYPTION_STATUS));
            assertEquals("Uncompressed", msg.getElementAsString(COMPRESSION_STATUS));
        }
    }

    @Test
    public void exampleEvent_SessionConnectionDown() {
        EventType eventType = EventType.SESSION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SESSION_CONNECTION_DOWN;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SessionConnectionDown>"
                        + "    <server>12.34.56.78:8194</server>"
                        + "    <serverId>ny-hostname</serverId>"
                        + "</SessionConnectionDown>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("12.34.56.78:8194", msg.getElementAsString(SERVER));
            assertEquals("ny-hostname", msg.getElementAsString(SERVER_ID));
        }
    }

    @Test
    public void exampleEvent_SessionClusterInfo() {
        EventType eventType = EventType.SESSION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SESSION_CLUSTER_INFO;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SessionClusterInfo>"
                        + "    <name>clustername</name>"
                        + "    <endpoints>"
                        + "        <address>12.34.56.78:8194</address>"
                        + "     </endpoints>"
                        + "    <endpoints>"
                        + "        <address>98.76.54.32:8194</address>"
                        + "     </endpoints>"
                        + "</SessionClusterInfo>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("clustername", msg.getElementAsString(NAME));
            Element endpoint1 = msg.getElement(ENDPOINTS).getValueAsElement(0);
            assertEquals("12.34.56.78:8194", endpoint1.getElementAsString(ADDRESS));
            Element endpoint2 = msg.getElement(ENDPOINTS).getValueAsElement(1);
            assertEquals("98.76.54.32:8194", endpoint2.getElementAsString(ADDRESS));
        }
    }

    @Test
    public void exampleEvent_SessionClusterUpdate() {
        EventType eventType = EventType.SESSION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SESSION_CLUSTER_UPDATE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SessionClusterUpdate>"
                        + "    <name>clustername</name>"
                        + "    <endpointsAdded>"
                        + "        <address>12.34.56.78:8194</address>"
                        + "     </endpointsAdded>"
                        + "    <endpointsRemoved>"
                        + "        <address>98.76.54.32:8194</address>"
                        + "    </endpointsRemoved>"
                        + "</SessionClusterUpdate>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("clustername", msg.getElementAsString(NAME));
            Element endpointsAdded = msg.getElement(ENDPOINTS_ADDED).getValueAsElement(0);
            assertEquals("12.34.56.78:8194", endpointsAdded.getElementAsString(ADDRESS));
            Element endpointsRemoved = msg.getElement(ENDPOINTS_REMOVED).getValueAsElement(0);
            assertEquals("98.76.54.32:8194", endpointsRemoved.getElementAsString(ADDRESS));
        }
    }

    @Test
    public void exampleEvent_SlowConsumerWarning() {
        EventType eventType = EventType.ADMIN;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SLOW_CONSUMER_WARNING;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        TestUtil.appendMessage(expectedEvent, schema);
    }

    @Test
    public void exampleEvent_SlowConsumerWarningCleared() {
        EventType eventType = EventType.ADMIN;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SLOW_CONSUMER_WARNING_CLEARED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SlowConsumerWarningCleared>"
                        + "    <eventsDropped>123</eventsDropped>"
                        + "</SlowConsumerWarningCleared>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals(123, msg.getElementAsInt32(EVENTS_DROPPED));
        }
    }

    @Test
    public void exampleEvent_DataLoss() {
        EventType eventType = EventType.ADMIN;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.DATA_LOSS;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<DataLoss>"
                        + "    <id>id</id>"
                        + "    <source>Test</source>"
                        + "    <numMessagesDropped>1235</numMessagesDropped>"
                        + "</DataLoss>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("id", msg.getElementAsString(ID));
            assertEquals("Test", msg.getElementAsString(SOURCE));
            assertEquals(1235, msg.getElementAsInt32(NUM_MESSAGES_DROPPED));
        }
    }

    @Test
    public void exampleEvent_ServiceOpened() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_OPENED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceOpened>"
                        + "    <serviceName>//blp/myservice</serviceName>"
                        + "</ServiceOpened>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("//blp/myservice", msg.getElementAsString(SERVICE_NAME));
        }
    }

    @Test
    public void exampleEvent_ServiceOpenFailure() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_OPEN_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceOpenFailure>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "     </reason>"
                        + "</ServiceOpenFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_ServiceRegistered() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_REGISTERED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceRegistered>"
                        + "    <serviceName>//blp/myservice</serviceName>"
                        + "</ServiceRegistered>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("//blp/myservice", msg.getElementAsString(SERVICE_NAME));
        }
    }

    @Test
    public void exampleEvent_ServiceRegisterFailure() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_REGISTER_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceRegisterFailure>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "     </reason>"
                        + "</ServiceRegisterFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_ServiceDeregistered() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_DEREGISTERED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceDeregistered>"
                        + "    <serviceName>//blp/myservice</serviceName>"
                        + "</ServiceDeregistered>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("//blp/myservice", msg.getElementAsString(SERVICE_NAME));
        }
    }

    @Test
    public void exampleEvent_ServiceUp() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_UP;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceUp>"
                        + "    <serviceName>//blp/myservice</serviceName>"
                        + "    <endpoint>12.34.56.78</endpoint>"
                        + "</ServiceUp>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("//blp/myservice", msg.getElementAsString(SERVICE_NAME));
            assertEquals("12.34.56.78", msg.getElementAsString(ENDPOINT));
        }
    }

    @Test
    public void exampleEvent_ServiceDown() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_DOWN;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceDown>"
                        + "    <serviceName>//blp/myservice</serviceName>"
                        + "    <endpoint>12.34.56.78</endpoint>"
                        + "</ServiceDown>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("//blp/myservice", msg.getElementAsString(SERVICE_NAME));
            assertEquals("12.34.56.78", msg.getElementAsString(ENDPOINT));
        }
    }

    @Test
    public void exampleEvent_ServiceAvailabilityInfo() {
        EventType eventType = EventType.SERVICE_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SERVICE_AVAILABILITY_INFO;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ServiceAvailabilityInfo>"
                        + "    <serviceName>//blp/myservice</serviceName>"
                        + "    <serverAdded>"
                        + "        <address>12.34.56.78:8194</address>"
                        + "     </serverAdded>"
                        + "    <serverRemoved>"
                        + "        <address>98.76.54.32:8194</address>"
                        + "    </serverRemoved>"
                        + "    <servers>12.34.56.78</servers>"
                        + "    <servers>87.65.43.21</servers>"
                        + "</ServiceAvailabilityInfo>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("//blp/myservice", msg.getElementAsString(SERVICE_NAME));
            assertEquals(
                    "12.34.56.78:8194", msg.getElement(SERVER_ADDED).getElementAsString(ADDRESS));
            assertEquals(
                    "98.76.54.32:8194", msg.getElement(SERVER_REMOVED).getElementAsString(ADDRESS));

            Element servers = msg.getElement(SERVERS);
            assertEquals("12.34.56.78", servers.getValueAsString(0));
            assertEquals("87.65.43.21", servers.getValueAsString(1));
        }
    }

    @Test
    public void exampleEvent_ResolutionSuccess() {
        EventType eventType = EventType.RESOLUTION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.RESOLUTION_SUCCESS;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ResolutionSuccess>"
                        + "    <resolvedTopic>//blp/myservice/rtopic</resolvedTopic>"
                        + "</ResolutionSuccess>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("//blp/myservice/rtopic", msg.getElementAsString(RESOLVED_TOPIC));
        }
    }

    @Test
    public void exampleEvent_ResolutionFailure() {
        EventType eventType = EventType.RESOLUTION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.RESOLUTION_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<ResolutionFailure>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "     </reason>"
                        + "</ResolutionFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_PermissionRequest() {
        EventType eventType = EventType.REQUEST;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.PERMISSION_REQUEST;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<PermissionRequest>"
                        + "    <topics>topic1</topics>"
                        + "    <topics>topic2</topics>"
                        + "    <uuid>1234</uuid>"
                        + "    <seatType>5678</seatType>"
                        + "    <applicationId>9012</applicationId>"
                        + "    <userName>someName</userName>"
                        + "    <appName>myAppName</appName>"
                        + "    <deviceAddress>myDevice</deviceAddress>"
                        + "</PermissionRequest>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            Element topics = msg.getElement(TOPICS);
            assertEquals("topic1", topics.getValueAsString(0));
            assertEquals("topic2", topics.getValueAsString(1));

            assertEquals(1234, msg.getElementAsInt32(UUID));
            assertEquals(5678, msg.getElementAsInt32(SEAT_TYPE));
            assertEquals(9012, msg.getElementAsInt32(APPLICATION_ID));

            assertEquals("someName", msg.getElementAsString(USER_NAME));
            assertEquals("myAppName", msg.getElementAsString(APP_NAME));
            assertEquals("myDevice", msg.getElementAsString(DEVICE_ADDRESS));
        }
    }

    @Test
    public void exampleEvent_PermissionResponse() {
        // Unlike the other admin messages, 'PermissionResponse' is not
        // delivered to applications by the SDK. It is used by resolvers to
        // respond to incoming 'PermissionRequest' messages. BLPAPI
        // applications are not expected to handle these messages.
        //
        // For testing if an application is publishing 'PermissionResponse'
        // messages with correct values, it is recommended to mock the related
        // 'ProviderSession::publish()' method to capture the published events.
        // See the provided testing examples for more details.
    }

    @Test
    public void exampleEvent_TopicCreated() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_CREATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent = "<TopicCreated><topic>mytopic</topic></TopicCreated>";
        fmtter.formatMessageXml(xmlMessageContent);
    }

    @Test
    public void exampleEvent_TopicCreateFailure() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_CREATE_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicCreateFailure>"
                        + "    <topic>mytopic</topic>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "     </reason>"
                        + "</TopicCreateFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_TopicDeleted() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_DELETED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicDeleted>"
                        + "    <topic>mytopic</topic>"
                        + "    <reason>TestUtil</reason>"
                        + "</TopicDeleted>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytopic", msg.getElementAsString(TOPIC));
            assertEquals("TestUtil", msg.getElementAsString(REASON));
        }
    }

    @Test
    public void exampleEvent_TopicSubscribed() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_SUBSCRIBED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicSubscribed>"
                        + "    <topic>mytopic</topic>"
                        + "    <topicWithOptions>topicwithopts</topicWithOptions>"
                        + "</TopicSubscribed>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytopic", msg.getElementAsString(TOPIC));
            assertEquals("topicwithopts", msg.getElementAsString(TOPIC_WITH_OPTIONS));
        }
    }

    @Test
    public void exampleEvent_TopicResubscribed() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_RESUBSCRIBED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicResubscribed>"
                        + "    <topic>mytopic</topic>"
                        + "    <topicWithOptions>topicwithopts</topicWithOptions>"
                        + "    <reason>TestUtil</reason>"
                        + "</TopicResubscribed>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytopic", msg.getElementAsString(TOPIC));
            assertEquals("topicwithopts", msg.getElementAsString(TOPIC_WITH_OPTIONS));
            assertEquals("TestUtil", msg.getElementAsString(REASON));
        }
    }

    @Test
    public void exampleEvent_TopicUnsubscribed() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_UNSUBSCRIBED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicUnsubscribed>"
                        + "    <topic>mytopic</topic>"
                        + "    <reason>TestUtil</reason>"
                        + "</TopicUnsubscribed>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytopic", msg.getElementAsString(TOPIC));
            assertEquals("TestUtil", msg.getElementAsString(REASON));
        }
    }

    @Test
    public void exampleEvent_TopicActivated() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_ACTIVATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicActivated>" + "    <topic>mytopic</topic>" + "</TopicActivated>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytopic", msg.getElementAsString(TOPIC));
        }
    }

    @Test
    public void exampleEvent_TopicDeactivated() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_DEACTIVATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicDeactivated>"
                        + "    <topic>mytopic</topic>"
                        + "    <reason>TestUtil</reason>"
                        + "</TopicDeactivated>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytopic", msg.getElementAsString(TOPIC));
            assertEquals("TestUtil", msg.getElementAsString(REASON));
        }
    }

    @Test
    public void exampleEvent_TopicRecap() {
        EventType eventType = EventType.TOPIC_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOPIC_RECAP;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TopicRecap>"
                        + "    <topic>mytopic</topic>"
                        + "    <isSolicited>true</isSolicited>"
                        + "</TopicRecap>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytopic", msg.getElementAsString(TOPIC));
            assertTrue(msg.getElementAsBool(IS_SOLICITED));
        }
    }

    @Test
    public void exampleEvent_SubscriptionStarted() {
        EventType eventType = EventType.SUBSCRIPTION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SUBSCRIPTION_STARTED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SubscriptionStarted>"
                        + "    <exceptions>"
                        + "        <reason>"
                        + "            <source>TestUtil</source>"
                        + "            <errorCode>-1</errorCode>"
                        + "            <category>CATEGORY</category>"
                        + "            <description>for testing</description>"
                        + "            <subcategory>SUBCATEGORY</subcategory>"
                        + "        </reason>"
                        + "    </exceptions>"
                        + "    <resubscriptionId>789</resubscriptionId>"
                        + "    <streamIds>123</streamIds>"
                        + "    <streamIds>456</streamIds>"
                        + "    <receivedFrom>"
                        + "        <address>12.34.56.78:8194</address>"
                        + "    </receivedFrom>"
                        + "    <reason>TestUtil</reason>"
                        + "</SubscriptionStarted>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            Element exceptions = msg.getElement(EXCEPTIONS);
            validateReasonElement(exceptions.getValueAsElement(0));

            assertEquals(789, msg.getElementAsInt32(RESUBSCRIPTION_ID));
            Element streamIds = msg.getElement(STREAM_IDS);
            assertEquals("123", streamIds.getValueAsString(0));
            assertEquals("456", streamIds.getValueAsString(1));

            assertEquals(
                    "12.34.56.78:8194", msg.getElement(RECEIVED_FROM).getElementAsString(ADDRESS));
            assertEquals("TestUtil", msg.getElementAsString(REASON));
        }
    }

    @Test
    public void exampleEvent_SubscriptionFailure() {
        EventType eventType = EventType.SUBSCRIPTION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SUBSCRIPTION_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SubscriptionFailure>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "    </reason>"
                        + "    <failureDetails>"
                        + "        <fieldId>field</fieldId>"
                        + "        <reason>"
                        + "            <source>TestUtil</source>"
                        + "            <errorCode>-1</errorCode>"
                        + "            <category>CATEGORY</category>"
                        + "            <description>for testing</description>"
                        + "            <subcategory>SUBCATEGORY</subcategory>"
                        + "         </reason>"
                        + "    </failureDetails>"
                        + "    <resubscriptionId>123</resubscriptionId>"
                        + "</SubscriptionFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
            Element details = msg.getElement(FAILURE_DETAILS);

            assertEquals("field", details.getValueAsElement(0).getElementAsString(FIELD_ID));
            validateReasonElement(details.getValueAsElement(0));

            assertEquals("123", msg.getElementAsString(RESUBSCRIPTION_ID));
        }
    }

    @Test
    public void exampleEvent_SubscriptionStreamsActivated() {
        EventType eventType = EventType.SUBSCRIPTION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SUBSCRIPTION_STREAMS_ACTIVATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SubscriptionStreamsActivated>"
                        + "    <streams>"
                        + "        <id>streamId</id>"
                        + "        <endpoint>"
                        + "            <address>12.34.56.78:8194</address>"
                        + "        </endpoint>"
                        + "    </streams>"
                        + "    <reason>TestUtil</reason>"
                        + "</SubscriptionStreamsActivated>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            Element streams = msg.getElement(STREAMS);

            assertEquals("streamId", streams.getValueAsElement(0).getElementAsString(ID));
            assertEquals(
                    "12.34.56.78:8194",
                    streams.getValueAsElement(0).getElement(ENDPOINT).getElementAsString(ADDRESS));
            assertEquals("TestUtil", msg.getElementAsString(REASON));
        }
    }

    @Test
    public void exampleEvent_SubscriptionStreamsDeactivated() {
        EventType eventType = EventType.SUBSCRIPTION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SUBSCRIPTION_STREAMS_DEACTIVATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SubscriptionStreamsDeactivated>"
                        + "    <streams>"
                        + "        <id>streamId</id>"
                        + "        <endpoint>"
                        + "            <address>12.34.56.78:8194</address>"
                        + "        </endpoint>"
                        + "    </streams>"
                        + "    <reason>TestUtil</reason>"
                        + "</SubscriptionStreamsDeactivated>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            Element streams = msg.getElement(STREAMS);

            assertEquals("streamId", streams.getValueAsElement(0).getElementAsString(ID));
            assertEquals(
                    "12.34.56.78:8194",
                    streams.getValueAsElement(0).getElement(ENDPOINT).getElementAsString(ADDRESS));
            assertEquals("TestUtil", msg.getElementAsString(REASON));
        }
    }

    @Test
    public void exampleEvent_SubscriptionTerminated() {
        EventType eventType = EventType.SUBSCRIPTION_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.SUBSCRIPTION_TERMINATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<SubscriptionTerminated>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "    </reason>"
                        + "</SubscriptionTerminated>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_RequestTemplateAvailable() {
        EventType eventType = EventType.ADMIN;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.REQUEST_TEMPLATE_AVAILABLE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<RequestTemplateAvailable>"
                        + "    <boundTo>"
                        + "        <dataConnection>"
                        + "            <address>12.34.56.78:8194</address>"
                        + "        </dataConnection>"
                        + "    </boundTo>"
                        + "</RequestTemplateAvailable>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            Element dataC = msg.getElement(BOUND_TO).getElement(DATA_CONNECTION);
            assertEquals(
                    "12.34.56.78:8194", dataC.getValueAsElement(0).getElementAsString(ADDRESS));
        }
    }

    @Test
    public void exampleEvent_RequestTemplatePending() {
        EventType eventType = EventType.ADMIN;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.REQUEST_TEMPLATE_PENDING;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        TestUtil.appendMessage(expectedEvent, schema);
    }

    @Test
    public void exampleEvent_RequestTemplateTerminated() {
        EventType eventType = EventType.ADMIN;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.REQUEST_TEMPLATE_TERMINATED;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<RequestTemplateTerminated>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "    </reason>"
                        + "</RequestTemplateTerminated>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_RequestFailure() {
        EventType eventType = EventType.REQUEST_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.REQUEST_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<RequestFailure>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "    </reason>"
                        + "</RequestFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }

    @Test
    public void exampleEvent_TokenGenerationSuccess() {
        EventType eventType = EventType.TOKEN_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOKEN_GENERATION_SUCCESS;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TokenGenerationSuccess>"
                        + "    <token>mytoken</token>"
                        + "</TokenGenerationSuccess>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            assertEquals("mytoken", msg.getElementAsString(TOKEN));
        }
    }

    @Test
    public void exampleEvent_TokenGenerationFailure() {
        EventType eventType = EventType.TOKEN_STATUS;
        Event expectedEvent = TestUtil.createEvent(eventType);

        Name messageType = Names.TOKEN_GENERATION_FAILURE;
        SchemaElementDefinition schema = TestUtil.getAdminMessageDefinition(messageType);

        MessageFormatter fmtter = TestUtil.appendMessage(expectedEvent, schema);
        String xmlMessageContent =
                "<TokenGenerationFailure>"
                        + "    <reason>"
                        + "        <source>TestUtil</source>"
                        + "        <errorCode>-1</errorCode>"
                        + "        <category>CATEGORY</category>"
                        + "        <description>for testing</description>"
                        + "        <subcategory>SUBCATEGORY</subcategory>"
                        + "    </reason>"
                        + "</TokenGenerationFailure>";
        fmtter.formatMessageXml(xmlMessageContent);

        for (Message msg : expectedEvent) {
            validateReasonElement(msg.asElement());
        }
    }
}
