package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InputPacket implements Packet {
    public int playerId;
    public boolean up, down, left, right, attack;
    public float targetAngle;

    public InputPacket() {}
    public InputPacket(int playerId, boolean up, boolean down, boolean left, boolean right, boolean attack, float targetAngle) {
        this.playerId = playerId;
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.attack = attack;
        this.targetAngle = targetAngle;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeBoolean(up);
        out.writeBoolean(down);
        out.writeBoolean(left);
        out.writeBoolean(right);
        out.writeBoolean(attack);
        out.writeFloat(targetAngle);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        up = in.readBoolean();
        down = in.readBoolean();
        left = in.readBoolean();
        right = in.readBoolean();
        attack = in.readBoolean();
        targetAngle = in.readFloat();
    }

    @Override
    public byte getOpCode() {
        return OpCode.UDP_INPUT;
    }
}
