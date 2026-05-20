package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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

public class LobbyScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Label statusLabel;
    private Label ipLabel;
    private TextButton startButton;
    private boolean isHost;
    private DungeonMap receivedMap;
    private com.badlogic.gdx.graphics.Texture background;
    private BitmapFont customFont;

    public LobbyScreen(Main game, boolean isHost) {
        this.game = game;
        this.isHost = isHost;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        // Generate and inject custom font into the skin
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/InkSlime.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 18;
        parameter.color = Color.WHITE;
        parameter.borderWidth = 1.2f;
        parameter.borderColor = Color.BLACK;
        customFont = generator.generateFont(parameter);
        generator.dispose();

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.add("default-font", customFont, BitmapFont.class);

        // Update styles to use the new font
        Label.LabelStyle ls = skin.get(Label.LabelStyle.class);
        ls.font = customFont;
        TextButton.TextButtonStyle tbs = skin.get(TextButton.TextButtonStyle.class);
        tbs.font = customFont;

        background = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("textures/menu_bg.jpg"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        String initialIpText = isHost ? "Getting IP..." : "Joined Lobby";
        if (isHost) {
            try {
                // Better local IP detection
                java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                String bestIp = null;
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;
                    java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        if (addr instanceof java.net.Inet4Address) {
                            bestIp = addr.getHostAddress();
                            break;
                        }
                    }
                }
                initialIpText = "Host IP: " + (bestIp != null ? bestIp : "127.0.0.1");
            } catch (Exception e) {
                initialIpText = "Host IP: 127.0.0.1";
            }
        }

        Label titleLabel = new Label("LOBBY", skin);
        titleLabel.setFontScale(1.8f);
        titleLabel.setColor(Color.GOLD);

        ipLabel = new Label(initialIpText, skin);
        ipLabel.setColor(Color.CYAN);

        statusLabel = new Label(isHost ? "Waiting for players..." : "Waiting for host to start...", skin);
        statusLabel.setColor(Color.LIGHT_GRAY);
        
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

        table.add(titleLabel).pad(20).row();
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
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        game.batch.draw(background, 0, 0, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (background != null) background.dispose();
    }
}
