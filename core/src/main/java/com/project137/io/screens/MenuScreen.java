package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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
    private com.badlogic.gdx.graphics.Texture background;
    private BitmapFont customFont;

    public MenuScreen(Main game) {
        this.game = game;
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
        TextField.TextFieldStyle tfs = skin.get(TextField.TextFieldStyle.class);
        tfs.font = customFont;

        background = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("textures/menu_bg.jpg"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("PROJECT-137", skin);
        title.setFontScale(2.0f);
        title.setColor(Color.GOLD);

        
        TextButton hostButton = new TextButton("Host Game", skin);
        
        Label ipLabel = new Label("Enter Host IP:", skin);
        final TextField ipField = new TextField("127.0.0.1", skin);
        ipField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.FocusListener() {
            @Override
            public void keyboardFocusChanged(com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor, boolean focused) {
                if (focused) {
                    ipField.selectAll();
                }
            }
        });
        TextButton joinButton = new TextButton("Join Game", skin);
        
        errorLabel = new Label("", skin);
        errorLabel.setColor(com.badlogic.gdx.graphics.Color.RED);

        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                errorLabel.setText("Starting Server...");
                new Thread(() -> {
                    try {
                        if (game.localServer != null) {
                            game.localServer.stop();
                            game.localServer = null;
                        }
                        game.networkManager.disconnect();

                        ServerNetworkManager server = new ServerNetworkManager();
                        server.start();
                        game.localServer = server;

                        // Retry loop for local connection
                        boolean connected = false;
                        for (int i = 0; i < 10; i++) {
                            try {
                                game.networkManager.connect("127.0.0.1");
                                connected = true;
                                break;
                            } catch (IOException e) {
                                Thread.sleep(200);
                            }
                        }

                        if (connected) {
                            Gdx.app.postRunnable(() -> game.setScreen(new LobbyScreen(game, true)));
                        } else {
                            Gdx.app.postRunnable(() -> errorLabel.setText("Failed to connect to local server."));
                        }
                    } catch (Exception e) {
                        Gdx.app.postRunnable(() -> errorLabel.setText("Failed to host server locally."));
                        e.printStackTrace();
                    }
                }).start();
            }
        });

        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String ip = ipField.getText();
                errorLabel.setText("Connecting to " + ip + "...");
                new Thread(() -> {
                    try {
                        if (game.localServer != null) {
                            game.localServer.stop();
                            game.localServer = null;
                        }
                        game.networkManager.disconnect();

                        game.networkManager.connect(ip);
                        Gdx.app.postRunnable(() -> game.setScreen(new LobbyScreen(game, false)));
                    } catch (IOException e) {
                        Gdx.app.postRunnable(() -> errorLabel.setText("Error: Could not connect to host."));
                    }
                }).start();
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
        if (customFont != null) customFont.dispose();
    }
}
