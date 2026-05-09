package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TileUpdatePacket implements Packet {
    public int x, y, tile;

    public TileUpdatePacket() {}
    public TileUpdatePacket(int x, int y, int tile) {
        this.x = x;
        this.y = y;
        this.tile = tile;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(tile);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        x = in.readInt();
        y = in.readInt();
        tile = in.readInt();
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_TILE_UPDATE;
    }
}
