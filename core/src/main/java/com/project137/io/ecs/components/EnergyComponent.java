package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class EnergyComponent implements Component {
    public float maxEnergy;
    public float currentEnergy;
    public float regenRate = 2.0f; // Energy per second

    public EnergyComponent(float energy) {
        this.maxEnergy = energy;
        this.currentEnergy = energy;
    }
}
