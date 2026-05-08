package com.project137.io.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonGenerator {
    private final Random random = new Random();
    public final List<Room> rooms = new ArrayList<>();

    public DungeonMap generate(int width, int height) {
        DungeonMap map = new DungeonMap(width, height);
        rooms.clear();
        
        // Increase number of rooms and spacing
        for (int i = 0; i < 10; i++) {
            int rw = 8 + random.nextInt(6);
            int rh = 8 + random.nextInt(6);
            int rx = random.nextInt(width - rw - 1) + 1;
            int ry = random.nextInt(height - rh - 1) + 1;
            
            Room room = new Room(rx, ry, rw, rh);
            if (canPlaceRoom(rooms, room)) {
                rooms.add(room);
                fillRoom(map, room);
            }
        }
        
        // Connect rooms with WIDER corridors (3 tiles wide)
        for (int i = 0; i < rooms.size() - 1; i++) {
            connectRooms(map, rooms.get(i), rooms.get(i + 1));
        }
        
        addWalls(map);
        return map;
    }

    public static class Room {
        public int x, y, w, h;
        public Room(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        public int centerX() { return x + w / 2; }
        public int centerY() { return y + h / 2; }
    }

    private boolean canPlaceRoom(List<Room> rooms, Room newRoom) {
        // Increase padding between rooms (at least 6 tiles)
        for (Room r : rooms) {
            if (newRoom.x < r.x + r.w + 6 && newRoom.x + newRoom.w + 6 > r.x &&
                newRoom.y < r.y + r.h + 6 && newRoom.y + newRoom.h + 6 > r.y) return false;
        }
        return true;
    }

    private void fillRoom(DungeonMap map, Room room) {
        for (int x = room.x; x < room.x + room.w; x++) {
            for (int y = room.y; y < room.y + room.h; y++) {
                map.tiles[x][y] = DungeonMap.TILE_FLOOR;
            }
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
        // Carve 3x3 area around center point for wide corridors
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
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
