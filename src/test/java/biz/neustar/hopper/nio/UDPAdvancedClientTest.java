package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.nio.example.AdvancedEchoHandler;
import biz.neustar.hopper.nio.example.EchoServerHandler;

/**
 * Test for the UDP client
 */
public class UDPAdvancedClientTest {

    @Test
    public void testBasic() throws TextParseException, UnknownHostException,
            InterruptedException {

        DnsServer server = DnsServer.builder().port(0)
                .serverMessageHandler(new EchoServerHandler()).build();
        AdvancedMessageReceivedTrap messageReceivedTrap = new AdvancedMessageReceivedTrap(
                1);
        DnsClient client = DnsClient.builder().advanced(true)
                .advancedClientMessageHandler(messageReceivedTrap)
                .closeConnectionOnMessageReceipt(true).build();
        client.sendUDP(TCPClientTest.getQuery(0), new InetSocketAddress(
                "localhost", server.getLocalAddress().getPort()));
        try {
            Assert.assertTrue(messageReceivedTrap.latch.await(2,
                    TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testAdvanced() throws TextParseException, UnknownHostException,
            InterruptedException {

        DnsServer server = DnsServer.builder().port(0).advanced(true)
                .advancedServerMessageHandler(new AdvancedEchoHandler())
                .build();
        AdvancedMessageReceivedTrap messageReceivedTrap = new AdvancedMessageReceivedTrap(
                1);
        DnsClient client = DnsClient.builder().advanced(true)
                .advancedClientMessageHandler(messageReceivedTrap)
                .closeConnectionOnMessageReceipt(true).build();
        client.sendUDP(TCPClientTest.getQuery(0), new InetSocketAddress(
                "localhost", server.getLocalAddress().getPort()));
        try {
            Assert.assertTrue(messageReceivedTrap.latch.await(2,
                    TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }
}