package biz.neustar.hopper.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;


import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.nio.example.EchoServerHandler;
import biz.neustar.hopper.nio.example.LoggingClientHandler;
import biz.neustar.hopper.record.ARecord;

/**
 * Test for the TCP client
 */
public class TCPClientTest {

    @BeforeClass
    public static void setLogging() {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    @Test
    public void connectTCP() throws InterruptedException {

        // start server
        DnsServer server = DnsServer.builder().port(0).serverMessageHandler(new EchoServerHandler()).build();
        SocketAddress serverAddress = new InetSocketAddress("localhost", server.getLocalAddress().getPort());
        DnsClient client = DnsClient.builder().clientMessageHandler(new LoggingClientHandler()).build();

        // get a new connection
        ChannelFuture connectTCP = client.connectTCP(serverAddress);
        Assert.assertTrue(connectTCP.await(500));
        Assert.assertTrue(connectTCP.isDone());
        Assert.assertTrue(connectTCP.isSuccess());

        // try to connect again, should be the same channel
        int count = 0;
        do {
            Channel channel = connectTCP.getChannel();
            connectTCP = client.connectTCP(serverAddress);
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
        DnsServer server = DnsServer.builder().port(0).serverMessageHandler(new EchoServerHandler()).build();
        SocketAddress serverAddress = new InetSocketAddress("localhost", server.getLocalAddress().getPort());

        int messageCount = 20;

        MessageReceivedTrap responseReceivedTrap = new MessageReceivedTrap(messageCount);
        DnsClient client = DnsClient.builder().clientMessageHandler(responseReceivedTrap).build();

        // send messages
        for (int i = 0; i < messageCount; i++) {
            Message query = getQuery(i);
            client.sendTCP(query, serverAddress);
        }

        // wait to receive them prior to time out
        try {
            Assert.assertTrue(responseReceivedTrap.latch.await(2, TimeUnit.SECONDS));
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void manyClientsOneServerTCP() throws TextParseException, UnknownHostException, InterruptedException {

        // set up a bunch of client and server and have them chat for a while
        // start server
        DnsServer server = DnsServer.builder().serverMessageHandler(new EchoServerHandler()).port(0).build();
        SocketAddress serverAddress = new InetSocketAddress("localhost", server.getLocalAddress().getPort());

        int messageCount = 900;
        int clientCount = 3;
        MessageReceivedTrap responseReceivedTrap =
                new MessageReceivedTrap(messageCount);
        List<DnsClient> clients = new ArrayList<DnsClient>(clientCount);
        for (int i = 0; i < clientCount; i++) {
            clients.add(DnsClient.builder().clientMessageHandler(responseReceivedTrap).build());
        }
        // start the conversation
        Random random = new Random();
        for (int i = 0; i < messageCount; i++) {
            DnsClient client = clients.get(random.nextInt(clients.size()));
            client.sendTCP(getQuery(i), serverAddress);
        }

        try {
            // wait for all of the responses to come back
            Assert.assertTrue(responseReceivedTrap.latch.await(25, TimeUnit.SECONDS));
        } finally {
            // shut down
            for (DnsClient client : clients) {
                client.stop();
            }
            server.stop();
        }
    }

    @Test
    @Ignore
    // This is looking more like a performance test - tunning is needed to make
    // it work
    public void manyClientsManyServerTCP() throws TextParseException, UnknownHostException, InterruptedException {

        runClientsAndServers(10000, 1, 1);
        runClientsAndServers(10000, 25, 2);
        runClientsAndServers(10000, 2, 25);
        runClientsAndServers(10000, 20, 20);

        // gives an out of memory error , does not work , see NETTY-424
        // runClientsAndServers(10000, 2, 2000);
    }

    private void runClientsAndServers(int messageCount, int serverCount, int clientCount) throws TextParseException,
            UnknownHostException, InterruptedException {

        // set up a bunch of client and server and have them chat for a while

        List<DnsServer> servers = new ArrayList<DnsServer>(serverCount);
        for (int i = 0; i < serverCount; i++) {
            servers.add(DnsServer.builder().port(0).serverMessageHandler(new EchoServerHandler()).build());
        }
        MessageReceivedTrap responseReceivedTrap = new MessageReceivedTrap(messageCount);
        List<DnsClient> clients = new ArrayList<DnsClient>(clientCount);
        for (int i = 0; i < clientCount; i++) {
            clients.add(DnsClient.builder().clientMessageHandler(responseReceivedTrap).build());
        }
        // start the conversation
        Random random = new Random();
        for (int i = 0; i < messageCount; i++) {
            DnsClient client = clients.get(random.nextInt(clients.size()));
            client.sendTCP(getQuery(i), new InetSocketAddress("localhost", servers.get(i % serverCount).getLocalAddress().getPort()));
        }

        try {
            // wait for all of the responses to come back
            Assert.assertTrue(responseReceivedTrap.latch.await(3, TimeUnit.SECONDS));
        } finally {
            // shut down
            for (DnsClient client : clients) {
                client.stop();
            }
            for (DnsServer server : servers) {
                server.stop();
            }
        }
    }

    public static Message getQuery(int i) throws TextParseException, UnknownHostException {

        return Message.newQuery(new ARecord(new Name(i + ".example.biz."), DClass.IN, 0l, InetAddress
                .getByName("127.0.0.1")));
    }
}
