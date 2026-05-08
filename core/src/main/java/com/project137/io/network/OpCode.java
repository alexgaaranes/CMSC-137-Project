package com.project137.io.network;

public class OpCode {
    // Reliable (TCP) Codes
    public static final byte TCP_WELCOME = 1;
    public static final byte TCP_JOIN_LOBBY = 2;
    public static final byte TCP_START_GAME = 3;
    public static final byte TCP_MAP_DATA = 4;
    public static final byte TCP_PICKUP_ITEM = 5;
    public static final byte TCP_DISCONNECT = 99;

    // Unreliable (UDP) Codes
    public static final byte UDP_HELLO = 10;
    public static final byte UDP_INPUT = 15;
    public static final byte UDP_PLAYER_UPDATE = 11;
    public static final byte UDP_ENEMY_UPDATE = 12;
    public static final byte UDP_BULLET_SPAWN = 13;
    public static final byte UDP_HEALTH_UPDATE = 14;
}
