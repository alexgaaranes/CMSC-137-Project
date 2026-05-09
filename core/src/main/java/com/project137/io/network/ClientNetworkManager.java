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
    private Runnable onDisconnect;

    public void setPacketListener(Consumer<Packet> listener) {
        this.packetListener = listener;
    }

    public void setOnDisconnect(Runnable callback) {
        this.onDisconnect = callback;
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
                
                switch (opCode) {
                    case OpCode.TCP_WELCOME -> {
                        WelcomePacket welcome = new WelcomePacket();
                        welcome.read(tcpIn);
                        this.playerId = welcome.playerId;
                        sendUDP(new HelloPacket(playerId));
                        packet = welcome;
                    }
                    case OpCode.TCP_MAP_DATA -> {
                        MapDataPacket mapData = new MapDataPacket();
                        mapData.read(tcpIn);
                        packet = mapData;
                    }
                    case OpCode.TCP_LOBBY_INFO -> {
                        LobbyInfoPacket lobbyInfo = new LobbyInfoPacket();
                        lobbyInfo.read(tcpIn);
                        packet = lobbyInfo;
                    }
                    case OpCode.TCP_START_GAME -> packet = new StartGamePacket();
                    case OpCode.TCP_ENTITY_REMOVE -> {
                        EntityRemovePacket remove = new EntityRemovePacket();
                        remove.read(tcpIn);
                        packet = remove;
                    }
                    case OpCode.TCP_TILE_UPDATE -> {
                        TileUpdatePacket tileUpdate = new TileUpdatePacket();
                        tileUpdate.read(tcpIn);
                        packet = tileUpdate;
                    }
                    case OpCode.TCP_TELEPORT -> {
                        TeleportPacket tp = new TeleportPacket();
                        tp.read(tcpIn);
                        packet = tp;
                    }
                    case OpCode.TCP_WEAPON_CHANGE -> {
                        WeaponChangePacket wc = new WeaponChangePacket();
                        wc.read(tcpIn);
                        packet = wc;
                    }
                    case OpCode.TCP_ITEM_SPAWN -> {
                        ItemSpawnPacket is = new ItemSpawnPacket();
                        is.read(tcpIn);
                        packet = is;
                    }
                }
                
                if (packet != null && packetListener != null) {
                    packetListener.accept(packet);
                }
            }
        } catch (IOException e) {
            System.out.println("[Client] TCP connection lost: " + e.getMessage());
            handleDisconnect();
        }
    }

    private void handleDisconnect() {
        if (!running) return;
        running = false;
        if (onDisconnect != null) onDisconnect.run();
    }

    private void listenUDP() {
        byte[] buffer = new byte[2048]; // Larger buffer
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                DataInputStream dis = new DataInputStream(bais);
                byte opCode = dis.readByte();

                Packet udpPacket = null;
                if (opCode == OpCode.UDP_PLAYER_UPDATE) {
                    udpPacket = new PlayerUpdatePacket();
                } else if (opCode == OpCode.UDP_RESOURCE_UPDATE) {
                    udpPacket = new ResourceUpdatePacket();
                }

                if (udpPacket != null) {
                    udpPacket.read(dis);
                    if (packetListener != null) packetListener.accept(udpPacket);
                }
            } catch (IOException e) {
                if (running) handleDisconnect();
            }
        }
    }

    public void sendTCP(Packet packet) {
        if (!running) return;
        try {
            synchronized (tcpOut) {
                tcpOut.writeByte(packet.getOpCode());
                packet.write(tcpOut);
                tcpOut.flush();
            }
        } catch (IOException e) {
            handleDisconnect();
        }
    }

    public void sendUDP(Packet packet) {
        if (!running) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(packet.getOpCode());
            packet.write(dos);
            byte[] data = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, tcpSocket.getInetAddress(), NetworkConfig.PORT);
            udpSocket.send(datagramPacket);
        } catch (IOException e) {
            // UDP silent failure usually ok
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
