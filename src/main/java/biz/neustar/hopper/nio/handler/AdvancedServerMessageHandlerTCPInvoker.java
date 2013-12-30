package biz.neustar.hopper.nio.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.AdvancedServerMessageHandler;

/**
 * Netty handler for invoking a ServerMessageHandler (a server side callback).
  */
public class AdvancedServerMessageHandlerTCPInvoker extends SimpleChannelUpstreamHandler {

    /**
     * The server message callback.
     */
    private final AdvancedServerMessageHandler handler;

    /**
     * The constructor.
     *
     * @param handlerArg The server side handler
     */
    public AdvancedServerMessageHandlerTCPInvoker(
            final AdvancedServerMessageHandler handlerArg) {
        this.handler = handlerArg;
    }

    /**
     * Invoked when a message object (e.g: {@link ChannelBuffer}) was received
     * from a remote peer.
     *
     * @param ctx The channel context.
     * @param e The message event.
     *
     * @throws Exception in case of any error while calling call backs.
     */
    @Override
    public void messageReceived(
            final ChannelHandlerContext ctx,
            final MessageEvent e) throws Exception {

        Object request = e.getMessage();
        if (request instanceof Message) {
            Message response = handler.handleRequest(ctx, (Message) request);
            ctx.getChannel().write(response);
        }
        super.messageReceived(ctx, e);
    }

    /**
     * Invoked when an exception was raised by an I/O thread or a
     * {@link ChannelHandler}.
     *
     * @param ctx The channel context.
     * @param e The exception event.
     *
     * @throws Exception In case of any error while invoking call backs.
     */
    @Override
    public void exceptionCaught(
            final ChannelHandlerContext ctx, final ExceptionEvent e)
                    throws Exception {
        handler.handleException(ctx, e.getCause());
        super.exceptionCaught(ctx, e);
    }

}
