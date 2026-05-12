package com.project137.io.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project137.io.Main;
import com.project137.io.network.GameOverPacket;

public class EndGameScreen extends ScreenAdapter {
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final Label messageLabel;

    public EndGameScreen(Main game, String initialMessage) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);

        messageLabel = new Label(initialMessage, skin);
        messageLabel.setFontScale(2f);
        messageLabel.setAlignment(Align.center);
        table.add(messageLabel).padBottom(50).row();

        TextButton menuBtn = new TextButton("Return to Menu", skin);
        menuBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.networkManager.disconnect();
                game.setScreen(new MenuScreen(game));
            }
        });
        table.add(menuBtn).width(200).height(50).row();

        stage.addActor(table);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        
        // Listen for GameOverPacket if we are waiting on this screen (e.g. after dying)
        game.networkManager.setPacketListener(packet -> {
            if (packet instanceof GameOverPacket) {
                Gdx.app.postRunnable(() -> messageLabel.setText("GAME OVER!\nAll players have died."));
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
