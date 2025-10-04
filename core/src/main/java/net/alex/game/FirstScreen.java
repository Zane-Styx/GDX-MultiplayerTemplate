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

public class FirstScreen implements Screen {
    private final MainGame game;
    private Stage stage;
    private Skin skin;
    private TextField ipField;
    private TextField nameField;
    private TextButton joinButton;
    private TextButton leaveButton;

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

        // IP input
        ipField = new TextField("127.0.0.1", skin);
        ipField.setMessageText("Enter server IP");

        // Name input
        nameField = new TextField("", skin);
        nameField.setMessageText("Enter player name");

        // Join button
        joinButton = new TextButton("Join", skin);
        joinButton.addListener(event -> {
            if (!joinButton.isPressed()) return false;

            String ip = ipField.getText().trim();
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Player_" + System.currentTimeMillis();

            if (clientManager.connect(ip, name)) {
                // don’t immediately switch
                System.out.println("Connecting to server...");
            } else {
                System.out.println("Already connecting or failed to start connection.");
            }
            return true;
        });

        // Leave button
        leaveButton = new TextButton("Leave", skin);
        leaveButton.addListener(event -> {
            if (!leaveButton.isPressed()) return false;
            clientManager.disconnect(); // clean leave
            return true;
        });

        // Layout
        table.add(ipField).width(200).pad(10);
        table.row();
        table.add(nameField).width(200).pad(10);
        table.row();
        table.add(joinButton).pad(10);
        table.row();
        table.add(leaveButton).pad(10);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        clientManager.update(delta);
        stage.act(delta);
        stage.draw();

        // ✅ Safe transition once the client is really connected
        if (clientManager.isReadyToEnterGame()) {
            game.setScreen(new TestGame(clientManager));
            clientManager.setReadyToEnterGame(false);
        }
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
        if (clientManager != null) clientManager.dispose();
    }
}
