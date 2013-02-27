package biz.neustar.hopper.nio.example;

import biz.neustar.hopper.nio.DnsServer;

/**
 * A DNS Sever that echo the request
 */
public class EchoServer {

    public static void main(String[] args) throws InterruptedException {

        // start the server
        DnsServer.builder().port(1053).serverMessageHandler(new EchoServerHandler()).build();

        // wait around forever listening for client request
        synchronized (EchoServer.class) {
            EchoServer.class.wait();
        }
    }
}