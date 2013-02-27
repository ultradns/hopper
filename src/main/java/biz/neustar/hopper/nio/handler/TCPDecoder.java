package biz.neustar.hopper.nio.handler;

import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

/**
 * Decodes a TCP message.
 */
public class TCPDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MAX_DNS_FRAME_LENGTH = 65535;
    private static final int LENGTH_BYTES = 2;

    /**
     * Constructor.
     */
    public TCPDecoder() {
        super(MAX_DNS_FRAME_LENGTH, 0, LENGTH_BYTES, 0, LENGTH_BYTES);
    }

}

