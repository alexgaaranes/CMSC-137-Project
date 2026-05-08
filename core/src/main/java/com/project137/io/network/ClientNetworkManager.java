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

        Thread.ofVirtual().name("Client-TCP").start(this::listenTCP);
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
                    sendUDP(new HelloPacket(playerId));
                    packet = welcome;
                } else if (opCode == OpCode.TCP_MAP_DATA) {
                    MapDataPacket mapData = new MapDataPacket();
                    mapData.read(tcpIn);
                    packet = mapData;
                } else if (opCode == OpCode.TCP_LOBBY_INFO) {
                    LobbyInfoPacket lobbyInfo = new LobbyInfoPacket();
                    lobbyInfo.read(tcpIn);
                    packet = lobbyInfo;
                } else if (opCode == OpCode.TCP_START_GAME) {
                    packet = new StartGamePacket();
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

    public void sendTCP(Packet packet) {
        try {
            synchronized (tcpOut) {
                tcpOut.writeByte(packet.getOpCode());
                packet.write(tcpOut);
                tcpOut.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendUDP(Packet packet) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(packet.getOpCode());
            packet.write(dos);
            byte[] data = baos.toByteArray();
            // Note: In a real app, should use the connected server IP
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, tcpSocket.getInetAddress(), NetworkConfig.PORT);
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

    public int getPlayerId() { return playerId; }
}
