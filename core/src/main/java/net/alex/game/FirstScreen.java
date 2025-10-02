package net.alex.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import net.alex.game.network.Network;

public class FirstScreen implements Screen {
    private final MainGame game;
    private Stage stage;
    private Skin skin;
    private TextField ipField;
    private TextButton joinButton;

    private ClientManager clientManager;

    public FirstScreen(MainGame game) {
        this.game = game;
        this.clientManager = new ClientManager();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("skin/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        ipField = new TextField("127.0.0.1", skin);
        ipField.setMessageText("Enter server IP");

        joinButton = new TextButton("Join", skin);
        joinButton.addListener(event -> {
            if (!joinButton.isPressed()) return false;
            clientManager.connect(ipField.getText());
            return true;
        });

        table.add(ipField).width(200).pad(10);
        table.row();
        table.add(joinButton).pad(10);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        // Example: draw all players (replace with your renderer)
        for (ClientManager.PlayerData player : clientManager.getPlayers().values()) {
            System.out.println("Player " + player.id + " at " + player.x + "," + player.y);
        }
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); skin.dispose(); clientManager.dispose(); }
}
