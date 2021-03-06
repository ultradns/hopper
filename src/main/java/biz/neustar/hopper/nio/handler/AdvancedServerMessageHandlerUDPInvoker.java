package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.AdvancedServerMessageHandler;
import biz.neustar.hopper.nio.ChannelType;

/**
 * Netty handler for invoking a ServerMessageHandler.
 */
public class AdvancedServerMessageHandlerUDPInvoker extends SimpleChannelUpstreamHandler {

    /**
     * The server message callback.
     */
    private final AdvancedServerMessageHandler handler;

    /**
     * The constructor.
     *
     * @param handlerArg The server side handler
     */
    public AdvancedServerMessageHandlerUDPInvoker(
            final AdvancedServerMessageHandler handlerArg) {
        this.handler = handlerArg;
    }

    @Override
    public void messageReceived(
            final ChannelHandlerContext ctx,
            final MessageEvent e) throws Exception {
        handler.setContext();
        try {
            Object request = e.getMessage();
            if (request instanceof Message) {
                Message response = handler.handleRequestOnUdp(ctx,
                        (Message) request, e, ChannelType.UDP);
                if (null != response) {
                    ctx.getChannel().write(response, e.getRemoteAddress());
                }
            }
            super.messageReceived(ctx, e);
        } finally {
            handler.clearContext();
        }
    }

    @Override
    public void exceptionCaught(
            final ChannelHandlerContext ctx,
            final ExceptionEvent e) throws Exception {
        handler.handleException(ctx, e.getCause(), e, ChannelType.UDP);
        super.exceptionCaught(ctx, e);
    }
}


