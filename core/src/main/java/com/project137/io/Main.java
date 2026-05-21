package com.project137.io;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.project137.io.network.ClientNetworkManager;
import com.project137.io.screens.MenuScreen;

public class Main extends Game {
    public SpriteBatch batch;
    public ClientNetworkManager networkManager;
    public com.project137.io.network.server.ServerNetworkManager localServer;
    public AssetManager assetManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assetManager = new AssetManager();
        networkManager = new ClientNetworkManager();

        // Queue the atlas for loading
        assetManager.load("atlas/game.atlas", TextureAtlas.class);
        assetManager.finishLoading();

        setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // Continue loading assets
        if (assetManager.update()) {
            super.render();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        assetManager.dispose();
        networkManager.disconnect();
        if (localServer != null) {
            localServer.stop();
        }
    }
}

