package com.project137.io.network;

import com.project137.io.world.DungeonMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MapDataPacket implements Packet {
    public int width, height;
    public int[][] tiles;

    public MapDataPacket() {}
    public MapDataPacket(DungeonMap map) {
        this.width = map.width;
        this.height = map.height;
        this.tiles = map.tiles;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(width);
        out.writeInt(height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out.writeInt(tiles[x][y]);
            }
        }
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        width = in.readInt();
        height = in.readInt();
        tiles = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = in.readInt();
            }
        }
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_MAP_DATA;
    }
}
