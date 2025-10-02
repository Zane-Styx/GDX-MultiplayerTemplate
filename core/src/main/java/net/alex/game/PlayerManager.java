package net.alex.game;

import java.util.HashMap;
import net.alex.game.network.Network;

public class PlayerManager {
    private final HashMap<Integer, PlayerData> players = new HashMap<>();

    public void addOrUpdate(int id, float x, float y, String name) {
        PlayerData p = players.get(id);
        if (p == null) {
            p = new PlayerData(id, name, x, y);
            players.put(id, p);
        } else {
            p.x = x;
            p.y = y;
        }
    }

    public void remove(int id) {
        players.remove(id);
    }

    public HashMap<Integer, PlayerData> getPlayers() {
        return players;
    }

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
