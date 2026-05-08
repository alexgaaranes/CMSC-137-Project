package com.project137.io.ecs;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.project137.io.NetworkConfig;
import com.project137.io.ecs.components.*;
import com.project137.io.ecs.systems.NetworkSyncSystem;
import com.project137.io.ecs.systems.PhysicsSystem;
import com.project137.io.network.server.ServerNetworkManager;
import com.project137.io.world.DungeonGenerator;
import com.project137.io.world.DungeonMap;

import java.util.concurrent.ConcurrentHashMap;

public class ServerEngine {
    // Filter Bits
    public static final short BIT_WALL = 1;
    public static final short BIT_PLAYER = 2;

    private final Engine engine;
    private final World world;
    private final ConcurrentHashMap<Integer, Entity> playerEntities = new ConcurrentHashMap<>();
    private final DungeonMap map;
    private final DungeonGenerator generator;

    public ServerEngine(ServerNetworkManager networkManager) {
        this.engine = new Engine();
        this.world = new World(new Vector2(0, 0), true);
        // Generate Map
        this.generator = new DungeonGenerator();
        this.map = generator.generate(80, 80); 

        createWallPhysics();

        engine.addSystem(new PhysicsSystem(world));
        engine.addSystem(new NetworkSyncSystem(networkManager));
    }

    private void createWallPhysics() {
        for (int x = 0; x < map.width; x++) {
            for (int y = 0; y < map.height; y++) {
                if (map.tiles[x][y] == DungeonMap.TILE_WALL) {
                    BodyDef bodyDef = new BodyDef();
                    bodyDef.type = BodyDef.BodyType.StaticBody;
                    bodyDef.position.set((x * 16 + 8) / NetworkConfig.PPM, (y * 16 + 8) / NetworkConfig.PPM);
                    
                    Body body = world.createBody(bodyDef);
                    PolygonShape box = new PolygonShape();
                    box.setAsBox(8 / NetworkConfig.PPM, 8 / NetworkConfig.PPM);
                    
                    FixtureDef fdef = new FixtureDef();
                    fdef.shape = box;
                    fdef.filter.categoryBits = BIT_WALL;
                    fdef.filter.maskBits = BIT_PLAYER;
                    
                    body.createFixture(fdef);
                    box.dispose();
                }
            }
        }
    }

    public void update(float delta) {
        engine.update(delta);
    }

    public void addPlayer(int playerId) {
        Entity entity = new Entity();
        
        NetworkComponent net = new NetworkComponent();
        net.id = playerId;
        entity.add(net);
        
        entity.add(new PlayerComponent());

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
        fdef.filter.maskBits = BIT_WALL; // Only collide with walls
        
        body.createFixture(fdef);
        circle.dispose();
        
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        
        engine.addEntity(entity);
        playerEntities.put(playerId, entity);
    }

    public void removePlayer(int playerId) {
        Entity entity = playerEntities.remove(playerId);
        if (entity != null) {
            BodyComponent bc = entity.getComponent(BodyComponent.class);
            if (bc != null && bc.body != null) {
                world.destroyBody(bc.body);
            }
            engine.removeEntity(entity);
        }
    }

    public void handleInput(int playerId, boolean up, boolean down, boolean left, boolean right) {
        Entity entity = playerEntities.get(playerId);
        if (entity != null) {
            BodyComponent bc = entity.getComponent(BodyComponent.class);
            if (bc != null && bc.body != null) {
                float speed = 5f;
                Vector2 velocity = new Vector2(0, 0);
                if (up) velocity.y += 1;
                if (down) velocity.y -= 1;
                if (left) velocity.x -= 1;
                if (right) velocity.x += 1;
                
                velocity.nor().scl(speed);
                bc.body.setLinearVelocity(velocity);
            }
        }
    }

    public DungeonMap getMap() { return map; }
}
