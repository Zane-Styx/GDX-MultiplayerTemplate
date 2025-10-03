package net.alex.game.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {
    public static final int tcpPort = 54555;
    public static final int udpPort = 54777;

    // === Packets ===
    public static class RegisterPlayer {
        public int id;   // 0 if new player
        public String name;
    }

    public static class AssignId {
        public int id;   // permanent player ID
    }

    public static class PlayerJoined {
        public int id;
        public String name;
    }

    public static class PlayerLeft {
        public int id;
    }

    // Old simple pos update
    public static class PlayerPosition {
        public int id;
        public float x, y;
    }

    // ✅ New: Full update with position + shape
    public static class PlayerUpdate {
        public int id;
        public float x, y;
        public int shape;  // 1=triangle, 2=circle, 3=box
    }

    public static class WorldState {
        // send complete player states when a client joins
        public PlayerUpdate[] players;
    }

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(RegisterPlayer.class);
        kryo.register(AssignId.class);
        kryo.register(PlayerJoined.class);
        kryo.register(PlayerLeft.class);
        kryo.register(PlayerPosition.class);
        kryo.register(PlayerUpdate.class);
        kryo.register(WorldState.class);
        kryo.register(PlayerPosition[].class);
        kryo.register(PlayerUpdate[].class);
    }
}
