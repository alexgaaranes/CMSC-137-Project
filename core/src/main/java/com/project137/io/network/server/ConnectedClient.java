package com.project137.io.network.server;

import java.net.Socket;
import java.net.InetAddress;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConnectedClient {
    public final int id;
    public final Socket tcpSocket;
    public final DataInputStream in;
    public final DataOutputStream out;
    public InetAddress udpAddress;
    public int udpPort;

    public ConnectedClient(int id, Socket socket) throws IOException {
        this.id = id;
        this.tcpSocket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public void close() {
        try {
            tcpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
