package ru.byprogminer.Lab6_Programming.udp;

import java.nio.ByteBuffer;

public class ParsedPacket extends Packet {

    public final long hash;

    public ParsedPacket(Action action, long previous, int count, ByteBuffer content, long hash) {
        super(action, previous, count, content);

        this.hash = hash;
    }
}
