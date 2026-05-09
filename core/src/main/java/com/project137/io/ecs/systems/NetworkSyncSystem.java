package com.project137.io.ecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.project137.io.NetworkConfig;
import com.project137.io.ecs.components.BodyComponent;
import com.project137.io.ecs.components.NetworkComponent;
import com.project137.io.network.PlayerUpdatePacket;
import com.project137.io.network.server.ServerNetworkManager;

public class NetworkSyncSystem extends IteratingSystem {
    private final ServerNetworkManager networkManager;
    private final ComponentMapper<BodyComponent> bm = ComponentMapper.getFor(BodyComponent.class);
    private final ComponentMapper<NetworkComponent> nm = ComponentMapper.getFor(NetworkComponent.class);
    private long sequence = 0;

    public NetworkSyncSystem(ServerNetworkManager networkManager) {
        super(Family.all(BodyComponent.class, NetworkComponent.class).get());
        this.networkManager = networkManager;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        BodyComponent body = bm.get(entity);
        NetworkComponent net = nm.get(entity);
        
        // We reuse PlayerUpdatePacket for all dynamic entities (enemies, players)
        // In a real app, we'd have different packets, but this works for a skeleton.
        PlayerUpdatePacket packet = new PlayerUpdatePacket(
            net.id,
            body.body.getPosition().x * NetworkConfig.PPM,
            body.body.getPosition().y * NetworkConfig.PPM,
            body.body.getAngle(),
            sequence++
        );
        
        networkManager.broadcastUDP(packet);
    }
}
