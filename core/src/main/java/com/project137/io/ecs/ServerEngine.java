package com.project137.io.ecs;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.project137.io.NetworkConfig;
import com.project137.io.ecs.components.*;
import com.project137.io.ecs.data.DataManager;
import com.project137.io.ecs.data.EnemyTemplate;
import com.project137.io.ecs.data.WeaponTemplate;
import com.project137.io.ecs.systems.*;
import com.project137.io.network.*;
import com.project137.io.network.server.ServerNetworkManager;
import com.project137.io.world.DungeonGenerator;
import com.project137.io.world.DungeonMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerEngine {
    public static final short BIT_WALL = 1;
    public static final short BIT_PLAYER = 2;
    public static final short BIT_ENEMY = 4;
    public static final short BIT_PROJECTILE = 8;
    public static final short BIT_CRATE = 16;
    public static final short BIT_ITEM = 32;

    private final Engine engine;
    private final World world;
    private final ConcurrentHashMap<Integer, Entity> entities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, InputPacket> pendingInputs = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<InputPacket> eventQueue = new ConcurrentLinkedQueue<>();
    
    private final AtomicInteger nextEnemyId = new AtomicInteger(1000);
    private final AtomicInteger nextProjectileId = new AtomicInteger(2000); 
    private final AtomicInteger nextCrateId = new AtomicInteger(3000);
    private final AtomicInteger nextItemId = new AtomicInteger(5000);
    
    private DungeonMap map;
    private final DungeonGenerator generator;
    private final ServerNetworkManager networkManager;
    private final Random random = new Random();
    
    private final List<Entity> removalQueue = new ArrayList<>();
    private final List<Body> wallBodies = new ArrayList<>();
    private final List<Body> gateBodies = new ArrayList<>();
    private final List<Body> bodiesToRemove = new ArrayList<>();
    private final java.util.concurrent.ConcurrentLinkedQueue<Runnable> mainThreadTasks = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private boolean gameOver = false;
    private boolean isVoting = false;
    private final ConcurrentHashMap<Integer, Integer> playerVotes = new ConcurrentHashMap<>();
    private final String[] currentBuffOptions = {"Vitality (+20 Max HP)", "Adrenaline (+20% Speed)", "Power (+5 Damage)", "Efficiency (-25% Energy Cost)"};

    public ServerEngine(ServerNetworkManager networkManager) {
        this.networkManager = networkManager;
        this.engine = new Engine();
        this.world = new World(new Vector2(0, 0), true);
        
        try { DataManager.load(); } catch (Exception e) {}

        this.generator = new DungeonGenerator();
        this.map = generator.generate(160, 160); 
        
        createPhysicsObjects();
        spawnInitialEnemies();
        spawnInitialItems();

        engine.addSystem(new PhysicsSystem(world));
        engine.addSystem(new EnemyAISystem());
        engine.addSystem(new NetworkSyncSystem(networkManager));
        setupContactListener();
    }

    private void createPhysicsObjects() {
        for (Body b : wallBodies) bodiesToRemove.add(b);
        wallBodies.clear();
        for (int x = 0; x < map.width; x++) {
            for (int y = 0; y < map.height; y++) {
                int tile = map.tiles[x][y];
                if (tile == DungeonMap.TILE_WALL) {
                    Body b = createStaticBox(x, y, BIT_WALL, (short)(BIT_PLAYER | BIT_ENEMY | BIT_PROJECTILE));
                    wallBodies.add(b);
                } else if (tile == DungeonMap.TILE_CRATE) {
                    spawnCrate(x, y);
                }
            }
        }
    }

    private Body createStaticBox(int tx, int ty, short category, short mask) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set((tx * 16 + 8) / NetworkConfig.PPM, (ty * 16 + 8) / NetworkConfig.PPM);
        Body body = world.createBody(bodyDef);
        PolygonShape box = new PolygonShape();
        box.setAsBox(8 / NetworkConfig.PPM, 8 / NetworkConfig.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = box;
        fdef.filter.categoryBits = category;
        fdef.filter.maskBits = mask;
        body.createFixture(fdef);
        box.dispose();
        return body;
    }

    private void spawnCrate(int tx, int ty) {
        int id = nextCrateId.getAndIncrement();
        Entity entity = new Entity();
        NetworkComponent net = new NetworkComponent();
        net.id = id;
        entity.add(net);
        entity.add(new HealthComponent(20f));

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set((tx * 16 + 8) / NetworkConfig.PPM, (ty * 16 + 8) / NetworkConfig.PPM);
        Body body = world.createBody(bodyDef);
        PolygonShape box = new PolygonShape();
        box.setAsBox(7 / NetworkConfig.PPM, 7 / NetworkConfig.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = box;
        fdef.filter.categoryBits = BIT_CRATE;
        fdef.filter.maskBits = (short)(BIT_PLAYER | BIT_ENEMY | BIT_PROJECTILE);
        body.createFixture(fdef);
        body.setUserData(entity);
        box.dispose();
        
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        engine.addEntity(entity);
        entities.put(id, entity);
    }

    private void spawnInitialEnemies() {
        ArrayList<EnemyTemplate> templates = DataManager.getAllEnemies();
        if (templates.isEmpty()) return;

        for (DungeonGenerator.Room room : generator.rooms) {
            if (room.type == DungeonGenerator.RoomType.ENEMY) {
                for (int i = 0; i < 4; i++) {
                    EnemyTemplate t = templates.get(random.nextInt(templates.size()));
                    spawnEnemyFromTemplate(t, room.x + 8 + i % 2 * 12, room.y + 8 + i / 2 * 12);
                }
            }
        }
    }

    private void spawnInitialItems() {
        for (DungeonGenerator.Room room : generator.rooms) {
            if (room.type == DungeonGenerator.RoomType.SHOP) {
                spawnItem("fast_smg", room.centerX() - 4, room.centerY());
                spawnItem("heavy_cannon", room.centerX() + 4, room.centerY());
            }
        }
    }

    private void spawnItem(String templateId, int tx, int ty) {
        int id = nextItemId.getAndIncrement();
        Entity entity = new Entity();
        NetworkComponent net = new NetworkComponent();
        net.id = id;
        entity.add(net);
        InteractableComponent interact = new InteractableComponent();
        interact.templateId = templateId;
        entity.add(interact);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set((tx * 16 + 8) / NetworkConfig.PPM, (ty * 16 + 8) / NetworkConfig.PPM);
        Body body = world.createBody(bodyDef);
        CircleShape circle = new CircleShape();
        circle.setRadius(8f / NetworkConfig.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = circle;
        fdef.isSensor = true;
        fdef.filter.categoryBits = BIT_ITEM;
        fdef.filter.maskBits = BIT_PLAYER;
        body.createFixture(fdef);
        body.setUserData(entity);
        circle.dispose();

        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        engine.addEntity(entity);
        entities.put(id, entity);
        networkManager.broadcastTCP(new ItemSpawnPacket(id, templateId, body.getPosition().x * NetworkConfig.PPM, body.getPosition().y * NetworkConfig.PPM));
    }

    private void spawnEnemyFromTemplate(EnemyTemplate t, int tx, int ty) {
        int id = nextEnemyId.getAndIncrement();
        Entity entity = new Entity();
        NetworkComponent net = new NetworkComponent();
        net.id = id;
        entity.add(net);
        
        EnemyComponent enemy = new EnemyComponent();
        enemy.detectRange = 0; 
        enemy.speed = t.speed;
        enemy.damage = t.damage;
        enemy.attackRate = t.attackRate;
        entity.add(enemy);
        entity.add(new HealthComponent(t.hp));
        
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set((tx * 16 + 8) / NetworkConfig.PPM, (ty * 16 + 8) / NetworkConfig.PPM);
        bodyDef.fixedRotation = true;
        Body body = world.createBody(bodyDef);
        CircleShape circle = new CircleShape();
        circle.setRadius(14f / NetworkConfig.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = circle;
        fdef.density = 1f;
        fdef.filter.categoryBits = BIT_ENEMY;
        fdef.filter.maskBits = (short)(BIT_WALL | BIT_PLAYER | BIT_PROJECTILE | BIT_CRATE);
        body.createFixture(fdef);
        body.setUserData(entity);
        circle.dispose();
        
        body.setActive(false); // Dormant initially

        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        engine.addEntity(entity);
        entities.put(id, entity);
    }

    public void spawnProjectile(int ownerId, float x, float y, float angle, float damage, float speed) {
        int id = nextProjectileId.getAndIncrement();
        Entity entity = new Entity();
        NetworkComponent net = new NetworkComponent();
        net.id = id;
        entity.add(net);
        ProjectileComponent pc = new ProjectileComponent();
        pc.ownerId = ownerId;
        pc.damage = damage;
        entity.add(pc);
        
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.bullet = true;
        Body body = world.createBody(bodyDef);
        CircleShape circle = new CircleShape();
        circle.setRadius(5 / NetworkConfig.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = circle;
        fdef.filter.categoryBits = BIT_PROJECTILE;
        fdef.filter.maskBits = (short)(BIT_WALL | BIT_ENEMY | BIT_CRATE);
        fdef.isSensor = true;
        body.createFixture(fdef);
        body.setUserData(entity);
        circle.dispose();
        
        Vector2 velocity = new Vector2(1, 0).setAngleDeg(angle).scl(speed);
        body.setLinearVelocity(velocity);
        
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        engine.addEntity(entity);
        entities.put(id, entity);
    }

    private void setupContactListener() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture fa = contact.getFixtureA();
                Fixture fb = contact.getFixtureB();
                Object ua = fa.getBody().getUserData();
                Object ub = fb.getBody().getUserData();
                if (ua instanceof Entity ea && ub instanceof Entity eb) {
                    checkHit(ea, eb);
                    checkHit(eb, ea);
                    checkPickup(ea, eb);
                    checkPickup(eb, ea);
                }
                checkWallHit(fa, fb);
                checkWallHit(fb, fa);
            }
            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void checkWallHit(Fixture bulletFix, Fixture otherFix) {
        Object userData = bulletFix.getBody().getUserData();
        if (userData instanceof Entity entity) {
            ProjectileComponent pc = entity.getComponent(ProjectileComponent.class);
            short category = otherFix.getFilterData().categoryBits;
            if (pc != null && (category == BIT_WALL || category == BIT_CRATE)) {
                synchronized (removalQueue) {
                    if (!removalQueue.contains(entity)) removalQueue.add(entity);
                }
            }
        }
    }

    private void checkPickup(Entity a, Entity b) {
        PlayerComponent pc = a.getComponent(PlayerComponent.class);
        InteractableComponent item = b.getComponent(InteractableComponent.class);
        if (pc != null && !pc.isDead && item != null && "health_orb".equals(item.templateId)) {
            HealthComponent hc = a.getComponent(HealthComponent.class);
            if (hc != null && hc.currentHealth < hc.maxHealth) {
                hc.currentHealth = Math.min(hc.maxHealth, hc.currentHealth + 10);
                synchronized (removalQueue) {
                    if (!removalQueue.contains(b)) removalQueue.add(b);
                }
            }
        }
    }

    private void checkHit(Entity projectile, Entity target) {
        ProjectileComponent pc = projectile.getComponent(ProjectileComponent.class);
        HealthComponent hc = target.getComponent(HealthComponent.class);
        if (pc != null && hc != null && hc.isAlive()) {
            hc.takeDamage(pc.damage);
            synchronized (removalQueue) {
                if (!removalQueue.contains(projectile)) removalQueue.add(projectile);
                if (hc.currentHealth <= 0 && !removalQueue.contains(target)) {
                    if (target.getComponent(PlayerComponent.class) == null) {
                        removalQueue.add(target);
                    }
                }
            }
        }
    }

    public void update(float delta) {
        Runnable task;
        while ((task = mainThreadTasks.poll()) != null) {
            task.run();
        }

        if (isVoting) return; // Pause updates during voting
        
        processOneShotEvents();
        processInputs(delta);
        checkRoomStates();
        engine.update(delta);
        
        synchronized (bodiesToRemove) {
            for (Body b : bodiesToRemove) world.destroyBody(b);
            bodiesToRemove.clear();
        }
        
        synchronized (removalQueue) {
            for (Entity entity : removalQueue) {
                NetworkComponent net = entity.getComponent(NetworkComponent.class);
                BodyComponent bc = entity.getComponent(BodyComponent.class);
                if (net != null) {
                    networkManager.broadcastTCP(new EntityRemovePacket(net.id));
                    entities.remove(net.id);
                    if (net.id >= 3000 && net.id < 4000 && bc != null) {
                        int tx = (int)(bc.body.getPosition().x * NetworkConfig.PPM / 16);
                        int ty = (int)(bc.body.getPosition().y * NetworkConfig.PPM / 16);
                        if (tx >= 0 && tx < map.width && ty >= 0 && ty < map.height) {
                            map.tiles[tx][ty] = DungeonMap.TILE_FLOOR;
                            networkManager.broadcastTCP(new TileUpdatePacket(tx, ty, DungeonMap.TILE_FLOOR));
                            if (random.nextFloat() < 0.05f) {
                                spawnItem("health_orb", tx, ty);
                            }
                        }
                    }
                }
                if (bc != null && bc.body != null) world.destroyBody(bc.body);
                engine.removeEntity(entity);
            }
            removalQueue.clear();
        }

        for (Entity entity : entities.values()) {
            WeaponComponent wc = entity.getComponent(WeaponComponent.class);
            if (wc != null && wc.cooldown > 0) wc.cooldown -= delta;
            EnergyComponent ec = entity.getComponent(EnergyComponent.class);
            if (ec != null && ec.currentEnergy < ec.maxEnergy) {
                ec.currentEnergy = Math.min(ec.maxEnergy, ec.currentEnergy + ec.regenRate * delta);
            }
            ProjectileComponent pc = entity.getComponent(ProjectileComponent.class);
            if (pc != null) {
                pc.lifeTime -= delta;
                if (pc.lifeTime <= 0) {
                    synchronized (removalQueue) { if (!removalQueue.contains(entity)) removalQueue.add(entity); }
                }
            }
            HealthComponent hc = entity.getComponent(HealthComponent.class);
            if (hc != null && hc.currentHealth <= 0) {
                PlayerComponent playerComp = entity.getComponent(PlayerComponent.class);
                if (playerComp != null) {
                    playerComp.isDead = true;
                    // Ensure velocity is zero when dead
                    BodyComponent bc = entity.getComponent(BodyComponent.class);
                    if (bc != null && bc.body != null) bc.body.setLinearVelocity(0, 0);
                } else {
                    synchronized (removalQueue) { if (!removalQueue.contains(entity)) removalQueue.add(entity); }
                }
            }

            // Send resource updates for players
            PlayerComponent pc_player = entity.getComponent(PlayerComponent.class);
            if (pc_player != null) {
                HealthComponent hc_player = entity.getComponent(HealthComponent.class);
                EnergyComponent ec_player = entity.getComponent(EnergyComponent.class);
                if (hc_player != null && ec_player != null) {
                    networkManager.broadcastUDP(new ResourceUpdatePacket(
                        entity.getComponent(NetworkComponent.class).id,
                        hc_player.currentHealth, hc_player.maxHealth,
                        ec_player.currentEnergy, ec_player.maxEnergy,
                        pc_player.reviveProgress
                    ));
                }
            }
        }

        if (!gameOver && networkManager.hasClients()) {
            boolean playersAlive = false;
            for (Entity entity : entities.values()) {
                PlayerComponent pc = entity.getComponent(PlayerComponent.class);
                if (pc != null && !pc.isDead) {
                    playersAlive = true;
                    break;
                }
            }
            if (!playersAlive) {
                gameOver = true;
                networkManager.broadcastTCP(new GameOverPacket(false));
                networkManager.stop();
            }
        }

        // Handle Revival Progress
        for (Entity entity : entities.values()) {
            PlayerComponent pc = entity.getComponent(PlayerComponent.class);
            if (pc != null && pc.isDead && pc.reviverId != null) {
                Entity reviver = entities.get(pc.reviverId);
                boolean valid = false;
                if (reviver != null) {
                    PlayerComponent rpc = reviver.getComponent(PlayerComponent.class);
                    BodyComponent rbc = reviver.getComponent(BodyComponent.class);
                    BodyComponent dbc = entity.getComponent(BodyComponent.class);
                    if (rpc != null && !rpc.isDead && rbc != null && dbc != null) {
                        float dist = rbc.body.getPosition().dst(dbc.body.getPosition());
                        if (dist < 1.5f) {
                            valid = true;
                            pc.reviveProgress += delta;
                            if (pc.reviveProgress >= 5.0f) {
                                pc.isDead = false;
                                pc.reviveProgress = 0;
                                pc.reviverId = null;
                                HealthComponent hc = entity.getComponent(HealthComponent.class);
                                if (hc != null) hc.currentHealth = 50f;
                            }
                        }
                    }
                }
                if (!valid) {
                    pc.reviveProgress = 0;
                    pc.reviverId = null;
                }
            }
        }
    }

    private void processOneShotEvents() {
        InputPacket event;
        while ((event = eventQueue.poll()) != null) {
            int playerId = event.playerId;
            Entity entity = entities.get(playerId);
            if (entity == null) continue;
            InventoryComponent inv = entity.getComponent(InventoryComponent.class);
            BodyComponent bc = entity.getComponent(BodyComponent.class);
            WeaponComponent wc = entity.getComponent(WeaponComponent.class);
            if (event.swapTrigger && inv != null) {
                inv.activeSlot = (inv.activeSlot + 1) % 2;
                updateWeaponFromTemplate(wc, inv.weaponSlots[inv.activeSlot]);
                networkManager.broadcastTCP(new WeaponChangePacket(playerId, inv.weaponSlots[0], inv.weaponSlots[1], inv.activeSlot));
            }
            if (event.interactTrigger && inv != null && bc != null) {
                handleInteraction(playerId, inv, bc);
            }
        }
    }

    private void checkRoomStates() {
        for (DungeonGenerator.Room room : generator.rooms) {
            if (room.type != DungeonGenerator.RoomType.ENEMY || room.cleared) continue;
            boolean playerInside = false;
            float px = 0, py = 0;
            for (Entity entity : entities.values()) {
                if (entity.getComponent(PlayerComponent.class) != null) {
                    BodyComponent bc = entity.getComponent(BodyComponent.class);
                    if (room.isInside(bc.body.getPosition().x * NetworkConfig.PPM / 16, bc.body.getPosition().y * NetworkConfig.PPM / 16)) {
                        playerInside = true;
                        px = bc.body.getPosition().x;
                        py = bc.body.getPosition().y;
                        break;
                    }
                }
            }
            if (playerInside && !room.locked) {
                lockRoom(room, px, py);
            } else if (room.locked) {
                boolean enemiesAlive = false;
                for (Entity entity : entities.values()) {
                    if (entity.getComponent(EnemyComponent.class) != null) {
                        BodyComponent bc = entity.getComponent(BodyComponent.class);
                        if (room.isInside(bc.body.getPosition().x * NetworkConfig.PPM / 16, bc.body.getPosition().y * NetworkConfig.PPM / 16)) {
                            enemiesAlive = true;
                            break;
                        }
                    }
                }
                if (!enemiesAlive) {
                    System.out.println("[Progression] Room at (" + room.x + "," + room.y + ") cleared! Opening gates.");
                    unlockRoom(room);
                    checkLevelCleared();
                }
            }
        }
    }

    private void checkLevelCleared() {
        boolean allCleared = true;
        for (DungeonGenerator.Room room : generator.rooms) {
            if (room.type == DungeonGenerator.RoomType.ENEMY && !room.cleared) {
                allCleared = false;
                break;
            }
        }
        if (allCleared) {
            System.out.println("[Progression] ALL ROOMS CLEARED! Starting buff vote...");
            startBuffVote();
        }
    }

    private void startBuffVote() {
        isVoting = true;
        playerVotes.clear();
        networkManager.broadcastTCP(new BuffPackets.BuffVoteStartPacket(currentBuffOptions));
    }

    public void handleBuffVote(int playerId, int buffIndex) {
        if (!isVoting) return;
        playerVotes.put(playerId, buffIndex);
        // Check if everyone has voted
        if (playerVotes.size() >= networkManager.hasClientsCount()) {
            mainThreadTasks.add(this::applyVoteResults);
        }
    }

    private void applyVoteResults() {
        for (Map.Entry<Integer, Integer> entry : playerVotes.entrySet()) {
            int playerId = entry.getKey();
            int buffIndex = entry.getValue();
            String buff = currentBuffOptions[buffIndex];
            
            // Find the player entity
            for (Entity e : entities.values()) {
                NetworkComponent net = e.getComponent(NetworkComponent.class);
                if (net != null && net.id == playerId) {
                    PlayerComponent pc = e.getComponent(PlayerComponent.class);
                    if (pc != null) {
                        pc.buffs.add(buff);
                        // Send individual notification to player
                        networkManager.sendTCPTo(playerId, new BuffPackets.BuffVoteResultPacket(buff));
                    }
                    break;
                }
            }
        }
        
        System.out.println("[Progression] All players picked individual buffs. Starting next level.");
        isVoting = false;
        mainThreadTasks.add(this::nextLevel);
    }

    private void nextLevel() {
        // Clear old entities (except players)
        for (Integer id : entities.keySet()) {
            if (id >= 10) { 
                Entity e = entities.get(id);
                synchronized (removalQueue) { if (!removalQueue.contains(e)) removalQueue.add(e); }
            }
        }

        // Generate new map
        generator.rooms.clear();
        this.map = generator.generate(160, 160);
        
        createPhysicsObjects();
        spawnInitialEnemies();
        spawnInitialItems();
        
        networkManager.broadcastTCP(new MapDataPacket(map));
        
        DungeonGenerator.Room spawnRoom = generator.rooms.get(0);
        float spawnX = (spawnRoom.centerX() * 16 + 8) / NetworkConfig.PPM;
        float spawnY = (spawnRoom.centerY() * 16 + 8) / NetworkConfig.PPM;
        
        for (Entity entity : entities.values()) {
            PlayerComponent pc = entity.getComponent(PlayerComponent.class);
            if (pc != null) {
                // Reset player state for new level
                pc.isDead = false;
                pc.reviveProgress = 0;
                pc.reviverId = null;
                
                BodyComponent bc = entity.getComponent(BodyComponent.class);
                NetworkComponent net = entity.getComponent(NetworkComponent.class);
                if (bc != null && bc.body != null) {
                    bc.body.setTransform(spawnX, spawnY, 0);
                    bc.body.setLinearVelocity(0, 0);
                    bc.body.setAwake(true);
                }
                networkManager.broadcastTCP(new TeleportPacket(net.id, spawnX * NetworkConfig.PPM, spawnY * NetworkConfig.PPM));
                
                // Apply all active buffs to player stats FIRST so max HP is correct
                applyBuffsToPlayer(entity);

                // Heal players slightly on next level (Second Wind)
                HealthComponent hc = entity.getComponent(HealthComponent.class);
                if (hc != null) hc.currentHealth = Math.min(hc.maxHealth, hc.currentHealth + 20);
            }
        }
    }

    private void applyBuffsToPlayer(Entity player) {
        HealthComponent hc = player.getComponent(HealthComponent.class);
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        if (pc == null || hc == null) return;

        float hpBonus = 0;
        for (String buff : pc.buffs) {
            if (buff.contains("Vitality")) hpBonus += 20;
        }

        float oldMax = hc.maxHealth;
        hc.maxHealth = 100 + hpBonus;
        if (hc.maxHealth > oldMax) {
            hc.currentHealth += (hc.maxHealth - oldMax);
        }
        hc.currentHealth = Math.min(hc.maxHealth, hc.currentHealth);
    }

    private void lockRoom(DungeonGenerator.Room room, float teleportX, float teleportY) {
        room.locked = true;
        for (Entity entity : entities.values()) {
            if (entity.getComponent(PlayerComponent.class) != null) {
                BodyComponent bc = entity.getComponent(BodyComponent.class);
                NetworkComponent net = entity.getComponent(NetworkComponent.class);
                if (!room.isInside(bc.body.getPosition().x * NetworkConfig.PPM / 16, bc.body.getPosition().y * NetworkConfig.PPM / 16)) {
                    bc.body.setTransform(teleportX, teleportY, 0);
                    networkManager.broadcastTCP(new TeleportPacket(net.id, teleportX * NetworkConfig.PPM, teleportY * NetworkConfig.PPM));
                }
            }
        }
        for (Entity entity : entities.values()) {
            EnemyComponent enemy = entity.getComponent(EnemyComponent.class);
            BodyComponent bc = entity.getComponent(BodyComponent.class);
            if (enemy != null && bc != null) {
                if (room.isInside(bc.body.getPosition().x * NetworkConfig.PPM / 16, bc.body.getPosition().y * NetworkConfig.PPM / 16)) {
                    enemy.detectRange = 800f;
                    bc.body.setActive(true); // Wake up
                }
            }
        }
        for (int x = room.x - 1; x <= room.x + room.w; x++) {
            for (int y = room.y - 1; y <= room.y + room.h; y++) {
                if (x == room.x - 1 || x == room.x + room.w || y == room.y - 1 || y == room.y + room.h) {
                    if (map.tiles[x][y] == DungeonMap.TILE_FLOOR) spawnGate(x, y);
                }
            }
        }
    }

    private void spawnGate(int tx, int ty) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set((tx * 16 + 8) / NetworkConfig.PPM, (ty * 16 + 8) / NetworkConfig.PPM);
        Body body = world.createBody(bodyDef);
        PolygonShape box = new PolygonShape();
        box.setAsBox(8 / NetworkConfig.PPM, 8 / NetworkConfig.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = box;
        fdef.filter.categoryBits = BIT_WALL;
        body.createFixture(fdef);
        box.dispose();
        gateBodies.add(body);
        map.tiles[tx][ty] = DungeonMap.TILE_GATE;
        networkManager.broadcastTCP(new TileUpdatePacket(tx, ty, DungeonMap.TILE_GATE));
    }

    private void unlockRoom(DungeonGenerator.Room room) {
        room.locked = false;
        room.cleared = true;
        for (Body body : gateBodies) {
            int tx = (int)(body.getPosition().x * NetworkConfig.PPM / 16);
            int ty = (int)(body.getPosition().y * NetworkConfig.PPM / 16);
            map.tiles[tx][ty] = DungeonMap.TILE_FLOOR;
            networkManager.broadcastTCP(new TileUpdatePacket(tx, ty, DungeonMap.TILE_FLOOR));
            bodiesToRemove.add(body);
        }
        gateBodies.clear();
    }

    private void processInputs(float delta) {
        for (Integer playerId : pendingInputs.keySet()) {
            InputPacket input = pendingInputs.get(playerId);
            Entity entity = entities.get(playerId);
            if (entity == null) continue;
            BodyComponent bc = entity.getComponent(BodyComponent.class);
            WeaponComponent wc = entity.getComponent(WeaponComponent.class);
            InventoryComponent inv = entity.getComponent(InventoryComponent.class);
            EnergyComponent ec = entity.getComponent(EnergyComponent.class);
            if (bc != null && bc.body != null) {
                PlayerComponent pc = entity.getComponent(PlayerComponent.class);
                if (pc != null && pc.isDead) {
                    bc.body.setLinearVelocity(0, 0);
                    continue;
                }
                
                float speed = 6.25f;
                for (String buff : pc.buffs) if (buff.contains("Adrenaline")) speed *= 1.2f;
                
                Vector2 velocity = new Vector2(0, 0);
                if (input.up) velocity.y += 1;
                if (input.down) velocity.y -= 1;
                if (input.left) velocity.x -= 1;
                if (input.right) velocity.x += 1;
                velocity.nor().scl(speed);
                bc.body.setLinearVelocity(velocity);
                if (input.attack && wc != null && wc.cooldown <= 0) {
                    WeaponTemplate template = DataManager.getWeapon(inv.weaponSlots[inv.activeSlot]);
                    if (template != null && ec != null) {
                        float cost = template.energyCost;
                        for (String buff : pc.buffs) if (buff.contains("Efficiency")) cost *= 0.75f;
                        
                        if (ec.currentEnergy >= cost) {
                            float damage = wc.damage;
                            for (String buff : pc.buffs) if (buff.contains("Power")) damage += 5;
                            
                            spawnProjectile(playerId, bc.body.getPosition().x, bc.body.getPosition().y, input.targetAngle, damage, wc.projectileSpeed);
                            wc.cooldown = wc.fireRate;
                            ec.currentEnergy -= cost;
                        }
                    }
                }
            }
        }
    }

    private void handleInteraction(int playerId, InventoryComponent inv, BodyComponent bc) {
        Entity nearestItem = null;
        float minDist = 1.5f; 
        for (Entity e : entities.values()) {
            InteractableComponent interact = e.getComponent(InteractableComponent.class);
            if (interact != null && !"health_orb".equals(interact.templateId)) {
                BodyComponent itemBc = e.getComponent(BodyComponent.class);
                float dist = bc.body.getPosition().dst(itemBc.body.getPosition());
                if (dist < minDist) {
                    minDist = dist;
                    nearestItem = e;
                }
            }
        }
        if (nearestItem != null) {
            InteractableComponent item = nearestItem.getComponent(InteractableComponent.class);
            String oldWeapon = inv.weaponSlots[inv.activeSlot];
            inv.weaponSlots[inv.activeSlot] = item.templateId;
            updateWeaponFromTemplate(entities.get(playerId).getComponent(WeaponComponent.class), item.templateId);
            synchronized (removalQueue) { removalQueue.add(nearestItem); }
            if (oldWeapon != null) {
                spawnItem(oldWeapon, (int)(bc.body.getPosition().x * NetworkConfig.PPM / 16), (int)(bc.body.getPosition().y * NetworkConfig.PPM / 16));
            }
            networkManager.broadcastTCP(new WeaponChangePacket(playerId, inv.weaponSlots[0], inv.weaponSlots[1], inv.activeSlot));
        }

        // Check for dead players to start resurrection
        for (Entity e : entities.values()) {
            PlayerComponent targetPc = e.getComponent(PlayerComponent.class);
            if (targetPc != null && targetPc.isDead && e != entities.get(playerId)) {
                BodyComponent targetBc = e.getComponent(BodyComponent.class);
                if (bc.body.getPosition().dst(targetBc.body.getPosition()) < 1.5f) {
                    if (targetPc.reviverId == null) {
                        targetPc.reviverId = playerId;
                        targetPc.reviveProgress = 0;
                    }
                }
            }
        }
    }

    private void updateWeaponFromTemplate(WeaponComponent wc, String templateId) {
        if (wc == null) return;
        WeaponTemplate t = DataManager.getWeapon(templateId);
        if (t != null) {
            wc.damage = t.damage;
            wc.fireRate = t.fireRate;
            wc.projectileSpeed = t.projectileSpeed;
        }
    }

    public void queueInput(InputPacket input) {
        pendingInputs.put(input.playerId, input);
        if (input.swapTrigger || input.interactTrigger) eventQueue.add(input);
    }

    public void addPlayer(int playerId) {
        Entity entity = new Entity();
        NetworkComponent net = new NetworkComponent();
        net.id = playerId;
        entity.add(net);
        entity.add(new PlayerComponent());
        entity.add(new InventoryComponent());
        entity.add(new WeaponComponent());
        updateWeaponFromTemplate(entity.getComponent(WeaponComponent.class), "starter_pistol");
        entity.add(new HealthComponent(100f));
        entity.add(new EnergyComponent(200f));
        DungeonGenerator.Room spawnRoom = generator.rooms.get(0);
        float spawnX = (spawnRoom.centerX() * 16 + 8) / NetworkConfig.PPM;
        float spawnY = (spawnRoom.centerY() * 16 + 8) / NetworkConfig.PPM;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnX, spawnY); 
        bodyDef.fixedRotation = true;
        Body body = world.createBody(bodyDef);
        CircleShape circle = new CircleShape();
        circle.setRadius(15f / NetworkConfig.PPM);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = circle;
        fdef.density = 1f;
        fdef.filter.categoryBits = BIT_PLAYER;
        fdef.filter.maskBits = (short)(BIT_WALL | BIT_ENEMY | BIT_CRATE | BIT_ITEM);
        body.createFixture(fdef);
        body.setUserData(entity);
        circle.dispose();
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        engine.addEntity(entity);
        entities.put(playerId, entity);
    }

    public void removePlayer(int playerId) {
        Entity entity = entities.remove(playerId);
        pendingInputs.remove(playerId);
        if (entity != null) {
            BodyComponent bc = entity.getComponent(BodyComponent.class);
            if (bc != null && bc.body != null) world.destroyBody(bc.body);
            engine.removeEntity(entity);
        }
    }

    public void startGame() {
        this.gameOver = false;
        // Also reset any existing players just in case
        for (Entity entity : entities.values()) {
            PlayerComponent pc = entity.getComponent(PlayerComponent.class);
            if (pc != null) {
                pc.isDead = false;
                pc.reviveProgress = 0;
                pc.reviverId = null;
                HealthComponent hc = entity.getComponent(HealthComponent.class);
                if (hc != null) hc.currentHealth = hc.maxHealth;
            }
        }
    }

    public DungeonMap getMap() { return map; }
}
