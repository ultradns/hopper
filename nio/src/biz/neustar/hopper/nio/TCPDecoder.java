package biz.neustar.hopper.nio;

import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

/**
 * Decodes a TCP message
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class TCPDecoder extends LengthFieldBasedFrameDecoder {

	public TCPDecoder() {
		// max frame size is 2^16 - 1
		super(65535, 0, 2, 0, 2);
	}

}
