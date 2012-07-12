package biz.neustar.hopper.nio;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * Decodes a DNS message from wire format to Java Objects
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class MessageDecoder extends OneToOneDecoder {

	private final static Logger log = LoggerFactory.getLogger(MessageDecoder.class);
	
	@Override
	protected Object decode(ChannelHandlerContext context, Channel channel, Object message) throws Exception {
		
		if(log.isDebugEnabled()) {
			log.debug("decode {}", channel.getId());
		}
		ChannelBuffer buffer = (ChannelBuffer) message;
		byte[] messageBytes = buffer.array();
		return new Message(messageBytes);
	}

}
