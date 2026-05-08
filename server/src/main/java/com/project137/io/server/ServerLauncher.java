package com.project137.io.server;

import com.project137.io.NetworkConfig;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.project137.io.network.server.ServerNetworkManager;

/** Launches the server application. */
public class ServerLauncher {
    public static void main(String[] args) {
        ServerNetworkManager networkManager = new ServerNetworkManager();
        networkManager.start();

        // Keep main thread alive
        Runtime.getRuntime().addShutdownHook(new Thread(networkManager::stop));
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}