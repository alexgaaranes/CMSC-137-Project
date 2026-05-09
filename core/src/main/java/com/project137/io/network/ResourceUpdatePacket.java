package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ResourceUpdatePacket implements Packet {
    public int playerId;
    public float hp;
    public float energy;

    public ResourceUpdatePacket() {}
    public ResourceUpdatePacket(int playerId, float hp, float energy) {
        this.playerId = playerId;
        this.hp = hp;
        this.energy = energy;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeFloat(hp);
        out.writeFloat(energy);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        hp = in.readFloat();
        energy = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_RESOURCE_UPDATE;
    }
}
