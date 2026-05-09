package com.project137.io.ecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.project137.io.ecs.components.BodyComponent;
import com.project137.io.ecs.components.EnemyComponent;
import com.project137.io.ecs.components.NetworkComponent;
import com.project137.io.ecs.components.PlayerComponent;

public class EnemyAISystem extends IteratingSystem {
    private final ComponentMapper<BodyComponent> bm = ComponentMapper.getFor(BodyComponent.class);
    private final ComponentMapper<EnemyComponent> em = ComponentMapper.getFor(EnemyComponent.class);
    private final Family playerFamily = Family.all(PlayerComponent.class, BodyComponent.class).get();

    public EnemyAISystem() {
        super(Family.all(EnemyComponent.class, BodyComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        BodyComponent body = bm.get(entity);
        EnemyComponent enemy = em.get(entity);
        
        // Find nearest player
        Entity nearestPlayer = null;
        float minDist = enemy.detectRange;
        
        for (Entity player : getEngine().getEntitiesFor(playerFamily)) {
            BodyComponent pBody = bm.get(player);
            float dist = body.body.getPosition().dst(pBody.body.getPosition());
            if (dist < minDist) {
                minDist = dist;
                nearestPlayer = player;
            }
        }

        if (enemy.detectRange > 0 && nearestPlayer != null) {
            enemy.state = EnemyComponent.State.CHASE;
            Vector2 pPos = bm.get(nearestPlayer).body.getPosition();
            Vector2 direction = pPos.cpy().sub(body.body.getPosition()).nor();
            body.body.setLinearVelocity(direction.scl(3f)); // Enemy speed
        } else {
            enemy.state = EnemyComponent.State.IDLE;
            body.body.setLinearVelocity(0, 0);
        }
    }
}
