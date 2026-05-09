package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class WeaponChangePacket implements Packet {
    public int playerId;
    public String slot0;
    public String slot1;
    public int activeSlot;

    public WeaponChangePacket() {}
    public WeaponChangePacket(int playerId, String slot0, String slot1, int activeSlot) {
        this.playerId = playerId;
        this.slot0 = slot0 != null ? slot0 : "";
        this.slot1 = slot1 != null ? slot1 : "";
        this.activeSlot = activeSlot;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(playerId);
        out.writeUTF(slot0);
        out.writeUTF(slot1);
        out.writeInt(activeSlot);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        playerId = in.readInt();
        slot0 = in.readUTF();
        slot1 = in.readUTF();
        activeSlot = in.readInt();
    }

    @Override
    public byte getOpCode() {
        return OpCode.TCP_WEAPON_CHANGE;
    }
}
