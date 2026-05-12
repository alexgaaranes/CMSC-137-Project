package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ResourceUpdatePacket implements Packet {
    public int playerId;
    public float hp;
    public float maxHp;
    public float energy;
    public float maxEnergy;
    public float reviveProgress;

    public ResourceUpdatePacket() {}
    public ResourceUpdatePacket(int playerId, float hp, float maxHp, float energy, float maxEnergy, float reviveProgress) {
        this.playerId = playerId;
        this.hp = hp;
        this.maxHp = maxHp;
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.reviveProgress = reviveProgress;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeFloat(hp);
        out.writeFloat(maxHp);
        out.writeFloat(energy);
        out.writeFloat(maxEnergy);
        out.writeFloat(reviveProgress);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        hp = in.readFloat();
        maxHp = in.readFloat();
        energy = in.readFloat();
        maxEnergy = in.readFloat();
        reviveProgress = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_RESOURCE_UPDATE;
    }
}
