package biz.neustar.hopper.nio.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * Decodes a DNS message from wire format to Java Objects.
 */
public class DNSMessageDecoder extends OneToOneDecoder {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DNSMessageDecoder.class);

    @Override
    protected Object decode(
            final ChannelHandlerContext context,
            final Channel channel, final Object message) throws Exception {

        LOGGER.debug("decode {}", channel.getId());
        ChannelBuffer buffer = (ChannelBuffer) message;
        return new Message(buffer.array());
    }
}

