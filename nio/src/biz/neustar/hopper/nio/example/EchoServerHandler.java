package biz.neustar.hopper.nio.example;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ServerMessageHandler;

/**
 * A Server side DNS message processor that echos the request back to clients
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 *
 */
public class EchoServerHandler implements ServerMessageHandler {
	
	@Override
	public Message handleRequest(Message request) {
		return request;
	}
}
