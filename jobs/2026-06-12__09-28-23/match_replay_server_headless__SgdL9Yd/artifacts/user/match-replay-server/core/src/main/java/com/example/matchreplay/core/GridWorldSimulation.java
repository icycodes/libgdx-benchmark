package com.example.matchreplay.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class GridWorldSimulation {
    private final WorldSpec world;
    private final Map<Integer, MutablePlayer> playersById = new TreeMap<>();

    public GridWorldSimulation(MatchLog matchLog) {
        this.world = matchLog.getWorld();
        for (PlayerStart start : matchLog.getPlayerStarts()) {
            playersById.put(start.getId(), new MutablePlayer(start));
        }
    }

    public void applyTick(Map<Integer, Command> commandsByPlayerId) {
        for (Map.Entry<Integer, Command> commandEntry : new TreeMap<>(commandsByPlayerId).entrySet()) {
            MutablePlayer player = playersById.get(commandEntry.getKey());
            if (player != null) {
                applyCommand(player, commandEntry.getValue());
            }
        }
    }

    public List<PlayerSnapshot> snapshotPlayers() {
        List<PlayerSnapshot> snapshots = new ArrayList<>();
        for (MutablePlayer player : playersById.values()) {
            snapshots.add(new PlayerSnapshot(player.id, player.startX, player.startY, player.x, player.y));
        }
        return Collections.unmodifiableList(snapshots);
    }

    public String canonicalStateString() {
        StringBuilder builder = new StringBuilder();
        builder.append("W=").append(world.getWidth())
                .append(";H=").append(world.getHeight())
                .append(";P=");
        boolean first = true;
        for (MutablePlayer player : playersById.values()) {
            if (!first) {
                builder.append('|');
            }
            builder.append(player.id).append(':').append(player.x).append(',').append(player.y);
            first = false;
        }
        return builder.toString();
    }

    private void applyCommand(MutablePlayer player, Command command) {
        int nextX = player.x;
        int nextY = player.y;
        switch (command) {
            case MOVE_UP:
                nextY++;
                break;
            case MOVE_DOWN:
                nextY--;
                break;
            case MOVE_LEFT:
                nextX--;
                break;
            case MOVE_RIGHT:
                nextX++;
                break;
            case NOOP:
                break;
            default:
                throw new IllegalArgumentException("Unsupported command: " + command);
        }

        if (nextX >= 0 && nextX < world.getWidth() && nextY >= 0 && nextY < world.getHeight()) {
            player.x = nextX;
            player.y = nextY;
        }
    }

    private static final class MutablePlayer {
        private final int id;
        private final int startX;
        private final int startY;
        private int x;
        private int y;

        private MutablePlayer(PlayerStart start) {
            this.id = start.getId();
            this.startX = start.getX();
            this.startY = start.getY();
            this.x = start.getX();
            this.y = start.getY();
        }
    }
}
