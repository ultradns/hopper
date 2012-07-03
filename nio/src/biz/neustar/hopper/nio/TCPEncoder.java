package biz.neustar.hopper.nio;

import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

/**
 * Encodes a TCP message. Adds the length header to the message, send the
 * message over the wire and then closes the connection
 * 
 * @author Marty Kube marty@beavercreekconsulting.com
 * 
 */
public class TCPEncoder extends SimpleChannelDownstreamHandler {

	@Override
	public void handleDownstream(ChannelHandlerContext context, ChannelEvent event) throws Exception {

		if (event instanceof MessageEvent) {

			// We should have a buffer holding the wire format of the message
			ChannelBuffer buffer = (ChannelBuffer) ((MessageEvent) event).getMessage();

			// write the size headers
			int messageSize = buffer.readableBytes();
			ChannelBuffer header = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, 2);
			header.writeShort(messageSize);
			event.getChannel().write(header);

			// write the message and close the connection
			ChannelFuture writeFuture = event.getChannel().write(buffer);
			writeFuture.addListener(new ChannelFutureListener() {
				
				public void operationComplete(ChannelFuture future) {
					future.getChannel().close();
				}
			});
		}

		super.handleDownstream(context, event);
	}
}
