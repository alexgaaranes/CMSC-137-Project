package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project137.io.Main;
import com.project137.io.network.server.ServerNetworkManager;
import java.io.IOException;

public class MenuScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Label errorLabel;

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

        Label title = new Label("PROJECT-137", skin);
        title.setFontScale(2.0f);
        
        TextButton hostButton = new TextButton("Host Game", skin);
        
        Label ipLabel = new Label("Enter Host IP:", skin);
        TextField ipField = new TextField("127.0.0.1", skin);
        TextButton joinButton = new TextButton("Join Game", skin);
        
        errorLabel = new Label("", skin);
        errorLabel.setColor(com.badlogic.gdx.graphics.Color.RED);

        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                errorLabel.setText("Starting Server...");
                ServerNetworkManager server = new ServerNetworkManager();
                server.start();
                
                // Allow a small delay for server to bind
                Gdx.app.postRunnable(() -> {
                    try {
                        game.networkManager.connect("127.0.0.1");
                        game.setScreen(new LobbyScreen(game, true));
                    } catch (IOException e) {
                        errorLabel.setText("Failed to host server locally.");
                        e.printStackTrace();
                    }
                });
            }
        });

        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String ip = ipField.getText();
                errorLabel.setText("Connecting to " + ip + "...");
                try {
                    game.networkManager.connect(ip);
                    game.setScreen(new LobbyScreen(game, false));
                } catch (IOException e) {
                    errorLabel.setText("Error: Could not connect to host.");
                    e.printStackTrace();
                }
            }
        });

        table.add(title).pad(20).row();
        table.add(hostButton).pad(10).width(200).row();
        table.add(new Label("--- OR ---", skin)).pad(10).row();
        table.add(ipLabel).pad(5).row();
        table.add(ipField).pad(5).width(200).row();
        table.add(joinButton).pad(10).width(200).row();
        table.add(errorLabel).pad(10);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
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
