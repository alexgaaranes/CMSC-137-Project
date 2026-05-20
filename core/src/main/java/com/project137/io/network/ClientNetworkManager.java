package com.project137.io.network;

import com.project137.io.NetworkConfig;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ClientNetworkManager {
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private DataInputStream tcpIn;
    private DataOutputStream tcpOut;
    private int playerId;
    private boolean running = false;
    private boolean threadsStarted = false;
    private Consumer<Packet> packetListener;
    private Runnable onDisconnect;
    private final ConcurrentLinkedQueue<Packet> packetBuffer = new ConcurrentLinkedQueue<>();

    public void setPacketListener(Consumer<Packet> listener) {
        this.packetListener = listener;
        if (listener != null) {
            // Process buffered packets
            Packet p;
            while ((p = packetBuffer.poll()) != null) {
                listener.accept(p);
            }
            
            if (running && !threadsStarted) {
                startThreads();
            }
        }
    }

    private void startThreads() {
        if (threadsStarted) return;
        threadsStarted = true;
        Thread.ofVirtual().name("Client-TCP").start(this::listenTCP);
        Thread.ofVirtual().name("Client-UDP").start(this::listenUDP);
    }

    public void setOnDisconnect(Runnable callback) {
        this.onDisconnect = callback;
    }

    public void connect(String ip) throws IOException {
        disconnect(); // Ensure clean state
        tcpSocket = new Socket();
        tcpSocket.connect(new InetSocketAddress(ip, NetworkConfig.PORT), 5000);
        tcpIn = new DataInputStream(tcpSocket.getInputStream());
        tcpOut = new DataOutputStream(tcpSocket.getOutputStream());
        udpSocket = new DatagramSocket();
        running = true;
        threadsStarted = false;
        
        if (packetListener != null) {
            startThreads();
        }
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
                    case OpCode.TCP_GAME_OVER -> {
                        GameOverPacket gameOver = new GameOverPacket();
                        gameOver.read(tcpIn);
                        packet = gameOver;
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
                    case OpCode.TCP_BUFF_VOTE_START -> {
                        BuffPackets.BuffVoteStartPacket start = new BuffPackets.BuffVoteStartPacket();
                        start.read(tcpIn);
                        packet = start;
                    }
                    case OpCode.TCP_BUFF_VOTE_RESULT -> {
                        BuffPackets.BuffVoteResultPacket res = new BuffPackets.BuffVoteResultPacket();
                        res.read(tcpIn);
                        packet = res;
                    }
                    case OpCode.TCP_ROOM_PROGRESS -> {
                        RoomProgressPacket progress = new RoomProgressPacket();
                        progress.read(tcpIn);
                        packet = progress;
                    }
                    case OpCode.UDP_PLAYER_UPDATE -> {
                        PlayerUpdatePacket pup = new PlayerUpdatePacket();
                        pup.read(tcpIn);
                        packet = pup;
                    }
                    case OpCode.UDP_RESOURCE_UPDATE -> {
                        ResourceUpdatePacket rup = new ResourceUpdatePacket();
                        rup.read(tcpIn);
                        packet = rup;
                    }
                    case OpCode.UDP_BULLET_SPAWN -> {
                        BulletSpawnPacket bsp = new BulletSpawnPacket();
                        bsp.read(tcpIn);
                        packet = bsp;
                    }
                }
                
                if (packet != null) {
                    if (packetListener != null) {
                        packetListener.accept(packet);
                    } else {
                        packetBuffer.add(packet);
                    }
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
                } else if (opCode == OpCode.UDP_BULLET_SPAWN) {
                    udpPacket = new BulletSpawnPacket();
                }

                if (udpPacket != null) {
                    udpPacket.read(dis);
                    if (packetListener != null) {
                        packetListener.accept(udpPacket);
                    } else {
                        packetBuffer.add(udpPacket);
                    }
                }
            } catch (IOException e) {
                // UDP failure shouldn't disconnect TCP client
                if (running) {
                    System.out.println("[Client] UDP error (continuing via TCP): " + e.getMessage());
                }
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
        sendTCP(packet); // Always send TCP for absolute reliability

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
        threadsStarted = false;
        packetListener = null;
        packetBuffer.clear();
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public int getPlayerId() { return playerId; }
}
