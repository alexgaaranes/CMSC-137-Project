package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class InteractableComponent implements Component {
    public enum Type { WEAPON, CHEST }
    public Type type = Type.WEAPON;
    public String templateId;
    public float radius = 40f;
}
