package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BulletSpawnPacket implements Packet {
    public int entityId;
    public float x, y, vx, vy;

    public BulletSpawnPacket() {}
    public BulletSpawnPacket(int entityId, float x, float y, float vx, float vy) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(entityId);
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(vx);
        out.writeFloat(vy);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        entityId = in.readInt();
        x = in.readFloat();
        y = in.readFloat();
        vx = in.readFloat();
        vy = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_BULLET_SPAWN;
    }
}
