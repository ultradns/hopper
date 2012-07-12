package biz.neustar.hopper.nio;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * A handler which logs a DNS Message
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class LogMessageHandler extends SimpleChannelUpstreamHandler {

	private static Logger log = LoggerFactory.getLogger(LogMessageHandler.class);

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

		if (e instanceof MessageEvent) {
			Message message = (Message) ((MessageEvent) e).getMessage();
			log.debug("messageReceived:\n{}", message);
		}
		super.handleUpstream(ctx, e);
	}

}
