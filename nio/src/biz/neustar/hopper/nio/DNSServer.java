package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A server for the DNS protocol
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class DNSServer {

	private final static Logger log = LoggerFactory.getLogger(DNSServer.class);
	
	/**
	 * The port to which the server will bind to
	 */
	final private int port;

	/**
	 * Construct a new server instance on the specified port
	 * 
	 * @param port
	 */
	public DNSServer(int port) {
		this.port = port;
	}

	/**
	 * Construct a new server instance on the default port
	 * 
	 * @param port
	 */
	public DNSServer() {
		this(8080);
	}

	/**
	 * Configure the server and start accepting client request
	 */
	public void start() {

		ChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		ServerBootstrap bootstrap = new ServerBootstrap(factory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() {
				return Channels.pipeline();
			}
		});
		log.info("Binding to {}", port);
		bootstrap.bind(new InetSocketAddress(port));
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return "DNSServer [port=" + port + "]";
	}

	public static void main(String[] args) {
		
		new DNSServer().start();
	}
}
