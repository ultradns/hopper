package biz.neustar.hopper.nio;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upstream handler to log counts of messages received and exceptions caught
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 *
 */
public class ReceiveStatsHandler extends SimpleChannelUpstreamHandler {

	private static Logger log = LoggerFactory.getLogger(LogMessageHandler.class);
	final private AtomicInteger messageReceivedCount = new AtomicInteger();
	final private AtomicInteger exceptionCaughtCount = new AtomicInteger();

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		log.debug("messageReceived {}", e);
		messageReceivedCount.incrementAndGet();
		super.messageReceived(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		log.debug("exceptionCaught {}", e);
		exceptionCaughtCount.incrementAndGet();
		super.exceptionCaught(ctx, e);
	}

	/**
	 * Get the count of messages received
	 * @return the count
	 */
	protected int getMessageReceivedCount() {
		return messageReceivedCount.intValue();
	}

	/**
	 * The count of exceptionsCaught
	 * @return the count
	 */
	protected int getExceptionCaughtCount() {
		return exceptionCaughtCount.intValue();
	}
}
