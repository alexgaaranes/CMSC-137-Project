package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class ProjectileComponent implements Component {
    public int ownerId;
    public float damage;
    public String templateId = "bullet";
    public float lifeTime = 2.0f; // Seconds before self-destruct
}
