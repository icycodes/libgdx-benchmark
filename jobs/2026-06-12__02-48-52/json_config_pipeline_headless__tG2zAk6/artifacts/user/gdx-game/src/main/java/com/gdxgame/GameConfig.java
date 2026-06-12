package com.gdxgame;

import java.util.concurrent.CountDownLatch;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * libGDX {@link ApplicationAdapter} that reads a game-configuration JSON file,
 * computes aggregate statistics, and prints them to stdout.
 */
public class GameConfig extends ApplicationAdapter {

    private final String configPath;
    private final CountDownLatch latch;
    private int exitCode = 0;

    public GameConfig(String configPath, CountDownLatch latch) {
        this.configPath = configPath;
        this.latch = latch;
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void create() {
        try {
            processConfig();
        } catch (ConfigException e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            exitCode = 1;
        } catch (Exception e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            exitCode = 1;
        } finally {
            Gdx.app.exit();
            latch.countDown();
        }
    }

    private void processConfig() {
        FileHandle file = Gdx.files.absolute(configPath);
        if (!file.exists()) {
            throw new ConfigException("file not found");
        }

        String text = file.readString("UTF-8");
        JsonValue root;
        try {
            root = new JsonReader().parse(text);
        } catch (Exception e) {
            throw new ConfigException("malformed JSON");
        }

        if (root == null || !root.isObject()) {
            throw new ConfigException("root must be a JSON object");
        }

        // game
        JsonValue gameVal = root.get("game");
        if (gameVal == null || !gameVal.isString()) {
            throw new ConfigException("missing or invalid 'game' field");
        }
        String game = gameVal.asString();
        if (game.isEmpty()) {
            throw new ConfigException("'game' must be non-empty");
        }

        // levels
        JsonValue levelsVal = root.get("levels");
        if (levelsVal == null || !levelsVal.isArray()) {
            throw new ConfigException("missing or invalid 'levels' field");
        }

        int levelCount = 0;
        int totalEnemies = 0;
        long totalHp = 0;
        String strongestType = null;
        long strongestHp = -1;

        for (JsonValue level : levelsVal) {
            if (!level.isObject()) {
                throw new ConfigException("each level must be a JSON object");
            }

            // level name
            JsonValue nameVal = level.get("name");
            if (nameVal == null || !nameVal.isString()) {
                throw new ConfigException("missing or invalid 'name' in level");
            }
            String levelName = nameVal.asString();
            if (levelName.isEmpty()) {
                throw new ConfigException("level 'name' must be non-empty");
            }

            // level enemies
            JsonValue enemiesVal = level.get("enemies");
            if (enemiesVal == null || !enemiesVal.isArray()) {
                throw new ConfigException("missing or invalid 'enemies' in level '" + levelName + "'");
            }

            for (JsonValue enemy : enemiesVal) {
                if (!enemy.isObject()) {
                    throw new ConfigException("each enemy must be a JSON object");
                }

                // type
                JsonValue typeVal = enemy.get("type");
                if (typeVal == null || !typeVal.isString()) {
                    throw new ConfigException("missing or invalid 'type' in enemy");
                }
                String type = typeVal.asString();
                if (type.isEmpty()) {
                    throw new ConfigException("enemy 'type' must be non-empty");
                }

                // hp
                JsonValue hpVal = enemy.get("hp");
                if (hpVal == null || !hpVal.isNumber()) {
                    throw new ConfigException("missing or invalid 'hp' in enemy '" + type + "'");
                }
                long hp = hpVal.asLong();
                if (hp < 0) {
                    throw new ConfigException("'hp' must be non-negative in enemy '" + type + "'");
                }

                totalEnemies++;
                totalHp += hp;

                // "first enemy whose hp is strictly greater than the current best wins"
                if (hp > strongestHp) {
                    strongestHp = hp;
                    strongestType = type;
                }
            }

            levelCount++;
        }

        // Print summary
        System.out.println("Game: " + game);
        System.out.println("Levels: " + levelCount);
        System.out.println("Enemies: " + totalEnemies);
        System.out.println("Total HP: " + totalHp);
        if (strongestType != null) {
            System.out.println("Strongest enemy: " + strongestType + " (" + strongestHp + ")");
        } else {
            System.out.println("Strongest enemy: none");
        }
    }

    /** Unchecked exception used to short-circuit validation. */
    private static class ConfigException extends RuntimeException {
        ConfigException(String message) {
            super(message);
        }
    }
}