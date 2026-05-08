package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LobbyInfoPacket implements Packet {
    public int playerCount;
    public boolean isHost;

    public LobbyInfoPacket() {}
    public LobbyInfoPacket(int playerCount, boolean isHost) {
        this.playerCount = playerCount;
        this.isHost = isHost;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerCount);
        out.writeBoolean(isHost);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerCount = in.readInt();
        isHost = in.readBoolean();
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_LOBBY_INFO;
    }
}
