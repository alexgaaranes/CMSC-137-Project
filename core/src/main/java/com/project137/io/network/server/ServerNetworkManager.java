package com.project137.io.network.server;

import com.project137.io.NetworkConfig;
import com.project137.io.ecs.ServerEngine;
import com.project137.io.network.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerNetworkManager {
    private final ConcurrentHashMap<Integer, ConnectedClient> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    private final ServerEngine gameEngine = new ServerEngine(this);
    private DatagramSocket udpSocket;
    private ServerSocket serverSocket;
    private boolean running = false;
    private long lastTickTime = System.currentTimeMillis();

    public void start() {
        running = true;
        try {
            serverSocket = new ServerSocket(NetworkConfig.PORT);
            udpSocket = new DatagramSocket(NetworkConfig.PORT);

            Thread.ofVirtual().name("TCP-Accept").start(this::acceptTCPConnections);
            Thread.ofVirtual().name("UDP-Receive").start(this::receiveUDPPackets);
            Thread.ofVirtual().name("Game-Loop").start(this::gameLoop);

            System.out.println("[Server] Started on port " + NetworkConfig.PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void gameLoop() {
        while (running) {
            long now = System.currentTimeMillis();
            float delta = (now - lastTickTime) / 1000f;
            lastTickTime = now;

            gameEngine.update(delta);

            try {
                Thread.sleep(16); 
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void acceptTCPConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                int id = nextPlayerId.getAndIncrement();
                ConnectedClient client = new ConnectedClient(id, socket);
                clients.put(id, client);

                System.out.println("[Server] Client " + id + " connected.");

                gameEngine.addPlayer(id);
                sendTCP(client, new WelcomePacket(id));
                
                // Notify all clients about lobby state
                broadcastLobbyUpdate();

                Thread.ofVirtual().name("TCP-Client-" + id).start(() -> handleTCPClient(client));

            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    private void handleTCPClient(ConnectedClient client) {
        try {
            while (running) {
                byte opCode = client.in.readByte();
                if (opCode == OpCode.TCP_START_GAME) {
                    // Only Host (Client ID 1) can start the game
                    if (client.id == 1) {
                        broadcastTCP(new MapDataPacket(gameEngine.getMap()));
                        broadcastTCP(new StartGamePacket());
                    }
                } else if (opCode == OpCode.TCP_DISCONNECT) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] Client " + client.id + " disconnected.");
        } finally {
            gameEngine.removePlayer(client.id);
            clients.remove(client.id);
            broadcastLobbyUpdate();
            client.close();
        }
    }

    private void broadcastLobbyUpdate() {
        LobbyInfoPacket hostInfo = new LobbyInfoPacket(clients.size(), true);
        LobbyInfoPacket guestInfo = new LobbyInfoPacket(clients.size(), false);
        
        for (ConnectedClient c : clients.values()) {
            sendTCP(c, c.id == 1 ? hostInfo : guestInfo);
        }
    }

    private void receiveUDPPackets() {
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                DataInputStream dis = new DataInputStream(bais);
                byte opCode = dis.readByte();

                if (opCode == OpCode.UDP_HELLO) {
                    HelloPacket hello = new HelloPacket();
                    hello.read(dis);
                    ConnectedClient client = clients.get(hello.playerId);
                    if (client != null) {
                        client.udpAddress = packet.getAddress();
                        client.udpPort = packet.getPort();
                    }
                } else if (opCode == OpCode.UDP_INPUT) {
                    InputPacket input = new InputPacket();
                    input.read(dis);
                    gameEngine.handleInput(input.playerId, input.up, input.down, input.left, input.right);
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    public void sendTCP(ConnectedClient client, Packet packet) {
        try {
            synchronized (client.out) {
                client.out.writeByte(packet.getOpCode());
                packet.write(client.out);
                client.out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastTCP(Packet packet) {
        for (ConnectedClient c : clients.values()) {
            sendTCP(c, packet);
        }
    }

    public void broadcastUDP(Packet packet) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(packet.getOpCode());
            packet.write(dos);
            byte[] data = baos.toByteArray();
            
            for (ConnectedClient client : clients.values()) {
                if (client.udpAddress != null) {
                    DatagramPacket datagramPacket = new DatagramPacket(data, data.length, client.udpAddress, client.udpPort);
                    udpSocket.send(datagramPacket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
