package net.alex.game;

import com.esotericsoftware.kryonet.*;
import net.alex.game.network.Network;

import java.util.HashMap;

public class ClientManager {
    private Client client;
    private boolean connecting = false;

    private final HashMap<Integer, PlayerData> players = new HashMap<>();
    private PlayerData localPlayer;

    public ClientManager() {
        createClient();
    }

    /** Create a new KryoNet Client and register listeners */
    private void createClient() {
        client = new Client();
        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void connected(Connection c) {
                System.out.println("Connected to server.");
                Network.RegisterPlayer reg = new Network.RegisterPlayer();
                reg.name = "Player"; // TODO: replace with user input later
                client.sendTCP(reg);
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("Disconnected from server.");
                players.clear();
                localPlayer = null;
                connecting = false;
            }

            @Override
            public void received(Connection c, Object object) {
                if (object instanceof Network.WorldState) {
                    Network.WorldState state = (Network.WorldState) object;
                    for (Network.PlayerPosition pos : state.players) {
                        addOrUpdate(pos.id, pos.x, pos.y, null);
                    }

                } else if (object instanceof Network.PlayerJoined) {
                    Network.PlayerJoined joined = (Network.PlayerJoined) object;
                    System.out.println("Player joined: " + joined.name);
                    addOrUpdate(joined.id, 0, 0, joined.name);

                } else if (object instanceof Network.PlayerLeft) {
                    Network.PlayerLeft left = (Network.PlayerLeft) object;
                    System.out.println("Player left: " + left.id);
                    players.remove(left.id);

                } else if (object instanceof Network.PlayerPosition) {
                    Network.PlayerPosition pos = (Network.PlayerPosition) object;
                    PlayerData p = players.get(pos.id);
                    if (p != null) {
                        p.targetX = pos.x;
                        p.targetY = pos.y;
                    }
                }
            }
        });
    }

    /** Try connecting to a server */
    public void connect(String ip) {
        if (connecting) return;
        connecting = true;

        // If client was stopped (from dispose), make a new one
        if (client == null) {
            createClient();
        }

        new Thread(() -> {
            try {
                client.start();
                client.connect(5000, ip, Network.tcpPort, Network.udpPort);
            } catch (Exception e) {
                e.printStackTrace();
                connecting = false;
            }
        }).start();
    }

    /** Cleanly disconnect from server (can reconnect later) */
    public void disconnect() {
        if (client != null && client.isConnected()) {
            client.close(); // closes connection, server gets disconnect
        }
        players.clear();
        localPlayer = null;
        connecting = false;
    }

    /** Dispose when shutting down the whole game (LibGDX lifecycle) */
    public void dispose() {
        if (client != null) {
            client.stop(); // fully stop threads
            client = null; // force recreation next time
        }
        players.clear();
        localPlayer = null;
        connecting = false;
    }

    /** Send local player position to server */
    public void sendPosition(float x, float y) {
        if (localPlayer == null) return;

        Network.PlayerPosition pos = new Network.PlayerPosition();
        pos.id = localPlayer.id;
        pos.x = x;
        pos.y = y;
        client.sendUDP(pos);

        localPlayer.x = x;
        localPlayer.y = y;
        localPlayer.targetX = x;
        localPlayer.targetY = y;
    }

    /** Interpolate remote players each frame */
    public void update(float delta) {
        for (PlayerData p : players.values()) {
            if (localPlayer == null || p.id != localPlayer.id) {
                p.update(delta);
            }
        }
    }

    private void addOrUpdate(int id, float x, float y, String name) {
        PlayerData p = players.get(id);
        if (p == null) {
            p = new PlayerData(id, (name != null ? name : "Unknown"), x, y);
            players.put(id, p);
        } else {
            p.targetX = x;
            p.targetY = y;
            if (name != null) p.name = name;
        }
    }

    public HashMap<Integer, PlayerData> getPlayers() { return players; }
    public PlayerData getLocalPlayer() { return localPlayer; }

    // === Player Data Model ===
    public static class PlayerData {
        public int id;
        public String name;

        public float x, y;
        public float targetX, targetY;

        public PlayerData(int id, String name, float x, float y) {
            this.id = id;
            this.name = name;
            this.x = this.targetX = x;
            this.y = this.targetY = y;
        }

        public void update(float delta) {
            float lerp = 10f * delta;
            if (lerp > 1f) lerp = 1f;
            x += (targetX - x) * lerp;
            y += (targetY - y) * lerp;
        }
    }
}
