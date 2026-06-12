package com.example.gdxgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public final class ConfigSummaryApplication extends ApplicationAdapter {
    private final String configPath;
    private volatile boolean successful;

    public ConfigSummaryApplication(String configPath) {
        this.configPath = configPath;
    }

    @Override
    public void create() {
        try {
            Summary summary = readAndSummarizeConfig();
            System.out.print(summary.format());
            System.out.flush();
            successful = true;
        } catch (Exception e) {
            System.err.println("Error: invalid config: " + shortReason(e));
            System.err.flush();
            successful = false;
        } finally {
            Gdx.app.exit();
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    private Summary readAndSummarizeConfig() throws InvalidConfigException {
        FileHandle file = Gdx.files.absolute(configPath);
        if (!file.exists()) {
            throw new InvalidConfigException("file does not exist");
        }
        if (file.isDirectory()) {
            throw new InvalidConfigException("config path is a directory");
        }

        JsonValue root;
        try {
            root = new JsonReader().parse(file.readString("UTF-8"));
        } catch (Exception e) {
            throw new InvalidConfigException("invalid JSON");
        }

        if (root == null || !root.isObject()) {
            throw new InvalidConfigException("root must be an object");
        }

        String game = requiredNonEmptyString(root, "game", "game");
        JsonValue levels = requiredArray(root, "levels", "levels");

        int levelCount = 0;
        long enemyCount = 0L;
        long totalHp = 0L;
        String strongestType = null;
        long strongestHp = Long.MIN_VALUE;

        for (JsonValue level : levels) {
            levelCount++;
            if (!level.isObject()) {
                throw new InvalidConfigException("level must be an object");
            }

            requiredNonEmptyString(level, "name", "level name");
            JsonValue enemies = requiredArray(level, "enemies", "level enemies");

            for (JsonValue enemy : enemies) {
                if (!enemy.isObject()) {
                    throw new InvalidConfigException("enemy must be an object");
                }

                String type = requiredNonEmptyString(enemy, "type", "enemy type");
                long hp = requiredNonNegativeInteger(enemy, "hp", "enemy hp");

                enemyCount++;
                totalHp += hp;
                if (strongestType == null || hp > strongestHp) {
                    strongestType = type;
                    strongestHp = hp;
                }
            }
        }

        String strongest = strongestType == null ? "none" : strongestType + " (" + strongestHp + ")";
        return new Summary(game, levelCount, enemyCount, totalHp, strongest);
    }

    private static String requiredNonEmptyString(JsonValue object, String fieldName, String label) throws InvalidConfigException {
        JsonValue value = object.get(fieldName);
        if (value == null) {
            throw new InvalidConfigException("missing " + label);
        }
        if (!value.isString()) {
            throw new InvalidConfigException(label + " must be a string");
        }
        String text = value.asString();
        if (text.isEmpty()) {
            throw new InvalidConfigException(label + " must be non-empty");
        }
        return text;
    }

    private static JsonValue requiredArray(JsonValue object, String fieldName, String label) throws InvalidConfigException {
        JsonValue value = object.get(fieldName);
        if (value == null) {
            throw new InvalidConfigException("missing " + label);
        }
        if (!value.isArray()) {
            throw new InvalidConfigException(label + " must be an array");
        }
        return value;
    }

    private static long requiredNonNegativeInteger(JsonValue object, String fieldName, String label) throws InvalidConfigException {
        JsonValue value = object.get(fieldName);
        if (value == null) {
            throw new InvalidConfigException("missing " + label);
        }
        if (!value.isNumber() || !isIntegerLiteral(value.asString())) {
            throw new InvalidConfigException(label + " must be a non-negative integer");
        }
        long parsed;
        try {
            parsed = Long.parseLong(value.asString());
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(label + " must be a non-negative integer");
        }
        if (parsed < 0L) {
            throw new InvalidConfigException(label + " must be a non-negative integer");
        }
        return parsed;
    }

    private static boolean isIntegerLiteral(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String shortReason(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }
}
