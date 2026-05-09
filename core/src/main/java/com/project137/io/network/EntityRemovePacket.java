package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EntityRemovePacket implements Packet {
    public int entityId;

    public EntityRemovePacket() {}
    public EntityRemovePacket(int entityId) { this.entityId = entityId; }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(entityId);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        entityId = in.readInt();
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_ENTITY_REMOVE;
    }
}
