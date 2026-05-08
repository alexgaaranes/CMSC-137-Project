package com.project137.io.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

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
                }
            }
        }
        shapeRenderer.end();
    }

    public void renderMinimap(DungeonMap map, float localX, float localY) {
        if (map == null) return;

        int px = (int)(localX / 16);
        int py = (int)(localY / 16);
        
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                int nx = px + dx;
                int ny = py + dy;
                if (nx >= 0 && nx < map.width && ny >= 0 && ny < map.height) {
                    map.explored[nx][ny] = true;
                }
            }
        }

        float size = 3f;
        float offset = 10;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < map.width; x++) {
            for (int y = 0; y < map.height; y++) {
                if (!map.explored[x][y]) continue;
                
                int tile = map.tiles[x][y];
                if (tile == DungeonMap.TILE_WALL) shapeRenderer.setColor(Color.WHITE);
                else if (tile == DungeonMap.TILE_FLOOR) shapeRenderer.setColor(Color.BLUE);
                else continue;
                
                shapeRenderer.rect(640 - map.width * size - offset + x * size, 480 - map.height * size - offset + y * size, size, size);
            }
        }
        
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(640 - map.width * size - offset + px * size, 480 - map.height * size - offset + py * size, size);
        shapeRenderer.end();
    }
}
