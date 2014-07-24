package io.divolte.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private final Undertow undertow;

    public Server(final Config config) {
        final String host = config.getString("divolte.server.host");
        final int port = config.getInt("divolte.server.port");

        final DivolteEventHandler divolteEventHandler = new DivolteEventHandler(config);

        final PathHandler handler = new PathHandler();
        handler.addExactPath("/ping", PingHandler::handlePingRequest);
        handler.addExactPath("/event", divolteEventHandler::handleEventRequest);
        final SetHeaderHandler headerHandler =
                new SetHeaderHandler(handler, Headers.SERVER_STRING, "divolte");

        undertow = Undertow.builder()
                           .addHttpListener(port, host)
                           .setHandler(headerHandler)
                           .build();
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    logger.info("Stopping server.");
                    undertow.stop();
                }
        ));
        logger.info("Starting server.");
        undertow.start();
    }

    public static void main(final String[] args) {
        // Tell Undertow to use Slf4J for logging by default.
        if (null == System.getProperty("org.jboss.logging.provider")) {
            System.setProperty("org.jboss.logging.provider", "slf4j");
        }
        new Server(ConfigFactory.load()).run();
    }
}