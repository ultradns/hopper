package biz.neustar.hopper.nio.example;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.AdvancedServerMessageHandler;
import biz.neustar.hopper.nio.ChannelType;

public class AdvancedEchoHandler implements AdvancedServerMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AdvancedEchoHandler.class);

    @Override
    public Message handleRequest(ChannelHandlerContext ctx, Message request,
            MessageEvent e, ChannelType channelType) {
        return request;
    }

    @Override
    public void handleException(ChannelHandlerContext ctx, Throwable throwable,
            ExceptionEvent e, ChannelType channelType) {
        LOGGER.error("Exception!", throwable);
    }

}
