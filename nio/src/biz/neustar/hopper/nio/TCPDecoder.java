package biz.neustar.hopper.nio;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes a TCP message
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class TCPDecoder extends FrameDecoder {

	private final static Logger log = LoggerFactory.getLogger(TCPDecoder.class);

	@Override
	protected Object decode(ChannelHandlerContext context, Channel channel, ChannelBuffer buffer) throws Exception {

		// Make sure we have the length field
		if (buffer.readableBytes() < 2) {
			log.debug("Cannot read length yet");
			return null;
		}
		buffer.markReaderIndex();
		short length = buffer.readShort();
		log.debug("Read length {}", length);
		// make sure we can read the entire message
		if (buffer.readableBytes() < length) {
			log.debug("Cannot read entire message yet");
			buffer.resetReaderIndex();
			return null;
		}
		//read the message
		log.debug("Reading entire message");
		return buffer.readBytes(length);
	}

}
