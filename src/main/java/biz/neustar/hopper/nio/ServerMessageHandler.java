package biz.neustar.hopper.nio;

import biz.neustar.hopper.message.Message;

/**
 * Handler definition for server side message processing.
 */
public interface ServerMessageHandler {

    /**
     * Handle a request from a client.
     *
     * @param request The request from the client
     *
     * @return The response to the client
     */
    Message handleRequest(final Message request);

    /**
     * Handle exceptions raised while processing a client request.
     *
     * @param throwable
     *            The exception occurred during request handling.
     */
    void handleException(final Throwable throwable);

}

