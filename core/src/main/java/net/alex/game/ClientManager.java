package net.alex.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.esotericsoftware.kryonet.*;
import net.alex.game.network.Network;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ClientManager {
    private Client client;
    private boolean connecting = false;
    private volatile boolean connected = false;
    private boolean readyToEnterGame = false;

    private final HashMap<Integer, PlayerData> players = new HashMap<>();
    private PlayerData localPlayer;

    private Profile profile; // persistent profile
    private final Json json = new Json();
    private final File profileFile = new File("profile.json");

    public ClientManager() {
        loadProfile();
        setupClient();
    }

    /** Called once when creating client, can be reused for reconnect */
    private void setupClient() {
        client = new Client();
        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void connected(Connection c) {
                System.out.println("✅ Connected to server.");

                connected = true;
                connecting = false;

                Network.RegisterPlayer reg = new Network.RegisterPlayer();
                reg.id = profile.id;
                reg.name = profile.name;
                client.sendTCP(reg);

                // safely switch screen from LibGDX render thread
                Gdx.app.postRunnable(() -> readyToEnterGame = true);
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("⚠️ Disconnected from server.");
                players.clear();
                localPlayer = null;
                connected = false;
                connecting = false;

                try { client.stop(); } catch (Exception ignored) {}

                // Go back to first screen safely
                Gdx.app.postRunnable(() -> {
                    if (Gdx.app.getApplicationListener() instanceof MainGame game) {
                        game.setScreen(new FirstScreen(game));
                    }
                });
            }

            @Override
            public void received(Connection c, Object object) {
                if (object instanceof Network.AssignId assign) {
                    profile.id = assign.id;
                    saveProfile();
                    System.out.println("Assigned permanent ID: " + assign.id);

                } else if (object instanceof Network.WorldState state) {
                    players.clear();
                    for (Network.PlayerUpdate u : state.players) {
                        addOrUpdate(u.id, u.x, u.y, "Unknown", u.shape);
                    }

                } else if (object instanceof Network.PlayerJoined joined) {
                    System.out.println("Player joined: " + joined.name);
                    addOrUpdate(joined.id, 0, 0, joined.name, 1);

                } else if (object instanceof Network.PlayerLeft left) {
                    System.out.println("Player left: " + left.id);
                    players.remove(left.id);

                } else if (object instanceof Network.PlayerUpdate u) {
                    addOrUpdate(u.id, u.x, u.y, null, u.shape);
                }
            }
        });
    }

    // === Profile Persistence ===
    private void loadProfile() {
        try {
            if (profileFile.exists()) {
                profile = json.fromJson(Profile.class, new FileReader(profileFile));
            } else {
                profile = new Profile();
                profile.id = 0; // new player
                profile.name = "Player_" + System.currentTimeMillis();
                saveProfile();
            }
        } catch (Exception e) {
            e.printStackTrace();
            profile = new Profile();
            profile.id = 0;
            profile.name = "Player_" + System.currentTimeMillis();
        }
    }

    private void saveProfile() {
        try (FileWriter writer = new FileWriter(profileFile)) {
            writer.write(json.prettyPrint(profile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Profile {
        public int id;
        public String name;
    }

    // === Networking ===
    public synchronized boolean connect(String ip, String playerName) {
        if (connecting || connected) {
            System.out.println("⚠️ Already connecting or connected. Ignoring new attempt.");
            return false;
        }

        connecting = true;
        profile.name = playerName;
        saveProfile();

        new Thread(() -> {
            try {
                if (client == null || !client.isConnected()) setupClient();
                client.start();

                System.out.println("🌐 Connecting to server at " + ip + "...");
                client.connect(5000, ip, Network.tcpPort, Network.udpPort);

            } catch (Exception e) {
                System.out.println("❌ Connection failed: " + e.getMessage());
            } finally {
                // even on failure, reset so we can retry later
                connecting = false;
            }
        }, "ClientConnectThread").start();

        return true;
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.close(); // clean disconnect
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            players.clear();
            localPlayer = null;
            connecting = false;
        }
    }

    public void sendPlayerUpdate(PlayerData me) {
        if (me == null) return;
        Network.PlayerUpdate packet = new Network.PlayerUpdate();
        packet.id = me.id;
        packet.x = me.x;
        packet.y = me.y;
        packet.shape = me.shape;
        client.sendUDP(packet);

        // update local instantly
        localPlayer.x = me.x;
        localPlayer.y = me.y;
        localPlayer.shape = me.shape;
    }

    public void update(float delta) {
        for (PlayerData p : players.values()) {
            if (localPlayer == null || p.id != localPlayer.id) {
                p.update(delta);
            }
        }
    }

    private void addOrUpdate(int id, float x, float y, String name, int shape) {
        PlayerData p = players.get(id);
        if (p == null) {
            p = new PlayerData(id, name, x, y, shape);
            players.put(id, p);
            if (id == profile.id) {
                localPlayer = p;
            }
        } else {
            p.targetX = x;
            p.targetY = y;
            if (name != null) p.name = name;
            p.shape = shape;
        }
    }

    public HashMap<Integer, PlayerData> getPlayers() { return players; }
    public PlayerData getLocalPlayer() { return localPlayer; }
    public int getPlayerId() { return profile.id; }
    public boolean isConnected() {
        return connected && client != null && client.isConnected();
    }
    public boolean isReadyToEnterGame() { return readyToEnterGame; }
    public void setReadyToEnterGame(boolean ready) { this.readyToEnterGame = ready; }

    public void dispose() {
        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === PlayerData ===
    public static class PlayerData {
        public int id;
        public String name;
        public float x, y;
        public float targetX, targetY;
        public int shape = 1; // default: triangle

        public PlayerData(int id, String name, float x, float y, int shape) {
            this.id = id;
            this.name = name;
            this.x = this.targetX = x;
            this.y = this.targetY = y;
            this.shape = shape;
        }

        public void update(float delta) {
            float lerp = 10f * delta;
            if (lerp > 1f) lerp = 1f;
            x += (targetX - x) * lerp;
            y += (targetY - y) * lerp;
        }
    }
}
