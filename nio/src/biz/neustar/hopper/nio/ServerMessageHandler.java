package biz.neustar.hopper.nio;

import biz.neustar.hopper.message.Message;

/**
 * Handler definition for server side message processing.
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public interface ServerMessageHandler {

	/**
	 * Handle a request from a client
	 * @param request The request from the client
	 * @return The response to the client
	 */
	Message handleRequest(Message request);
	
	/**
	 * Handle exceptions raised while processing a client request
	 * 
	 * @param throwable
	 *            The exception
	 */
	void handleException(Throwable throwable);

}
