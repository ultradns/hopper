package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.ClientMessageHandler;

/**
 * Handler to invoke the client side application call back
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class ClientMessageHandlerInvoker extends SimpleChannelUpstreamHandler {

	/** The target callback on the application side */
	private final ClientMessageHandler clientMessageHandler;

	public ClientMessageHandlerInvoker(ClientMessageHandler clientMessageHandler) {
		this.clientMessageHandler = clientMessageHandler;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		Object request = e.getMessage();
		if (request instanceof Message) {
			clientMessageHandler.handleResponse((Message) request);
		}
		super.messageReceived(ctx, e);
	}

}
