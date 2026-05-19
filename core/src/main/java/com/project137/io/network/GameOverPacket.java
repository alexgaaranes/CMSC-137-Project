package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GameOverPacket implements Packet {
    public boolean win;

    public GameOverPacket() {}

    public GameOverPacket(boolean win) {
        this.win = win;
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_GAME_OVER;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeBoolean(win);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        win = in.readBoolean();
    }
}
