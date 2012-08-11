package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.handler.DNSMessageDecoder;
import biz.neustar.hopper.nio.handler.DNSMessageEncoder;
import biz.neustar.hopper.nio.handler.TCPDecoder;
import biz.neustar.hopper.nio.handler.TCPEncoder;

/**
 * TCP client for DNS messages.
 * 
 * This class is thread safe.
 * 
 * @author mkube
 * 
 */
public class TCPClient {

	private final static Logger log = LoggerFactory.getLogger(TCPClient.class);

	/** The channel pipeline for this client */
	final private ChannelPipeline pipeline = Channels.pipeline();

	/** The client helper */
	final private ClientBootstrap bootstrap;

	/** The host name or IP address to connect to */
	final String host;

	/** The port to connect to */
	final int port;

	/** The single channel open request associated with this client */
	final private AtomicReference<ChannelFuture> channelFurure = new AtomicReference<ChannelFuture>();

	/**
	 * Constructs a new client with end-point port of 53
	 */
	public TCPClient(String host) {

		this(host, 53);
	}

	/**
	 * Constructs a new client
	 */
	public TCPClient(String host, int port) {

		this.host = host;
		this.port = port;

		pipeline.addLast("TCPDecoder", new TCPDecoder());
		pipeline.addLast("TCPEncoder", new TCPEncoder());
		pipeline.addLast("Logger", new LoggingHandler());
		pipeline.addLast("MessageDecoder", new DNSMessageDecoder());
		pipeline.addLast("MessageEncoder", new DNSMessageEncoder());
		// a 10 thread pool for execution of 
		pipeline.addLast("ApplicationThreadPool", new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(10, 0, 0)));

		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() {
				return pipeline;
			}
		});
	}

	/**
	 * Shutdown the client. Close open channel and release resources.
	 */
	public void stop() {

		ChannelFuture channelFuture = channelFurure.get();
		if (channelFuture != null) {
			channelFuture.getChannel().close().awaitUninterruptibly();
		}
		bootstrap.releaseExternalResources();
	}

	/**
	 * Asynchronously open a connection or return an already open connection
	 * 
	 * @param server
	 *            To which the connection should be opened
	 * @return A ChannelFuture for the connection operation
	 */
	protected ChannelFuture connectTCP() {

		ChannelFuture toReturn = channelFurure.get();
		if (toReturn == null) {
			// open a connection
			toReturn = bootstrap.connect(new InetSocketAddress(host, port));
			log.debug("Opened {}", toReturn);
			if (!channelFurure.compareAndSet(null, toReturn)) {
				// another thread has request a connect. Discard this one and
				// use the other one.
				log.info("Failed to set as pending connection, discarding {}", toReturn);
				toReturn.getChannel().close().awaitUninterruptibly();
				toReturn = channelFurure.get();
			}
		}
		log.info("Returning channelFuture {}", toReturn);
		return toReturn;
	}

	/**
	 * Get the current pipeline
	 * 
	 * @return The channel pipeline
	 */
	public ChannelPipeline getPipeline() {
		return pipeline;
	}

	/**
	 * Send a message via TCP asynchronously. This method returns prior to
	 * completion of the request. Add a handler in the pipeline to process the
	 * returned message.
	 * 
	 * @param message
	 *            The DNS message
	 */
	public void sendTCP(final Message message) {

		connectTCP().addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {

				log.info("operationComplete for channel {}", future.getChannel());
				if (future.isSuccess()) {
					future.getChannel().write(message);
				} else {
					log.error("Could not open connection {} {}", host, port);
				}
			}
		});

	}
}
