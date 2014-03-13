package biz.neustar.hopper.nio.example;

import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.AdvancedClientMessageHandler;

public class LoggingAdvancedClientHandler implements
        AdvancedClientMessageHandler {

    private static final Logger log = LoggerFactory
            .getLogger(LoggingClientHandler.class);

    @Override
    public void handleResponse(Message response, MessageEvent me) {
        log.info("Response received {}", response);

    }

    @Override
    public void handleException(Throwable throwable, ExceptionEvent ee) {
        log.error("Exception {}", throwable);

    }

}
