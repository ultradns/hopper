package biz.neustar.hopper.nio;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * A handler which logs a DNS Message
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class LogMessageHandler extends SimpleChannelHandler {

	private static Logger log = LoggerFactory.getLogger(LogMessageHandler.class);

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		Message message = (Message) e.getMessage();
		log.debug("messageReceived:\n{}", message);
		e.getChannel().close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		log.error("exceptionCaught", e.getCause());
		e.getChannel().close();
	}
}
