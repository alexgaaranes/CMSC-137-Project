package com.project137.io.ecs.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.World;
import com.project137.io.ecs.components.BodyComponent;

public class PhysicsSystem extends IteratingSystem {
    private final World world;
    private float accumulator = 0f;
    private static final float TIME_STEP = 1/60f;

    public PhysicsSystem(World world) {
        super(Family.all(BodyComponent.class).get());
        this.world = world;
    }

    @Override
    public void update(float deltaTime) {
        accumulator += deltaTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        // No per-entity processing needed for simple physics step
    }
}
