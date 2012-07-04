package biz.neustar.hopper.nio;

import java.nio.ByteOrder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;

public class TCPDecoderTest {

	@Test
	public void decode() throws Exception {

		int bufferLength = 4;

		ChannelBuffer buffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, bufferLength);
		TCPDecoder tcpDecoder = new TCPDecoder();

		// send an empty buffer
		Assert.assertNull(tcpDecoder.decode(null, null, buffer));

		// send a buffer with half an length
		buffer.writeBytes(new byte[] { 1 });
		Assert.assertNull(tcpDecoder.decode(null, null, buffer));

		// send with only length
		buffer.clear();
		int payloadLength = bufferLength - 2;
		buffer.writeShort(payloadLength);
		Assert.assertNull(tcpDecoder.decode(null, null, buffer));

		// send exactly a message
		for (int i = 0; i < payloadLength; i++) {
			buffer.writeByte(i);
		}
		ChannelBuffer decoded = (ChannelBuffer) tcpDecoder.decode(null, null, buffer);
		Assert.assertNotNull(decoded);
		Assert.assertEquals(payloadLength, decoded.readableBytes());

		// Send a buffer with extra payload
		buffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, 2 * bufferLength);
		buffer.writeShort(payloadLength);
		for (int i = 0; i < 2 * payloadLength; i++) {
			buffer.writeByte(i);
		}
		decoded = (ChannelBuffer) tcpDecoder.decode(null, null, buffer);
		Assert.assertNotNull(decoded);
		Assert.assertEquals(payloadLength, decoded.readableBytes());

	}

}
