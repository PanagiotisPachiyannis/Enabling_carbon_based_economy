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
package com.bloomberglp.blpapiexamples.unittests.snippets.refdata;

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.SchemaElementDefinition;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.test.MessageFormatter;
import com.bloomberglp.blpapi.test.TestUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class RefDataTest {
    private static final String REFDATA_SCHEMA =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>                                                          "
                    + " <ServiceDefinition name=\"blp.refdata\" version=\"1.0.1.0\">                                        "
                    + "   <service name=\"//blp/refdata\" version=\"1.0.0.0\">                                              "
                    + "      <operation name=\"ReferenceDataRequest\" serviceId=\"84\">                                     "
                    + "        <request>ReferenceDataRequest</request>                                                      "
                    + "        <response>Response</response>                                                                "
                    + "        <responseSelection>ReferenceDataResponse</responseSelection>                                 "
                    + "      </operation>                                                                                   "
                    + "   </service>                                                                                        "
                    + "   <schema>                                                                                          "
                    + "    <sequenceType name=\"ReferenceDataRequest\">                                                     "
                    + "        <element name=\"securities\" type=\"String\" maxOccurs=\"unbounded\"/>                       "
                    + "        <element name=\"fields\" type=\"String\" maxOccurs=\"unbounded\"/>                           "
                    + "        <element name=\"overrides\" type=\"FieldOverride\" minOccurs=\"0\" maxOccurs=\"unbounded\"/> "
                    + "    </sequenceType>                                                                                  "
                    + "    <sequenceType name=\"FieldOverride\">                                                            "
                    + "        <element name=\"fieldId\" type=\"String\"/>                                                  "
                    + "        <element name=\"value\" type=\"String\"/>                                                    "
                    + "    </sequenceType>                                                                                  "
                    + "    <choiceType name=\"Response\">                                                                   "
                    + "        <element name=\"ReferenceDataResponse\" type=\"ReferenceDataResponseType\">                  "
                    + "            <cacheable>true</cacheable>                                                              "
                    + "            <cachedOnlyOnInitialPaint>false</cachedOnlyOnInitialPaint>                               "
                    + "        </element>                                                                                   "
                    + "    </choiceType>                                                                                    "
                    + "    <sequenceType name=\"ReferenceDataResponseType\">                                                "
                    + "        <element name=\"responseError\" type=\"ErrorInfo\"       />                                  "
                    + "        <element name=\"securityData\"  type=\"ReferenceSecurityData\"                               "
                    + "                                         minOccurs=\"1\" maxOccurs=\"unbounded\"/>                   "
                    + "    </sequenceType>                                                                                  "
                    + "    <sequenceType name=\"ReferenceSecurityData\">                                                    "
                    + "        <element name=\"security\"         type=\"String\"/>                                         "
                    + "        <element name=\"eidData\"          type=\"Int64\"                                            "
                    + "                                           minOccurs=\"0\" maxOccurs=\"unbounded\" />                "
                    + "        <element name=\"securityError\"    type=\"ErrorInfo\"                                        "
                    + "                                           minOccurs=\"0\" maxOccurs=\"1\"/>                         "
                    + "        <element name=\"fieldExceptions\"  type=\"FieldException\"                                   "
                    + "                                          minOccurs=\"0\" maxOccurs=\"unbounded\"/>                  "
                    + "        <element name=\"sequenceNumber\"  type=\"Int64\"                                             "
                    + "                                          minOccurs=\"0\" maxOccurs=\"1\"/>                          "
                    + "        <element name=\"fieldData\" type=\"FieldData\"/>                                             "
                    + "    </sequenceType>                                                                                  "
                    + "    <sequenceType name=\"FieldData\">                                                                "
                    + "      <description>The contents of this type depends on the response</description>                   "
                    + "        <element name=\"LAST_PRICE\" type=\"Float64\"  />                                            "
                    + "        <element name=\"PX_LAST\"    type=\"Float64\"  />                                            "
                    + "    </sequenceType>                                                                                  "
                    + "    <sequenceType name=\"FieldException\">                                                           "
                    + "      <element name=\"fieldId\"    type=\"String\"/>                                                 "
                    + "      <element name=\"errorInfo\"  type=\"ErrorInfo\"/>                                              "
                    + "    </sequenceType>                                                                                  "
                    + "    <sequenceType name=\"ErrorInfo\">                                                                "
                    + "      <element name=\"source\"   type=\"String\" />                                                  "
                    + "      <element name=\"code\"     type=\"Int64\"   />                                                 "
                    + "      <element name=\"category\" type=\"String\"  />                                                 "
                    + "      <element name=\"message\"  type=\"String\"/>                                                   "
                    + "      <element name=\"subcategory\" type=\"String\"                                                  "
                    + "                                  minOccurs=\"0\" maxOccurs=\"1\"/>                                  "
                    + "    </sequenceType>                                                                                  "
                    + "    </schema>                                                                                        "
                    + "</ServiceDefinition>";

    private static final Name OPERATION_NAME = Name.getName("ReferenceDataRequest");

    private static Service getServiceFromString(String schema) {

        InputStream schemaStream =
                new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));

        return TestUtil.deserializeService(schemaStream);
    }

    // Concern:
    // Verify that the provided sample refdata schema can be deserialized
    @Test
    public void testGetService() {
        getServiceFromString(REFDATA_SCHEMA);
    }

    // Concern:
    // Verify that a successful ReferenceDataResponse can be created with the
    // provided sample schema. Note that some of the fields are not filled in this
    // example.
    @Test
    public void testRefdataResponseSuccess() {
        Service refdataSvc = getServiceFromString(REFDATA_SCHEMA);

        Event evt = TestUtil.createEvent(Event.EventType.RESPONSE);

        SchemaElementDefinition schema =
                refdataSvc.getOperation(OPERATION_NAME).responseDefinition(0);

        MessageFormatter fmtter = TestUtil.appendMessage(evt, schema);

        String msgContent =
                "<Response>                                         "
                        + "   <securityData>                                  "
                        + "       <security>IBM US EQUITY</security>          "
                        + "       <fieldData>                                 "
                        + "           <LAST_PRICE>138.533300</LAST_PRICE>     "
                        + "       </fieldData>                                "
                        + "   </securityData>                                 "
                        + "</Response>                                        ";

        fmtter.formatMessageXml(msgContent);
    }

    // Concern:
    // Verify that an unsuccessful ReferenceDataResponse can be created with the
    // provided sample schema. Note that some of the fields are not filled in this
    // example.
    @Test
    public void testRefdataResponseFailure() {
        Service refdataSvc = getServiceFromString(REFDATA_SCHEMA);

        Event evt = TestUtil.createEvent(Event.EventType.RESPONSE);

        SchemaElementDefinition schema =
                refdataSvc.getOperation(OPERATION_NAME).responseDefinition(0);

        MessageFormatter fmtter = TestUtil.appendMessage(evt, schema);

        String msgContent =
                "<Response>                                                 "
                        + "   <securityData>                                          "
                        + "       <security>IBM US EQUITY</security>                  "
                        + "       <fieldData>                                         "
                        + "       </fieldData>                                        "
                        + "       <fieldExceptions>                                   "
                        + "           <fieldId>PX_LAST</fieldId>                      "
                        + "           <errorInfo>                                     "
                        + "               <source>src</source>                        "
                        + "               <code>5</code>                              "
                        + "               <category>NO_AUTH</category>                "
                        + "               <message>Field ..</message>                 "
                        + "               <subcategory>FIELD ..</subcategory>         "
                        + "           </errorInfo>                                    "
                        + "       </fieldExceptions>                                  "
                        + "   </securityData>                                         "
                        + "</Response>                                                ";

        fmtter.formatMessageXml(msgContent);
    }
}
