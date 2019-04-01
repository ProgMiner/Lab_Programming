package ru.byprogminer.Lab6_Programming.udp;

public enum Action {

    CONNECT, FINISH,
    TRANSPORT, CONFIRM;

    public static Action by(byte code) {
        return values()[code];
    }

    public byte getCode() {
        return (byte) ordinal();
    }
}
