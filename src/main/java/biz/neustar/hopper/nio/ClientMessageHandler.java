/**
 * Copyright.
 */
package biz.neustar.hopper.nio;

import biz.neustar.hopper.message.Message;

/**
 * Handler definition for client side message processing.
 * <p>
 * Send a message by invoking {@link DnsClient#sendTCP(Message, SocketAddress)} or
 * {@link DnsClient#sendUDP(Message, SocketAddress)}. When the server responds the
 * handleResponse method of this class is invoked.
 * </p>
 */
public interface ClientMessageHandler {

	/**
	 * Handle a response from a server.
	 *
	 * @param response
	 *            The response from the server.
	 */
	void handleResponse(final Message response);

	/**
	 * Handle exceptions raised during the response handling.
	 *
	 * @param throwable
	 *            The exception.
	 */
	void handleException(final Throwable throwable);
}
