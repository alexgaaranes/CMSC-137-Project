package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HelloPacket implements Packet {
    public int playerId;

    public HelloPacket() {}
    public HelloPacket(int playerId) { this.playerId = playerId; }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_HELLO;
    }
}
