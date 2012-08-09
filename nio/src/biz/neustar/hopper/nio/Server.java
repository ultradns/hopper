package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
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

/**
 * A Server skeleton for the DNS protocol that handles TCP and UPD request.
 * 
 * <p>
 * The server will have a instance of
 * org.jboss.netty.handler.logging.LoggingHandler as the first handler bound to
 * SLF4j as the implementation.
 * </p>
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class Server {

	/** A logger */
	private final static Logger log = LoggerFactory.getLogger(Server.class);

	/**
	 * The default TCP pipeline
	 */
	final private ChannelPipelineFactory tcpChannelPipelineFactory = new ChannelPipelineFactory() {

		@Override
		public ChannelPipeline getPipeline() {
			return Channels.pipeline(new LoggingHandler(), new TCPDecoder(), new TCPEncoder(), new MessageDecoder(),
					new MessageEncoder(), new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(10, 0, 0)),
					new EchoMessageHandler());
		}
	};

	/** The TCP Channel factory */
	final private AtomicReference<ChannelFactory> tcpChannelFactory = new AtomicReference<ChannelFactory>();

	/**
	 * The default UDP pipeline
	 */
	final private ChannelPipelineFactory udpChannelPipelineFactory = new ChannelPipelineFactory() {

		@Override
		public ChannelPipeline getPipeline() {
			return Channels.pipeline(new LoggingHandler(), new MessageDecoder(), new MessageEncoder(),
					new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(10, 0, 0)), new EchoMessageHandler());
		}
	};

	/** The TCP Channel factory */
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
	 * Load the SLF4J logging binding for
	 * org.jboss.netty.handler.logging.LoggingHandler
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
	public Server(int port) {

		log.debug("Binding to {}", port);

		// Start listening for UDP request
		udpChannelFactory.set(new NioDatagramChannelFactory(Executors.newCachedThreadPool()));
		ConnectionlessBootstrap udpBootstrap = new ConnectionlessBootstrap(udpChannelFactory.get());
		udpBootstrap.setOption("receiveBufferSize", 512);
		udpBootstrap.setOption("sendBufferSize", 512);
		udpBootstrap.setPipelineFactory(udpChannelPipelineFactory);
		Channel udpChannel = udpBootstrap.bind(new InetSocketAddress(port));
		channelGroup.add(udpChannel);
		this.boundTo.set(((InetSocketAddress) udpChannel.getLocalAddress()));

		// Start listening for TCP request on the same port
		tcpChannelFactory.set(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors
				.newCachedThreadPool()));
		ServerBootstrap tcpBootstrap = new ServerBootstrap(tcpChannelFactory.get());
		tcpBootstrap.setPipelineFactory(tcpChannelPipelineFactory);
		log.info("Binding to {}", port);
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

		Server server = new Server(1053);
		synchronized (server) {
			server.wait();
		}
	}

}
