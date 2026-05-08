package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project137.io.Main;
import com.project137.io.NetworkConfig;
import java.io.IOException;

import com.project137.io.network.server.ServerNetworkManager;

public class MenuScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private Skin skin;

    public MenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        TextButton hostButton = new TextButton("Host Game", skin);
        TextButton joinButton = new TextButton("Join Game", skin);

        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Start Internal Server
                ServerNetworkManager server = new ServerNetworkManager();
                server.start();
                
                try {
                    game.networkManager.connect(NetworkConfig.IP);
                    game.setScreen(new GameScreen(game));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    game.networkManager.connect(NetworkConfig.IP);
                    game.setScreen(new GameScreen(game));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        table.add(hostButton).pad(10).width(200);
        table.row();
        table.add(joinButton).pad(10).width(200);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
