package net.alex.game;

import com.esotericsoftware.kryonet.*;
import net.alex.game.network.Network;
import java.io.IOException;
import java.util.HashMap;

public class ServerMain {
    private static HashMap<Integer, Player> players = new HashMap<>();
    private static Server server;

    public static void main(String[] args) throws IOException {
        server = new Server();
        Network.register(server);

        server.addListener(new Listener() {
            @Override
            public void connected(Connection c) {
                System.out.println("New connection: " + c.getID());
                players.put(c.getID(), new Player(c.getID()));
            }

            @Override
            public void disconnected(Connection c) {
                Player removed = players.remove(c.getID());
                if (removed != null) {
                    System.out.println("Player " + removed.name + " left.");
                    Network.PlayerLeft msg = new Network.PlayerLeft();
                    msg.id = removed.id;
                    server.sendToAllTCP(msg);
                }
            }

            @Override
            public void received(Connection c, Object object) {
                if (object instanceof Network.RegisterPlayer) {
                    Network.RegisterPlayer reg = (Network.RegisterPlayer) object;

                    Player player = players.get(c.getID());
                    player.name = reg.name;

                    // Send existing world state to new player
                    Network.WorldState world = new Network.WorldState();
                    world.players = players.values().stream().map(p -> {
                        Network.PlayerPosition pos = new Network.PlayerPosition();
                        pos.id = p.id;
                        pos.x = p.x;
                        pos.y = p.y;
                        return pos;
                    }).toArray(Network.PlayerPosition[]::new);
                    server.sendToTCP(c.getID(), world);

                    // Broadcast PlayerJoined to others
                    Network.PlayerJoined joined = new Network.PlayerJoined();
                    joined.id = player.id;
                    joined.name = player.name;
                    server.sendToAllTCP(joined);

                    System.out.println("Registered: " + reg.name);

                } else if (object instanceof Network.PlayerPosition) {
                    Network.PlayerPosition pos = (Network.PlayerPosition) object;

                    Player player = players.get(c.getID());
                    if (player != null) {
                        player.x = pos.x;
                        player.y = pos.y;
                    }

                    // Broadcast new position via UDP
                    server.sendToAllUDP(pos);
                }
            }
        });

        server.bind(Network.tcpPort, Network.udpPort);
        server.start();
        System.out.println("Server running on TCP:" + Network.tcpPort + " UDP:" + Network.udpPort);
    }

    public static class Player {
        public int id;
        public String name;
        public float x, y;

        public Player(int id) {
            this.id = id;
        }
    }
}
