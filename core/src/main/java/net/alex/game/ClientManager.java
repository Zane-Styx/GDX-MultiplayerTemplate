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
        client = new Client();
        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void connected(Connection c) {
                System.out.println("Connected to server.");
                Network.RegisterPlayer reg = new Network.RegisterPlayer();
                reg.name = "Player_" + c.getID(); // could replace with UI input
                client.sendTCP(reg);
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("Disconnected from server.");
                players.clear();
            }

            @Override
            public void received(Connection c, Object object) {
                if (object instanceof Network.WorldState) {
                    Network.WorldState state = (Network.WorldState) object;
                    for (Network.PlayerPosition pos : state.players) {
                        addOrUpdate(pos.id, pos.x, pos.y, "Unknown");
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
                    addOrUpdate(pos.id, pos.x, pos.y, null);
                }
            }
        });
    }

    /** Try connecting to server */
    public void connect(String ip) {
        if (connecting) return;
        connecting = true;

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

    /** Send movement to server */
    public void sendPosition(float x, float y) {
        if (localPlayer == null) return;

        Network.PlayerPosition pos = new Network.PlayerPosition();
        pos.id = localPlayer.id;
        pos.x = x;
        pos.y = y;
        client.sendUDP(pos);

        localPlayer.x = x;
        localPlayer.y = y;
    }

    /** Called when WorldState/Joined/Position is received */
    private void addOrUpdate(int id, float x, float y, String name) {
        PlayerData p = players.get(id);
        if (p == null) {
            p = new PlayerData(id, name != null ? name : "Unknown", x, y);
            players.put(id, p);
            if (localPlayer == null || id == client.getID()) {
                localPlayer = p; // assign local player
            }
        } else {
            p.x = x;
            p.y = y;
            if (name != null) p.name = name;
        }
    }

    public HashMap<Integer, PlayerData> getPlayers() {
        return players;
    }

    public PlayerData getLocalPlayer() {
        return localPlayer;
    }

    public void dispose() {
        client.stop();
    }

    // === Player Data Model ===
    public static class PlayerData {
        public int id;
        public String name;
        public float x, y;

        public PlayerData(int id, String name, float x, float y) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }
}
