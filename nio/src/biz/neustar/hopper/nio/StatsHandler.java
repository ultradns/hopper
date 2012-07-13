package biz.neustar.hopper.nio;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upstream handler to log counts of messages received and exceptions caught
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 *
 */
public class StatsHandler extends SimpleChannelHandler {

	private final static Logger log = LoggerFactory.getLogger(StatsHandler.class);
	
	/** Count of send message starts */
	final private AtomicInteger writesStarted = new AtomicInteger();

	/** Count of send message completed */
	final private AtomicInteger writesCompleted = new AtomicInteger();

	/** Count of receive message completed */
	final private AtomicInteger readsreceived= new AtomicInteger();

	/** Count of send/receive failures */
	final private AtomicInteger exceptions= new AtomicInteger();

	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

		log.debug("handleDownstream {} {}", e.getClass().getSimpleName(), e);
		if(e instanceof DownstreamMessageEvent) {
			// write request
			log.debug("write started");
			writesStarted.incrementAndGet();
		}
		super.handleDownstream(ctx, e);
	}
	
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

		log.debug("handleUpstream {} {}", e.getClass().getSimpleName(), e);
		if(e instanceof WriteCompletionEvent) {
			log.debug("write completed");
			writesCompleted.incrementAndGet();
		} else if (e instanceof MessageEvent) {
			log.debug("read completed");
			readsreceived.incrementAndGet();
		}
		super.handleUpstream(ctx, e);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		log.debug("exceptionCaught {}", e);
		exceptions.incrementAndGet();
		super.exceptionCaught(ctx, e);
	}

	protected AtomicInteger getWritesStarted() {
		return writesStarted;
	}

	protected AtomicInteger getWritesCompleted() {
		return writesCompleted;
	}

	protected AtomicInteger getReadsreceived() {
		return readsreceived;
	}

	protected AtomicInteger getExceptions() {
		return exceptions;
	}

}
