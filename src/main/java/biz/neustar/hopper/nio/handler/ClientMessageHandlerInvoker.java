package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ClientMessageHandler;

/**
 * Handler to invoke the client side application call back.
 */
public class ClientMessageHandlerInvoker extends SimpleChannelUpstreamHandler {

    /**
     * Should the channel be closed after processing the message.
     */
    private final Boolean closeConnection;

    /**
     * The target callback on the application side.
     */
    private final ClientMessageHandler clientMessageHandler;

    public ClientMessageHandlerInvoker(
            final ClientMessageHandler clientMessageHandlerArg) {
        this(clientMessageHandlerArg, false);
    }

    public ClientMessageHandlerInvoker(
            final ClientMessageHandler clientMessageHandlerArg,
            final boolean closeConnectionArg) {
        this.clientMessageHandler = clientMessageHandlerArg;
        this.closeConnection = closeConnectionArg;
    }

    @Override
    public void messageReceived(
            final ChannelHandlerContext ctx,
            final MessageEvent e) throws Exception {

        Object request = e.getMessage();
        if (request instanceof Message) {
            if (closeConnection) {
                e.getChannel().close();
            }
            clientMessageHandler.handleResponse((Message) request);
        }
        super.messageReceived(ctx, e);
    }

    @Override
    public void exceptionCaught(
            final ChannelHandlerContext ctx,
            final ExceptionEvent e) throws Exception {

        if (closeConnection) {
            e.getChannel().close();
        }
        clientMessageHandler.handleException(e.getCause());
        super.exceptionCaught(ctx, e);
    }
}

