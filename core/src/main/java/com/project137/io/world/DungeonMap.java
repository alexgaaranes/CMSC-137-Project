package com.project137.io.world;

import java.io.Serializable;

public class DungeonMap implements Serializable {
    public static final int TILE_EMPTY = 0;
    public static final int TILE_FLOOR = 1;
    public static final int TILE_WALL = 2;
    public static final int TILE_DOOR = 3;
    public static final int TILE_GATE = 4;
    public static final int TILE_CRATE = 5;

    public int width, height;
    public int[][] tiles;
    public boolean[][] explored;

    public DungeonMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[width][height];
        this.explored = new boolean[width][height];
    }
}
