package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project137.io.Main;
import com.project137.io.network.*;
import com.project137.io.world.DungeonMap;
import com.project137.io.world.DungeonRenderer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameScreen extends ScreenAdapter {
    private final Main game;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;
    private final BitmapFont font;
    private final ExtendViewport viewport;
    private final DungeonRenderer dungeonRenderer;
    private DungeonMap map;
    
    private Stage hudStage;
    private Skin skin;
    private Label healthLabel;
    private Label energyLabel;
    private Label weaponLabel;
    private float currentHP = 100, currentEnergy = 200;
    
    private final ConcurrentHashMap<Integer, EntityState> entities = new ConcurrentHashMap<>();
    private final Set<Integer> removedIds = new HashSet<>();
    
    private float localX = 0, localY = 0;
    private float aimAngle = 0;
    private final float MOVE_SPEED = 200f; 
    private boolean debugEnabled = false;

    private static class EntityState {
        float x, y, angle, targetX, targetY;
        float hp = -1; // -1 means unknown
        float reviveProgress = 0;
        long lastSequence = -1;
        String templateId;

        public EntityState(float x, float y) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
        }
    }

    public GameScreen(Main game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(0.8f);
        this.viewport = new ExtendViewport(640, 480);
        this.dungeonRenderer = new DungeonRenderer(shapeRenderer);
    }

    public void setInitialMap(DungeonMap map) {
        this.map = map;
    }

    @Override
    public void show() {
        hudStage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        Table table = new Table();
        table.top().left();
        table.setFillParent(true);
        healthLabel = new Label("HP: 100/100", skin);
        energyLabel = new Label("Energy: 200/200", skin);
        weaponLabel = new Label("Weapon: Starter Pistol", skin);
        
        table.add(healthLabel).pad(10).left().row();
        table.add(energyLabel).pad(10).left().row();
        table.add(weaponLabel).pad(10).left().expandX();
        hudStage.addActor(table);

        game.networkManager.setOnDisconnect(() -> Gdx.app.postRunnable(() -> game.setScreen(new MenuScreen(game))));

        game.networkManager.setPacketListener(packet -> {
            if (packet instanceof MapDataPacket mapPacket) {
                this.map = new DungeonMap(mapPacket.width, mapPacket.height);
                this.map.tiles = mapPacket.tiles;
            } else if (packet instanceof EntityRemovePacket remove) {
                entities.remove(remove.entityId);
                if (remove.entityId >= 1000) {
                    synchronized (removedIds) { removedIds.add(remove.entityId); }
                }
            } else if (packet instanceof GameOverPacket gameOver) {
                Gdx.app.postRunnable(() -> game.setScreen(new EndGameScreen(game, "GAME OVER!\nAll players have died.")));
            } else if (packet instanceof TeleportPacket tp) {
                if (tp.playerId == game.networkManager.getPlayerId()) {
                    localX = tp.x;
                    localY = tp.y;
                }
            } else if (packet instanceof ResourceUpdatePacket res) {
                // Update local player stats
                if (res.playerId == game.networkManager.getPlayerId()) {
                    currentHP = res.hp;
                    currentEnergy = res.energy;
                    Gdx.app.postRunnable(() -> {
                        healthLabel.setText("HP: " + (int)currentHP + "/100");
                        energyLabel.setText("Energy: " + (int)currentEnergy + "/200");
                    });
                }
                
                // Track HP for all entities for debug HUD
                EntityState es = entities.get(res.playerId);
                if (es != null) {
                    es.hp = res.hp;
                    es.reviveProgress = res.reviveProgress;
                }
            } else if (packet instanceof WeaponChangePacket wchange) {
                if (wchange.playerId == game.networkManager.getPlayerId()) {
                    String active = wchange.activeSlot == 0 ? wchange.slot0 : wchange.slot1;
                    Gdx.app.postRunnable(() -> weaponLabel.setText("Weapon: " + active));
                }
            } else if (packet instanceof ItemSpawnPacket item) {
                EntityState es = new EntityState(item.x, item.y);
                es.templateId = item.templateId;
                entities.put(item.entityId, es);
            } else if (packet instanceof TileUpdatePacket tileUpdate) {
                if (this.map != null) this.map.tiles[tileUpdate.x][tileUpdate.y] = tileUpdate.tile;
            } else if (packet instanceof PlayerUpdatePacket update) {
                synchronized (removedIds) {
                    if (removedIds.contains(update.playerId)) return;
                }
                
                EntityState es = entities.get(update.playerId);
                if (es == null) {
                    es = new EntityState(update.x, update.y);
                    entities.put(update.playerId, es);
                }

                if (update.sequence > es.lastSequence) {
                    es.targetX = update.x;
                    es.targetY = update.y;
                    es.angle = update.angle;
                    es.lastSequence = update.sequence;
                    
                    if (update.playerId == game.networkManager.getPlayerId()) {
                        float dist = Vector2.dst(localX, localY, es.targetX, es.targetY);
                        if (dist > 60) {
                            localX = es.targetX;
                            localY = es.targetY;
                        } else if (dist > 2) {
                            localX = MathUtils.lerp(localX, es.targetX, 0.4f);
                            localY = MathUtils.lerp(localY, es.targetY, 0.4f);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) debugEnabled = !debugEnabled;

        handleLocalMovement(delta);
        updateInterpolation(delta);
        updateAimAngle();
        sendInput();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.getCamera().position.set(localX, localY, 0);
        viewport.getCamera().update();

        viewport.apply();
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        
        if (map != null) dungeonRenderer.render(map);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        shapeRenderer.setColor(Color.WHITE);
        float indicatorX = localX + MathUtils.cosDeg(aimAngle) * 35;
        float indicatorY = localY + MathUtils.sinDeg(aimAngle) * 35;
        shapeRenderer.rectLine(localX, localY, indicatorX, indicatorY, 2);

        for (Integer id : entities.keySet()) {
            EntityState es = entities.get(id);
            
            // Set color based on entity type and state
            if (id == game.networkManager.getPlayerId()) {
                shapeRenderer.setColor(currentHP <= 0 ? Color.GRAY : Color.BLUE);
                shapeRenderer.circle(localX, localY, 15);
            } else if (id < 10) { // Other players
                shapeRenderer.setColor(es.hp == 0 ? Color.GRAY : Color.RED);
                shapeRenderer.circle(es.x, es.y, 15);
            } else if (id < 2000) { // Enemies
                shapeRenderer.setColor(Color.PURPLE);
                shapeRenderer.circle(es.x, es.y, 14);
            } else if (id < 3000) { // Projectiles
                shapeRenderer.setColor(Color.ORANGE);
                shapeRenderer.circle(es.x, es.y, 5);
            } else if (id < 4000) { // Crates
                shapeRenderer.setColor(Color.BROWN);
                shapeRenderer.rect(es.x - 7, es.y - 7, 14, 14);
            } else if (id >= 5000) { // Items
                if ("health_pack".equals(es.templateId)) {
                    shapeRenderer.setColor(Color.LIME);
                } else {
                    shapeRenderer.setColor(Color.GOLD);
                }
                shapeRenderer.rect(es.x - 6, es.y - 6, 12, 12);
            }

            // Render Revive Progress Bar
            if (es.reviveProgress > 0) {
                shapeRenderer.setColor(Color.GRAY);
                shapeRenderer.rect(es.x - 20, es.y + 20, 40, 5);
                shapeRenderer.setColor(Color.GREEN);
                shapeRenderer.rect(es.x - 20, es.y + 20, (es.reviveProgress / 5.0f) * 40, 5);
            }
        }
        shapeRenderer.end();

        if (currentHP <= 0) {
            spriteBatch.setProjectionMatrix(hudStage.getCamera().combined);
            spriteBatch.begin();
            font.getData().setScale(2.0f);
            font.setColor(Color.RED);
            String msg = "YOU ARE DOWNED\nWait for a teammate to revive you (E)";
            font.draw(spriteBatch, msg, Gdx.graphics.getWidth()/2f - 200, Gdx.graphics.getHeight()/2f + 50);
            font.getData().setScale(0.8f); // Reset scale
            spriteBatch.end();
        }
        
        if (debugEnabled) {
            spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
            spriteBatch.begin();
            for (Integer id : entities.keySet()) {
                EntityState es = entities.get(id);
                String debugText = "ID:" + id;
                if (es.templateId != null) debugText += "\n" + es.templateId;
                if (es.hp >= 0) debugText += "\nHP:" + (int)es.hp;
                font.draw(spriteBatch, debugText, es.x + 15, es.y + 15);
            }
            spriteBatch.end();
        }
        
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        java.util.List<Vector2> otherPlayers = new java.util.ArrayList<>();
        for (Integer id : entities.keySet()) {
            if (id != game.networkManager.getPlayerId() && id < 10) {
                EntityState es = entities.get(id);
                otherPlayers.add(new Vector2(es.x, es.y));
            }
        }
        dungeonRenderer.renderMinimap(map, localX, localY, otherPlayers);
        
        hudStage.act(delta);
        hudStage.draw();
    }

    private void handleLocalMovement(float delta) {
        if (currentHP <= 0) return;
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

    private void updateInterpolation(float delta) {
        float lerpFactor = 15f * delta; 
        for (Integer id : entities.keySet()) {
            if (id == game.networkManager.getPlayerId()) continue;
            EntityState es = entities.get(id);
            es.x = MathUtils.lerp(es.x, es.targetX, lerpFactor);
            es.y = MathUtils.lerp(es.y, es.targetY, lerpFactor);
        }
    }

    private void updateAimAngle() {
        if (currentHP <= 0) return;
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
        aimAngle = MathUtils.atan2(mousePos.y - localY, mousePos.x - localX) * MathUtils.radiansToDegrees;
    }

    private void sendInput() {
        if (currentHP <= 0) {
            // Still send interaction input so they can potentially swap weapons while downed?
            // Actually, better to just allow interaction so they can be resurrected.
            // Wait, resurrection is handled by the ALIVE player interacting with the dead one.
            // So the dead player doesn't need to do anything.
            // But they might want to swap weapons? Let's just allow swap and interact.
            boolean swap = Gdx.input.isKeyJustPressed(Input.Keys.Q);
            boolean interact = Gdx.input.isKeyJustPressed(Input.Keys.E);
            if (swap || interact) {
                game.networkManager.sendUDP(new InputPacket(
                    game.networkManager.getPlayerId(),
                    false, false, false, false, false, swap, interact, aimAngle
                ));
            }
            return;
        }
        boolean up = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);
        boolean attack = Gdx.input.isTouched();
        boolean swap = Gdx.input.isKeyJustPressed(Input.Keys.Q);
        boolean interact = Gdx.input.isKeyJustPressed(Input.Keys.E);

        game.networkManager.sendUDP(new InputPacket(
            game.networkManager.getPlayerId(),
            up, down, left, right, attack, swap, interact, aimAngle
        ));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hudStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();
        hudStage.dispose();
        skin.dispose();
    }
}
