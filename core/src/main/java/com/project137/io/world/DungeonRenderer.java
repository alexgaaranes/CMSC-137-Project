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
                    shapeRenderer.rect(x * 16 + 1, y * 16 + 1, 14, 14); // Larger crate
                }
            }
        }
        shapeRenderer.end();
    }

    public void renderMinimap(DungeonMap map, float localX, float localY, List<Vector2> otherPlayers) {
        if (map == null) return;

        int px = (int)(localX / 16);
        int py = (int)(localY / 16);
        
        // Update explored
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                int nx = px + dx;
                int ny = py + dy;
                if (nx >= 0 && nx < map.width && ny >= 0 && ny < map.height) {
                    map.explored[nx][ny] = true;
                }
            }
        }

        float tileSize = 4f;
        int viewRadius = 15; // Number of tiles visible in minimap
        float minimapSize = viewRadius * 2 * tileSize;
        float offsetX = 10, offsetY = 480 - minimapSize - 10; // Top Left (relative to screen)
        // Wait, LibGDX screen Y is bottom-up. Top-left is (10, 480 - 10 - size).

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Background
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(offsetX, offsetY, minimapSize, minimapSize);

        for (int dx = -viewRadius; dx <= viewRadius; dx++) {
            for (int dy = -viewRadius; dy <= viewRadius; dy++) {
                int tx = px + dx;
                int ty = py + dy;
                
                if (tx >= 0 && tx < map.width && ty >= 0 && ty < map.height && map.explored[tx][ty]) {
                    int tile = map.tiles[tx][ty];
                    if (tile == DungeonMap.TILE_WALL || tile == DungeonMap.TILE_GATE) shapeRenderer.setColor(Color.WHITE);
                    else if (tile == DungeonMap.TILE_FLOOR) shapeRenderer.setColor(Color.BLUE);
                    else continue;
                    
                    float rx = offsetX + (dx + viewRadius) * tileSize;
                    float ry = offsetY + (dy + viewRadius) * tileSize;
                    shapeRenderer.rect(rx, ry, tileSize, tileSize);
                }
            }
        }
        
        // Local Player (Yellow)
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(offsetX + viewRadius * tileSize, offsetY + viewRadius * tileSize, 2f);

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
    }
}
