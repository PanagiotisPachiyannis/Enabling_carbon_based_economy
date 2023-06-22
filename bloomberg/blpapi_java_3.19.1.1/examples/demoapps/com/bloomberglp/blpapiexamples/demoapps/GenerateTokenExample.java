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
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.Arg;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;

public class GenerateTokenExample {

    private static final Name TOKEN = Name.getName("token");

    private static final String AUTH_USER = "AuthenticationType=OS_LOGON";
    private static final String AUTH_APP_PREFIX =
            "AuthenticationMode=APPLICATION_ONLY;"
                    + "ApplicationAuthenticationType=APPNAME_AND_KEY;ApplicationName=";
    private static final String AUTH_USER_APP_PREFIX =
            "AuthenticationMode=USER_AND_APPLICATION;"
                    + "AuthenticationType=OS_LOGON;"
                    + "ApplicationAuthenticationType=APPNAME_AND_KEY;ApplicationName=";
    private static final String AUTH_DIR_PREFIX =
            "AuthenticationType=DIRECTORY_SERVICE;DirSvcPropertyName=";
    private static final String AUTH_OPTION_USER = "user";
    private static final String AUTH_OPTION_APP = "app";
    private static final String AUTH_OPTION_USER_APP = "userapp";
    private static final String AUTH_OPTION_DIR = "dir";

    private String serverHost;
    private int serverPort;
    private String authOptions;

    public static void main(String[] args) throws java.lang.Exception {
        GenerateTokenExample example = new GenerateTokenExample();
        example.run(args);
        System.out.println("Press ENTER to quit");
        System.in.read();
    }

    private void parseAuthOptions(String value) {
        String[] tokens = value.split("=");
        String authType = tokens[0];

        switch (authType) {
            case AUTH_OPTION_USER:
                authOptions = AUTH_USER;
                break;

            case AUTH_OPTION_APP:
                if (tokens.length == 1) {
                    throw new IllegalArgumentException(
                            "auth " + authType + "= is missing application name");
                }

                authOptions = AUTH_APP_PREFIX + tokens[1];
                break;

            case AUTH_OPTION_USER_APP:
                if (tokens.length == 1) {
                    throw new IllegalArgumentException(
                            "auth " + authType + "= is missing application name");
                }

                authOptions = AUTH_USER_APP_PREFIX + tokens[1];
                break;

            case AUTH_OPTION_DIR:
                if (tokens.length == 1) {
                    throw new IllegalArgumentException(
                            "auth " + authType + "= is missing directory property");
                }

                authOptions = AUTH_DIR_PREFIX + tokens[1];
                break;

            default:
                throw new IllegalArgumentException("Invalid authentication option: " + value);
        }
    }

    private void parseServerPort(String value) {
        String[] tokens = value.split(":");
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid server option: " + value);
        }

        serverHost = tokens[0];
        try {
            serverPort = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid port: " + ex.getMessage());
        }
    }

    private void run(String[] args) throws Exception {
        ArgParser argParser =
                new ArgParser(
                        "Generate a token for a user to be used on the server side",
                        GenerateTokenExample.class);
        try {

            Arg argServer =
                    new Arg("-H", "--host")
                            .setMetaVar("host:port")
                            .setDescription("Server name or IP and port separated by ':'")
                            .setRequired(true)
                            .setAction(value -> parseServerPort(value));
            ArgGroup groupServer = new ArgGroup("Connections", argServer);
            argParser.addGroup(groupServer);

            String eol = System.lineSeparator();
            Arg argAuth =
                    new Arg("-a", "--auth")
                            .setMetaVar("option")
                            .setDescription(
                                    "Authentication option "
                                            + eol
                                            + "      user              as a user using OS logon information"
                                            + eol
                                            + "      dir=<property>    as a user using directory services"
                                            + eol
                                            + "      app=<app>         as the specified application"
                                            + eol
                                            + "      userapp=<app>     as user and application"
                                            + " using logon information for the user")
                            .setRequired(true)
                            .setAction(value -> parseAuthOptions(value));
            ArgGroup groupAuth = new ArgGroup("Authentication", argAuth);
            argParser.addGroup(groupAuth);

            argParser.parse(args);
        } catch (Exception ex) {
            System.err.println("Failed to parse arguments: " + ex.getMessage());
            argParser.printHelp();
            return;
        }

        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setServerHost(serverHost);
        sessionOptions.setServerPort(serverPort);
        sessionOptions.setAuthenticationOptions(authOptions);
        System.out.println("Connecting to " + serverHost + ":" + serverPort);
        Session session = new Session(sessionOptions);

        try {
            if (!session.start()) {
                System.err.println("Failed to start session.");
                return;
            }

            session.generateToken(new CorrelationID());

            while (true) {
                Event event = session.nextEvent();
                if (event.eventType() == Event.EventType.TOKEN_STATUS) {
                    for (Message msg : event) {
                        Name messageType = msg.messageType();
                        if (messageType.equals(Names.TOKEN_GENERATION_SUCCESS)) {
                            String token = msg.getElementAsString(TOKEN);
                            System.out.println("Token is successfully generated: " + token);
                        } else if (messageType.equals(Names.TOKEN_GENERATION_FAILURE)) {
                            System.out.println("Failed to generate token: " + msg);
                        }
                    }

                    break;
                }
            }
        } finally {
            session.stop();
        }
    }
}
