package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.nio.example.EchoServerHandler;
import biz.neustar.hopper.nio.handler.DNSMessageDecoder;
import biz.neustar.hopper.nio.handler.DNSMessageEncoder;
import biz.neustar.hopper.nio.handler.ServerMessageHandlerTCPInvoker;
import biz.neustar.hopper.nio.handler.ServerMessageHandlerUDPInvoker;
import biz.neustar.hopper.nio.handler.TCPDecoder;
import biz.neustar.hopper.nio.handler.TCPEncoder;

/**
 * A Server for the DNS protocol that handles TCP and UPD request. Register a
 * ServerMessageHandler to process client request.<br/>
 * 
 * Create a message Handler:
 * 
 * <pre>
 * public class EchoHandler implements ServerMessageHandler {
 * 	public Message handleRequest(Message request) {
 * 		return request;
 * 	}
 * }
 * </pre>
 * 
 * And start the server:
 * 
 * <pre>
 * Server.builder().port(1053).serverMessageHandler(new EchoHandler()).build();
 * </pre>
 * 
 * The server will have a instance of
 * org.jboss.netty.handler.logging.LoggingHandler as the first handler bound to
 * SLF4j as the implementation. </p>
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class Server {

	/** A logger */
	private final static Logger log = LoggerFactory.getLogger(Server.class);

	/**
	 * Server builder
	 */
	public static class Builder {

		protected int port = 53;
		protected ServerMessageHandler serverMessageHandler;
		protected int threadPoolSize = 10;
		protected NioDatagramChannelFactory nioDatagramChannelFactory = new NioDatagramChannelFactory();
		protected Map<String, Object> udpOptions = new HashMap<String, Object>();
		protected NioServerSocketChannelFactory nioServerSocketChannelFactory = new NioServerSocketChannelFactory();
		protected Map<String, Object> tcpOptions = new HashMap<String, Object>();
		protected OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor;

		public Builder() {

			udpOptions.put("receiveBufferSize", 512);
			udpOptions.put("sendBufferSize", 512);
		}

		/** The port the server will listen on. Default is port 53 */
		public Builder port(int port) {
			this.port = port;
			return this;
		}

		/** The message handler to be invoked when a request is received */
		public Builder serverMessageHandler(ServerMessageHandler serverMessageHandler) {
			this.serverMessageHandler = serverMessageHandler;
			return this;
		}

		/** The application thread pool size. Default is 10. */
		public Builder threadPoolSize(int threadPoolSize) {

			this.threadPoolSize = threadPoolSize;
			return this;
		}

		/**
		 * The UDP channel factory. Default is a channel factory that uses a
		 * Cache Thread pool for the worker thread pool
		 */
		public Builder nioDatagramChannelFactory(NioDatagramChannelFactory nioDatagramChannelFactory) {
			this.nioDatagramChannelFactory = nioDatagramChannelFactory;
			return this;
		}

		/**
		 * The UDP channel factory. Default is a channel factory that uses a
		 * Cache Thread pool for the boss and worker thread pool
		 */
		public Builder nioServerSocketChannelFactory(NioServerSocketChannelFactory nioServerSocketChannelFactory) {
			this.nioServerSocketChannelFactory = nioServerSocketChannelFactory;
			return this;
		}

		/**
		 * The UPD connection options. Default is send and receive buffer size
		 * of 512
		 */
		public Builder tcpOptions(Map<String, Object> tcpOptions) {

			this.tcpOptions = tcpOptions;
			return this;
		}

		/**
		 * The orderedMemoryAwareThreadPoolExecutor use to call application
		 * hooks. If this is set, threadPoolSize is ignored.
		 * 
		 * @param orderedMemoryAwareThreadPoolExecutor
		 * @return
		 */
		public Builder orderedMemoryAwareThreadPoolExecutor(
				OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor) {
			this.orderedMemoryAwareThreadPoolExecutor = orderedMemoryAwareThreadPoolExecutor;
			return this;
		}

		/**
		 * Create and start a new Server instance
		 * 
		 * @return A Server
		 */
		public Server build() {
			
			if(serverMessageHandler == null) {
				throw new IllegalStateException("serverMessageHandler must be set");
			}
			return new Server(this);
		}
	}

	/**
	 * Obtain a new builder instance
	 * 
	 * @return A Builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/** The TCP Channel factory */
	final private AtomicReference<ChannelFactory> tcpChannelFactory = new AtomicReference<ChannelFactory>();

	/** The UDP Channel factory */
	final private AtomicReference<ChannelFactory> udpChannelFactory = new AtomicReference<ChannelFactory>();

	/**
	 * The local address to which the server is bound. If port 0 is requested,
	 * the server will pick a random port which is readable from this attribute.
	 */
	final private AtomicReference<InetSocketAddress> boundTo = new AtomicReference<InetSocketAddress>();

	/**
	 * Open channels that need to be shutdown upon server shutdown
	 */
	final private ChannelGroup channelGroup = new DefaultChannelGroup();

	/**
	 * Load the SLF4J binding for org.jboss.netty.handler.logging.LoggingHandler
	 */
	static {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	/**
	 * Start a server listening on a local port
	 * 
	 * @param port
	 *            The port to listen on, if 0 then a random port is chosen.
	 */
	public Server(final Builder builder) {

		log.debug("Binding to {}", builder.port);

		final OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor = builder.orderedMemoryAwareThreadPoolExecutor == null ? new OrderedMemoryAwareThreadPoolExecutor(
				builder.threadPoolSize, 0, 0) : builder.orderedMemoryAwareThreadPoolExecutor;

		// Start listening for UDP request
		udpChannelFactory.set(builder.nioDatagramChannelFactory);
		ConnectionlessBootstrap udpBootstrap = new ConnectionlessBootstrap(udpChannelFactory.get());
		udpBootstrap.setOptions(builder.udpOptions);
		udpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() {
				return Channels.pipeline(new LoggingHandler(), new DNSMessageDecoder(), new DNSMessageEncoder(),
						new ExecutionHandler(orderedMemoryAwareThreadPoolExecutor), new ServerMessageHandlerUDPInvoker(
								builder.serverMessageHandler));
			}
		});
		Channel udpChannel = udpBootstrap.bind(new InetSocketAddress(builder.port));
		channelGroup.add(udpChannel);
		this.boundTo.set(((InetSocketAddress) udpChannel.getLocalAddress()));

		// Start listening for TCP request on the same port
		tcpChannelFactory.set(builder.nioServerSocketChannelFactory);
		ServerBootstrap tcpBootstrap = new ServerBootstrap(tcpChannelFactory.get());
		tcpBootstrap.setOptions(builder.tcpOptions);
		tcpBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() {
				return Channels.pipeline(new LoggingHandler(), new TCPDecoder(), new TCPEncoder(),
						new DNSMessageDecoder(), new DNSMessageEncoder(), new ExecutionHandler(
								orderedMemoryAwareThreadPoolExecutor), new ServerMessageHandlerTCPInvoker(
								builder.serverMessageHandler));
			}
		});
		log.info("Binding to {}", builder.port);
		Channel tcpChannel = tcpBootstrap.bind(new InetSocketAddress(this.boundTo.get().getPort()));
		channelGroup.add(tcpChannel);

		// let clients know what we are up to
		log.info("Bound to {}", udpChannel.getLocalAddress());
	}

	/**
	 * The address upon which the server is listening
	 * 
	 * @return The local address
	 */
	protected InetSocketAddress getLocalAddress() {
		return boundTo.get();
	}

	/**
	 * Shutdown the server
	 */
	public void stop() {

		log.info("Stopping...");
		channelGroup.close().awaitUninterruptibly();
		tcpChannelFactory.get().releaseExternalResources();
		udpChannelFactory.get().releaseExternalResources();
		log.info("Stopped");
	}

	/**
	 * A friendly and helpful message suitable for framing
	 */
	@Override
	public String toString() {

		return "DNSServer [" + boundTo.get() + "]";
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		Server server = Server.builder().port(1053).serverMessageHandler(new EchoServerHandler()).build();
		synchronized (server) {
			server.wait();
		}
		
	}

}
