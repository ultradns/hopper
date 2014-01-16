package biz.neustar.hopper.nio;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.stream.ChunkedInput;

import biz.neustar.hopper.message.Message;

/**
 * Handler definition for advanced server side message processing.
 */
public interface AdvancedServerMessageHandler {
    /**
     * Handle a request from a client and generates a stream of responses.
     * This callback is only for TCP channels.
     *
     * @param ctx The channel handler context for the pipeline.
     * @param request The request from the client
     * @param e Message event.
     * @param channelType The type of transport channel.
     *
     * @return The response to the client
     */
    ChunkedInput handleRequestAndGenerateResponseStream(
            final ChannelHandlerContext ctx,
            final Message request,
            final MessageEvent e,
            final ChannelType channelType);

    /**
     * Handle a request from a client.
     *
     * @param ctx The channel handler context for the pipeline.
     * @param request The request from the client
     * @param e Message event.
     * @param channelType The type of transport channel.
     *
     * @return The response to the client
     */
    Message handleRequest(final ChannelHandlerContext ctx,
            final Message request,
            final MessageEvent e,
            final ChannelType channelType);

    /**
     * Handle exceptions raised while processing a client request.
     *
     * @param ctx The channel handler context for the pipeline.
     * @param throwable
     *            The exception occurred during request handling.
     * @param e The exception event.
     * @param channelType The type of transport channel.
     */
    void handleException(final ChannelHandlerContext ctx,
            final Throwable throwable,
            final ExceptionEvent e,
            final ChannelType channelType);
}
