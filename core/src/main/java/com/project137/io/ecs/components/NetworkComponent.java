package com.project137.io.ecs.components;

import com.badlogic.ashley.core.Component;

public class NetworkComponent implements Component {
    public int id;
    public boolean isLocal = false;
}
