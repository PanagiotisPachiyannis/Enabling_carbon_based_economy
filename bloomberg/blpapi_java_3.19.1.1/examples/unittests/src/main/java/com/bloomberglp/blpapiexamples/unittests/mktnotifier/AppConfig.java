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

import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    protected static final String LAST_PRICE = "LAST_PRICE";

    private enum AuthOption {
        NONE(null, "none"),
        USER("AuthenticationType=OS_LOGON", "user"),
        APP(
                "AuthenticationMode=APPLICATION_ONLY;ApplicationAuthenticationType=APPNAME_AND_KEY;ApplicationName=",
                "app="),
        USER_APP(
                "AuthenticationMode=USER_AND_APPLICATION;AuthenticationType=OS_LOGON;ApplicationAuthenticationType=APPNAME_AND_KEY;ApplicationName=",
                "userapp="),
        DIR("AuthenticationType=DIRECTORY_SERVICE;DirSvcPropertyName=", "dir=");

        private String prefix;
        private String cmdOption;

        private AuthOption(String prefix, String cmdOption) {
            this.prefix = prefix;
            this.cmdOption = cmdOption;
        }

        private String getAuthOption(String value) {
            switch (this) {
                case NONE:
                case USER:
                    return prefix;

                case APP:
                case USER_APP:
                case DIR:
                    return prefix + value.substring(cmdOption.length());

                default:
                    return null;
            }
        }

        public static String parse(String value) {
            for (AuthOption ao : values()) {
                if (value.startsWith(ao.cmdOption)) {
                    return ao.getAuthOption(value);
                }
            }

            throw new IllegalArgumentException("Invalid auth option '" + value + "'");
        }
    }

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_TOPIC = "/ticker/IBM Equity";
    private static final String DEFAULT_SERVICE = "//blp/mktdata";
    private static final int DEFAULT_PORT = 8194;
    static final String DEFAULT_AUTH_SERVICE = "//blp/apiauth";

    String service = DEFAULT_SERVICE;
    List<String> hosts = new ArrayList<>();
    int port = DEFAULT_PORT;

    List<String> topics = new ArrayList<>();
    List<String> fields = new ArrayList<>();
    List<String> options = new ArrayList<>();

    String authService = DEFAULT_AUTH_SERVICE;
    String authOptions;

    public static AppConfig parseCommandLine(String[] args) {
        AppConfig appConfig = new AppConfig();

        for (int i = 0; i < args.length; ++i) {
            String option = args[i];
            if (i + 1 < args.length) {
                String optValue = args[++i];
                if (option.equalsIgnoreCase("-ip")) {
                    appConfig.hosts.add(optValue);
                } else if (option.equalsIgnoreCase("-p")) {
                    appConfig.port = Integer.parseInt(optValue);
                } else if (option.equalsIgnoreCase("-s")) {
                    appConfig.service = optValue;
                } else if (option.equalsIgnoreCase("-t")) {
                    appConfig.topics.add(optValue);
                } else if (option.equalsIgnoreCase("-f")) {
                    appConfig.fields.add(optValue);
                } else if (option.equalsIgnoreCase("-o")) {
                    appConfig.options.add(optValue);
                } else if (option.equalsIgnoreCase("-auth")) {
                    appConfig.authOptions = AuthOption.parse(optValue);
                } else {
                    throw new IllegalArgumentException("Invalid argument: '" + option + "'!");
                }
            } else {
                throw new IllegalArgumentException("Missing argument!");
            }
        }

        if (appConfig.hosts.isEmpty()) {
            appConfig.hosts.add(DEFAULT_HOST);
        }

        if (appConfig.topics.isEmpty()) {
            appConfig.topics.add(DEFAULT_TOPIC);
        }

        if (!appConfig.fields.contains(LAST_PRICE)) {
            // always add LAST_PRICE
            appConfig.fields.add(LAST_PRICE);
        }

        return appConfig;
    }

    static void printUsage() {
        System.out.println(
                "Retrieve realtime data.\n"
                        + "Usage:\n"
                        + "\t[-ip   <ipAddress>] server name or IP (default: localhost)\n"
                        + "\t[-p    <tcpPort>]   server port (default: 8194)\n"
                        + "\t[-s    <service>]   service name (default: //blp/mktdata))\n"
                        + "\t[-t    <topic>]     topic name (default: /ticker/IBM Equity)\n"
                        + "\t[-f    <field>]     field to subscribe to (default: LAST_PRICE)\n"
                        + "\t[-o    <option>]    subscription options (default: empty)\n"
                        + "\t[-auth <option>]    authentication option (default: none):\n"
                        + "\t\tnone\n"
                        + "\t\tuser           as a user using OS logon information\n"
                        + "\t\tapp=<app>      as the specified application\n"
                        + "\t\tuserapp=<app>  as user and application using logon information for the user\n"
                        + "\t\tdir=<property> as a user using directory services\n");
    }
}
