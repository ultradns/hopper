/**
 * Copyright.
 */
package biz.neustar.hopper.nio;

import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import biz.neustar.hopper.message.Message;

/**
 * Handler definition for client side message processing.
 * <p>
 * Send a message by invoking {@link DnsClient#send(Message)}. When the server
 * responds the handleResponse method of this class is invoked.
 * </p>
 */
public interface AdvancedClientMessageHandler {

    /**
     * Handle a response from a server.
     * 
     * @param response
     *            The response from the server.
     */
    void handleResponse(final Message response, MessageEvent me);

    /**
     * Handle exceptions raised during the response handling.
     * 
     * @param throwable
     *            The exception.
     */
    void handleException(final Throwable throwable, ExceptionEvent ee);
}
