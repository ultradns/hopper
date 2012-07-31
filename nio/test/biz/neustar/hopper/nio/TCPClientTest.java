package biz.neustar.hopper.nio;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.junit.BeforeClass;
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
public class TCPClientTest {

	@BeforeClass
	public static void setLogging() {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	@Test
	public void defaultPipeline() {

		// check the names of the default pipeline
		Assert.assertEquals("[TCPDecoder, TCPEncoder, MessageDecoder, MessageEncoder, Logger, ApplicationThreadPool]",
				new TCPClient("").getPipeline().getNames().toString());
	}

	@Test
	public void connectTCP() throws InterruptedException {

		// start server
		TCPServer server = new TCPServer(0);
		server.start();
		TCPClient client = new TCPClient("localhost", server.getPort());

		// get a new connection
		ChannelFuture connectTCP = client.connectTCP();
		Assert.assertTrue(connectTCP.await(500));
		Assert.assertTrue(connectTCP.isDone());
		Assert.assertTrue(connectTCP.isSuccess());

		// try to connect again, should be the same channel
		int count = 0;
		do {
			Channel channel = connectTCP.getChannel();
			connectTCP = client.connectTCP();
			connectTCP.await(500);
			Assert.assertTrue(connectTCP.isDone());
			Assert.assertTrue(connectTCP.isSuccess());
			Assert.assertEquals(channel, connectTCP.getChannel());
			channel = connectTCP.getChannel();
		} while (count++ < 100);

		// shutdown
		client.stop();
		server.stop();
	}

	@Test
	public void oneClientSerialSendsOneServerTCP() throws TextParseException, UnknownHostException,
			InterruptedException {

		// one client thread sending many message to a single server destination

		// start server
		TCPServer server = new TCPServer(0);
//		server.start();
//		Client client = new Client("localhost", server.getPort());
		TCPClient client = new TCPClient("localhost", 1052);

		int messageCount = 100;

		MessageReceivedTrap responseReceivedTrap = new MessageReceivedTrap(messageCount);
		client.getPipeline().addLast("trap", responseReceivedTrap);

		// send messages
		for (int i = 0; i < 1; i++) {
			Message query = getQuery(i);
			client.sendTCP(query);
		}

		// wait to receive them prior to time out
		try {
			Assert.assertTrue(responseReceivedTrap.latch.await(2, TimeUnit.SECONDS));
		} finally {
			// shutdown
			client.stop();
			server.stop();
		}
	}

	@Test
	public void manyClientsOneServerTCP() throws TextParseException, UnknownHostException, InterruptedException {

		// set up a bunch of client and server and have them chat for a while
		// start server
		TCPServer server = new TCPServer(0);
		server.start();

		int messageCount = 900;
		int clientCount = 3;
		int port = server.getPort();
		MessageReceivedTrap responseReceivedTrap = new MessageReceivedTrap(messageCount);
		List<TCPClient> clients = new ArrayList<TCPClient>(clientCount);
		for (int i = 0; i < clientCount; i++) {
			clients.add(new TCPClient("localhost", port));
			clients.get(i).getPipeline().addLast("trap", responseReceivedTrap);
		}
		// start the conversation
		Random random = new Random();
		for (int i = 0; i < messageCount; i++) {
			TCPClient client = clients.get(random.nextInt(clients.size()));
			client.sendTCP(getQuery(i));
		}

		try {
			// wait for all of the responses to come back
			Assert.assertTrue(responseReceivedTrap.latch.await(3, TimeUnit.SECONDS));
		} finally {
			// shut down
			for (TCPClient client : clients) {
				client.stop();
			}
			server.stop();
		}
	}

	@Test
	public void manyClientsManyServerTCP() throws TextParseException, UnknownHostException, InterruptedException {

		runClientsAndServers(10000, 1, 1);
		runClientsAndServers(10000, 25, 2);
		runClientsAndServers(10000, 2, 25);
		runClientsAndServers(10000, 200, 200);

		// give out of memory error not work , see NETTY-424
		// runClientsAndServers(10000, 2, 2000);
	}

	private void runClientsAndServers(int messageCount, int serverCount, int clientCount) throws TextParseException,
			UnknownHostException, InterruptedException {
		// set up a bunch of client and server and have them chat for a while
		// start server
		TCPServer server = new TCPServer(0);
		server.start();

		List<TCPServer> servers = new ArrayList<TCPServer>(serverCount);
		for (int i = 0; i < serverCount; i++) {
			servers.add(new TCPServer(0));
			servers.get(i).start();
		}
		MessageReceivedTrap responseReceivedTrap = new MessageReceivedTrap(messageCount);
		List<TCPClient> clients = new ArrayList<TCPClient>(clientCount);
		for (int i = 0; i < clientCount; i++) {
			clients.add(new TCPClient("localhost", servers.get(i % serverCount).getPort()));
			clients.get(i).getPipeline().addLast("trap", responseReceivedTrap);
		}
		// start the conversation
		Random random = new Random();
		for (int i = 0; i < messageCount; i++) {
			TCPClient client = clients.get(random.nextInt(clients.size()));
			client.sendTCP(getQuery(i));
		}

		try {
			// wait for all of the responses to come back
			Assert.assertTrue(responseReceivedTrap.latch.await(3, TimeUnit.SECONDS));
		} finally {
			// shut down
			for (TCPClient client : clients) {
				client.stop();
			}
			server.stop();
		}
	}

	public static Message getQuery(int i) throws TextParseException, UnknownHostException {

		return Message.newQuery(new ARecord(new Name(i + ".example.biz."), DClass.IN, 0l, InetAddress
				.getByName("127.0.0.1")));
	}
}
