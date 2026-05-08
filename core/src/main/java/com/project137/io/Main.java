package com.project137.io;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.project137.io.network.ClientNetworkManager;
import com.project137.io.screens.MenuScreen;

public class Main extends Game {
    public SpriteBatch batch;
    public ClientNetworkManager networkManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        networkManager = new ClientNetworkManager();
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
        networkManager.disconnect();
    }
}
