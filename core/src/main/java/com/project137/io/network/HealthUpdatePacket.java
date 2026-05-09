package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HealthUpdatePacket implements Packet {
    public int entityId;
    public float currentHealth;

    public HealthUpdatePacket() {}
    public HealthUpdatePacket(int entityId, float currentHealth) {
        this.entityId = entityId;
        this.currentHealth = currentHealth;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(entityId);
        out.writeFloat(currentHealth);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        entityId = in.readInt();
        currentHealth = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_HEALTH_UPDATE;
    }
}
