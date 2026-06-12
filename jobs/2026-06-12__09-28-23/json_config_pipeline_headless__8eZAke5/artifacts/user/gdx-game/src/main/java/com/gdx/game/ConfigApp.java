package com.gdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigApp extends ApplicationAdapter {
    private final String configPath;
    private final AtomicBoolean success;
    private final AtomicReference<String> errorMessage;
    private final CountDownLatch latch;

    public ConfigApp(String configPath, AtomicBoolean success, AtomicReference<String> errorMessage, CountDownLatch latch) {
        this.configPath = configPath;
        this.success = success;
        this.errorMessage = errorMessage;
        this.latch = latch;
    }

    @Override
    public void create() {
        try {
            // 1. Obtain FileHandle via Gdx.files.absolute
            FileHandle file = Gdx.files.absolute(configPath);
            if (!file.exists()) {
                throw new IllegalArgumentException("file does not exist");
            }
            if (file.isDirectory()) {
                throw new IllegalArgumentException("path is a directory");
            }

            // 2. Parse file contents with JsonReader
            JsonValue root;
            try {
                JsonReader reader = new JsonReader();
                root = reader.parse(file);
            } catch (Exception e) {
                throw new IllegalArgumentException("not valid JSON");
            }

            if (root == null) {
                throw new IllegalArgumentException("root is null");
            }
            if (!root.isObject()) {
                throw new IllegalArgumentException("root is not a JSON object");
            }

            // 3. Validate and Parse "game"
            JsonValue gameVal = root.get("game");
            if (gameVal == null) {
                throw new IllegalArgumentException("missing required field 'game'");
            }
            if (!gameVal.isString()) {
                throw new IllegalArgumentException("field 'game' has wrong JSON type");
            }
            String game = gameVal.asString();
            if (game == null || game.isEmpty()) {
                throw new IllegalArgumentException("'game' is empty");
            }

            // 4. Validate and Parse "levels"
            JsonValue levelsVal = root.get("levels");
            if (levelsVal == null) {
                throw new IllegalArgumentException("missing required field 'levels'");
            }
            if (!levelsVal.isArray()) {
                throw new IllegalArgumentException("field 'levels' has wrong JSON type");
            }

            int levelCount = 0;
            int totalEnemyCount = 0;
            long totalHp = 0;
            String strongestEnemyType = null;
            long strongestEnemyHp = -1;

            for (JsonValue levelVal : levelsVal) {
                levelCount++;
                if (levelVal == null || !levelVal.isObject()) {
                    throw new IllegalArgumentException("level is not a JSON object");
                }

                JsonValue nameVal = levelVal.get("name");
                if (nameVal == null) {
                    throw new IllegalArgumentException("missing required field 'name'");
                }
                if (!nameVal.isString()) {
                    throw new IllegalArgumentException("field 'name' has wrong JSON type");
                }
                String name = nameVal.asString();
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("'name' is empty");
                }

                JsonValue enemiesVal = levelVal.get("enemies");
                if (enemiesVal == null) {
                    throw new IllegalArgumentException("missing required field 'enemies'");
                }
                if (!enemiesVal.isArray()) {
                    throw new IllegalArgumentException("field 'enemies' has wrong JSON type");
                }

                for (JsonValue enemyVal : enemiesVal) {
                    totalEnemyCount++;
                    if (enemyVal == null || !enemyVal.isObject()) {
                        throw new IllegalArgumentException("enemy is not a JSON object");
                    }

                    JsonValue typeVal = enemyVal.get("type");
                    if (typeVal == null) {
                        throw new IllegalArgumentException("missing required field 'type'");
                    }
                    if (!typeVal.isString()) {
                        throw new IllegalArgumentException("field 'type' has wrong JSON type");
                    }
                    String type = typeVal.asString();
                    if (type == null || type.isEmpty()) {
                        throw new IllegalArgumentException("'type' is empty");
                    }

                    JsonValue hpVal = enemyVal.get("hp");
                    if (hpVal == null) {
                        throw new IllegalArgumentException("missing required field 'hp'");
                    }
                    if (!hpVal.isNumber()) {
                        throw new IllegalArgumentException("field 'hp' has wrong JSON type");
                    }
                    
                    double hpDouble = hpVal.asDouble();
                    if (hpDouble < 0) {
                        throw new IllegalArgumentException("'hp' value is negative");
                    }
                    if (hpDouble != Math.floor(hpDouble) || Double.isInfinite(hpDouble) || Double.isNaN(hpDouble)) {
                        throw new IllegalArgumentException("field 'hp' has wrong JSON type");
                    }

                    long hp = hpVal.asLong();

                    totalHp += hp;

                    if (hp > strongestEnemyHp) {
                        strongestEnemyHp = hp;
                        strongestEnemyType = type;
                    }
                }
            }

            // All validations and calculations succeeded
            String strongestEnemyAnswer = "none";
            if (strongestEnemyType != null) {
                strongestEnemyAnswer = strongestEnemyType + " (" + strongestEnemyHp + ")";
            }

            System.out.println("Game: " + game);
            System.out.println("Levels: " + levelCount);
            System.out.println("Enemies: " + totalEnemyCount);
            System.out.println("Total HP: " + totalHp);
            System.out.println("Strongest enemy: " + strongestEnemyAnswer);

            success.set(true);
        } catch (IllegalArgumentException e) {
            errorMessage.set(e.getMessage());
            success.set(false);
        } catch (Exception e) {
            errorMessage.set(e.getMessage() != null ? e.getMessage() : e.toString());
            success.set(false);
        } finally {
            latch.countDown();
            Gdx.app.exit();
        }
    }
}
