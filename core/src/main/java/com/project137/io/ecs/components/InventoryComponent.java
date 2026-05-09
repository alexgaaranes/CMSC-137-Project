package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class InventoryComponent implements Component {
    public String[] weaponSlots = new String[2];
    public int activeSlot = 0;

    public InventoryComponent() {
        weaponSlots[0] = "starter_pistol";
        weaponSlots[1] = null;
    }
}
