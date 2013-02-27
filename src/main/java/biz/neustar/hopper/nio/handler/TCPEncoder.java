package biz.neustar.hopper.nio.handler;

import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;

/**
 * Encodes a TCP message. Adds the length header to the message, send the
 * message over the wire and then closes the connection.
 */
public class TCPEncoder extends LengthFieldPrepender {

    private static final int LENGTH_BYTES = 2;
    public TCPEncoder() {
        super(LENGTH_BYTES);
    }

}

