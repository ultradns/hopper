package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.nio.example.EchoServerHandler;

/**
 * Test for the UDP client
 */
public class UDPClientTest {

    @Test
    public void test() throws TextParseException, UnknownHostException, InterruptedException {

        DnsServer server = DnsServer.builder().port(0).serverMessageHandler(new EchoServerHandler()).build();
        MessageReceivedTrap messageReceivedTrap = new MessageReceivedTrap(1);
        DnsClient client = DnsClient.builder().clientMessageHandler(messageReceivedTrap).closeConnectionOnMessageReceipt(true).build();
        client.sendUDP(TCPClientTest.getQuery(0), new InetSocketAddress("localhost", server.getLocalAddress().getPort()));
        try {
            Assert.assertTrue(messageReceivedTrap.latch.await(2, TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }

}