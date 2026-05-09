package com.project137.io.ecs.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {
    private static final HashMap<String, WeaponTemplate> weapons = new HashMap<>();
    private static final HashMap<String, EnemyTemplate> enemies = new HashMap<>();

    public static void load() {
        Json json = new Json();
        
        // Load Weapons
        ArrayList<WeaponTemplate> weaponList = json.fromJson(ArrayList.class, WeaponTemplate.class, Gdx.files.internal("data/weapons.json"));
        for (WeaponTemplate t : weaponList) {
            weapons.put(t.id, t);
        }

        // Load Enemies
        ArrayList<EnemyTemplate> enemyList = json.fromJson(ArrayList.class, EnemyTemplate.class, Gdx.files.internal("data/enemies.json"));
        for (EnemyTemplate t : enemyList) {
            enemies.put(t.id, t);
        }
        
        System.out.println("[DataManager] Loaded " + weapons.size() + " weapons and " + enemies.size() + " enemies.");
    }

    public static WeaponTemplate getWeapon(String id) { return weapons.get(id); }
    public static EnemyTemplate getEnemy(String id) { return enemies.get(id); }
    public static ArrayList<WeaponTemplate> getAllWeapons() { return new ArrayList<>(weapons.values()); }
    public static ArrayList<EnemyTemplate> getAllEnemies() { return new ArrayList<>(enemies.values()); }
}
