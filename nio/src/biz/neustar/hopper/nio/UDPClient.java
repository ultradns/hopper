package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.nio.handler.DNSMessageDecoder;
import biz.neustar.hopper.nio.handler.DNSMessageEncoder;

/**
 * A UDP client for the DNS protocol
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class UDPClient {

	private static final int TIMEOUT_MILLIS = 5000;

	private final static Logger log = LoggerFactory.getLogger(UDPClient.class);
	static {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	/** The channel pipeline for this client */
	final private ChannelPipeline pipeline = Channels.pipeline();

	/** The client helper */
	final private ConnectionlessBootstrap bootstrap;

	/** The host name or IP address to connect to */
	final String host;

	/** The port to connect to */
	final int port;

	final private ExecutorService ioThreadPool = Executors.newCachedThreadPool();

	public UDPClient() {
		this("localhost", 53);
	}

	public UDPClient(String host, int port) {

		this.host = host;
		this.port = port;
		log.info("{} {}", host, port);

		pipeline.addLast("Logger", new LoggingHandler());
		pipeline.addLast("MessageDecoder", new DNSMessageDecoder());
		pipeline.addLast("MessageEncoder", new DNSMessageEncoder());

		// Configure the client.
		bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(ioThreadPool));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() {
				return pipeline;
			}
		});

		bootstrap.setOption("sendBufferSize", 512);
		bootstrap.setOption("receiveBufferSize", 512);
		bootstrap.setOption("broadcast", "false");

	}

	public void stop() {

		bootstrap.releaseExternalResources();
		ioThreadPool.shutdown();
	}

	public void sendUDP(Message message, SocketAddress destination) {

		Channel channel = bootstrap.bind(new InetSocketAddress(0));
		ChannelFuture write = channel.write(message, destination);
		write.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				
				log.info("Message sent");
			}
		});
		if (!channel.getCloseFuture().awaitUninterruptibly(TIMEOUT_MILLIS)) {
			log.error("Request timed out.");
			channel.close().awaitUninterruptibly();
		}
	}

	protected ChannelPipeline getPipeline() {
		return pipeline;
	}

}
