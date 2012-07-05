package biz.neustar.hopper.nio;

import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;

/**
 * Encodes a TCP message. Adds the length header to the message, send the
 * message over the wire and then closes the connection
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class TCPEncoder extends LengthFieldPrepender {

	public TCPEncoder() {
		super(2);
	}

}
