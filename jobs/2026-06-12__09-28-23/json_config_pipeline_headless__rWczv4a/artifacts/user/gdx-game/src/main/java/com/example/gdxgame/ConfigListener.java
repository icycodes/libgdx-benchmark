package com.example.gdxgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.concurrent.CountDownLatch;

public class ConfigListener extends ApplicationAdapter {

    private final String configPath;
    private volatile boolean error;
    private final CountDownLatch exitLatch;

    public ConfigListener(String configPath) {
        this.configPath = configPath;
        this.error = false;
        this.exitLatch = new CountDownLatch(1);
    }

    public boolean hasError() {
        return error;
    }

    public void awaitExit() {
        try {
            exitLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void create() {
        try {
            processConfig();
        } catch (ConfigException e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            error = true;
        } catch (Exception e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            error = true;
        }
        Gdx.app.exit();
    }

    @Override
    public void dispose() {
        exitLatch.countDown();
    }

    private void processConfig() throws ConfigException {
        FileHandle file = Gdx.files.absolute(configPath);
        if (!file.exists()) {
            throw new ConfigException("file not found: " + configPath);
        }

        String raw = file.readString("UTF-8");

        JsonValue root;
        try {
            root = new JsonReader().parse(raw);
        } catch (Exception e) {
            throw new ConfigException("malformed JSON");
        }

        if (root == null || !root.isObject()) {
            throw new ConfigException("root must be a JSON object");
        }

        // --- "game" field ---
        JsonValue gameVal = root.get("game");
        if (gameVal == null) {
            throw new ConfigException("missing required field \"game\"");
        }
        if (!gameVal.isString()) {
            throw new ConfigException("\"game\" must be a string");
        }
        String game = gameVal.asString();
        if (game.isEmpty()) {
            throw new ConfigException("\"game\" must be a non-empty string");
        }

        // --- "levels" field ---
        JsonValue levelsVal = root.get("levels");
        if (levelsVal == null) {
            throw new ConfigException("missing required field \"levels\"");
        }
        if (!levelsVal.isArray()) {
            throw new ConfigException("\"levels\" must be an array");
        }

        int levelCount = levelsVal.size;
        int totalEnemyCount = 0;
        long totalHp = 0;
        String strongestType = null;
        int strongestHp = -1;

        for (JsonValue level : levelsVal) {
            if (!level.isObject()) {
                throw new ConfigException("each level must be a JSON object");
            }

            // --- level "name" ---
            JsonValue levelNameVal = level.get("name");
            if (levelNameVal == null) {
                throw new ConfigException("missing required field \"name\" in level");
            }
            if (!levelNameVal.isString()) {
                throw new ConfigException("\"name\" in level must be a string");
            }
            String levelName = levelNameVal.asString();
            if (levelName.isEmpty()) {
                throw new ConfigException("\"name\" in level must be a non-empty string");
            }

            // --- level "enemies" ---
            JsonValue enemiesVal = level.get("enemies");
            if (enemiesVal == null) {
                throw new ConfigException("missing required field \"enemies\" in level \"" + levelName + "\"");
            }
            if (!enemiesVal.isArray()) {
                throw new ConfigException("\"enemies\" in level \"" + levelName + "\" must be an array");
            }

            for (JsonValue enemy : enemiesVal) {
                if (!enemy.isObject()) {
                    throw new ConfigException("each enemy must be a JSON object");
                }

                // --- enemy "type" ---
                JsonValue typeVal = enemy.get("type");
                if (typeVal == null) {
                    throw new ConfigException("missing required field \"type\" in enemy");
                }
                if (!typeVal.isString()) {
                    throw new ConfigException("\"type\" in enemy must be a string");
                }
                String type = typeVal.asString();
                if (type.isEmpty()) {
                    throw new ConfigException("\"type\" in enemy must be a non-empty string");
                }

                // --- enemy "hp" ---
                JsonValue hpVal = enemy.get("hp");
                if (hpVal == null) {
                    throw new ConfigException("missing required field \"hp\" in enemy \"" + type + "\"");
                }
                if (!hpVal.isNumber()) {
                    throw new ConfigException("\"hp\" in enemy \"" + type + "\" must be a number");
                }
                int hp = hpVal.asInt();
                if (hp < 0) {
                    throw new ConfigException("\"hp\" in enemy \"" + type + "\" must be non-negative");
                }

                totalEnemyCount++;
                totalHp += hp;

                if (hp > strongestHp) {
                    strongestHp = hp;
                    strongestType = type;
                }
            }
        }

        // --- print summary ---
        System.out.println("Game: " + game);
        System.out.println("Levels: " + levelCount);
        System.out.println("Enemies: " + totalEnemyCount);
        System.out.println("Total HP: " + totalHp);

        if (strongestType == null) {
            System.out.println("Strongest enemy: none");
        } else {
            System.out.println("Strongest enemy: " + strongestType + " (" + strongestHp + ")");
        }
    }

    private static class ConfigException extends Exception {
        ConfigException(String message) {
            super(message);
        }
    }
}
