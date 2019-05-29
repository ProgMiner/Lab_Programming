package ru.byprogminer.Lab6_Programming.udp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Packets structure
 *
 * +-----------+--------+----------+-------+---------+
 * | Signature | Action | Previous | Count | Content |
 * +-----------+--------+----------+-------+---------+
 *
 *   - Signature -  int   - Signature
 *   - Action    -  byte  - Action ({@see Action})
 *   - Previous  -  long  - Hash of the previous sent packet
 *   - Count     -  byte  - Count of packets in this object (decreased by one)
 *   - Content   - byte[] - Content of this packet
 */
public final class PacketUtils {

    private static final int SIGNATURE = 0x55_44_50_53;
    private static final int HEADER_SIZE = 2 * Byte.BYTES + Integer.BYTES + Long.BYTES;

    public static final int OPTIMAL_PACKET_SIZE = 1536 - HEADER_SIZE;

    final int packetSize;
    final int contentSize;

    private final BigDecimal bigContentSize;

    PacketUtils(int contentSize) {
        this.contentSize = contentSize;

        bigContentSize = BigDecimal.valueOf(contentSize);
        packetSize = HEADER_SIZE + contentSize;
    }

    /**
     * Calculates count of packets that need for transport some count of data
     *
     * @param dataSize count of data in bytes
     *
     * @return calculated packets' count
     */
    public short getPacketsCount(int dataSize) {
        return BigDecimal.valueOf(dataSize).divide(bigContentSize, 0, RoundingMode.UP)
                .toBigInteger().subtract(BigInteger.ONE).and(BigInteger.valueOf(0xFF))
                .shortValueExact();
    }

    Packet makePacket(Action action, long previous, int count, ByteBuffer content) {
        final ByteBuffer buffer = ByteBuffer.allocate(Math.min(contentSize, content.remaining()));

        while (buffer.hasRemaining() && content.hasRemaining()) {
            buffer.put(content.get());
        }

        buffer.flip();
        return new Packet(action, previous, count, buffer);
    }

    /**
     * Parses packet from buffer and returns parsed packet
     * or null if it is not packet in the buffer
     *
     * @return parsed packet or null if it is not packet in the buffer
     */
    ParsedPacket parsePacket(ByteBuffer buffer) {
        final int startPosition = buffer.position();
        final long hash = crc32(buffer);
        buffer.position(startPosition);

        if (buffer.remaining() < HEADER_SIZE || buffer.getInt() != SIGNATURE) {
            return null;
        }

        final Action action = Action.by(buffer.get());
        final long previous = buffer.getLong();
        final byte count = buffer.get();
        final ByteBuffer content = buffer.slice();
        content.limit(Math.min(content.remaining(), contentSize));

        return new ParsedPacket(action, previous, count, content, hash);
    }

    /**
     * Writes packet to the byte buffer
     *
     * @param packet packet to write
     *
     * @return written packet
     */
    ByteBuffer writePacket(Packet packet) {
        final ByteBuffer buffer = ByteBuffer.allocate(packetSize);

        buffer.putInt(SIGNATURE);
        buffer.put(packet.action.getCode());
        buffer.putLong(packet.previous);
        buffer.put((byte) packet.count);
        buffer.put(packet.getContent());
        buffer.flip();

        return buffer.asReadOnlyBuffer();
    }

    public static long crc32(ByteBuffer buffer) {
        final CRC32 crc32 = new CRC32();
        crc32.update(buffer);

        return crc32.getValue();
    }
}
