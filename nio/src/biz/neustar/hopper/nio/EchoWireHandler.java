package biz.neustar.hopper.nio;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A server handler which echos a wire format message.
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class EchoWireHandler extends SimpleChannelHandler {

	private static Logger log = LoggerFactory.getLogger(EchoWireHandler.class);

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		log.debug("messageReceived");
		e.getChannel().write(e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		log.error("exceptionCaught", e.getCause());
		e.getChannel().close();
	}
}
