package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TeleportPacket implements Packet {
    public int playerId;
    public float x, y;

    public TeleportPacket() {}
    public TeleportPacket(int playerId, float x, float y) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeFloat(x);
        out.writeFloat(y);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        x = in.readFloat();
        y = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_TELEPORT;
    }
}
