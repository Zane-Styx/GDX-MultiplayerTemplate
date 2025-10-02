package net.alex.game.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {
    public static final int tcpPort = 54555;
    public static final int udpPort = 54777;

    // Packets
    public static class RegisterPlayer {
        public String name;
    }

    public static class PlayerJoined {
        public int id;
        public String name;
    }

    public static class PlayerLeft {
        public int id;
    }

    public static class PlayerPosition {
        public int id;
        public float x, y;
    }

    public static class WorldState {
        public PlayerPosition[] players;
    }

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(RegisterPlayer.class);
        kryo.register(PlayerJoined.class);
        kryo.register(PlayerLeft.class);
        kryo.register(PlayerPosition.class);
        kryo.register(WorldState.class);
        kryo.register(PlayerPosition[].class); // arrays must be registered
    }
}
