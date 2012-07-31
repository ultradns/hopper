package biz.neustar.hopper.nio;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import biz.neustar.hopper.exception.TextParseException;

/**
 * Test for the UDP client 
 * 
 * @author Marty Kube <marty@beavercreekconsulting.com>
 * 
 */
public class UDPClientTest {

	@Test
	public void test() throws TextParseException, UnknownHostException {
		
		new UDPClient().sendUDP(TCPClientTest.getQuery(0), new InetSocketAddress("localhost", 53));
	}

}
