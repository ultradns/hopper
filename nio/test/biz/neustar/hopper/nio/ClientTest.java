package biz.neustar.hopper.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		Assert.assertTrue(connectTCP.await(5000));
		Assert.assertTrue(connectTCP.isDone());
		Assert.assertTrue(connectTCP.isSuccess());
		
		// try to connect again, should be the same channel
		Channel channel = connectTCP.getChannel();
		connectTCP = client.connectTCP(new InetSocketAddress("localhost", port));
		connectTCP.await(5000);
		Assert.assertTrue(connectTCP.isDone());
		Assert.assertTrue(connectTCP.isSuccess());
		Assert.assertEquals(channel, connectTCP.getChannel());
		Assert.assertTrue(channel.isOpen());
		
	}
	
	
	public void oneClientSerialSendsOneServer() throws TextParseException, UnknownHostException {
		
		client.getPipeline().addLast("log", new LoggingHandler());
		client.getPipeline().addLast("stats", new StatsHandler());
		
		
		for (int i = 0; i < 1; i++) {
			Message query = Message.newQuery(new ARecord(new Name(i + ".example.biz."), DClass.IN, 0l,
					InetAddress.getByName("127.0.0.1")));

			client.sendTCP("localhost", 53, query);
		}

	}
}
