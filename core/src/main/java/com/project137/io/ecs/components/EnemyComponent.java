package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class EnemyComponent implements Component {
    public enum State { IDLE, CHASE, ATTACK }
    public State state = State.IDLE;
    public float detectRange = 0f; // Default 0
    public float speed = 3f;
    public boolean active = false;
}
