package com.csedition.config;

import com.csedition.CSEditionMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * Конфиг прицела — клиентский JSON в config/csedition/crosshair.json.
 * Загружается один раз в FMLClientSetupEvent, сохраняется по кнопке Save.
 */
public class CrosshairConfig {

    // --- Режим ---
    public boolean dynamic = true;

    // --- Геометрия ---
    public int length = 6;
    public int thickness = 2;
    public int gap = 3;
    public boolean dot = false;
    public int dotSize = 1;
    public boolean tStyle = false;
    public boolean outline = true;
    public int outlineAlpha = 160;

    // --- Цвет ---
    public int color = 0xFF00FF00;

    // --- Динамика ---
    public float baseSpread = 0f;
    public float moveSpread = 3f;
    public float sprintSpread = 4f;
    public float airSpread = 6f;
    public float shotSpread = 5f;
    public float shotRecovery = 0.35f;
    public float lerpSpeed = 0.30f;

    // --- Поведение ---
    public boolean hideOnSniperAds = true;
    public boolean hideInSpectator = true;
    public boolean hideWithHud = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path currentFile = null;

    public CrosshairConfig() {}

    /** Создаём копию (для Cancel). */
    public CrosshairConfig copy() {
        CrosshairConfig c = new CrosshairConfig();
        c.dynamic = this.dynamic;
        c.length = this.length;
        c.thickness = this.thickness;
        c.gap = this.gap;
        c.dot = this.dot;
        c.dotSize = this.dotSize;
        c.tStyle = this.tStyle;
        c.outline = this.outline;
        c.outlineAlpha = this.outlineAlpha;
        c.color = this.color;
        c.baseSpread = this.baseSpread;
        c.moveSpread = this.moveSpread;
        c.sprintSpread = this.sprintSpread;
        c.airSpread = this.airSpread;
        c.shotSpread = this.shotSpread;
        c.shotRecovery = this.shotRecovery;
        c.lerpSpeed = this.lerpSpeed;
        c.hideOnSniperAds = this.hideOnSniperAds;
        c.hideInSpectator = this.hideInSpectator;
        c.hideWithHud = this.hideWithHud;
        return c;
    }

    /** Санитизация всех полей — clamp к диапазонам. */
    public void sanitize() {
        length = Math.max(0, Math.min(32, length));
        thickness = Math.max(1, Math.min(8, thickness));
        gap = Math.max(0, Math.min(32, gap));
        dotSize = Math.max(1, Math.min(8, dotSize));
        outlineAlpha = Math.max(0, Math.min(255, outlineAlpha));
        baseSpread = Math.max(0f, Math.min(32f, baseSpread));
        moveSpread = Math.max(0f, Math.min(32f, moveSpread));
        sprintSpread = Math.max(0f, Math.min(32f, sprintSpread));
        airSpread = Math.max(0f, Math.min(32f, airSpread));
        shotSpread = Math.max(0f, Math.min(32f, shotSpread));
        shotRecovery = Math.max(0.05f, Math.min(1f, shotRecovery));
        lerpSpeed = Math.max(0.05f, Math.min(1f, lerpSpeed));
    }

    // ====================== Серийлизация ======================

    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("dynamic", dynamic);
        root.addProperty("length", length);
        root.addProperty("thickness", thickness);
        root.addProperty("gap", gap);
        root.addProperty("dot", dot);
        root.addProperty("dotSize", dotSize);
        root.addProperty("tStyle", tStyle);
        root.addProperty("outline", outline);
        root.addProperty("outlineAlpha", outlineAlpha);
        root.addProperty("color", Integer.toHexString(color));
        root.addProperty("baseSpread", baseSpread);
        root.addProperty("moveSpread", moveSpread);
        root.addProperty("sprintSpread", sprintSpread);
        root.addProperty("airSpread", airSpread);
        root.addProperty("shotSpread", shotSpread);
        root.addProperty("shotRecovery", shotRecovery);
        root.addProperty("lerpSpeed", lerpSpeed);
        root.addProperty("hideOnSniperAds", hideOnSniperAds);
        root.addProperty("hideInSpectator", hideInSpectator);
        root.addProperty("hideWithHud", hideWithHud);
        return GSON.toJson(root);
    }

    public static CrosshairConfig fromJson(String json) {
        try {
            JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            CrosshairConfig c = new CrosshairConfig();
            if (root.has("dynamic")) c.dynamic = root.get("dynamic").getAsBoolean();
            if (root.has("length")) c.length = root.get("length").getAsInt();
            if (root.has("thickness")) c.thickness = root.get("thickness").getAsInt();
            if (root.has("gap")) c.gap = root.get("gap").getAsInt();
            if (root.has("dot")) c.dot = root.get("dot").getAsBoolean();
            if (root.has("dotSize")) c.dotSize = root.get("dotSize").getAsInt();
            if (root.has("tStyle")) c.tStyle = root.get("tStyle").getAsBoolean();
            if (root.has("outline")) c.outline = root.get("outline").getAsBoolean();
            if (root.has("outlineAlpha")) c.outlineAlpha = root.get("outlineAlpha").getAsInt();
            if (root.has("color")) {
                String hex = root.get("color").getAsString();
                if (!hex.isEmpty()) c.color = Integer.parseInt(hex, 16);
            }
            if (root.has("baseSpread")) c.baseSpread = (float) root.get("baseSpread").getAsDouble();
            if (root.has("moveSpread")) c.moveSpread = (float) root.get("moveSpread").getAsDouble();
            if (root.has("sprintSpread")) c.sprintSpread = (float) root.get("sprintSpread").getAsDouble();
            if (root.has("airSpread")) c.airSpread = (float) root.get("airSpread").getAsDouble();
            if (root.has("shotSpread")) c.shotSpread = (float) root.get("shotSpread").getAsDouble();
            if (root.has("shotRecovery")) c.shotRecovery = (float) root.get("shotRecovery").getAsDouble();
            if (root.has("lerpSpeed")) c.lerpSpeed = (float) root.get("lerpSpeed").getAsDouble();
            if (root.has("hideOnSniperAds")) c.hideOnSniperAds = root.get("hideOnSniperAds").getAsBoolean();
            if (root.has("hideInSpectator")) c.hideInSpectator = root.get("hideInSpectator").getAsBoolean();
            if (root.has("hideWithHud")) c.hideWithHud = root.get("hideWithHud").getAsBoolean();
            c.sanitize();
            return c;
        } catch (Exception e) {
            CSEditionMod.LOGGER.warn("[CS-Edition] Failed to parse crosshair.json: {}", e.getMessage());
            return new CrosshairConfig();
        }
    }

    /** Грузит конфиг из файла. Вызывается один раз в FMLClientSetupEvent. */
    public static CrosshairConfig load() {
        Path file = getConfigPath();
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                if (!json.isBlank()) {
                    CrosshairConfig c = fromJson(json);
                    currentFile = file;
                    return c;
                }
            }
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to load crosshair.json", e);
        }
        currentFile = file;
        return new CrosshairConfig();
    }

    /** Сохраняет конфиг в файл. */
    public static void save(CrosshairConfig cfg) {
        if (currentFile == null) return;
        try {
            Path dir = currentFile.getParent();
            if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(currentFile, StandardCharsets.UTF_8)) {
                w.write(cfg.toJson());
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Saved crosshair config to {}", currentFile);
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to save crosshair.json", e);
        }
    }

    private static Path getConfigPath() {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve("csedition")
                .resolve("crosshair.json");
    }
}
