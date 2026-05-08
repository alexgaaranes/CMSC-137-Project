package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.project137.io.Main;
import com.project137.io.network.InputPacket;
import com.project137.io.network.MapDataPacket;
import com.project137.io.network.PlayerUpdatePacket;
import com.project137.io.world.DungeonMap;
import com.project137.io.world.DungeonRenderer;
import java.util.concurrent.ConcurrentHashMap;

public class GameScreen extends ScreenAdapter {
    private final Main game;
    private final ShapeRenderer shapeRenderer;
    private final ExtendViewport viewport;
    private final DungeonRenderer dungeonRenderer;
    private DungeonMap map;
    
    private final ConcurrentHashMap<Integer, PlayerState> players = new ConcurrentHashMap<>();
    
    private float localX = 320, localY = 240;
    private final float MOVE_SPEED = 160f; 

    private static class PlayerState {
        float x, y, targetX, targetY;
        long lastSequence = -1;

        public PlayerState(float x, float y) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
        }
    }

    public GameScreen(Main game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
        this.viewport = new ExtendViewport(640, 480);
        this.dungeonRenderer = new DungeonRenderer(shapeRenderer);
    }

    @Override
    public void show() {
        game.networkManager.setPacketListener(packet -> {
            if (packet instanceof MapDataPacket mapPacket) {
                this.map = new DungeonMap(mapPacket.width, mapPacket.height);
                this.map.tiles = mapPacket.tiles;
                System.out.println("[Client] Map Received: " + mapPacket.width + "x" + mapPacket.height);
            } else if (packet instanceof PlayerUpdatePacket update) {
                PlayerState ps = players.get(update.playerId);
                if (ps == null) {
                    ps = new PlayerState(update.x, update.y);
                    players.put(update.playerId, ps);
                }

                if (update.sequence > ps.lastSequence) {
                    ps.targetX = update.x;
                    ps.targetY = update.y;
                    ps.lastSequence = update.sequence;
                    
                    if (update.playerId == game.networkManager.getPlayerId()) {
                        // CLIENT LAG-BACK: If client is too far from server, snap it back.
                        float dist = Vector2.dst(localX, localY, ps.targetX, ps.targetY);
                        if (dist > 15) { // 15px threshold for lag-back
                            localX = ps.targetX;
                            localY = ps.targetY;
                        } else if (dist > 1) {
                            // Soft correction for minor drifts
                            localX = MathUtils.lerp(localX, ps.targetX, 0.5f);
                            localY = MathUtils.lerp(localY, ps.targetY, 0.5f);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void render(float delta) {
        handleLocalMovement(delta);
        updateRemoteInterpolation(delta);
        sendInput();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        
        // Render Dungeon First
        if (map != null) {
            dungeonRenderer.render(map);
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Integer id : players.keySet()) {
            PlayerState ps = players.get(id);
            if (id == game.networkManager.getPlayerId()) {
                shapeRenderer.setColor(Color.BLUE); // Local
                shapeRenderer.circle(localX, localY, 15);
            } else {
                shapeRenderer.setColor(Color.RED); // Remote
                shapeRenderer.circle(ps.x, ps.y, 15);
            }
        }
        shapeRenderer.end();
        
        // UI layer (Minimap) - uses screen projection
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, 640, 480));
        dungeonRenderer.renderMinimap(map, localX, localY);
    }

    private void handleLocalMovement(float delta) {
        Vector2 velocity = new Vector2(0, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.W)) velocity.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) velocity.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) velocity.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) velocity.x += 1;

        if (!velocity.isZero()) {
            velocity.nor().scl(MOVE_SPEED * delta);
            localX += velocity.x;
            localY += velocity.y;
        }
    }

    private void updateRemoteInterpolation(float delta) {
        float lerpFactor = 15f * delta; 
        for (Integer id : players.keySet()) {
            if (id == game.networkManager.getPlayerId()) continue;
            PlayerState ps = players.get(id);
            ps.x = MathUtils.lerp(ps.x, ps.targetX, lerpFactor);
            ps.y = MathUtils.lerp(ps.y, ps.targetY, lerpFactor);
        }
    }

    private void sendInput() {
        boolean up = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);

        game.networkManager.sendUDP(new InputPacket(
            game.networkManager.getPlayerId(),
            up, down, left, right, 0
        ));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
