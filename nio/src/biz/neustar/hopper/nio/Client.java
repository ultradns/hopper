package biz.neustar.hopper.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

	final ReceiveStatsHandler statsHandler = new ReceiveStatsHandler();

	final ChannelPipeline PIPELINE = Channels.pipeline(new TCPDecoder(), new TCPEncoder(), new MessageDecoder(),
			new MessageEncoder(), new LogMessageHandler(), statsHandler);

	/** the number of messages attempted to be sent */
	final AtomicInteger sends = new AtomicInteger();

	/** the number of messages that failed to be sent */
	final AtomicInteger sendFails = new AtomicInteger();

	/** the number of messages that were sent successfully */
	final AtomicInteger sendSuccesses = new AtomicInteger();

	/** The Netty client helper */
	final AtomicReference<ClientBootstrap> bootstrap = new AtomicReference<ClientBootstrap>();

	public Client() {

		// Configure the client.
		bootstrap.set(new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors
				.newCachedThreadPool())));

		// Set up the pipeline factory.
		bootstrap.get().setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() {
				return PIPELINE;
			}
		});

	}

	public ChannelFuture connect(InetSocketAddress server) {

		return bootstrap.get().connect(server);
	}

	public static void main(String[] args) throws TextParseException, UnknownHostException {

		Client client = new Client();
		client.sendSomeMessages();
	}

	private void sendSomeMessages() {

		ChannelFuture connect = connect(new InetSocketAddress("localhost", 1053));
		connect.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {

				if (!future.isSuccess()) {
					throw new RuntimeException("Could not connect");
				}
				for (int i = 0; i < 10; i++) {
					Message query = Message.newQuery(new ARecord(new Name(i + ".example.biz."), DClass.IN, 0l,
							InetAddress.getByName("127.0.0.1")));
					sends.incrementAndGet();

					ChannelFuture write = future.getChannel().write(query);
					write.addListener(new ChannelFutureListener() {

						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							// send complete, record what happened
							if (future.isSuccess() != true) {
								sendFails.incrementAndGet();
							} else {
								sendSuccesses.incrementAndGet();
							}
						}
					});
				}
			}
		});
	}
}
