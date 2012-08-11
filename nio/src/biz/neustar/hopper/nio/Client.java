package biz.neustar.hopper.nio;

import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
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

		protected int port = 53;
		protected String host;
		protected Nature nature = Nature.TCP;
		protected ClientMessageHandler clientMessageHandler;
		protected int threadPoolSize = 10;
		protected OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor;
		protected NioClientSocketChannelFactory nioClientSocketChannelFactory = new NioClientSocketChannelFactory();
		protected NioDatagramChannelFactory nioDatagramChannelFactory = new NioDatagramChannelFactory();
		protected Map<String, Object> options = new HashMap<String, Object>();

		/** The client pipeline */
		protected ChannelPipeline channelPipeline = Channels.pipeline();

		private Builder() {
		}

		/** Use TCP protocol. Default is UDP. */
		public Builder tcp() {
			nature = Nature.TCP;
			return this;
		}

		/** Use UDP protocol. Default is UDP. */
		public Builder udp() {
			nature = Nature.UDP;
			return this;
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

		/** The remote endpoint port */
		public Builder port(int port) {
			this.port = port;
			return this;
		}

		/** The remote endpoint hostname or IP address */
		public Builder host(String host) {
			this.host = host;
			return this;
		}

		/** Set the connection options */
		public Builder options(Map<String, Object> options) {
			this.options = options;
			return this;
		}

		/** Obtain a new Client */
		public Client build() {

			// set up the application side thread pool
			OrderedMemoryAwareThreadPoolExecutor orderedMemoryAwareThreadPoolExecutor = this.orderedMemoryAwareThreadPoolExecutor != null ? this.orderedMemoryAwareThreadPoolExecutor
					: new OrderedMemoryAwareThreadPoolExecutor(threadPoolSize, 0, 0);

			// build the pipeline
			if (nature == Nature.TCP) {
				channelPipeline.addLast("TCPDecoder", new TCPDecoder());
				channelPipeline.addLast("TCPEncoder", new TCPEncoder());
			}
			channelPipeline.addLast("MessageDecoder", new DNSMessageDecoder());
			channelPipeline.addLast("MessageEncoder", new DNSMessageEncoder());
			channelPipeline
					.addLast("ApplicationThreadPool", new ExecutionHandler(orderedMemoryAwareThreadPoolExecutor));
			channelPipeline.addLast("Logger", new LoggingHandler());
			channelPipeline.addLast("ClientMessageHandlerInvoker",
					new ClientMessageHandlerInvoker(clientMessageHandler));

			return new Client(this);
		}
	}
	
	final static Logger log = LoggerFactory.getLogger(Client.class);

	/** Is this a client using UDP or TCP */
	private enum Nature {
		TCP, UDP
	}
	
	/** The connection boostrap */
	final private Bootstrap bootstrap;

	/**
	 * Construct a new Client
	 * 
	 * @param builder
	 *            Which has the client configuration
	 */
	public Client(final Builder builder) {

		log.info("{} {}", builder.host, builder.port);
		if(builder.nature ==Nature.UDP) {
			bootstrap = new ConnectionlessBootstrap(builder.nioDatagramChannelFactory);
		} else {
			bootstrap = new ClientBootstrap(builder.nioClientSocketChannelFactory);
		}
		bootstrap.setOptions(builder.options);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return builder.channelPipeline;
			}
		});
	}

	/** Obtain a new client builder */
	public static Builder builder() {
		return new Builder();
	}
}
