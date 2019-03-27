package ru.byprogminer.Lab6_Programming.udp;

public enum Action {

    CONNECT,
    TRANSPORT, CARING_TRANSPORT, REPEAT,
    CONFIRM_TRANSPORT, CONFIRM_CONFIRM;

    public static Action by(byte code) {
        return values()[code];
    }

    public byte getCode() {
        return (byte) ordinal();
    }
}
