package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TCP server for the DNS protocol
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class Server {

	private final static Logger log = LoggerFactory.getLogger(Server.class);

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
	public Server(int port) {
		this.port.set(port);
	}

	/**
	 * Construct a new server instance on the default port
	 * 
	 * @param port
	 */
	public Server() {
		this(53);
	}

	/**
	 * Configure the server and start accepting client request
	 */
	public void start() {

		channelFactory.set(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors
				.newCachedThreadPool()));
		ServerBootstrap bootstrap = new ServerBootstrap(channelFactory.get());
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() {
				return Channels.pipeline(new TCPDecoder(), new TCPEncoder(), new MessageDecoder(),
						new MessageEncoder(), new EchoMessageHandler());
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

	public int getPort() {
		return port.get();
	}

	@Override
	public String toString() {
		return "DNSServer [port=" + port.get() + "]";
	}

	public static void main(String[] args) {

		new Server(1052).start();
	}
}
