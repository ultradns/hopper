package biz.neustar.hopper.nio;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * Encodes a Java Objects to wire format
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class MessageEncoder extends OneToOneEncoder {

	private final static Logger log = LoggerFactory.getLogger(MessageEncoder.class);

	@Override
	protected Object encode(ChannelHandlerContext context, Channel channel, Object message) throws Exception {

		
		if (!(message instanceof Message)) {
			log.debug("Not Encoding");
			return message;
		}
		log.debug("Encoding");
		byte[] wireMessage = ((Message) message).toWire();
		return ChannelBuffers.copiedBuffer(wireMessage);
	}

}
