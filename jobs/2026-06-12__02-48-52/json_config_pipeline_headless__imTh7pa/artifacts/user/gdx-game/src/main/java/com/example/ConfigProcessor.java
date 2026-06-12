package com.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.concurrent.CountDownLatch;

public class ConfigProcessor extends ApplicationAdapter {
    private final String configPath;
    private final CountDownLatch latch;
    private final int[] exitCode;

    public ConfigProcessor(String configPath, CountDownLatch latch, int[] exitCode) {
        this.configPath = configPath;
        this.latch = latch;
        this.exitCode = exitCode;
    }

    @Override
    public void create() {
        try {
            processConfig();
        } catch (Exception e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            exitCode[0] = 1;
        } finally {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        latch.countDown();
    }

    private void processConfig() {
        FileHandle file = Gdx.files.absolute(configPath);
        if (!file.exists()) {
            throw new RuntimeException("file does not exist");
        }

        JsonReader reader = new JsonReader();
        JsonValue root;
        try {
            root = reader.parse(file);
        } catch (Exception e) {
            throw new RuntimeException("invalid JSON");
        }

        if (root == null || !root.isObject()) {
            throw new RuntimeException("root must be a JSON object");
        }

        if (!root.has("game")) {
            throw new RuntimeException("missing 'game' field");
        }
        JsonValue gameVal = root.get("game");
        if (!gameVal.isString()) {
            throw new RuntimeException("'game' must be a string");
        }
        String game = gameVal.asString();
        if (game.isEmpty()) {
            throw new RuntimeException("'game' cannot be empty");
        }

        if (!root.has("levels")) {
            throw new RuntimeException("missing 'levels' field");
        }
        JsonValue levelsVal = root.get("levels");
        if (!levelsVal.isArray()) {
            throw new RuntimeException("'levels' must be an array");
        }

        int levelCount = 0;
        long totalEnemyCount = 0;
        long totalHp = 0;
        String strongestEnemyType = null;
        long strongestEnemyHp = -1;

        for (JsonValue level : levelsVal) {
            levelCount++;
            if (!level.isObject()) {
                throw new RuntimeException("level must be an object");
            }
            if (!level.has("name")) {
                throw new RuntimeException("missing 'name' in level");
            }
            JsonValue nameVal = level.get("name");
            if (!nameVal.isString()) {
                throw new RuntimeException("'name' must be a string");
            }
            String name = nameVal.asString();
            if (name.isEmpty()) {
                throw new RuntimeException("'name' cannot be empty");
            }

            if (!level.has("enemies")) {
                throw new RuntimeException("missing 'enemies' in level");
            }
            JsonValue enemiesVal = level.get("enemies");
            if (!enemiesVal.isArray()) {
                throw new RuntimeException("'enemies' must be an array");
            }

            for (JsonValue enemy : enemiesVal) {
                totalEnemyCount++;
                if (!enemy.isObject()) {
                    throw new RuntimeException("enemy must be an object");
                }
                if (!enemy.has("type")) {
                    throw new RuntimeException("missing 'type' in enemy");
                }
                JsonValue typeVal = enemy.get("type");
                if (!typeVal.isString()) {
                    throw new RuntimeException("'type' must be a string");
                }
                String type = typeVal.asString();
                if (type.isEmpty()) {
                    throw new RuntimeException("'type' cannot be empty");
                }

                if (!enemy.has("hp")) {
                    throw new RuntimeException("missing 'hp' in enemy");
                }
                JsonValue hpVal = enemy.get("hp");
                if (!hpVal.isNumber()) {
                    throw new RuntimeException("'hp' must be a number");
                }
                long hp = hpVal.asLong();
                if (hp < 0) {
                    throw new RuntimeException("'hp' cannot be negative");
                }

                totalHp += hp;

                if (hp > strongestEnemyHp) {
                    strongestEnemyHp = hp;
                    strongestEnemyType = type;
                }
            }
        }

        String answer = strongestEnemyType != null ? (strongestEnemyType + " (" + strongestEnemyHp + ")") : "none";

        System.out.println("Game: " + game);
        System.out.println("Levels: " + levelCount);
        System.out.println("Enemies: " + totalEnemyCount);
        System.out.println("Total HP: " + totalHp);
        System.out.println("Strongest enemy: " + answer);
    }
}
