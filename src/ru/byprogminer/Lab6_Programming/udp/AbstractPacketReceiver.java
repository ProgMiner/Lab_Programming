package ru.byprogminer.Lab6_Programming.udp;

import ru.byprogminer.Lab6_Programming.Packet;
import ru.byprogminer.Lab6_Programming.Pair;
import ru.byprogminer.Lab6_Programming.PriorityThing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

public abstract class AbstractPacketReceiver<S> implements PacketReceiver {

    protected final S source;
    protected final int partSize;
    protected final Map<SocketAddress, Map<Long, PriorityQueue<PriorityThing<Byte, ByteBuffer>>>> packetsParts = new HashMap<>();
    protected final PriorityQueue<PriorityThing<Long, SocketAddress>> packetAddressesQueue = new PriorityQueue<>();
    protected final Map<SocketAddress, PriorityQueue<PriorityThing<Long, Packet>>> packets = new HashMap<>();
    protected final Map<SocketAddress, Map<Long, Long>> packetsSendTime = new HashMap<>();
    protected final Map<SocketAddress, Set<Long>> packetsEndReceived = new HashMap<>();

    public AbstractPacketReceiver(S source, int partSize) {
        this.source = source;
        this.partSize = partSize;
    }

    @Override
    public synchronized Pair<Packet, SocketAddress> receive() throws IOException, ClassNotFoundException {
        PriorityThing<Long, SocketAddress> priorityPacketAddress;

        // Don't throwing .remove()
        while ((priorityPacketAddress = packetAddressesQueue.poll()) == null) {
            receivePart();
        }

        final PriorityQueue<PriorityThing<Long, Packet>> packets =
                this.packets.get(priorityPacketAddress.getThing());

        final Packet ret = packets.remove().getThing();
        if (packets.isEmpty()) {
            this.packets.remove(priorityPacketAddress.getThing());
        }

        return new Pair<>(ret, priorityPacketAddress.getThing());
    }

    protected synchronized void receivePart() throws IOException, ClassNotFoundException {
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

        final ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + partSize);
        SocketAddress address = receiveDatagram(buffer);
        if (buffer.position() < HEADER_SIZE) {
            receivePart();
        }

        buffer.flip();
        final byte index = buffer.get();
        final byte count = buffer.get();
        final Long id = buffer.getLong();
        final Long time = buffer.getLong();
        buffer.compact();
        buffer.flip();

        final Map<Long, PriorityQueue<PriorityThing<Byte, ByteBuffer>>> packetsParts = this.packetsParts
                .computeIfAbsent(address, socketAddress -> new HashMap<>());

        final PriorityQueue<PriorityThing<Byte, ByteBuffer>> packetParts = packetsParts
                .computeIfAbsent(id, aLong -> new PriorityQueue<>());

        packetParts.add(new PriorityThing<>(index, buffer));

        final Map<Long, Long> packetsSendTime = this.packetsSendTime
                .computeIfAbsent(address, socketAddress -> new HashMap<>());

        final Long prevTime = packetsSendTime.put(id, time);
        if (prevTime != null && prevTime < time) {
            packetsSendTime.put(id, prevTime);
        }

        final Set<Long> packetsEndReceived = this.packetsEndReceived
                .computeIfAbsent(address, socketAddress -> new HashSet<>());

        if (index == count) {
            packetsEndReceived.add(id);
        }

        if (packetsEndReceived.contains(id) && packetParts.size() - 1 == count) {
            constructPacket(address, id);
        }
    }

    private synchronized void constructPacket(SocketAddress address, Long id) throws IOException, ClassNotFoundException {
        final Map<Long, PriorityQueue<PriorityThing<Byte, ByteBuffer>>> packetsParts =
                this.packetsParts.get(address);

        final PriorityQueue<PriorityThing<Byte, ByteBuffer>> packetParts = packetsParts.remove(id);
        if (packetsParts.isEmpty()) {
            this.packetsParts.remove(address);
        }

        final byte[] buffer = new byte[packetParts.size() * partSize];
        final ByteBuffer packetBuffer = ByteBuffer.wrap(buffer);
        while (!packetParts.isEmpty()) {
            packetBuffer.put(packetParts.remove().getThing());
        }

        ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(buffer, 0, packetBuffer.position()));
        Object packet = objectStream.readObject();
        objectStream.close();

        if (packet instanceof Packet) {
            final Map<Long, Long> packetsSendTime = this.packetsSendTime.get(address);

            final Long packetSendTime = packetsSendTime.remove(id);
            if (packetsSendTime.isEmpty()) {
                this.packetsSendTime.remove(address);
            }

            packets.computeIfAbsent(address, socketAddress -> new PriorityQueue<>())
                    .add(new PriorityThing<>(packetSendTime, (Packet) packet));

            packetAddressesQueue.add(new PriorityThing<>(packetSendTime, address));
        }
    }

    protected abstract SocketAddress receiveDatagram(ByteBuffer buffer) throws IOException;

    public S getSource() {
        return source;
    }
}
