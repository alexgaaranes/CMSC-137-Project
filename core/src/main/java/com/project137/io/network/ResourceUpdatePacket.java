package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ResourceUpdatePacket implements Packet {
    public int playerId;
    public float hp;
    public float energy;
    public float reviveProgress;

    public ResourceUpdatePacket() {}
    public ResourceUpdatePacket(int playerId, float hp, float energy) {
        this(playerId, hp, energy, 0);
    }
    public ResourceUpdatePacket(int playerId, float hp, float energy, float reviveProgress) {
        this.playerId = playerId;
        this.hp = hp;
        this.energy = energy;
        this.reviveProgress = reviveProgress;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeFloat(hp);
        out.writeFloat(energy);
        out.writeFloat(reviveProgress);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        hp = in.readFloat();
        energy = in.readFloat();
        reviveProgress = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_RESOURCE_UPDATE;
    }
}
