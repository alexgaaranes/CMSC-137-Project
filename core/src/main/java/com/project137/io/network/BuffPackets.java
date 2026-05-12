package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BuffPackets {

    public static class BuffVoteStartPacket implements Packet {
        public String[] options;

        public BuffVoteStartPacket() {}
        public BuffVoteStartPacket(String[] options) {
            this.options = options;
        }

        @Override
        public byte getOpCode() { return OpCode.TCP_BUFF_VOTE_START; }

        @Override
        public void write(DataOutputStream dos) throws IOException {
            dos.writeInt(options.length);
            for (String s : options) dos.writeUTF(s);
        }

        @Override
        public void read(DataInputStream dis) throws IOException {
            int len = dis.readInt();
            options = new String[len];
            for (int i = 0; i < len; i++) options[i] = dis.readUTF();
        }
    }

    public static class BuffVoteSubmitPacket implements Packet {
        public int buffIndex;

        public BuffVoteSubmitPacket() {}
        public BuffVoteSubmitPacket(int index) {
            this.buffIndex = index;
        }

        @Override
        public byte getOpCode() { return OpCode.TCP_BUFF_VOTE_SUBMIT; }

        @Override
        public void write(DataOutputStream dos) throws IOException {
            dos.writeInt(buffIndex);
        }

        @Override
        public void read(DataInputStream dis) throws IOException {
            buffIndex = dis.readInt();
        }
    }

    public static class BuffVoteResultPacket implements Packet {
        public String winnerName;

        public BuffVoteResultPacket() {}
        public BuffVoteResultPacket(String winnerName) {
            this.winnerName = winnerName;
        }

        @Override
        public byte getOpCode() { return OpCode.TCP_BUFF_VOTE_RESULT; }

        @Override
        public void write(DataOutputStream dos) throws IOException {
            dos.writeUTF(winnerName);
        }

        @Override
        public void read(DataInputStream dis) throws IOException {
            winnerName = dis.readUTF();
        }
    }
}
