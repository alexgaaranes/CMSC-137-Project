package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PlayerUpdatePacket implements Packet {
    public int playerId;
    public float x, y, angle;
    public long sequence;

    public PlayerUpdatePacket() {}
    public PlayerUpdatePacket(int playerId, float x, float y, float angle, long sequence) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.sequence = sequence;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(angle);
        out.writeLong(sequence);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        x = in.readFloat();
        y = in.readFloat();
        angle = in.readFloat();
        sequence = in.readLong();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_PLAYER_UPDATE;
    }
}
