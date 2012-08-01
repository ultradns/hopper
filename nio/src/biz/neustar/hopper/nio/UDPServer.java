package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UDP server for the DNS protocol
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class UDPServer {

	private final static Logger log = LoggerFactory.getLogger(UDPServer.class);

	/** Keep track of open connections */
	final private ChannelGroup channelGroup = new DefaultChannelGroup();

	/**
	 * The port to which the server will bind to
	 */
	final private AtomicInteger port = new AtomicInteger();

	/** the channel factory for this server */
	final private AtomicReference<ChannelFactory> channelFactory = new AtomicReference<ChannelFactory>();

	/**
	 * Construct a new server instance on the specified port
	 * 
	 * @param port
	 */
	public UDPServer(int port) {

		this.port.set(port);
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	/**
	 * Construct a new server instance on the default port
	 * 
	 * @param port
	 */
	public UDPServer() {

		this(53);
	}

	public void start() {

		channelFactory.set(new NioDatagramChannelFactory(Executors
				.newCachedThreadPool()));

		ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(
				channelFactory.get());

		bootstrap.setOption("receiveBufferSize", 512);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() {
				return Channels.pipeline(new LoggingHandler(),
						new MessageDecoder(), new MessageEncoder(),
						new ExecutionHandler(
								new OrderedMemoryAwareThreadPoolExecutor(10, 0,
										0)), new EchoMessageHandler());
			}
		});
		log.info("Binding to {}", port);
		Channel channel = bootstrap.bind(new InetSocketAddress(port.get()));
		channelGroup.add(channel);
		log.info("Bound to {}", channel.getLocalAddress());
		port.set(((InetSocketAddress) channel.getLocalAddress()).getPort());
	}

	/**
	 * Shutdown the server
	 */
	public void stop() {

		log.info("Stopping...");
		channelGroup.close().awaitUninterruptibly();
		channelFactory.get().releaseExternalResources();
		log.info("Stopped");
	}

	public static void main(String[] args) {
		UDPServer udpServer = new UDPServer(1053);
		udpServer.start();
	}
}
