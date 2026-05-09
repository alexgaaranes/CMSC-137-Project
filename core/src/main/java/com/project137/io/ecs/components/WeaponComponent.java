package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class WeaponComponent implements Component {
    public enum Type { MELEE, PROJECTILE, ARCANE }
    
    public Type type = Type.PROJECTILE;
    public float damage = 10f;
    public float fireRate = 0.5f; // Seconds between shots
    public float cooldown = 0f;
    public float projectileSpeed = 10f;
}
