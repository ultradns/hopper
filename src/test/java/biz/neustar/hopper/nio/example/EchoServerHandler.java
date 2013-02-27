package biz.neustar.hopper.nio.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ServerMessageHandler;

/**
 * A Server side DNS message processor that echos the request back to clients
 */
public class EchoServerHandler implements ServerMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(EchoServerHandler.class);

    @Override
    public Message handleRequest(Message request) {
        return request;
    }

    @Override
    public void handleException(Throwable throwable) {

        log.error("Exception!", throwable);
    }

}
