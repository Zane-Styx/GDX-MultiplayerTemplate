package net.alex.game;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.esotericsoftware.kryonet.*;
import net.alex.game.network.Network;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ServerMain {
    private static HashMap<Integer, Player> players = new HashMap<>();
    private static Server server;
    private static int nextPlayerId = 1; // permanent IDs, not tied to connection IDs
    private static final String SAVE_FILE = "players.json";

    public static void main(String[] args) throws IOException {
        // Load players from disk first
        loadPlayers();

        server = new Server();
        Network.register(server);

        server.addListener(new Listener() {
            @Override
            public void connected(Connection c) {
                System.out.println("New connection: " + c.getID());
            }

            @Override
            public void disconnected(Connection c) {
                for (Player p : players.values()) {
                    if (p.connectionId == c.getID()) {
                        p.connectionId = -1;
                        System.out.println("Player " + p.name + " disconnected.");
                        Network.PlayerLeft msg = new Network.PlayerLeft();
                        msg.id = p.id;
                        server.sendToAllTCP(msg);
                        savePlayers();
                        break;
                    }
                }
            }

            @Override
            public void received(Connection c, Object object) {
                if (object instanceof Network.RegisterPlayer) {
                    Network.RegisterPlayer reg = (Network.RegisterPlayer) object;

                    Player player;
                    if (reg.id == 0 || !players.containsKey(reg.id)) {
                        // New player
                        int newId = nextPlayerId++;
                        player = new Player(newId, reg.name, c.getID());
                        players.put(newId, player);

                        // Send back permanent ID
                        Network.AssignId assign = new Network.AssignId();
                        assign.id = newId;
                        server.sendToTCP(c.getID(), assign);

                        // Announce join
                        Network.PlayerJoined joined = new Network.PlayerJoined();
                        joined.id = newId;
                        joined.name = reg.name;
                        server.sendToAllTCP(joined);

                        System.out.println("Registered new: " + reg.name + " (" + newId + ")");
                    } else {
                        // Returning player
                        player = players.get(reg.id);
                        player.connectionId = c.getID();
                        player.name = reg.name;

                        // Announce reconnect as join
                        Network.PlayerJoined joined = new Network.PlayerJoined();
                        joined.id = player.id;
                        joined.name = player.name;
                        server.sendToAllTCP(joined);

                        System.out.println("Reconnected: " + reg.name + " (" + reg.id + ")");
                    }

                    savePlayers();

                    // Send full world state to just this client
                    Network.WorldState world = new Network.WorldState();
                    world.players = players.values().stream().map(p -> {
                        Network.PlayerUpdate u = new Network.PlayerUpdate();
                        u.id = p.id;
                        u.x = p.x;
                        u.y = p.y;
                        u.shape = p.shape;
                        return u;
                    }).toArray(Network.PlayerUpdate[]::new);
                    server.sendToTCP(c.getID(), world);

                } else if (object instanceof Network.PlayerUpdate) {
                    Network.PlayerUpdate update = (Network.PlayerUpdate) object;
                    Player player = players.get(update.id);
                    if (player != null) {
                        player.x = update.x;
                        player.y = update.y;
                        player.shape = update.shape;
                        savePlayers();
                        // Broadcast to everyone (including sender, so states stay in sync)
                        server.sendToAllUDP(update);
                    }
                }
            }
        });

        server.bind(Network.tcpPort, Network.udpPort);
        server.start();
        System.out.println("Server running on TCP:" + Network.tcpPort + " UDP:" + Network.udpPort);
    }

    // ---- Save/Load ----
    private static void savePlayers() {
        try {
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            json.setUsePrototypes(false);

            FileHandle file = new FileHandle(new File(SAVE_FILE));
            String pretty = json.prettyPrint(players);
            file.writeString(pretty, false);
            System.out.println("Saved " + players.size() + " players to " + SAVE_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadPlayers() {
        try {
            FileHandle file = new FileHandle(new File(SAVE_FILE));
            if (file.exists()) {
                Json json = new Json();
                HashMap<String, Player> raw = json.fromJson(HashMap.class, Player.class, file);

                // Convert String keys back to Integer
                players = new HashMap<>();
                for (String key : raw.keySet()) {
                    players.put(Integer.parseInt(key), raw.get(key));
                }

                int maxId = players.keySet().stream().mapToInt(i -> i).max().orElse(0);
                nextPlayerId = maxId + 1;

                System.out.println("Loaded " + players.size() + " players from " + SAVE_FILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- Player ----
    public static class Player {
        public int id;
        public int connectionId;
        public String name;
        public float x, y;
        public int shape = 1; // default triangle

        public Player() {} // needed for JSON

        public Player(int id, String name, int connectionId) {
            this.id = id;
            this.name = name;
            this.connectionId = connectionId;
        }
    }
}
