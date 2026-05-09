package com.project137.io.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonGenerator {
    public enum RoomType { SPAWN, ENEMY, SHOP, EXIT }
    
    public static class Room {
        public int x, y, w, h;
        public RoomType type = RoomType.ENEMY;
        public boolean cleared = false;
        public boolean locked = false;
        
        public Room(int x, int y, int w, int h) { 
            this.x = x; this.y = y; this.w = w; this.h = h; 
        }
        public int centerX() { return x + w / 2; }
        public int centerY() { return y + h / 2; }
        
        public boolean isInside(float tx, float ty) {
            return tx >= x && tx < x + w && ty >= y && ty < y + h;
        }
    }

    private final Random random = new Random();
    public final List<Room> rooms = new ArrayList<>();

    public DungeonMap generate(int width, int height) {
        DungeonMap map = new DungeonMap(width, height);
        rooms.clear();

        int gridSize = 4;
        int cellW = width / gridSize;
        int cellH = height / gridSize;
        int roomSize = 32; // Requested 32x32

        // 1. Grid Walk
        Room[][] grid = new Room[gridSize][gridSize];
        int targetRooms = 6 + random.nextInt(4);
        int curX = 0, curY = 0;
        
        while (rooms.size() < targetRooms) {
            if (grid[curX][curY] == null) {
                // Center room in cell
                int rx = curX * cellW + (cellW - roomSize) / 2;
                int ry = curY * cellH + (cellH - roomSize) / 2;
                Room room = new Room(rx, ry, roomSize, roomSize);
                
                if (rooms.isEmpty()) room.type = RoomType.SPAWN;
                else if (rooms.size() == targetRooms - 1) room.type = RoomType.EXIT;
                
                grid[curX][curY] = room;
                rooms.add(room);
                fillRoom(map, room);
            }
            int dir = random.nextInt(4);
            if (dir == 0 && curX < gridSize - 1) curX++;
            else if (dir == 1 && curX > 0) curX--;
            else if (dir == 2 && curY < gridSize - 1) curY++;
            else if (dir == 3 && curY > 0) curY--;
        }

        // 2. Multi-Connections
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (grid[i][j] == null) continue;
                if (i + 1 < gridSize && grid[i+1][j] != null) connectRooms(map, grid[i][j], grid[i+1][j]);
                if (j + 1 < gridSize && grid[i][j+1] != null) connectRooms(map, grid[i][j], grid[i][j+1]);
            }
        }

        // Ensure SHOP
        if (rooms.size() > 3) {
            Room shop = rooms.get(1 + random.nextInt(rooms.size() - 2));
            if (shop.type == RoomType.ENEMY) shop.type = RoomType.SHOP;
        }

        // 3. Obstacles
        for (Room r : rooms) {
            if (r.type == RoomType.ENEMY) applyPatternedObstacles(map, r);
        }

        addWalls(map);
        return map;
    }

    private void fillRoom(DungeonMap map, Room room) {
        for (int x = room.x; x < room.x + room.w; x++) {
            for (int y = room.y; y < room.y + room.h; y++) {
                if (x >= 0 && x < map.width && y >= 0 && y < map.height) {
                    map.tiles[x][y] = DungeonMap.TILE_FLOOR;
                }
            }
        }
    }

    private void applyPatternedObstacles(DungeonMap map, Room r) {
        int pattern = random.nextInt(3);
        switch (pattern) {
            case 0: // Symmetrical Pillars
                for (int x = r.x + 6; x < r.x + r.w - 6; x += 8) {
                    for (int y = r.y + 6; y < r.y + r.h - 6; y += 8) {
                        for (int dx = 0; dx < 2; dx++) {
                            for (int dy = 0; dy < 2; dy++) {
                                map.tiles[x+dx][y+dy] = (dx+dy == 0) ? DungeonMap.TILE_WALL : DungeonMap.TILE_CRATE;
                            }
                        }
                    }
                }
                break;
            case 1: // Arena Cross
                int cx = r.centerX();
                int cy = r.centerY();
                for (int i = -10; i <= 10; i++) {
                    if (Math.abs(i) < 4) continue;
                    map.tiles[cx + i][cy] = DungeonMap.TILE_CRATE;
                    map.tiles[cx][cy + i] = DungeonMap.TILE_CRATE;
                    map.tiles[cx + i][cy+1] = DungeonMap.TILE_WALL;
                    map.tiles[cx+1][cy+i] = DungeonMap.TILE_WALL;
                }
                break;
            case 2: // Random Clusters
                for (int k = 0; k < 10; k++) {
                    int sx = r.x + 5 + random.nextInt(r.w - 10);
                    int sy = r.y + 5 + random.nextInt(r.h - 10);
                    for (int dx = 0; dx < 3; dx++) {
                        for (int dy = 0; dy < 3; dy++) {
                            if (random.nextFloat() > 0.4f) map.tiles[sx+dx][sy+dy] = DungeonMap.TILE_CRATE;
                        }
                    }
                }
                break;
        }
    }

    private void connectRooms(DungeonMap map, Room r1, Room r2) {
        int x = r1.centerX();
        int y = r1.centerY();
        while (x != r2.centerX()) {
            carve(map, x, y);
            x += (x < r2.centerX()) ? 1 : -1;
        }
        while (y != r2.centerY()) {
            carve(map, x, y);
            y += (y < r2.centerY()) ? 1 : -1;
        }
    }

    private void carve(DungeonMap map, int x, int y) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < map.width && ny >= 0 && ny < map.height) {
                    map.tiles[nx][ny] = DungeonMap.TILE_FLOOR;
                }
            }
        }
    }

    private void addWalls(DungeonMap map) {
        for (int x = 0; x < map.width; x++) {
            for (int y = 0; y < map.height; y++) {
                if (map.tiles[x][y] == DungeonMap.TILE_FLOOR) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < map.width && ny >= 0 && ny < map.height) {
                                if (map.tiles[nx][ny] == DungeonMap.TILE_EMPTY) {
                                    map.tiles[nx][ny] = DungeonMap.TILE_WALL;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
