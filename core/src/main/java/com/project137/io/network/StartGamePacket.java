package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StartGamePacket implements Packet {
    @Override
    public void write(DataOutputStream out) throws IOException {}
    @Override
    public void read(DataInputStream in) throws IOException {}
    @Override
    public byte getOpCode() {
        return OpCode.TCP_START_GAME;
    }
}
