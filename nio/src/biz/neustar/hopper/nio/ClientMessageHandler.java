package biz.neustar.hopper.nio;

import biz.neustar.hopper.message.Message;

/**
 * Handler definition for client side message processing.
 * <p>
 * Send a message by invoking {@link Client#send(Message)}. When the server
 * responds the handleResponse method of this class is invoked.
 * </p>
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public interface ClientMessageHandler {

	/**
	 * Handle a response from a server
	 * 
	 * @param response
	 *            The request from the server
	 */
	void handleResponse(Message response);

	/**
	 * Handle exceptions raised during the request
	 * 
	 * @param throwable
	 *            The exception
	 */
	void handleException(Throwable throwable);
}
