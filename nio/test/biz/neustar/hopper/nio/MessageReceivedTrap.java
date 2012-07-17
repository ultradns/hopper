package biz.neustar.hopper.nio;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageReceivedTrap extends SimpleChannelUpstreamHandler {

	private final static Logger log = LoggerFactory.getLogger(MessageReceivedTrap.class);
	final public CountDownLatch latch;
	final private AtomicInteger counter = new AtomicInteger();

	public MessageReceivedTrap(int count) {

		latch = new CountDownLatch(count);
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

		log.debug("handleUpstream {}", e);
		if (e instanceof MessageEvent) {
			// message recieved
			log.debug("Received message {}", counter.getAndIncrement());
			latch.countDown();
		}
		super.handleUpstream(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		log.debug("exceptionCaught {}", e);
		super.exceptionCaught(ctx, e);
	}

}
