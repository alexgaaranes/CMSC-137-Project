package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ItemSpawnPacket implements Packet {
    public int entityId;
    public String templateId;
    public float x, y;

    public ItemSpawnPacket() {}
    public ItemSpawnPacket(int entityId, String templateId, float x, float y) {
        this.entityId = entityId;
        this.templateId = templateId;
        this.x = x;
        this.y = y;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(entityId);
        out.writeUTF(templateId);
        out.writeFloat(x);
        out.writeFloat(y);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        entityId = in.readInt();
        templateId = in.readUTF();
        x = in.readFloat();
        y = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_ITEM_SPAWN;
    }
}
