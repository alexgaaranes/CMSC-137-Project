package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RoomProgressPacket implements Packet {
    public int clearedRooms;
    public int totalRooms;

    public RoomProgressPacket() {}
    public RoomProgressPacket(int clearedRooms, int totalRooms) {
        this.clearedRooms = clearedRooms;
        this.totalRooms = totalRooms;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(clearedRooms);
        out.writeInt(totalRooms);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        clearedRooms = in.readInt();
        totalRooms = in.readInt();
    }

    @Override
    public byte getOpCode() {
        return 22; // OpCode.TCP_ROOM_PROGRESS
    }
}
