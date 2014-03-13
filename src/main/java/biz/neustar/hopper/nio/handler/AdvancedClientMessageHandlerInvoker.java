package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.AdvancedClientMessageHandler;

public class AdvancedClientMessageHandlerInvoker extends
        SimpleChannelUpstreamHandler {

    /**
     * Should the channel be closed after processing the message.
     */
    private final Boolean closeConnection;

    /**
     * The target callback on the application side.
     */
    private final AdvancedClientMessageHandler advClientMessageHandler;

    public AdvancedClientMessageHandlerInvoker(
            final AdvancedClientMessageHandler clientMessageHandlerArg) {
        this(clientMessageHandlerArg, false);
    }

    public AdvancedClientMessageHandlerInvoker(
            final AdvancedClientMessageHandler advClientMessageHandlerArg,
            final boolean closeConnectionArg) {
        this.advClientMessageHandler = advClientMessageHandlerArg;
        this.closeConnection = closeConnectionArg;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx,
            final MessageEvent e) throws Exception {

        Object request = e.getMessage();
        if (request instanceof Message) {
            if (closeConnection) {
                e.getChannel().close();
            }
            advClientMessageHandler.handleResponse((Message) request, e);
        }
        super.messageReceived(ctx, e);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final ExceptionEvent e) throws Exception {

        if (closeConnection) {
            e.getChannel().close();
        }
        advClientMessageHandler.handleException(e.getCause(), e);
        super.exceptionCaught(ctx, e);
    }
}
