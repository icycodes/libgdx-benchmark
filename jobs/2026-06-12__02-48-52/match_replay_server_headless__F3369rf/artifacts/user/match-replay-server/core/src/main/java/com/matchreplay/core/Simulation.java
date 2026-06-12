package com.matchreplay.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, tick-based authoritative simulation of a grid world.
 *
 * <p>Each call to {@link #tick()} advances the simulation by one step,
 * applying every command registered for the current tick in ascending
 * player-id order, then clamping positions to world bounds.
 *
 * <p>This class intentionally has no libGDX backend dependency; it could
 * be used from a graphical client just as well as from the headless server.
 */
public class Simulation {

    private final int               worldWidth;
    private final int               worldHeight;
    private final List<Player>      players;
    private final List<TickCommand> commands;

    /** Index into {@code commands} pointing at the next un-applied command. */
    private int commandCursor   = 0;
    /** Number of ticks that have been executed so far. */
    private int currentTick     = 0;
    /** Running count of commands applied (NOOPs count too). */
    private int commandsApplied = 0;

    /**
     * Construct a simulation from a parsed match log.
     * The players list inside {@code log} is used directly (mutable state).
     */
    public Simulation(MatchLog log) {
        this.worldWidth  = log.worldWidth;
        this.worldHeight = log.worldHeight;
        // Copy the mutable player list so we own the reference.
        this.players = new ArrayList<>(log.players);
        // Pre-sort commands: primary key = tick, secondary key = player id.
        List<TickCommand> sorted = new ArrayList<>(log.commands);
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(a.tick, b.tick);
            if (cmp != 0) return cmp;
            return Integer.compare(a.playerId, b.playerId);
        });
        this.commands = sorted;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Apply all commands scheduled for the current tick (in ascending
     * player-id order), advance the tick counter by one, and clamp all
     * player positions to world bounds.
     */
    public void tick() {
        // Collect commands for this tick (already sorted by player id).
        while (commandCursor < commands.size()
               && commands.get(commandCursor).tick == currentTick) {
            TickCommand tc = commands.get(commandCursor);
            applyCommand(tc);
            commandsApplied++;
            commandCursor++;
        }
        currentTick++;
    }

    /** @return the current simulation tick (number of ticks completed so far). */
    public int getCurrentTick() {
        return currentTick;
    }

    /** @return total commands applied so far (all COMMAND lines for declared players). */
    public int getCommandsApplied() {
        return commandsApplied;
    }

    /** @return live player state list (sorted by id ascending). */
    public List<Player> getPlayers() {
        return players;
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private void applyCommand(TickCommand tc) {
        Player p = findPlayer(tc.playerId);
        if (p == null) return; // should not happen -- log parser already filtered

        int nx = p.x;
        int ny = p.y;

        switch (tc.command) {
            case MOVE_UP:    ny += 1; break;
            case MOVE_DOWN:  ny -= 1; break;
            case MOVE_LEFT:  nx -= 1; break;
            case MOVE_RIGHT: nx += 1; break;
            case NOOP:       break;
        }

        // Clamp to world bounds; out-of-bounds moves leave position unchanged.
        if (nx >= 0 && nx < worldWidth)  p.x = nx;
        if (ny >= 0 && ny < worldHeight) p.y = ny;
    }

    private Player findPlayer(int id) {
        for (Player p : players) {
            if (p.id == id) return p;
        }
        return null;
    }
}
