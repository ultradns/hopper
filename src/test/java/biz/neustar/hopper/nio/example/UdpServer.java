package biz.neustar.hopper.nio.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpServer {

    public static void main(String args[]) throws Exception
    {
        DatagramSocket serverSocket = new DatagramSocket(5353);
        byte[] receiveData = new byte[1024];
        byte [] sendData = 
            {(byte) 0xb5, 0x65, (byte) 0x81, (byte) 0x80, 0x0, 0x1, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0, 0x6, 0x69, 0x73, 0x61, 0x74, 0x61, 0x70, 0x2, 0x6f, 0x6e, 0x2, 0x77, 0x73, 0x0,
                0x0, 0x1, 0x0, 0x1, (byte) 0xc0, 0xc, 0x0, 0x1, 0x0, 0x1, 0x0, 0x0, 0x1, 0x2c, 0x0, 0x0};
        while(true)
        {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            byte [] receive = receivePacket.getData();
            String sentence = new String(receive);
            System.out.println("RECEIVED: " + sentence);
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();

            sendData[0] =  receive[0];
            sendData[1] =  receive[1];
            DatagramPacket sendPacket =
                    new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
        }
    }
}
