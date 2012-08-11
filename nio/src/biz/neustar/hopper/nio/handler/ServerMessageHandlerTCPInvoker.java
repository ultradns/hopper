package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ServerMessageHandler;

/**
 * Netty handler for invoking a ServerMessageHandler
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public class ServerMessageHandlerTCPInvoker extends SimpleChannelUpstreamHandler {

	private final ServerMessageHandler handler;

	public ServerMessageHandlerTCPInvoker(ServerMessageHandler handler) {

		this.handler = handler;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		Object request = e.getMessage();
		if (request instanceof Message) {
			Message response = handler.handleRequest((Message) request);
			ctx.getChannel().write(response);
		}
		super.messageReceived(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		handler.handleException(e.getCause());
		super.exceptionCaught(ctx, e);
	}

}
