package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DiscreteInputPacket implements Packet {
    public int playerId;
    public boolean swapTrigger;
    public boolean interactTrigger;

    public DiscreteInputPacket() {}

    public DiscreteInputPacket(int playerId, boolean swapTrigger, boolean interactTrigger) {
        this.playerId = playerId;
        this.swapTrigger = swapTrigger;
        this.interactTrigger = interactTrigger;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeBoolean(swapTrigger);
        out.writeBoolean(interactTrigger);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        swapTrigger = in.readBoolean();
        interactTrigger = in.readBoolean();
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_DISCRETE_INPUT;
    }
}
