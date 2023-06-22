/*
 * Copyright 2021, Bloomberg Finance L.P.
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
package com.bloomberglp.blpapiexamples.demoapps.util;

import com.bloomberglp.blpapi.AuthApplication;
import com.bloomberglp.blpapi.AuthOptions;
import com.bloomberglp.blpapi.AuthToken;
import com.bloomberglp.blpapi.AuthUser;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.SessionOptions.ServerAddress;
import com.bloomberglp.blpapi.TlsOptions;
import com.bloomberglp.blpapi.TlsOptions.TlsInitializationException;
import com.bloomberglp.blpapi.ZfpUtil;
import com.bloomberglp.blpapi.ZfpUtil.Remote;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.Arg;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgGroup;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgMode;
import com.bloomberglp.blpapiexamples.demoapps.util.argparser.ArgParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Helper class that adds the options for connection and authorization to {@link ArgParser}. It
 * creates a {@link SessionOptions} using the following command line arguments:
 *
 * <ul>
 *   <li>connections where servers, TLS and ZFP over Leased lines are specified.
 *   <li>authorization options that is used as session identity options.
 * </ul>
 */
public class ConnectionAndAuthOptions {

    private static final String AUTH_OPTION_NONE = "none";
    private static final String AUTH_OPTION_USER = "user";
    private static final String AUTH_OPTION_APP = "app";
    private static final String AUTH_OPTION_USER_APP = "userapp";
    private static final String AUTH_OPTION_DIR = "dir";
    private static final String AUTH_OPTION_MANUAL = "manual";

    List<ServerAddress> servers = new ArrayList<>();
    AuthOptions sessionIdentityAuthOptions;
    String clientCredentials;
    String clientCredentialsPassword;
    String trustMaterial;
    Remote remote;

    private boolean clientServerSetup = false;
    private AuthApplication authApplication;
    private final List<String> tokens = new ArrayList<>();
    private final List<String[]> userIdAndIps = new ArrayList<>();

    private ConnectionAndAuthOptions(ArgParser argParser, boolean clientServerSetup) {
        Objects.requireNonNull(argParser, "argParser must not be null");

        this.clientServerSetup = clientServerSetup;

        addArgGroupServer(argParser);
        if (clientServerSetup) {
            addArgGroupAuthClientServerSetup(argParser);
        } else {
            addArgGroupAuth(argParser);
        }
        addArgGroupTls(argParser);
        addArgGroupZfpLeasedLine(argParser);
    }

    public ConnectionAndAuthOptions(ArgParser argParser) {
        this(argParser, false);
    }

    public static ConnectionAndAuthOptions forClientServerSetup(ArgParser argParser) {
        return new ConnectionAndAuthOptions(argParser, true);
    }

    public SessionOptions createSessionOption()
            throws InterruptedException, TlsInitializationException {
        SessionOptions sessionOptions = new SessionOptions();

        TlsOptions tlsOptions = null;
        if (clientCredentials != null
                && clientCredentialsPassword != null
                && trustMaterial != null) {
            tlsOptions =
                    TlsOptions.createFromFiles(
                            clientCredentials,
                            clientCredentialsPassword.toCharArray(),
                            trustMaterial);
        }

        if (remote == null) {
            sessionOptions.setServerAddresses(servers.toArray(new ServerAddress[0]));
            sessionOptions.setTlsOptions(tlsOptions);
        } else {
            if (tlsOptions == null) {
                throw new IllegalArgumentException("ZFP connections require TLS parameters");
            }

            sessionOptions = ZfpUtil.getZfpOptionsForLeasedLines(remote, tlsOptions);
        }

        sessionOptions.setSessionIdentityOptions(sessionIdentityAuthOptions);
        System.out.println("Connecting to " + Arrays.toString(sessionOptions.getServerAddresses()));
        return sessionOptions;
    }

    /**
     * Creates a map which contains key-value pairs of the identifier representing a user, either
     * userId:IP or token, and the {@link AuthOptions}, either manual option (userId + IP + App) or
     * token.
     */
    public Map<String, AuthOptions> createClientServerSetupAuthOptions() {
        Map<String, AuthOptions> authOptionsByIdentifier = new HashMap<>();
        for (String[] userIdIp : userIdAndIps) {
            String userId = userIdIp[0];
            String ip = userIdIp[1];
            AuthUser authUser = AuthUser.createWithManualOptions(userId, ip);
            AuthOptions authOptions = new AuthOptions(authUser, authApplication);
            authOptionsByIdentifier.put(userId + ":" + ip, authOptions);
        }

        for (int i = 0; i < tokens.size(); ++i) {
            AuthOptions authOptions = new AuthOptions(new AuthToken(tokens.get(i)));
            authOptionsByIdentifier.put("token #" + (i + 1), authOptions);
        }

        return authOptionsByIdentifier;
    }

    public int numClientServerSetupAuthOptions() {
        return userIdAndIps.size() + tokens.size();
    }

    private void addArgGroupServer(ArgParser argParser) {
        Arg argServer =
                new Arg("-H", "--host")
                        .setMetaVar("host:port")
                        .setDescription("Server name or IP and port separated by ':'")
                        .setDefaultValue("localhost:8194")
                        .setAction(this::parseServerAddress)
                        .setMode(ArgMode.MULTIPLE_VALUES); // allow multiple servers

        ArgGroup groupServer = new ArgGroup("Connections", argServer);
        argParser.addGroup(groupServer);
    }

    private void addArgGroupAuth(ArgParser argParser) {
        String eol = System.lineSeparator();
        Arg argAuth =
                new Arg("-a", "--auth")
                        .setMetaVar("option")
                        .setDescription(
                                "authorization option"
                                        + eol
                                        + "none                  applicable to Desktop API product that requires"
                                        + eol
                                        + "                          Bloomberg Professional "
                                        + "service to be installed locally"
                                        + eol
                                        + "user                  as a user using OS logon information"
                                        + eol
                                        + "dir=<property>        as a user using directory services"
                                        + eol
                                        + "app=<app>             as the specified application"
                                        + eol
                                        + "userapp=<app>         as user and application using"
                                        + " logon information for the user"
                                        + eol
                                        + "manual=<app,ip,user>  as user and application, with manually provided"
                                        + eol
                                        + "                          IP address and EMRS user")
                        .setDefaultValue(AUTH_OPTION_NONE)
                        .setAction(this::parseAuthOptions);
        ArgGroup groupAuth = new ArgGroup("Authorization", argAuth);

        argParser.addGroup(groupAuth);
    }

    private void addArgGroupAuthClientServerSetup(ArgParser argParser) {
        Arg argAuth =
                new Arg("-a", "--auth")
                        .setMetaVar("app=<app>")
                        .setDescription(
                                "authorize this application using the specified application")
                        .setRequired(true)
                        .setAction(this::parseAuthOptions);
        ArgGroup groupAuth = new ArgGroup("Authorization", argAuth);
        argParser.addGroup(groupAuth);

        Arg argUserIdIp =
                new Arg("-u", "--userid-ip")
                        .setMetaVar("userId:IP")
                        .setDescription("authorize a user using userId and IP separated by ':'")
                        .setAction(this::parseUserIdIp)
                        .setMode(ArgMode.MULTIPLE_VALUES);

        Arg argToken =
                new Arg("-T", "--token")
                        .setMetaVar("token")
                        .setDescription("authorize a user using the specified token")
                        .setMode(ArgMode.MULTIPLE_VALUES)
                        .setAction(tokens::add);
        ArgGroup groupEntitlements =
                new ArgGroup("User Authorization/Entitlements", argUserIdIp, argToken);
        argParser.addGroup(groupEntitlements);
    }

    private void addArgGroupTls(ArgParser argParser) {
        Arg argTlsClientCedentials =
                new Arg("--tls-client-credentials")
                        .setMetaVar("file")
                        .setDescription(
                                "name a PKCS#12 file to use as a source of client credentials")
                        .setAction(value -> clientCredentials = value);
        Arg argTlsClientCredentialsPassword =
                new Arg("--tls-client-credentials-password")
                        .setMetaVar("password")
                        .setDescription("specify password for accessing client credentials")
                        .setAction(value -> clientCredentialsPassword = value);
        Arg argTlsTrustMaterial =
                new Arg("--tls-trust-material")
                        .setMetaVar("file")
                        .setDescription(
                                "name a PKCS#7 file to use as a source of trusted certificates")
                        .setAction(value -> trustMaterial = value);
        ArgGroup groupTls =
                new ArgGroup(
                        "TLS (specify all or none)",
                        argTlsClientCedentials,
                        argTlsClientCredentialsPassword,
                        argTlsTrustMaterial);
        argParser.addGroup(groupTls);
    }

    private void addArgGroupZfpLeasedLine(ArgParser argParser) {
        Arg argZfpOverLeasedLine =
                new Arg("-z", "--zfp-over-leased-line")
                        .setMetaVar("port")
                        .setDescription(
                                "enable ZFP connections over leased lines on"
                                        + " the specified port (8194 or 8196)"
                                        + System.lineSeparator()
                                        + "(When this option is enabled, option -H/--host is ignored.)")
                        .setAction(this::parseRemote);
        ArgGroup groupZfpLl =
                new ArgGroup(
                        "ZFP connections over leased lines (requires TLS)", argZfpOverLeasedLine);
        argParser.addGroup(groupZfpLl);
    }

    private void parseServerAddress(String value) {
        String[] splitTokens = value.split(":");
        if (splitTokens.length != 2) {
            throw new IllegalArgumentException("Invalid server option: " + value);
        }

        int port;
        try {
            port = Integer.parseInt(splitTokens[1]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid port: " + ex.getMessage());
        }

        servers.add(new ServerAddress(splitTokens[0], port));
    }

    private void parseRemote(String port) {
        if ("8194".equals(port)) {
            remote = Remote.REMOTE_8194;
            return;
        }

        if ("8196".equals(port)) {
            remote = Remote.REMOTE_8196;
            return;
        }

        throw new IllegalArgumentException("Invalid port " + port);
    }

    private void parseAuthOptions(String value) {
        String[] splitTokens = value.split("=");
        String authType = splitTokens[0];

        switch (authType) {
            case AUTH_OPTION_NONE:
                sessionIdentityAuthOptions = new AuthOptions();
                break;

            case AUTH_OPTION_USER:
                sessionIdentityAuthOptions = new AuthOptions(AuthUser.createWithLogonName());
                break;

            case AUTH_OPTION_APP:
                if (splitTokens.length == 1) {
                    throw new IllegalArgumentException(
                            "auth " + authType + "= is missing application name");
                }

                // Save AuthApplication for client server setup
                authApplication = new AuthApplication(splitTokens[1]);
                sessionIdentityAuthOptions = new AuthOptions(authApplication);
                break;

            case AUTH_OPTION_USER_APP:
                if (splitTokens.length == 1) {
                    throw new IllegalArgumentException(
                            "auth " + authType + "= is missing application name");
                }
                sessionIdentityAuthOptions =
                        new AuthOptions(
                                AuthUser.createWithLogonName(),
                                new AuthApplication(splitTokens[1]));
                break;

            case AUTH_OPTION_DIR:
                if (splitTokens.length == 1) {
                    throw new IllegalArgumentException(
                            "auth " + authType + "= is missing directory property");
                }
                sessionIdentityAuthOptions =
                        new AuthOptions(AuthUser.createWithActiveDirectoryProperty(splitTokens[1]));
                break;

            case AUTH_OPTION_MANUAL:
                {
                    String[] params = splitTokens[1].split(",");
                    if (params == null || params.length != 3) {
                        throw new IllegalArgumentException(
                                "auth " + authType + "= is missing values");
                    }

                    String appName = params[0];
                    String ip = params[1];
                    String userId = params[2];
                    sessionIdentityAuthOptions =
                            new AuthOptions(
                                    AuthUser.createWithManualOptions(userId, ip),
                                    new AuthApplication(appName));
                    break;
                }

            default:
                throw new IllegalArgumentException("Wrong auth option " + authType);
        }

        if (clientServerSetup && authApplication == null) {
            throw new IllegalArgumentException("Invalid auth option " + value);
        }
    }

    private void parseUserIdIp(String value) {
        String[] splitTokens = value.split(":");
        if (splitTokens.length != 2) {
            throw new IllegalArgumentException("Invalid userId:IP option: " + value);
        }

        userIdAndIps.add(new String[] {splitTokens[0], splitTokens[1]});
    }
}
