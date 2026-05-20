package com.project137.io.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

public class DungeonRenderer {
    private final ShapeRenderer shapeRenderer;

    public DungeonRenderer(ShapeRenderer shapeRenderer) {
        this.shapeRenderer = shapeRenderer;
    }

    public void render(DungeonMap map) {
        if (map == null) return;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < map.width; x++) {
            for (int y = 0; y < map.height; y++) {
                int tile = map.tiles[x][y];
                if (tile == DungeonMap.TILE_WALL) {
                    shapeRenderer.setColor(Color.DARK_GRAY);
                    shapeRenderer.rect(x * 16, y * 16, 16, 16);
                } else if (tile == DungeonMap.TILE_FLOOR) {
                    shapeRenderer.setColor(Color.LIGHT_GRAY);
                    shapeRenderer.rect(x * 16, y * 16, 16, 16);
                } else if (tile == DungeonMap.TILE_GATE) {
                    shapeRenderer.setColor(Color.MAROON);
                    shapeRenderer.rect(x * 16, y * 16, 16, 16);
                } else if (tile == DungeonMap.TILE_CRATE) {
                    shapeRenderer.setColor(Color.BROWN);
                    shapeRenderer.rect(x * 16 + 1, y * 16 + 1, 14, 14);
                }
            }
        }
        shapeRenderer.end();
    }

    public void renderMinimap(DungeonMap map, float localX, float localY, List<Vector2> otherPlayers, int clearedRooms, int totalRooms) {
        if (map == null) return;

        int px = (int)(localX / 16);
        int py = (int)(localY / 16);
        
        // Fog of War (slightly larger reveal radius)
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                int nx = px + dx;
                int ny = py + dy;
                if (nx >= 0 && nx < map.width && ny >= 0 && ny < map.height) {
                    map.explored[nx][ny] = true;
                }
            }
        }

        float tileSize = 2.5f; // Scaled down for more coverage
        int viewRadius = 30; // Doubled coverage (60x60 tiles view)
        float minimapSize = viewRadius * 2 * tileSize;
        float offsetX = 10, offsetY = 480 - minimapSize - 10;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Background
        shapeRenderer.setColor(0, 0, 0, 0.6f);
        shapeRenderer.rect(offsetX, offsetY, minimapSize, minimapSize);

        for (int dx = -viewRadius; dx <= viewRadius; dx++) {
            for (int dy = -viewRadius; dy <= viewRadius; dy++) {
                int tx = px + dx;
                int ty = py + dy;
                
                if (tx >= 0 && tx < map.width && ty >= 0 && ty < map.height && map.explored[tx][ty]) {
                    int tile = map.tiles[tx][ty];
                    if (tile == DungeonMap.TILE_WALL || tile == DungeonMap.TILE_GATE) shapeRenderer.setColor(Color.WHITE);
                    else if (tile == DungeonMap.TILE_FLOOR) shapeRenderer.setColor(0.2f, 0.4f, 0.8f, 1f); // Darker blue floor
                    else if (tile == DungeonMap.TILE_CRATE) shapeRenderer.setColor(Color.BROWN);
                    else continue;
                    
                    float rx = offsetX + (dx + viewRadius) * tileSize;
                    float ry = offsetY + (dy + viewRadius) * tileSize;
                    shapeRenderer.rect(rx, ry, tileSize, tileSize);
                }
            }
        }
        
        // Local Player (Yellow)
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(offsetX + viewRadius * tileSize, offsetY + viewRadius * tileSize, 2.5f);

        // Other Players (Red)
        shapeRenderer.setColor(Color.RED);
        for (Vector2 pos : otherPlayers) {
            int ox = (int)(pos.x / 16);
            int oy = (int)(pos.y / 16);
            int dx = ox - px;
            int dy = oy - py;
            if (Math.abs(dx) <= viewRadius && Math.abs(dy) <= viewRadius && map.explored[ox][oy]) {
                shapeRenderer.circle(offsetX + (dx + viewRadius) * tileSize, offsetY + (dy + viewRadius) * tileSize, 2f);
            }
        }
        
        shapeRenderer.end();

        // Draw the background border of the minimap
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(0.2f, 0.2f, 0.2f, 0.8f));
        shapeRenderer.rect(offsetX, offsetY, minimapSize, minimapSize);
        shapeRenderer.end();

        // Draw Clockwise Progress Bar around the minimap square border
        if (totalRooms > 0) {
            float ratio = Math.min(1.0f, (float)clearedRooms / totalRooms);
            float totalLen = minimapSize * 4;
            float fillLen = totalLen * ratio;
            
            Color progressColor = (ratio >= 1.0f) ? Color.YELLOW : Color.CYAN;
            
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(progressColor);
            
            float remaining = fillLen;
            
            // Segment 1: Middle-Top -> Top-Right
            float seg1Max = minimapSize / 2;
            float seg1Len = Math.min(remaining, seg1Max);
            if (seg1Len > 0) {
                shapeRenderer.rectLine(offsetX + minimapSize / 2, offsetY + minimapSize, offsetX + minimapSize / 2 + seg1Len, offsetY + minimapSize, 3);
                remaining -= seg1Len;
            }
            
            // Segment 2: Top-Right -> Bottom-Right
            float seg2Max = minimapSize;
            float seg2Len = Math.min(remaining, seg2Max);
            if (seg2Len > 0) {
                shapeRenderer.rectLine(offsetX + minimapSize, offsetY + minimapSize, offsetX + minimapSize, offsetY + minimapSize - seg2Len, 3);
                remaining -= seg2Len;
            }
            
            // Segment 3: Bottom-Right -> Bottom-Left
            float seg3Max = minimapSize;
            float seg3Len = Math.min(remaining, seg3Max);
            if (seg3Len > 0) {
                shapeRenderer.rectLine(offsetX + minimapSize, offsetY, offsetX + minimapSize - seg3Len, offsetY, 3);
                remaining -= seg3Len;
            }
            
            // Segment 4: Bottom-Left -> Top-Left
            float seg4Max = minimapSize;
            float seg4Len = Math.min(remaining, seg4Max);
            if (seg4Len > 0) {
                shapeRenderer.rectLine(offsetX, offsetY, offsetX, offsetY + seg4Len, 3);
                remaining -= seg4Len;
            }
            
            // Segment 5: Top-Left -> Middle-Top
            float seg5Max = minimapSize / 2;
            float seg5Len = Math.min(remaining, seg5Max);
            if (seg5Len > 0) {
                shapeRenderer.rectLine(offsetX, offsetY + minimapSize, offsetX + seg5Len, offsetY + minimapSize, 3);
            }
            
            shapeRenderer.end();
        }
    }
}
