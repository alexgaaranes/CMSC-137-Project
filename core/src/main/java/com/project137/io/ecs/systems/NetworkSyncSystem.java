package com.project137.io.ecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.project137.io.NetworkConfig;
import com.project137.io.ecs.components.BodyComponent;
import com.project137.io.ecs.components.EnergyComponent;
import com.project137.io.ecs.components.HealthComponent;
import com.project137.io.ecs.components.NetworkComponent;
import com.project137.io.network.PlayerUpdatePacket;
import com.project137.io.network.ResourceUpdatePacket;
import com.project137.io.network.server.ServerNetworkManager;

public class NetworkSyncSystem extends IteratingSystem {
    private final ServerNetworkManager networkManager;
    private final ComponentMapper<BodyComponent> bm = ComponentMapper.getFor(BodyComponent.class);
    private final ComponentMapper<NetworkComponent> nm = ComponentMapper.getFor(NetworkComponent.class);
    private final ComponentMapper<HealthComponent> hm = ComponentMapper.getFor(HealthComponent.class);
    private final ComponentMapper<EnergyComponent> em = ComponentMapper.getFor(EnergyComponent.class);
    private final ComponentMapper<com.project137.io.ecs.components.PlayerComponent> pm = ComponentMapper.getFor(com.project137.io.ecs.components.PlayerComponent.class);
    private long sequence = 0;

    public NetworkSyncSystem(ServerNetworkManager networkManager) {
        super(Family.all(BodyComponent.class, NetworkComponent.class).get());
        this.networkManager = networkManager;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        BodyComponent body = bm.get(entity);
        NetworkComponent net = nm.get(entity);
        
        // Position Sync
        PlayerUpdatePacket packet = new PlayerUpdatePacket(
            net.id,
            body.body.getPosition().x * NetworkConfig.PPM,
            body.body.getPosition().y * NetworkConfig.PPM,
            body.body.getAngle(),
            sequence++
        );
        networkManager.broadcastUDP(packet);

        // Resource Sync (HP/Energy)
        HealthComponent hc = hm.get(entity);
        EnergyComponent ec = em.get(entity);
        if (hc != null) {
            float energy = (ec != null) ? ec.currentEnergy : 0;
            float maxEnergy = (ec != null) ? ec.maxEnergy : 0;
            float progress = 0;
            com.project137.io.ecs.components.PlayerComponent pc = pm.get(entity);
            if (pc != null) progress = pc.reviveProgress;
            networkManager.broadcastUDP(new ResourceUpdatePacket(net.id, hc.currentHealth, hc.maxHealth, energy, maxEnergy, progress));
        }
    }
}
