package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab6_Programming.Packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

public abstract class AbstractPacketSender<S> implements PacketSender {

    protected final S source;
    protected final int partSize;

    public AbstractPacketSender(S source, int partSize) {
        this.source = source;
        this.partSize = partSize;
    }

    @Override
    public void send(Packet packet, SocketAddress to) throws IOException {
        /* Parts structure
         *
         * +-------+-------+----+------+---------+
         * | Index | Count | ID | Time | Content |
         * +-------+-------+----+------+---------+
         *
         *   - Index   -  byte  - Number of this part
         *   - Count   -  byte  - Count of parts in this packet (decreased by one)
         *   - ID      -  long  - Unique ID of this packet
         *   - Time    -  long  - Send time of this part
         *   - Content - byte[] - Content of this part
         *
         */
        final int HEADER_SIZE = 2 * Byte.BYTES + 2 * Long.BYTES;

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(packet);
        objectStream.close();

        final long id = makeId();
        final ByteBuffer buffer = ByteBuffer.wrap(byteStream.toByteArray());
        final short count = assertByte(Math.ceil((double) buffer.remaining() / partSize) - 1);
        for (int index = 0; index <= count; ++index) {
            ByteBuffer part = ByteBuffer.allocate(HEADER_SIZE + partSize);
            part.put((byte) index);
            part.put((byte) count);
            part.putLong(id);
            part.putLong(System.currentTimeMillis());
            while (part.hasRemaining() && buffer.hasRemaining()) {
                part.put(buffer.get());
            }

            part.flip();
            sendDatagram(part, to);
        }
    }

    private static byte assertByte(Number n) {
        if (n.longValue() > 0xFF) {
            throw new IllegalArgumentException("number is greater than 255");
        }

        return n.byteValue();
    }

    protected long makeId() {
        return System.currentTimeMillis() ^ new Random().nextLong();
    }

    protected abstract void sendDatagram(ByteBuffer buffer, SocketAddress to) throws IOException;

    public S getSource() {
        return source;
    }
}
