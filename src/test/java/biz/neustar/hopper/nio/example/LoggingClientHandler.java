package biz.neustar.hopper.nio.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ClientMessageHandler;

/**
 * A client side message handler that logs messages received from the server.
 */
public class LoggingClientHandler implements ClientMessageHandler {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingClientHandler.class);

    @Override
    public void handleResponse(final Message response) {
        log.info("Response received {}", response);
    }

    @Override
    public void handleException(final Throwable throwable) {

        log.error("Exception {}", throwable);
    }

}
