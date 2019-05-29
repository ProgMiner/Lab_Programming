package ru.byprogminer.Lab6_Programming.udp;

import java.nio.ByteBuffer;
import java.util.Objects;

class Packet {

    public final Action action;
    public final long previous;
    public final short count;
    private final ByteBuffer content;

    public Packet(Action action, long previous, int count, ByteBuffer content) {
        this.action = Objects.requireNonNull(action);
        this.previous = previous;
        this.count = (short) (count & 0xFF);

        final ByteBuffer thisContent = ByteBuffer.allocate(content.remaining());
        this.content = thisContent.asReadOnlyBuffer();
        thisContent.put(content);
    }

    public Packet(Action action, long previous) {
        this(action, previous, 0, ByteBuffer.allocate(0));
    }

    public Packet(Action action) {
        this(action, 0);
    }

    public ByteBuffer getContent() {
        return content.duplicate();
    }

    @Override
    public String toString() {
        synchronized (content) {
            final int startContentPosition = content.position();

            try {
                final int contentSize = content.remaining();

                final StringBuilder packetStart = new StringBuilder();
                for (int i = 0; i < 3 && content.hasRemaining(); ++i) {
                    if (i != 0) {
                        packetStart.append(", ");
                    }

                    packetStart.append(String.format("0x%02X", content.get()));
                }

                if (content.hasRemaining()) {
                    packetStart.append("...");
                }

                return String.format("%2$d -> %1$s/%3$d new byte[%4$d] {%5$s}",
                        action, previous, count, contentSize, packetStart);
            } finally {
                content.position(startContentPosition);
            }
        }
    }
}
