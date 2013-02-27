package biz.neustar.hopper.nio.handler;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

/**
 * Encodes a Java Objects to wire format.
 */
public class DNSMessageEncoder extends OneToOneEncoder {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DNSMessageEncoder.class);

    @Override
    protected Object encode(
            final ChannelHandlerContext context,
            final Channel channel,
            final Object message) throws Exception {

        if (!(message instanceof Message)) {
            LOGGER.debug("Skipping Encoding {}", channel.getId());
            return message;
        }

        LOGGER.debug("Encoding {}", channel.getId());
        byte[] wireMessage = ((Message) message).toWire();
        return ChannelBuffers.copiedBuffer(wireMessage);
    }
}

