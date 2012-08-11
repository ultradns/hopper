package biz.neustar.hopper.nio.handler;

import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

/**
 * Decodes a TCP message
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class TCPDecoder extends LengthFieldBasedFrameDecoder {

	public TCPDecoder() {
		super(65535, 0, 2, 0, 2);
	}

}
