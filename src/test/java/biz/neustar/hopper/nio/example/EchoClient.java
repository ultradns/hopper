package biz.neustar.hopper.nio.example;

import java.net.InetSocketAddress;

import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.nio.DnsClient;
import biz.neustar.hopper.record.Record;

public class EchoClient {
    public static void main(String[] args) throws InterruptedException, TextParseException {
    DnsClient client = DnsClient.builder().clientMessageHandler(
            new LoggingClientHandler())
            .closeConnectionOnMessageReceipt(true).build();

    String name = "\006isatap\002on\002ws.";
    Record rec = Record.newRecord(new Name("\006isatap\002on\002ws."),
            Type.A, DClass.IN);
    Message query = Message.newQuery(rec);
    query.getHeader().setID(46432);
    client.sendUDP(query, new InetSocketAddress("192.168.148.56",
            Integer.valueOf("53")));
    client.stop();
    }
}
