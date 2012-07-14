package biz.neustar.hopper.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.record.ARecord;

/**
 * Test for the TCP client
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class ClientTest {

	Server server;
	int port;
	Client client;

	@BeforeClass
	public static void setLogging() {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	@Before
	public void before() {
		server = new Server(0);
		server.start();
		port = server.getPort();
		client = new Client();

	}

	@After
	public void after() {
		client.stop();
		server.stop();
	}

	@Test
	public void defaultPipeline() {

		// check the names of the default pipeline
		Assert.assertEquals("[TCPDecoder, TCPEncoder, MessageDecoder, MessageEncoder]", new Client().getPipeline()
				.getNames().toString());
	}

	@Test
	public void connectTCP() throws InterruptedException {

		// get a new connection
		ChannelFuture connectTCP = client.connectTCP(new InetSocketAddress("localhost", port));
		Assert.assertTrue(connectTCP.await(500));
		Assert.assertTrue(connectTCP.isDone());
		Assert.assertTrue(connectTCP.isSuccess());

		// try to connect again, should be the same channel
		Channel channel = connectTCP.getChannel();
		connectTCP = client.connectTCP(new InetSocketAddress("localhost", port));
		connectTCP.await(500);
		Assert.assertTrue(connectTCP.isDone());
		Assert.assertTrue(connectTCP.isSuccess());
		Assert.assertEquals(channel, connectTCP.getChannel());
		Assert.assertTrue(channel.isOpen());

	}

	public static class MessageReceivedTrap extends SimpleChannelUpstreamHandler {

		private final static Logger log = LoggerFactory.getLogger(MessageReceivedTrap.class);
		final public CountDownLatch latch;
		final private AtomicInteger counter = new AtomicInteger();

		public MessageReceivedTrap(int count) {

			latch = new CountDownLatch(count);
		}

		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

			log.debug("handleUpstream {}", e);
			if (e instanceof MessageEvent) {
				// message recieved
				log.debug("Received message {}", counter.getAndIncrement());
				latch.countDown();
			}
			super.handleUpstream(ctx, e);
		}
		
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

			log.debug("exceptionCaught {}", e);
			super.exceptionCaught(ctx, e);
		}
	}

	@Test
	public void oneClientSerialSendsOneServerTCP() throws TextParseException, UnknownHostException,
			InterruptedException {

		// one client thread sending many message to a single server destination

		int messageCount = 1;

		MessageReceivedTrap responseReceivedTrap = new MessageReceivedTrap(messageCount);
		client.getPipeline().addLast("log", new LoggingHandler());
		client.getPipeline().addLast("trap", responseReceivedTrap);

		// send messages
		for (int i = 0; i < messageCount; i++) {
			Message query = getQuery(i);
			client.sendTCP("localhost", port, query);
		}

		// wait to receive them prior to time out
		Assert.assertTrue(responseReceivedTrap.latch.await(2, TimeUnit.SECONDS));
	}

	@Test
	@Ignore("This fails due connection open failures...  Something is wrong in the connection tracking")
	public void manyClientsOneServerTCP() throws TextParseException, UnknownHostException, InterruptedException {

		// set up a bunch of client and server and have them chat for a while

		int messageCount = 100;
		int clientCount = 4;
		MessageReceivedTrap responseReceivedTrap = new MessageReceivedTrap(messageCount);
		List<Client> clients = new ArrayList<Client>(clientCount);
		for (int i = 0; i < clientCount; i++) {
			clients.add(new Client());
			clients.get(i).getPipeline().addLast("log", new LoggingHandler());
			clients.get(i).getPipeline().addLast("trap", responseReceivedTrap);
		}
		// start the conversation
		Random random = new Random();
		for (int i = 0; i < messageCount; i++) {
			Client client = clients.get(random.nextInt(clients.size()));
			client.sendTCP("localhost", server.getPort(), getQuery(i));
		}

		try {
			// wait for all of the responses to come back
			Assert.assertTrue(responseReceivedTrap.latch.await(3, TimeUnit.SECONDS));
		} finally {
			// shut down
			for (Client client : clients) {
				client.stop();
			}
		}
	}

	private Message getQuery(int i) throws TextParseException, UnknownHostException {

		return Message.newQuery(new ARecord(new Name(i + ".example.biz."), DClass.IN, 0l, InetAddress
				.getByName("127.0.0.1")));
	}
}
