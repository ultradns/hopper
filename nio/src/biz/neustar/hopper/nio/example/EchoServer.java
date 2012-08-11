package biz.neustar.hopper.nio.example;

import biz.neustar.hopper.nio.Server;

/**
 * A DNS Sever that echo the request
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 */
public class EchoServer {

	public static void main(String[] args) throws InterruptedException {

		// start the server
		Server.builder().port(1053).serverMessageHandler(new EchoServerHandler()).build();

		// wait around forever listening for client request
		synchronized (EchoServer.class) {
			EchoServer.class.wait();
		}
	}
}
