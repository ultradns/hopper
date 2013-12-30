package biz.neustar.hopper.nio.example;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.AdvancedServerMessageHandler;

public class AdvancedEchoHandler implements AdvancedServerMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AdvancedEchoHandler.class);

    @Override
    public Message handleRequest(ChannelHandlerContext ctx, Message request) {
        return request;
    }

    @Override
    public void handleException(ChannelHandlerContext ctx, Throwable throwable) {
        LOGGER.error("Exception!", throwable);
    }

}
