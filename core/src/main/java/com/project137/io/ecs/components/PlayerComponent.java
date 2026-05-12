package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class PlayerComponent implements Component {
    // Player specific state like health, score, etc.
    public boolean isDead = false;
    public float reviveProgress = 0;
    public Integer reviverId = null;
}
