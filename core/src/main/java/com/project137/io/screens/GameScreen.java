package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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
import com.project137.io.ecs.data.DataManager;
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
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> portalIdleAnim, portalStartAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> explosionAnim;

    private java.util.List<Effect> effects = new java.util.ArrayList<>();

    private static class Effect {
        float x, y, stateTime = 0;
        com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim;
        String text;
        com.badlogic.gdx.graphics.Color color;
        boolean isDead = false;

        Effect(float x, float y, com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim) {
            this.x = x; this.y = y; this.anim = anim;
        }
        Effect(float x, float y, String text, com.badlogic.gdx.graphics.Color color) {
            this.x = x; this.y = y; this.text = text; this.color = color;
        }
        void update(float delta) {
            stateTime += delta;
            if (anim != null && anim.isAnimationFinished(stateTime)) isDead = true;
            if (text != null) {
                y += 30 * delta;
                if (stateTime > 0.8f) isDead = true;
            }
        }
    }

    // Sprite & Weapon Textures
    private com.badlogic.gdx.graphics.Texture smallBulletTex;
    private com.badlogic.gdx.graphics.Texture longBulletTex;
    private com.badlogic.gdx.graphics.Texture rocketTex;
    private com.badlogic.gdx.graphics.Texture revolverTex;
    private com.badlogic.gdx.graphics.Texture smgTex;
    private com.badlogic.gdx.graphics.Texture rocketLauncherTex;
    private com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> healthOrbAnim;
    private com.badlogic.gdx.graphics.Texture barBack, barHP, barEnergy, barHP_red;
    private com.badlogic.gdx.graphics.g2d.TextureRegion barrelRegion;

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
            
            // For the boss or any multi-row sheet, let's just take the first row to avoid flickering
            int rowsToUse = (path.contains("boss") || path.contains("health")) ? 1 : tmp.length;
            for (int i = 0; i < rowsToUse; i++) {
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
        
        // Initialize custom font
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/InkSlime.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 18;
        param.color = Color.WHITE;
        param.borderWidth = 1f;
        param.borderColor = Color.BLACK;
        this.font = gen.generateFont(param);
        gen.dispose();

        this.viewport = new ExtendViewport(640, 480);
        this.dungeonRenderer = new DungeonRenderer(shapeRenderer);

        try {
            smallBulletTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/projectile/small-bullet.png"));
            longBulletTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/projectile/long-bullet.png"));
            rocketTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/projectile/rocket.png"));
            revolverTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/weapons/revolver.png"));
            smgTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/weapons/smg.png"));
            rocketLauncherTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("sprites/weapons/rocketlauncher.png"));
            healthOrbAnim = createAnim("sprites/pickups/health.png", 65, 62, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            
            // Load wooden crate from tileset
            com.badlogic.gdx.graphics.Texture tileset = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("textures/tileset.png"));
            loadedTextures.add(tileset);
            com.badlogic.gdx.graphics.g2d.TextureRegion[][] tiles = com.badlogic.gdx.graphics.g2d.TextureRegion.split(tileset, 16, 16);
            barrelRegion = tiles[2][0]; // Wooden Box

            barBack = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("textures/hud/HPBar_back.png"));
            barHP = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("textures/hud/HPBar_green.png"));
            barHP_red = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("textures/hud/HPBar_red.png"));
            barEnergy = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("textures/hud/HPBar_yellow.png"));

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

            bossIdleAnim = createAnim("sprites/boss/boss_idle.png", 416, 340, 0.2f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            bossHitAnim = createAnim("sprites/boss/boss_hit.png", 416, 340, 0.15f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);

            portalIdleAnim = createAnim("sprites/portals/Portal1_Idle.png", 96, 96, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
            portalStartAnim = createAnim("sprites/portals/Portal1_Start.png", 96, 96, 0.1f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
            explosionAnim = createAnim("sprites/projectile/rocket-explosion.png", 48, 48, 0.08f, com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL);
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
        weaponLabel = new Label("Weapon: Starter Pistol", skin);
        
        table.add(weaponLabel).pad(10).padTop(60).left().expandX();
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
        resultLabel.setVisible(false);
        hudStage.addActor(resultLabel);

        // Room Progress Table is now visual-only around the minimap border!
        Gdx.input.setInputProcessor(hudStage);

        game.networkManager.setOnDisconnect(() -> Gdx.app.postRunnable(() -> game.setScreen(new MenuScreen(game))));
        
        // Register UDP with server
        game.networkManager.sendUDP(new HelloPacket(game.networkManager.getPlayerId()));

        game.networkManager.setPacketListener(packet -> {
            if (packet instanceof MapDataPacket mapPacket) {
                this.map = new DungeonMap(mapPacket.width, mapPacket.height);
                this.map.tiles = mapPacket.tiles;
            } else if (packet instanceof EntityRemovePacket remove) {
                EntityState removed = entities.remove(remove.entityId);
                if (removed != null && "rocket".equals(removed.templateId)) {
                    synchronized(effects) { effects.add(new Effect(removed.x, removed.y, explosionAnim)); }
                }
                if (remove.entityId >= 1000) {
                    synchronized (removedIds) { removedIds.add(remove.entityId); }
                }
            } else if (packet instanceof BulletSpawnPacket bullet) {
                EntityState es = new EntityState(bullet.x, bullet.y);
                es.templateId = bullet.type;
                entities.put(bullet.entityId, es);
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
                }
                
                // Track HP for all entities for debug HUD
                EntityState es = entities.get(res.playerId);
                if (es != null) {
                    if (es.hp >= 0 && res.hp < es.hp) {
                        float damage = es.hp - res.hp;
                        // Skip damage numbers for crates (Id range 3000-4000)
                        if (res.playerId < 3000 || res.playerId >= 4000) {
                            synchronized(effects) {
                                Color dmgColor = (res.playerId < 10) ? (res.playerId == game.networkManager.getPlayerId() ? Color.RED : Color.WHITE) : Color.YELLOW;
                                effects.add(new Effect(es.x, es.y + 10, "-" + (int)damage, dmgColor));
                            }
                        }
                    }
                    es.hp = res.hp;
                    es.maxHp = res.maxHp;
                    es.reviveProgress = res.reviveProgress;
                }
            } else if (packet instanceof WeaponChangePacket wchange) {
                try {
                    String active = wchange.activeSlot == 0 ? wchange.slot0 : wchange.slot1;
                    if (wchange.playerId == game.networkManager.getPlayerId()) {
                        localActiveWeaponId = active;
                        Gdx.app.postRunnable(() -> {
                            com.project137.io.ecs.data.WeaponTemplate wt = DataManager.getWeapon(active);
                            weaponLabel.setText("Weapon: " + (wt != null ? wt.name : "Unknown"));
                        });
                    }
                    EntityState es = entities.get(wchange.playerId);
                    if (es != null) es.activeWeaponId = active;
                } catch (Exception e) {}
            } else if (packet instanceof ItemSpawnPacket is) {
                EntityState es = new EntityState(is.x, is.y);
                es.templateId = is.templateId;
                entities.put(is.entityId, es);
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
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        
        if (map != null) dungeonRenderer.render(map, spriteBatch);

        // Render Textured Sprites
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
                if ("boss_goblin".equals(es.templateId)) {
                    if (es.hp > 0 && bossIdleAnim != null) {
                        drawFlipped(bossIdleAnim.getKeyFrame(es.stateTime), es.x - 104, es.y - 85, 208, 170, flipX);
                    } else if (es.hp <= 0 && bossHitAnim != null) {
                        drawFlipped(bossHitAnim.getKeyFrame(es.stateTime), es.x - 104, es.y - 85, 208, 170, flipX);
                    }
                } else if ("slime".equals(es.templateId)) {
                    if (es.hp > 0) {
                        com.badlogic.gdx.graphics.g2d.TextureRegion frame;
                        if (es.isMoving) frame = skelRunAnim != null ? skelRunAnim.getKeyFrame(es.stateTime) : null;
                        else frame = skelAttackAnim != null ? skelAttackAnim.getKeyFrame(es.stateTime) : (skelIdleAnim != null ? skelIdleAnim.getKeyFrame(es.stateTime) : null);
                        drawFlipped(frame, es.x - 32, es.y - 32, 64, 64, flipX);
                    } else if (skelDeathAnim != null) {
                        drawFlipped(skelDeathAnim.getKeyFrame(es.stateTime), es.x - 32, es.y - 32, 64, 64, flipX);
                    }
                } else { // goblin or other
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
                float bW = 16, bH = 16;
                
                if ("rocket".equals(es.templateId)) {
                    bulletTex = rocketTex;
                    bW = 24; bH = 24;
                } else if ("small_bullet".equals(es.templateId)) {
                    bulletTex = smallBulletTex;
                    bW = 12; bH = 12;
                }
                
                float bulletAngle = es.angle * MathUtils.radiansToDegrees;
                if (Math.abs(es.targetX - es.x) > 0.1f || Math.abs(es.targetY - es.y) > 0.1f) {
                    bulletAngle = MathUtils.atan2(es.targetY - es.y, es.targetX - es.x) * MathUtils.radiansToDegrees;
                    es.angle = bulletAngle / MathUtils.radiansToDegrees; // cache the angle
                }
                
                spriteBatch.draw(
                    new com.badlogic.gdx.graphics.g2d.TextureRegion(bulletTex),
                    es.x - bW/2, es.y - bH/2,
                    bW/2, bH/2,
                    bW, bH,
                    1.0f, 1.0f,
                    bulletAngle
                );
            } else if (id >= 5000 || (id >= 3000 && id < 4000)) {
                if ("health_orb".equals(es.templateId)) {
                    com.badlogic.gdx.graphics.g2d.TextureRegion frame = healthOrbAnim.getKeyFrame(es.stateTime);
                    spriteBatch.draw(frame, es.x - 10, es.y - 10, 20, 20);
                } else if ("portal".equals(es.templateId)) {
                    com.badlogic.gdx.graphics.g2d.TextureRegion frame;
                    if (es.stateTime < portalStartAnim.getAnimationDuration()) {
                        frame = portalStartAnim.getKeyFrame(es.stateTime);
                    } else {
                        frame = portalIdleAnim.getKeyFrame(es.stateTime);
                    }
                    spriteBatch.draw(frame, es.x - 32, es.y - 32, 64, 64);
                } else if ("crate".equals(es.templateId) || (id >= 3000 && id < 4000)) {
                    if (barrelRegion != null) {
                        spriteBatch.draw(barrelRegion, es.x - 8, es.y - 8, 16, 16);
                    }
                } else {
                    com.badlogic.gdx.graphics.Texture itemTex = revolverTex;
                    String displayName = "Starter Pistol";
                    if ("fast_smg".equals(es.templateId)) {
                        itemTex = smgTex;
                        displayName = "Fast SMG";
                    } else if ("heavy_cannon".equals(es.templateId)) {
                        itemTex = rocketLauncherTex;
                        displayName = "Heavy Cannon";
                    } else if ("revolver".equals(es.templateId)) {
                        displayName = "Starter Pistol";
                    }
                    
                    float floatOffset = 3f * (float)Math.sin(com.badlogic.gdx.utils.TimeUtils.nanoTime() / 150000000.0);
                    spriteBatch.draw(itemTex, es.x - 10, es.y - 10 + floatOffset, 20, 20);

                    // Draw Weapon Floating Text Label with Proximity Prompt
                    float dstToPlayer = com.badlogic.gdx.math.Vector2.dst(localX, localY, es.x, es.y);
                    if (dstToPlayer < 96f) { // approx 6 tiles radius
                        font.getData().setScale(0.5f);
                        if (dstToPlayer < 40f) { // close enough to interact (6.0 meters is ~96px, 40px is very comfortable)
                            font.setColor(Color.CYAN);
                            font.draw(spriteBatch, displayName + "\n[E to Equip]", es.x - 100, es.y + 35 + floatOffset, 200, com.badlogic.gdx.utils.Align.center, true);
                        } else {
                            font.setColor(Color.WHITE);
                            font.draw(spriteBatch, displayName, es.x - 100, es.y + 25 + floatOffset, 200, com.badlogic.gdx.utils.Align.center, true);
                        }
                        font.getData().setScale(1.0f); // Reset scale
                        font.setColor(Color.WHITE); // Reset color
                    }
                }
            }
        }

        // Draw active effects
        synchronized(effects) {
            java.util.Iterator<Effect> it = effects.iterator();
            while (it.hasNext()) {
                Effect e = it.next();
                e.update(delta);
                if (e.isDead) { it.remove(); continue; }
                
                if (e.anim != null) {
                    spriteBatch.draw(e.anim.getKeyFrame(e.stateTime), e.x - 24, e.y - 24, 48, 48);
                } else if (e.text != null) {
                    font.setColor(e.color);
                    font.draw(spriteBatch, e.text, e.x - 10, e.y);
                }
            }
        }

        spriteBatch.end();

        // Render Geometry & Overlays using ShapeRenderer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Integer id : entities.keySet()) {
            EntityState es = entities.get(id);
            
            if (id >= 3000 && id < 4000) {
                // Crates are now handled in DungeonRenderer with SpriteBatch
            } else if (id >= 5000 && "portal".equals(es.templateId)) {
                // Portal is now handled in SpriteBatch
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
        
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, hudStage.getViewport().getWorldWidth(), hudStage.getViewport().getWorldHeight()));
        java.util.List<Vector2> otherPlayers = new java.util.ArrayList<>();
        for (Integer id : entities.keySet()) {
            if (id != game.networkManager.getPlayerId() && id < 10) {
                EntityState es = entities.get(id);
                otherPlayers.add(new Vector2(es.x, es.y));
            }
        }
        dungeonRenderer.renderMinimap(map, localX, localY, otherPlayers, clearedRooms, totalRooms, hudStage.getViewport().getWorldWidth(), hudStage.getViewport().getWorldHeight());
        
        // Render Graphical HUD
        spriteBatch.setProjectionMatrix(hudStage.getViewport().getCamera().combined);
        spriteBatch.begin();
        float hudX = 20, hudY = hudStage.getViewport().getWorldHeight() - 40;
        float barW = 160, barH = 22;
        
        // HP Bar
        spriteBatch.draw(barBack, hudX, hudY, barW, barH);
        float hpRatio = Math.max(0, currentHP / maxHP);
        spriteBatch.draw(hpRatio < 0.25f ? barHP_red : barHP, hudX + 2, hudY + 2, (barW - 4) * hpRatio, barH - 4);
        
        // Energy Bar
        hudY -= 25;
        spriteBatch.draw(barBack, hudX, hudY, barW, barH);
        float energyRatio = Math.max(0, currentEnergy / maxEnergy);
        spriteBatch.draw(barEnergy, hudX + 2, hudY + 2, (barW - 4) * energyRatio, barH - 4);
        spriteBatch.end();

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
        resultLabel.pack(); // Size to text
        resultLabel.setPosition(hudStage.getViewport().getWorldWidth()/2f - resultLabel.getWidth()/2f, hudStage.getViewport().getWorldHeight() - 100);
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
        if (dungeonRenderer != null) dungeonRenderer.dispose();

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

        if (barBack != null) barBack.dispose();
        if (barHP != null) barHP.dispose();
        if (barHP_red != null) barHP_red.dispose();
        if (barEnergy != null) barEnergy.dispose();
    }
}
