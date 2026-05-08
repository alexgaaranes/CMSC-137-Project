package com.project137.io.network;

import com.project137.io.NetworkConfig;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class ClientNetworkManager {
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private DataInputStream tcpIn;
    private DataOutputStream tcpOut;
    private int playerId;
    private boolean running = false;
    private Consumer<Packet> packetListener;

    public void setPacketListener(Consumer<Packet> listener) {
        this.packetListener = listener;
    }

    public void connect(String ip) throws IOException {
        tcpSocket = new Socket(ip, NetworkConfig.PORT);
        tcpIn = new DataInputStream(tcpSocket.getInputStream());
        tcpOut = new DataOutputStream(tcpSocket.getOutputStream());
        udpSocket = new DatagramSocket();
        running = true;

        // Start TCP listener thread
        Thread.ofVirtual().name("Client-TCP").start(this::listenTCP);
        // Start UDP listener thread
        Thread.ofVirtual().name("Client-UDP").start(this::listenUDP);
    }

    private void listenTCP() {
        try {
            while (running) {
                byte opCode = tcpIn.readByte();
                Packet packet = null;
                
                if (opCode == OpCode.TCP_WELCOME) {
                    WelcomePacket welcome = new WelcomePacket();
                    welcome.read(tcpIn);
                    this.playerId = welcome.playerId;
                    System.out.println("[Client] Connected as Player " + playerId);
                    sendUDP(new HelloPacket(playerId));
                    packet = welcome;
                } else if (opCode == OpCode.TCP_MAP_DATA) {
                    MapDataPacket mapData = new MapDataPacket();
                    mapData.read(tcpIn);
                    System.out.println("[Client] Map Data Received");
                    packet = mapData;
                }
                
                if (packet != null && packetListener != null) {
                    packetListener.accept(packet);
                }
            }
        } catch (IOException e) {
            if (running) System.out.println("[Client] Disconnected from server.");
        }
    }

    private void listenUDP() {
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                DataInputStream dis = new DataInputStream(bais);
                byte opCode = dis.readByte();

                if (opCode == OpCode.UDP_PLAYER_UPDATE) {
                    PlayerUpdatePacket update = new PlayerUpdatePacket();
                    update.read(dis);
                    if (packetListener != null) packetListener.accept(update);
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    public void sendUDP(Packet packet) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(packet.getOpCode());
            packet.write(dos);
            byte[] data = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(NetworkConfig.IP), NetworkConfig.PORT);
            udpSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPlayerId() {
        return playerId;
    }
}
