package biz.neustar.hopper.nio.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ClientMessageHandler;

/**
 * A client side message handler that logs messages received from the server
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class LoggingClientHandler implements ClientMessageHandler {

	private static final Logger log = LoggerFactory.getLogger(LoggingClientHandler.class);

	@Override
	public void handleResponse(Message response) {

		log.info(response.toString());
	}

	@Override
	public void handleException(Throwable throwable) {

		log.error("Exception {}", throwable);
	}

}
