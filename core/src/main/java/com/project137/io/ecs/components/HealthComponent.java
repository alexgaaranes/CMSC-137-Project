package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class HealthComponent implements Component {
    public float maxHealth;
    public float currentHealth;

    public HealthComponent(float health) {
        this.maxHealth = health;
        this.currentHealth = health;
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }

    public void takeDamage(float damage) {
        currentHealth = Math.max(0, currentHealth - damage);
    }
}
