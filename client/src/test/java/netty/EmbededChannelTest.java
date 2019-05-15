package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.TooLongFrameException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * test netty EmbededChannel and ByteBuf
 */
public class EmbededChannelTest {

    @Test
    public void testEmbededChannel() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }

        ByteBuf input = buf.duplicate();
        EmbeddedChannel channel = new EmbeddedChannel(new FixedDecoder());
        assertTrue(channel.writeInbound(input.retain()));
        assertTrue(channel.finish());

        ByteBuf read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        assertNull(channel.readInbound());
        buf.release();
    }


    @Test
    public void testEmbededChannel2() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }

        ByteBuf input = buf.duplicate();

        EmbeddedChannel channel = new EmbeddedChannel(new FixedDecoder());
        assertFalse(channel.writeInbound(input.readBytes(2)));
        assertTrue(channel.writeInbound(input.readBytes(1)));
        assertTrue(channel.writeInbound(input.readBytes(6)));

        assertTrue(channel.finish());
        ByteBuf read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        assertNull(channel.readInbound());
        buf.release();
    }


    @Test
    public void testEmbededChannel3WithExceptionThrow() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }

        ByteBuf input = buf.duplicate();

        EmbeddedChannel channel = new EmbeddedChannel(new FixedDecoder());
        assertTrue(channel.writeInbound(input.readBytes(2)));
        try {
            assertTrue(channel.writeInbound(input.readBytes(4)));
            fail();
        } catch (TooLongFrameException e) {
            e.printStackTrace();
        }
        assertTrue(channel.writeInbound(input.readBytes(3)));

        assertTrue(channel.finish());
        ByteBuf read = channel.readInbound();
        assertEquals(buf.readSlice(2), read);
        read.release();

        read = channel.readInbound();
        assertEquals(buf.skipBytes(4).readSlice(3), read);
        read.release();
        buf.release();
    }


    @Test
    public void test3() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = -9; i < 0; i++) {
            buf.writeInt(i);
        }

        EmbeddedChannel channel = new EmbeddedChannel(new AbsIntegerEncoder());

        channel.writeOutbound(buf);
        assertTrue(channel.finish());

        Integer out = null;
        while ((out = channel.readOutbound()) != null) {
            System.out.println(out);
        }

    }

    @Test
    public void testReplayingDecoder() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }

        EmbeddedChannel channel = new EmbeddedChannel(new ToIntegerDecoder2());
        channel.writeOutbound(buf);
        assertTrue(channel.finish());

        Object out = null;
        while ((out = channel.readOutbound()) != null) {
            System.out.println(out);
        }
    }

}
