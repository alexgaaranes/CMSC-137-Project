package com.project137.io.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketReader {
    public static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes);
    }
}
