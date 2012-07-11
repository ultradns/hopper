package biz.neustar.hopper.nio;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * A server handler which echos a wire format message.
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class EchoMessageHandler extends SimpleChannelHandler {

	private static Logger log = LoggerFactory.getLogger(EchoMessageHandler.class);

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		Message message = (Message) e.getMessage();
		log.debug("messageReceived:\n{}", message);
		e.getChannel().write(message);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		log.error("exceptionCaught", e.getCause());
		e.getChannel().close();
	}
}
