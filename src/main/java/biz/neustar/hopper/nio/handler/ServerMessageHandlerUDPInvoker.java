package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ServerMessageHandler;

/**
 * Netty handler for invoking a ServerMessageHandler.
 */
public class ServerMessageHandlerUDPInvoker extends SimpleChannelUpstreamHandler {

    private final ServerMessageHandler handler;

    public ServerMessageHandlerUDPInvoker(
            final ServerMessageHandler handlerArg) {
        this.handler = handlerArg;
    }

    @Override
    public void messageReceived(
            final ChannelHandlerContext ctx,
            final MessageEvent e) throws Exception {

        Object request = e.getMessage();
        if (request instanceof Message) {
            Message response = handler.handleRequest((Message) request);
            ctx.getChannel().write(response, e.getRemoteAddress());
        }
        super.messageReceived(ctx, e);
    }

    @Override
    public void exceptionCaught(
            final ChannelHandlerContext ctx,
            final ExceptionEvent e) throws Exception {
        handler.handleException(e.getCause());
        super.exceptionCaught(ctx, e);
    }
}

