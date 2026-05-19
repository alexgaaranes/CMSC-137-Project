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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project137.io.Main;
import com.project137.io.network.*;
import com.project137.io.world.DungeonMap;                      
import com.project137.io.world.DungeonRenderer;
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
    private Table downedTable;
    private Table voteTable;
    private Label resultLabel;
    private int clearedRooms = 0;
    private int totalRooms = 0;
    private float currentHP = 100, maxHP = 100;
    private float currentEnergy = 200, maxEnergy = 200;
    
    private final ConcurrentHashMap<Integer, EntityState> entities = new ConcurrentHashMap<>();
    private final Set<Integer> removedIds = new HashSet<>();
    
    private float localX = 0, localY = 0;
    private float aimAngle = 0;
    private final float MOVE_SPEED = 200f; 
    private boolean debugEnabled = false;

    // Character Animations
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> p1IdleAnim, p1RunAnim, p1DeathAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> p2IdleAnim, p2RunAnim, p2DeathAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> skelIdleAnim, skelRunAnim, skelAttackAnim, skelDeathAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> vampIdleAnim, vampRunAnim, vampAttackAnim, vampDeathAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> bossIdleAnim, bossHitAnim;

    // Sprite & Weapon Textures
    private com.badlogic.gdx.graphics.Texture smallBulletTex;
    private com.badlogic.gdx.graphics.Texture longBulletTex;
    private com.badlogic.gdx.graphics.Texture rocketTex;
    private com.badlogic.gdx.graphics.Texture revolverTex;
    private com.badlogic.gdx.graphics.Texture smgTex;
    private com.badlogic.gdx.graphics.Texture rocketLauncherTex;
    private com.badlogic.gdx.graphics.Texture healthOrbTex;

    private java.util.List<com.badlogic.gdx.graphics.Texture> loadedTextures = new java.util.ArrayList<>();
    private String localActiveWeaponId = "starter_pistol";

    private float localStateTime = 0;
    private boolean localIsMoving = false;

    private static class EntityState {
        float x, y, angle, targetX, targetY;
        float hp = -1; // -1 means unknown
        float maxHp = 100;
        float reviveProgress = 0;
        long lastSequence = -1;
        String templateId;
        float stateTime = 0;
        boolean isMoving = false;
        String activeWeaponId = "starter_pistol";

        public EntityState(float x, float y) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
        }

        public void update(PlayerUpdatePacket p) {
            if (p.sequence > lastSequence) {
                targetX = p.x;
                targetY = p.y;
                angle = p.angle;
                lastSequence = p.sequence;
            }
        }
    }

    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> createAnim(String path, int frameW, int frameH, float duration, com.badlogic.gdx.graphics.g2d.Animation.PlayMode mode) {
        try {
            com.badlogic.gdx.graphics.Texture tex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal(path));
            loadedTextures.add(tex);
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] tmp = com.badlogic.gdx.graphics.g2d.TextureRegion.split(tex, frameW, frameH);
            com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g2d.TextureRegion> frames = new com.badlogic.gdx.utils.Array<>();
            for (int i = 0; i < tmp.length; i++) {
                for (int j = 0; j < tmp[i].length; j++) {
                    frames.add(tmp[i][j]);
                }
            }
            return new com.badlogic.gdx.graphics.g2d.Animation<>(duration, frames, mode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

        try {
            smallBulletTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/projectile/small-bullet.png"));
            longBulletTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/projectile/long-bullet.png"));
            rocketTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/projectile/rocket.png"));
            revolverTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/weapons/revolver.png"));
            smgTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/weapons/smg.png"));
            rocketLauncherTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/weapons/rocketlauncher.png"));
            healthOrbTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/boss/boss_orb.png"));

            p1IdleAnim = createAnim("sprites/player1/Idle1.png.png", 16, 16, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            p1RunAnim = createAnim("sprites/player1/Run1.png.png", 16, 16, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            p1DeathAnim = createAnim("sprites/player1/Death1.png.png", 16, 16, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);

            p2IdleAnim = createAnim("sprites/player2/Idle2.png", 16, 16, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            p2RunAnim = createAnim("sprites/player2/Run2.png", 16, 16, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            p2DeathAnim = createAnim("sprites/player2/Death2.png", 16, 16, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);

            skelIdleAnim = createAnim("sprites/enemy1/skeleton_idle.png", 32, 32, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            skelRunAnim = createAnim("sprites/enemy1/skeleton_movement.png", 32, 32, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            skelAttackAnim = createAnim("sprites/enemy1/skeleton_attack.png", 32, 32, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            skelDeathAnim = createAnim("sprites/enemy1/skeleton_death.png", 32, 32, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);

            vampIdleAnim = createAnim("sprites/enemy2/vampire_idle.png", 32, 32, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            vampRunAnim = createAnim("sprites/enemy2/vampire_movement.png", 32, 32, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            vampAttackAnim = createAnim("sprites/enemy2/vampire_attack.png", 32, 32, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            vampDeathAnim = createAnim("sprites/enemy2/vampire_death.png", 32, 32, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);

            bossIdleAnim = createAnim("sprites/boss/boss_idle.png", 208, 340, 0.2f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            bossHitAnim = createAnim("sprites/boss/boss_hit.png", 208, 340, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawFlipped(com.badlogic.gdx.graphics.g2d.TextureRegion region, float x, float y, float width, float height, boolean flipX) {
        if (region == null) return;
        float scaleX = flipX ? -1f : 1f;
        spriteBatch.draw(region, x, y, width/2f, height/2f, width, height, scaleX, 1f, 0f);
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

        // Downed Table
        downedTable = new Table();
        downedTable.setFillParent(true);
        TextButton leaveButton = new TextButton("Leave Game", skin);
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.networkManager.disconnect();
                Gdx.app.postRunnable(() -> game.setScreen(new MenuScreen(game)));
            }
        });
        Label downedLabel = new Label("YOU ARE DOWNED", skin);
        downedLabel.setFontScale(1.5f);
        downedLabel.setColor(Color.RED);
        downedTable.add(downedLabel).pad(10).row();
        downedTable.add(new Label("Wait for a teammate to revive you (E)", skin)).pad(10).row();
        downedTable.add(leaveButton).pad(10).width(200);
        downedTable.setVisible(false);
        hudStage.addActor(downedTable);

        // Vote Table
        voteTable = new Table();
        voteTable.setFillParent(true);
        voteTable.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.8f)));
        voteTable.setVisible(false);
        hudStage.addActor(voteTable);

        // Result Label
        resultLabel = new Label("", skin);
        resultLabel.setFontScale(1.2f);
        resultLabel.setColor(Color.GOLD);
        resultLabel.setPosition(Gdx.graphics.getWidth()/2f - 100, Gdx.graphics.getHeight() - 100);
        resultLabel.setVisible(false);
        hudStage.addActor(resultLabel);

        // Room Progress Table is now visual-only around the minimap border!
        Gdx.input.setInputProcessor(hudStage);

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
                    es.update(update);
                    
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
            } else if (packet instanceof BuffPackets.BuffVoteStartPacket start) {
                Gdx.app.postRunnable(() -> showVoteDialog(start.options));
            } else if (packet instanceof BuffPackets.BuffVoteResultPacket result) {
                Gdx.app.postRunnable(() -> showVoteResult(result.winnerName));
            }
 else if (packet instanceof TeleportPacket tp) {
                if (tp.playerId == game.networkManager.getPlayerId()) {
                    localX = tp.x;
                    localY = tp.y;
                }
            } else if (packet instanceof ResourceUpdatePacket res) {
                // Update local player stats
                if (res.playerId == game.networkManager.getPlayerId()) {
                    currentHP = res.hp;
                    maxHP = res.maxHp;
                    currentEnergy = res.energy;
                    maxEnergy = res.maxEnergy;
                    Gdx.app.postRunnable(() -> {
                        healthLabel.setText("HP: " + (int)currentHP + "/" + (int)maxHP);
                        energyLabel.setText("Energy: " + (int)currentEnergy + "/" + (int)maxEnergy);
                    });
                }
                
                // Track HP for all entities for debug HUD
                EntityState es = entities.get(res.playerId);
                if (es != null) {
                    es.hp = res.hp;
                    es.maxHp = res.maxHp;
                    es.reviveProgress = res.reviveProgress;
                }
            } else if (packet instanceof WeaponChangePacket wchange) {
                try {
                    String active = wchange.activeSlot == 0 ? wchange.slot0 : wchange.slot1;
                    if (wchange.playerId == game.networkManager.getPlayerId()) {
                        localActiveWeaponId = active;
                        Gdx.app.postRunnable(() -> weaponLabel.setText("Weapon: " + active));
                    }
                    EntityState esWeapon = entities.get(wchange.playerId);
                    if (esWeapon != null) esWeapon.activeWeaponId = active;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (packet instanceof ItemSpawnPacket item) {
                EntityState es = new EntityState(item.x, item.y);
                es.templateId = item.templateId;
                entities.put(item.entityId, es);
            } else if (packet instanceof TileUpdatePacket tileUpdate) {
                if (this.map != null) this.map.tiles[tileUpdate.x][tileUpdate.y] = tileUpdate.tile;
            } else if (packet instanceof RoomProgressPacket prog) {
                this.clearedRooms = prog.clearedRooms;
                this.totalRooms = prog.totalRooms;
            }
        });
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) debugEnabled = !debugEnabled;

        handleLocalMovement(delta);
        localStateTime += delta;
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

        // Render Textured Sprites using SpriteBatch first
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();
        for (Integer id : entities.keySet()) {
            EntityState es = entities.get(id);
            if (id == game.networkManager.getPlayerId()) {
                boolean flipX = MathUtils.cosDeg(aimAngle) < 0;
                if (currentHP > 0) {
                    com.badlogic.gdx.graphics.g2d.TextureRegion frame = localIsMoving && p1RunAnim != null ? p1RunAnim.getKeyFrame(localStateTime) : (p1IdleAnim != null ? p1IdleAnim.getKeyFrame(localStateTime) : null);
                    drawFlipped(frame, localX - 16, localY - 16, 32, 32, flipX);
                    
                    // Render active weapon held by the player
                    com.badlogic.gdx.graphics.Texture weaponTex = revolverTex;
                    if ("fast_smg".equals(localActiveWeaponId)) weaponTex = smgTex;
                    else if ("heavy_cannon".equals(localActiveWeaponId)) weaponTex = rocketLauncherTex;
                    
                    float weaponRot = flipX ? aimAngle + 180 : aimAngle;
                    spriteBatch.draw(
                        new com.badlogic.gdx.graphics.g2d.TextureRegion(weaponTex),
                        localX + MathUtils.cosDeg(aimAngle) * 12 - 8,
                        localY + MathUtils.sinDeg(aimAngle) * 12 - 8,
                        8, 8,
                        16, 16,
                        1.0f, flipX ? -1.0f : 1.0f,
                        aimAngle
                    );
                } else {
                    spriteBatch.setColor(Color.GRAY);
                    drawFlipped(p1DeathAnim != null ? p1DeathAnim.getKeyFrame(localStateTime) : null, localX - 16, localY - 16, 32, 32, flipX);
                    spriteBatch.setColor(Color.WHITE);
                }
            } else if (id < 10) {
                boolean flipX = MathUtils.cosDeg(es.angle) < 0;
                if (es.hp > 0) {
                    com.badlogic.gdx.graphics.g2d.TextureRegion frame = es.isMoving && p2RunAnim != null ? p2RunAnim.getKeyFrame(es.stateTime) : (p2IdleAnim != null ? p2IdleAnim.getKeyFrame(es.stateTime) : null);
                    drawFlipped(frame, es.x - 16, es.y - 16, 32, 32, flipX);

                    com.badlogic.gdx.graphics.Texture weaponTex = revolverTex;
                    if ("fast_smg".equals(es.activeWeaponId)) weaponTex = smgTex;
                    else if ("heavy_cannon".equals(es.activeWeaponId)) weaponTex = rocketLauncherTex;
                    spriteBatch.draw(
                        new com.badlogic.gdx.graphics.g2d.TextureRegion(weaponTex),
                        es.x + MathUtils.cosDeg(es.angle) * 12 - 8,
                        es.y + MathUtils.sinDeg(es.angle) * 12 - 8,
                        8, 8,
                        16, 16,
                        1.0f, flipX ? -1.0f : 1.0f,
                        es.angle
                    );
                } else {
                    spriteBatch.setColor(Color.GRAY);
                    drawFlipped(p2DeathAnim != null ? p2DeathAnim.getKeyFrame(es.stateTime) : null, es.x - 16, es.y - 16, 32, 32, flipX);
                    spriteBatch.setColor(Color.WHITE);
                }
            } else if (id < 2000) {
                boolean flipX = MathUtils.cosDeg(es.angle) < 0;
                if (es.maxHp >= 300) {
                    if (es.hp > 0 && bossIdleAnim != null) {
                        drawFlipped(bossIdleAnim.getKeyFrame(es.stateTime), es.x - 52, es.y - 40, 104, 170, flipX);
                    } else if (es.hp <= 0 && bossHitAnim != null) {
                        drawFlipped(bossHitAnim.getKeyFrame(es.stateTime), es.x - 52, es.y - 40, 104, 170, flipX);
                    }
                } else if (es.maxHp <= 40) {
                    if (es.hp > 0) {
                        com.badlogic.gdx.graphics.g2d.TextureRegion frame;
                        if (es.isMoving) frame = skelRunAnim != null ? skelRunAnim.getKeyFrame(es.stateTime) : null;
                        else frame = skelAttackAnim != null ? skelAttackAnim.getKeyFrame(es.stateTime) : (skelIdleAnim != null ? skelIdleAnim.getKeyFrame(es.stateTime) : null);
                        drawFlipped(frame, es.x - 32, es.y - 32, 64, 64, flipX);
                    } else if (skelDeathAnim != null) {
                        drawFlipped(skelDeathAnim.getKeyFrame(es.stateTime), es.x - 32, es.y - 32, 64, 64, flipX);
                    }
                } else {
                    if (es.hp > 0) {
                        com.badlogic.gdx.graphics.g2d.TextureRegion frame;
                        if (es.isMoving) frame = vampRunAnim != null ? vampRunAnim.getKeyFrame(es.stateTime) : null;
                        else frame = vampAttackAnim != null ? vampAttackAnim.getKeyFrame(es.stateTime) : (vampIdleAnim != null ? vampIdleAnim.getKeyFrame(es.stateTime) : null);
                        drawFlipped(frame, es.x - 32, es.y - 32, 64, 64, flipX);
                    } else if (vampDeathAnim != null) {
                        drawFlipped(vampDeathAnim.getKeyFrame(es.stateTime), es.x - 32, es.y - 32, 64, 64, flipX);
                    }
                }
            } else if (id < 3000) {
                com.badlogic.gdx.graphics.Texture bulletTex = longBulletTex;
                float bulletAngle = es.angle * MathUtils.radiansToDegrees;
                if (Math.abs(es.targetX - es.x) > 0.1f || Math.abs(es.targetY - es.y) > 0.1f) {
                    bulletAngle = MathUtils.atan2(es.targetY - es.y, es.targetX - es.x) * MathUtils.radiansToDegrees;
                    es.angle = bulletAngle / MathUtils.radiansToDegrees; // cache the angle
                }
                spriteBatch.draw(
                    new com.badlogic.gdx.graphics.g2d.TextureRegion(bulletTex),
                    es.x - 8, es.y - 8,
                    8, 8,
                    16, 16,
                    1.0f, 1.0f,
                    bulletAngle
                );
            } else if (id >= 5000) {
                if ("health_orb".equals(es.templateId)) {
                    spriteBatch.draw(healthOrbTex, es.x - 8, es.y - 8, 16, 16);
                } else if (!"portal".equals(es.templateId)) {
                    com.badlogic.gdx.graphics.Texture itemTex = revolverTex;
                    if ("fast_smg".equals(es.templateId)) itemTex = smgTex;
                    else if ("heavy_cannon".equals(es.templateId)) itemTex = rocketLauncherTex;
                    
                    float floatOffset = 3f * (float)Math.sin(com.badlogic.gdx.utils.TimeUtils.nanoTime() / 150000000.0);
                    spriteBatch.draw(itemTex, es.x - 10, es.y - 10 + floatOffset, 20, 20);
                }
            }
        }
        spriteBatch.end();

        // Render Geometry & Overlays using ShapeRenderer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Integer id : entities.keySet()) {
            EntityState es = entities.get(id);
            
            if (id >= 3000 && id < 4000) {
                // Crates
                shapeRenderer.setColor(Color.BROWN);
                shapeRenderer.rect(es.x - 7, es.y - 7, 14, 14);
            } else if (id >= 5000 && "portal".equals(es.templateId)) {
                // The portal remains a glowing geometric shape
                float pulse = 16 + 8 * (float)Math.sin(com.badlogic.gdx.utils.TimeUtils.nanoTime() / 150000000.0);
                shapeRenderer.setColor(Color.GREEN);
                shapeRenderer.circle(es.x, es.y, pulse);
                shapeRenderer.setColor(Color.CYAN);
                shapeRenderer.circle(es.x, es.y, pulse * 0.6f);
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

        downedTable.setVisible(currentHP <= 0);
        
        if (debugEnabled) {
            spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
            spriteBatch.begin();
            
            // Draw player position and tile coordinates
            int tx = (int)(localX / 16);
            int ty = (int)(localY / 16);
            String posDebug = String.format("POS: %.1f, %.1f\nTILE: %d, %d", localX, localY, tx, ty);
            font.setColor(Color.YELLOW);
            font.draw(spriteBatch, posDebug, localX + 20, localY - 20);

            for (Integer id : entities.keySet()) {
                EntityState es = entities.get(id);
                String debugText = "ID:" + id;
                if (es.templateId != null) debugText += "\n" + es.templateId;
                if (es.hp >= 0) debugText += "\nHP:" + (int)es.hp;
                font.setColor(Color.WHITE);
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
        dungeonRenderer.renderMinimap(map, localX, localY, otherPlayers, clearedRooms, totalRooms);
        
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
            localIsMoving = true;
        } else {
            localIsMoving = false;
        }
    }

    private void updateInterpolation(float delta) {
        float lerpFactor = 15f * delta; 
        for (Integer id : entities.keySet()) {
            EntityState es = entities.get(id);
            es.stateTime += delta;
            if (id == game.networkManager.getPlayerId()) continue;
            
            float oldX = es.x;
            float oldY = es.y;
            es.x = MathUtils.lerp(es.x, es.targetX, lerpFactor);
            es.y = MathUtils.lerp(es.y, es.targetY, lerpFactor);
            
            if (Vector2.dst(oldX, oldY, es.x, es.y) > 0.05f) {
                es.isMoving = true;
            } else {
                es.isMoving = false;
            }
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

    private void showVoteDialog(String[] options) {
        voteTable.clear();
        Label title = new Label("CHOOSE A BUFF FOR THE NEXT LEVEL", skin);
        title.setFontScale(1.5f);
        title.setColor(Color.CYAN);
        voteTable.add(title).pad(20).row();

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            TextButton btn = new TextButton(options[i], skin);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.networkManager.sendTCP(new BuffPackets.BuffVoteSubmitPacket(index));
                    showWaitingScreen();
                }
            });
            voteTable.add(btn).width(300).pad(10).row();
        }
        voteTable.setVisible(true);
    }

    private void showWaitingScreen() {
        voteTable.clear();
        
        Table container = new Table();
        container.setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.85f)));
        container.pad(40);
        
        Label title = new Label("BUFF SELECTED!", skin);
        title.setFontScale(1.8f);
        title.setColor(Color.GOLD);
        container.add(title).padBottom(20).row();
        
        Label waitLabel = new Label("Waiting for other players to choose their buffs...", skin);
        waitLabel.setFontScale(1.2f);
        waitLabel.setColor(Color.LIGHT_GRAY);
        
        waitLabel.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.8f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.8f)
            )
        ));
        
        container.add(waitLabel).pad(10).row();
        voteTable.add(container).center();
        voteTable.setVisible(true);
    }

    private void showVoteResult(String winner) {
        voteTable.setVisible(false); // Hide the buff choosing/waiting panel
        resultLabel.setText("BUFF APPLIED: " + winner);
        resultLabel.setVisible(true);
        com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
            @Override
            public void run() {
                resultLabel.setVisible(false);
            }
        }, 5f);
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

        // Dispose textures
        for (com.badlogic.gdx.graphics.Texture tex : loadedTextures) {
            if (tex != null) tex.dispose();
        }
        if (smallBulletTex != null) smallBulletTex.dispose();
        if (longBulletTex != null) longBulletTex.dispose();
        if (rocketTex != null) rocketTex.dispose();
        if (revolverTex != null) revolverTex.dispose();
        if (smgTex != null) smgTex.dispose();
        if (rocketLauncherTex != null) rocketLauncherTex.dispose();
        if (healthOrbTex != null) healthOrbTex.dispose();
    }
}
