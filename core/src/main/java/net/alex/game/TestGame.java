package net.alex.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;

public class TestGame implements Screen {
    private final ClientManager client;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private ShapeRenderer shapeRenderer;

    // Store all players by ID
    private HashMap<Integer, ClientManager.PlayerData> players = new HashMap<>();
    private int localId; // our assigned ID

    public TestGame(ClientManager client) {
        this.client = client;
        this.localId = client.getPlayerId();
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        // Update from network
        client.update(delta);
        players = client.getPlayers();

        // Camera follows local player
        ClientManager.PlayerData me = players.get(localId);
        if (me != null) {
            camera.position.set(me.x, me.y, 0);
        }
        camera.update();

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw players
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (ClientManager.PlayerData p : players.values()) {
            switch (p.shape) {
                case 1: shapeRenderer.triangle(p.x - 10, p.y - 10, p.x + 10, p.y - 10, p.x, p.y + 15); break;
                case 2: shapeRenderer.circle(p.x, p.y, 12); break;
                case 3: shapeRenderer.rect(p.x - 10, p.y - 10, 20, 20); break;
            }
        }

        shapeRenderer.end();

        // Handle local input
        if (me != null) {
            float speed = 200 * delta;
            if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) me.y += speed;
            if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) me.y -= speed;
            if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) me.x -= speed;
            if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) me.x += speed;

            // Switch shape with 1,2,3
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_1)) me.shape = 1;
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_2)) me.shape = 2;
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_3)) me.shape = 3;

            // Send update to server
            client.sendPlayerUpdate(me);
        }
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { shapeRenderer.dispose(); }
}
