package biz.neustar.hopper.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.record.ARecord;

public class Client {

	public ClientBootstrap start() throws TextParseException, UnknownHostException {
		// Configure the client.
		ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() {
				return Channels.pipeline( 
						new TCPDecoder(), 
						new TCPEncoder(), 
						new MessageDecoder(),
						new MessageEncoder(),
						new LogMessageHandler());
			}
		});

		return bootstrap;
	}
	
	public static void main(String[] args) throws TextParseException, UnknownHostException {
		
		ClientBootstrap bootstrap = new Client().start();
		
		// Start the connection attempt.
		
		final Message query = Message.newQuery(new ARecord(new Name("example.biz."), DClass.IN, 0l, InetAddress.getByName("127.0.0.1")));

		
	       // Start the connection attempt.
        ChannelFuture future = bootstrap.connect(new InetSocketAddress("localhost", 1053));
        future.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				
				future.getChannel().write(query);
			}
		});

        // need to figure out when to shutdown.  Track connection status and responses.
	}
}
