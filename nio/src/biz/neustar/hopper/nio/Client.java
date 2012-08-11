package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.handler.ClientMessageHandlerInvoker;
import biz.neustar.hopper.nio.handler.DNSMessageDecoder;
import biz.neustar.hopper.nio.handler.DNSMessageEncoder;
import biz.neustar.hopper.nio.handler.TCPDecoder;
import biz.neustar.hopper.nio.handler.TCPEncoder;

/**
 * A client for the DNS protocol. Sends DNS messages via TCP or UDP.
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class Client {

	/**
	 * A builder of DNS clients
	 */
	public static class Builder {

		protected ClientMessageHandler clientMessageHandler;
		protected int threadPoolSize = 10;
		protected OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor;
		protected NioClientSocketChannelFactory nioClientSocketChannelFactory = new NioClientSocketChannelFactory();
		protected NioDatagramChannelFactory nioDatagramChannelFactory = new NioDatagramChannelFactory();
		protected Map<String, Object> options = new HashMap<String, Object>();
		protected Boolean closeConnectionOnMessageReceipt = Boolean.FALSE;
		protected int udpTimeoutSeconds = 20;

		/** The UDP client pipeline */
		protected ChannelPipeline udpChannelPipeline = Channels.pipeline();

		/** The TCP client pipeline */
		protected ChannelPipeline tcpChannelPipeline = Channels.pipeline();

		private Builder() {
		}

		/** Set the application processing thread pool size */
		public Builder threadPoolSize(int threadPoolSize) {
			this.threadPoolSize = threadPoolSize;
			return this;
		}

		/**
		 * Set the application thread pool executor. threadPoolSize is ignored
		 * when this is set.
		 */
		public Builder orderedMemoryAwareThreadPoolExecutor(
				OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor) {
			this.orderedMemoryAwareThreadPoolExecutor = orderedMemoryAwareThreadPoolExecutor;
			return this;
		}

		/**
		 * Register a client side message handler to be invoked when responses
		 * are received from a server
		 */
		public Builder clientMessageHandler(ClientMessageHandler clientMessageHandler) {
			this.clientMessageHandler = clientMessageHandler;
			return this;
		}

		/**
		 * Set the TCP channel factory. Defaults to a
		 * NioClientSocketChannelFactory with CacheThreadPool executors.
		 */
		public Builder nioClientSocketChannelFactory(NioClientSocketChannelFactory nioClientSocketChannelFactory) {
			this.nioClientSocketChannelFactory = nioClientSocketChannelFactory;
			return this;
		}

		/**
		 * Set the UDP channel factory. Defaults to a with a CacheThreadPool
		 * executor
		 */
		public Builder nioDatagramChannelFactory(NioDatagramChannelFactory nioDatagramChannelFactory) {
			this.nioDatagramChannelFactory = nioDatagramChannelFactory;
			return this;
		}

		/** Set the connection options */
		public Builder options(Map<String, Object> options) {
			this.options = options;
			return this;
		}
		
		/** How long to listen for a UDP response before giving up */
		public Builder udpTimeoutSeconds(int udpTimeoutSeconds) {
			this.udpTimeoutSeconds = udpTimeoutSeconds;
			return this;
		}

		/**
		 * Indicate if the connection should be closed after the response is
		 * received. Default is false.
		 */
		public Builder closeConnectionOnMessageReceipt(Boolean closeConnectionOnMessageReceipt) {
			this.closeConnectionOnMessageReceipt = closeConnectionOnMessageReceipt;
			return this;
		}

		/** Obtain a new Client */
		public Client build() {
			
			// Gotta have a client handler, otherwise what's the point?
			if(clientMessageHandler == null) {
				throw new IllegalStateException("clientMessageHandler must be set");
			}

			// set up the application side thread pool
			OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor = this.orderedMemoryAwareThreadPoolExecutor != null ? this.orderedMemoryAwareThreadPoolExecutor
					: new OrderedMemoryAwareThreadPoolExecutor(threadPoolSize, 0, 0);
			// client handler invoker
			ClientMessageHandlerInvoker clientMessageHandlerInvoker = new ClientMessageHandlerInvoker(
					clientMessageHandler, closeConnectionOnMessageReceipt);

			// build the pipeline
			udpChannelPipeline.addLast("Logger", new LoggingHandler());
			udpChannelPipeline.addLast("MessageDecoder", new DNSMessageDecoder());
			udpChannelPipeline.addLast("MessageEncoder", new DNSMessageEncoder());
			udpChannelPipeline.addLast("ApplicationThreadPool", new ExecutionHandler(
					orderedMemoryAwareThreadPoolExecutor));
			udpChannelPipeline.addLast("ClientMessageHandlerInvoker", clientMessageHandlerInvoker);

			tcpChannelPipeline.addLast("Logger", new LoggingHandler());
			tcpChannelPipeline.addLast("TCPDecoder", new TCPDecoder());
			tcpChannelPipeline.addLast("TCPEncoder", new TCPEncoder());
			tcpChannelPipeline.addLast("MessageDecoder", new DNSMessageDecoder());
			tcpChannelPipeline.addLast("MessageEncoder", new DNSMessageEncoder());
			tcpChannelPipeline.addLast("ApplicationThreadPool", new ExecutionHandler(
					orderedMemoryAwareThreadPoolExecutor));
			tcpChannelPipeline.addLast("ClientMessageHandlerInvoker", clientMessageHandlerInvoker);

			return new Client(this);
		}
	}

	final static Logger log = LoggerFactory.getLogger(Client.class);

	/**
	 * UDP close timeout
	 */
	private final int udpTimeoutSeconds;

	/** The connection bootstrap */
	final private ConnectionlessBootstrap udpBootstrap;

	/** The client helper */
	final private ClientBootstrap tcpBootstrap;

	/** Obtain a new client builder */
	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * Map from servers to connection open futures.
	 */
	ConcurrentHashMap<SocketAddress, ChannelFuture> openTcpConnections = new ConcurrentHashMap<SocketAddress, ChannelFuture>();


	/**
	 * Construct a new Client
	 * 
	 * @param builder
	 *            Which has the client configuration
	 */
	public Client(final Builder builder) {

		udpBootstrap = new ConnectionlessBootstrap(builder.nioDatagramChannelFactory);
		udpBootstrap.setOptions(builder.options);
		udpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return builder.udpChannelPipeline;
			}
		});
		udpTimeoutSeconds = builder.udpTimeoutSeconds;

		// Configure the TCP client.
		tcpBootstrap = new ClientBootstrap(builder.nioClientSocketChannelFactory);
		tcpBootstrap.setOptions(builder.options);
		tcpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() {
				return builder.tcpChannelPipeline;
			}
		});

	}

	public void sendUDP(final Message message, SocketAddress destination) {

		Channel channel = udpBootstrap.bind(new InetSocketAddress(0));
		ChannelFuture write = channel.write(message, destination);
		write.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {

				log.debug("Message sent {}", message);
			}
		});
		if (!channel.getCloseFuture().awaitUninterruptibly(udpTimeoutSeconds * 1000)) {
			log.error("Request timed out.");
			channel.close().awaitUninterruptibly();
		}
	}

	/**
	 * Send a message via TCP asynchronously. This method returns prior to
	 * completion of the request. Add a handler in the pipeline to process the
	 * returned message.
	 * 
	 * @param message
	 *            The DNS message
	 */
	public void sendTCP(final Message message, final SocketAddress destination) {

		connectTCP(destination).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {

				log.debug("operationComplete for channel {}", future.getChannel());
				if (future.isSuccess()) {
					future.getChannel().write(message);
				} else {
					log.error("Could not open connection to {}", destination);
				}
			}
		});

	}

	/**
	 * Shutdown the client. Close open channel and release resources.
	 */
	public void stop() {

		for (ChannelFuture channelFuture : openTcpConnections.values()) {
			try {
				channelFuture.getChannel().close().await();
			} catch (Exception e) {
				// no worries, we are shutting down
			}
		}
		tcpBootstrap.releaseExternalResources();
		udpBootstrap.releaseExternalResources();
	}

	/**
	 * Asynchronously open a connection or return an already open connection
	 * 
	 * @param server
	 *            To which the connection should be opened
	 * @return A ChannelFuture for the connection operation
	 */
	protected ChannelFuture connectTCP(final SocketAddress destination) {

		ChannelFuture toReturn = openTcpConnections.get(destination);
		if (toReturn == null) {
			// open a connection
			toReturn = tcpBootstrap.connect(destination);
			log.debug("Opened {}", toReturn);
			toReturn.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture arg0) throws Exception {
					openTcpConnections.remove(destination);
				}
			});
			ChannelFuture toUse = openTcpConnections.putIfAbsent(destination, toReturn);
			if (toUse != null) {
				// Already opened, discard this one and use the other one
				toReturn.getChannel().close();
				toReturn = toUse;
			}
		}
		log.debug("Returning channelFuture {}", toReturn);
		return toReturn;
	}

}
