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

import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.SessionOptions.ServerAddress;
import java.io.IOException;

public class Application {

    private final Session session;
    private final IAuthorizer authorizer;
    private final ISubscriber subscriber;
    private final AppConfig config;

    public Application(
            Session session, IAuthorizer authorizer, ISubscriber subscriber, AppConfig config) {
        super();
        this.session = session;
        this.authorizer = authorizer;
        this.subscriber = subscriber;
        this.config = config;
    }

    public void run() throws IOException, InterruptedException {
        if (!session.start()) {
            System.err.println("Failed to start session.");
            return;
        }

        // Exception is thrown if authorization fails
        Identity identity = authorizer.authorize(config.authService, config.authOptions);

        subscriber.subscribe(
                config.service, config.topics, config.fields, config.options, identity);
    }

    public static void main(String[] args) {
        try {
            AppConfig config = null;
            try {
                config = AppConfig.parseCommandLine(args);
            } catch (IllegalArgumentException ex) {
                System.err.println(ex.getMessage());
                AppConfig.printUsage();
                System.exit(1);
            }

            SessionOptions sessionOptions = new SessionOptions();
            ServerAddress[] serverAddresses = new ServerAddress[config.hosts.size()];
            for (int i = 0; i < serverAddresses.length; ++i) {
                serverAddresses[i] = new ServerAddress(config.hosts.get(i), config.port);
            }
            sessionOptions.setServerAddresses(serverAddresses);
            sessionOptions.setAuthenticationOptions(config.authOptions);

            INotifier notifier = new Notifier();
            IComputeEngine computeEngine = new ComputeEngine();
            EventHandler eventHandler = new EventProcessor(notifier, computeEngine);
            Session session = new Session(sessionOptions, eventHandler);

            ITokenGenerator tokenGenerator = new TokenGenerator(session);
            IAuthorizer authorizer = new Authorizer(session, tokenGenerator);
            ISubscriber subscriber = new Subscriber(session);

            Application app = new Application(session, authorizer, subscriber, config);
            app.run();

            System.out.println("Press ENTER to quit");
            System.in.read();
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
