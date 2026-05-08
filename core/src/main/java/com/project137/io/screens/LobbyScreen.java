package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project137.io.Main;
import com.project137.io.network.LobbyInfoPacket;
import com.project137.io.network.MapDataPacket;
import com.project137.io.network.StartGamePacket;
import com.project137.io.world.DungeonMap;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class LobbyScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Label statusLabel;
    private Label ipLabel;
    private TextButton startButton;
    private boolean isHost;
    private DungeonMap receivedMap;

    public LobbyScreen(Main game, boolean isHost) {
        this.game = game;
        this.isHost = isHost;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        String initialIpText = isHost ? "Getting IP..." : "Joined Lobby";
        if (isHost) {
            try {
                initialIpText = "Host IP: " + InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                initialIpText = "Host IP: 127.0.0.1";
            }
        }

        ipLabel = new Label(initialIpText, skin);
        statusLabel = new Label(isHost ? "Waiting for players..." : "Waiting for host to start...", skin);
        
        startButton = new TextButton("Start Game", skin);
        startButton.setVisible(isHost);
        startButton.setDisabled(true);

        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isHost && !startButton.isDisabled()) {
                    game.networkManager.sendTCP(new StartGamePacket()); 
                }
            }
        });

        table.add(new Label("LOBBY", skin)).pad(20).row();
        table.add(ipLabel).pad(10).row();
        table.add(statusLabel).pad(10).row();
        if (isHost) table.add(startButton).pad(20).width(200);

        game.networkManager.setPacketListener(packet -> {
            if (packet instanceof LobbyInfoPacket info) {
                Gdx.app.postRunnable(() -> {
                    statusLabel.setText("Players: " + info.playerCount + "/4");
                    if (isHost) {
                        startButton.setDisabled(info.playerCount < 1); // 1 for solo testing
                    }
                });
            } else if (packet instanceof MapDataPacket mapPacket) {
                this.receivedMap = new DungeonMap(mapPacket.width, mapPacket.height);
                this.receivedMap.tiles = mapPacket.tiles;
                System.out.println("[Lobby] Map Data Received Early");
            } else if (packet instanceof StartGamePacket) {
                Gdx.app.postRunnable(() -> {
                    GameScreen gameScreen = new GameScreen(game);
                    if (this.receivedMap != null) {
                        gameScreen.setInitialMap(this.receivedMap);
                    }
                    game.setScreen(gameScreen);
                });
            }
        });
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
