package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * TCP client for DNS messages.
 * 
 * This class is thread safe.
 * 
 * @author mkube
 * 
 */
public class Client {

	private final static Logger log = LoggerFactory.getLogger(Client.class);

	/** The channel pipeline for this client */
	final private ChannelPipeline pipeline = Channels.pipeline();

	/** The Netty client helper */
	final private ClientBootstrap bootstrap;

	/** Open TCP connections */
	final private ConcurrentMap<InetSocketAddress, Channel> openChannels = new ConcurrentHashMap<InetSocketAddress, Channel>();

	/**
	 * Constructs a new client
	 */
	public Client() {

		pipeline.addLast("TCPDecoder", new TCPDecoder());
		pipeline.addLast("TCPEncoder", new TCPEncoder());
		pipeline.addLast("MessageDecoder", new MessageDecoder());
		pipeline.addLast("MessageEncoder", new MessageEncoder());

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
	 * Shutdown the client.  Close open connections and release resources.
	 */
	public void stop() {
		
		for(Channel channel: openChannels.values()) {
			Channels.close(channel).awaitUninterruptibly();
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
	protected ChannelFuture connectTCP(final InetSocketAddress server) {

		ChannelFuture toReturn;
		Channel channel = openChannels.get(server);
		if (channel == null) {
			try {
				toReturn = bootstrap.connect(server);
			} catch (IllegalStateException ise) {
				// could not open, something is wrong.. could be an open in-progress
				toReturn = Channels.future(null);
				toReturn.setFailure(ise);
				return toReturn;
			}
			// record this as the open channel, since we must no block other openers in case we do connect		
			toReturn.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						// possibly record this connection as open
						Channel existingChannel = openChannels.putIfAbsent(server, future.getChannel());
						if (existingChannel != null) {
							// don't use this one, another different one has
							// been opened
							Channels.close(future.getChannel());
						}
					} else {
						// open failed, remove this one
						openChannels.remove(future.getChannel());
					}
				}
			});
		} else {
			toReturn = Channels.future(channel);
			toReturn.setSuccess();
		}

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
	 * completion of the request. It may block a little while opening a
	 * connection. Add a handler to process the returned message.
	 * 
	 * @param host
	 *            The server hostname or IP address
	 * @param port
	 *            The server port
	 * @param query
	 *            The DNS message
	 */
	public void sendTCP(final String host, final int port, final Message message) {
		
		connectTCP(new InetSocketAddress(host, port)).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					future.getChannel().write(message);
				} else {
					log.error("Could not open connection {} {}", host, port);
				}
			}
		});

	}
}
