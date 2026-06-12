package com.gdxgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * libGDX ApplicationAdapter that:
 *  1. Reads the config file via Gdx.files.absolute(path)
 *  2. Parses it with libGDX's JsonReader / JsonValue DOM
 *  3. Computes and prints a deterministic summary to stdout
 *  4. Calls Gdx.app.exit() to shut the headless loop down cleanly
 *
 * On any error it prints a single line to stderr and signals failure
 * via the volatile {@code failed} flag that the launcher reads after join().
 */
public class ConfigSummaryApp extends ApplicationAdapter {

    private final String configPath;

    /** Set to true when processing fails; read by launcher after join(). */
    volatile boolean failed = false;

    public ConfigSummaryApp(String configPath) {
        this.configPath = configPath;
    }

    @Override
    public void create() {
        try {
            processConfig();
        } catch (ConfigException e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            failed = true;
        } catch (GdxRuntimeException e) {
            // JsonReader throws GdxRuntimeException on malformed JSON
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            System.err.println("Error: invalid config: " + msg);
            failed = true;
        } catch (Exception e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            failed = true;
        } finally {
            Gdx.app.exit();
        }
    }

    // ---------------------------------------------------------------

    private void processConfig() throws ConfigException {
        // 1. Obtain FileHandle via Gdx.files.absolute
        FileHandle fh = Gdx.files.absolute(configPath);
        if (!fh.exists()) {
            throw new ConfigException("file not found: " + configPath);
        }

        // 2. Parse with libGDX JsonReader
        String text;
        try {
            text = fh.readString("UTF-8");
        } catch (GdxRuntimeException e) {
            throw new ConfigException("cannot read file: " + e.getMessage());
        }

        JsonValue root;
        try {
            root = new JsonReader().parse(text);
        } catch (GdxRuntimeException e) {
            throw new ConfigException("JSON parse error: " + e.getMessage());
        }

        if (root == null || !root.isObject()) {
            throw new ConfigException("root must be a JSON object");
        }

        // 3. Validate and extract "game"
        JsonValue gameVal = root.get("game");
        if (gameVal == null) {
            throw new ConfigException("missing field 'game'");
        }
        if (!gameVal.isString()) {
            throw new ConfigException("field 'game' must be a string");
        }
        String game = gameVal.asString();
        if (game == null || game.isEmpty()) {
            throw new ConfigException("field 'game' must not be empty");
        }

        // 4. Validate and extract "levels"
        JsonValue levelsVal = root.get("levels");
        if (levelsVal == null) {
            throw new ConfigException("missing field 'levels'");
        }
        if (!levelsVal.isArray()) {
            throw new ConfigException("field 'levels' must be a JSON array");
        }

        int levelCount = levelsVal.size;
        long totalEnemies = 0;
        long totalHp = 0;
        String strongestType = null;
        int strongestHp = Integer.MIN_VALUE;

        for (JsonValue level = levelsVal.child; level != null; level = level.next) {
            if (!level.isObject()) {
                throw new ConfigException("each level must be a JSON object");
            }

            // level "name"
            JsonValue nameVal = level.get("name");
            if (nameVal == null) {
                throw new ConfigException("level missing field 'name'");
            }
            if (!nameVal.isString()) {
                throw new ConfigException("level field 'name' must be a string");
            }
            String levelName = nameVal.asString();
            if (levelName == null || levelName.isEmpty()) {
                throw new ConfigException("level field 'name' must not be empty");
            }

            // level "enemies"
            JsonValue enemiesVal = level.get("enemies");
            if (enemiesVal == null) {
                throw new ConfigException("level '" + levelName + "' missing field 'enemies'");
            }
            if (!enemiesVal.isArray()) {
                throw new ConfigException("level '" + levelName + "' field 'enemies' must be a JSON array");
            }

            for (JsonValue enemy = enemiesVal.child; enemy != null; enemy = enemy.next) {
                if (!enemy.isObject()) {
                    throw new ConfigException("each enemy must be a JSON object");
                }

                // enemy "type"
                JsonValue typeVal = enemy.get("type");
                if (typeVal == null) {
                    throw new ConfigException("enemy missing field 'type'");
                }
                if (!typeVal.isString()) {
                    throw new ConfigException("enemy field 'type' must be a string");
                }
                String enemyType = typeVal.asString();
                if (enemyType == null || enemyType.isEmpty()) {
                    throw new ConfigException("enemy field 'type' must not be empty");
                }

                // enemy "hp"
                JsonValue hpVal = enemy.get("hp");
                if (hpVal == null) {
                    throw new ConfigException("enemy missing field 'hp'");
                }
                if (!hpVal.isNumber()) {
                    throw new ConfigException("enemy field 'hp' must be a number");
                }
                int hp = hpVal.asInt();
                if (hp < 0) {
                    throw new ConfigException("enemy field 'hp' must be non-negative, got " + hp);
                }

                totalEnemies++;
                totalHp += hp;

                // First enemy with strictly greater HP wins (array-order tie-break)
                if (strongestType == null || hp > strongestHp) {
                    strongestHp = hp;
                    strongestType = enemyType;
                }
            }
        }

        // 5. Build answer string for strongest enemy
        String answer = (strongestType == null) ? "none" : (strongestType + " (" + strongestHp + ")");

        // 6. Print exactly the five lines to stdout
        System.out.println("Game: " + game);
        System.out.println("Levels: " + levelCount);
        System.out.println("Enemies: " + totalEnemies);
        System.out.println("Total HP: " + totalHp);
        System.out.println("Strongest enemy: " + answer);
        System.out.flush();
    }

    // ---------------------------------------------------------------

    /** Checked exception used for config validation failures. */
    static class ConfigException extends Exception {
        ConfigException(String message) {
            super(message);
        }
    }
}
