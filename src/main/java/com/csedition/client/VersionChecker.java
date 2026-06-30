package com.csedition.client;

import com.csedition.CSEditionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks GitHub for a newer version of the mod and notifies the player in chat.
 *
 * Runs once on client start, then every 30 minutes.
 * Compares current mod version (from mods.toml) with the latest release tag
 * on GitHub. If newer version exists, shows a clickable chat message.
 */
@OnlyIn(Dist.CLIENT)
public final class VersionChecker {

    private static final String REPO = "Hinderchik/CSEdition";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final long CHECK_INTERVAL_MINUTES = 30;
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([0-9.]+)\"");

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "CS-Edition-VersionChecker");
                t.setDaemon(true);
                return t;
            });

    private static volatile boolean notifiedThisSession = false;
    private static volatile String currentVersion = null;

    private VersionChecker() {}

    /**
     * Запустить периодическую проверку. Вызывается из CSEditionMod.clientSetup().
     */
    public static void start() {
        currentVersion = detectCurrentVersion();
        // Первая проверка через 30 секунд (даём клиенту прогрузиться)
        SCHEDULER.schedule(VersionChecker::check, 30, TimeUnit.SECONDS);
        SCHEDULER.scheduleAtFixedRate(VersionChecker::check,
                CHECK_INTERVAL_MINUTES, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Запустить одну проверку (для тестирования или ручного вызова).
     */
    public static void checkNow() {
        SCHEDULER.execute(VersionChecker::check);
    }

    private static void check() {
        try {
            String latestTag = fetchLatestTag();
            if (latestTag == null) return;
            String latestVer = stripV(latestTag);
            String currentVer = currentVersion != null ? currentVersion : "0.0.0";
            if (isNewer(latestVer, currentVer)) {
                showUpdateNotification(latestVer, currentVer);
            }
        } catch (Exception e) {
            CSEditionMod.LOGGER.debug("[CS-Edition] Version check failed: {}", e.getMessage());
        }
    }

    private static String fetchLatestTag() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "CS-Edition-MC-Mod");
        try {
            int code = conn.getResponseCode();
            if (code != 200) return null;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                Matcher m = TAG_PATTERN.matcher(sb);
                return m.find() ? m.group(1) : null;
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Сравнивает две semver-строки (только major.minor.patch).
     * Возвращает true если a > b.
     */
    private static boolean isNewer(String a, String b) {
        try {
            String[] aa = a.split("\\.");
            String[] bb = b.split("\\.");
            for (int i = 0; i < Math.max(aa.length, bb.length); i++) {
                int av = i < aa.length ? Integer.parseInt(aa[i]) : 0;
                int bv = i < bb.length ? Integer.parseInt(bb[i]) : 0;
                if (av != bv) return av > bv;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String stripV(String tag) {
        return tag.startsWith("v") ? tag.substring(1) : tag;
    }

    private static String detectCurrentVersion() {
        try {
            var container = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList()
                    .getModFileById("csedition");
            if (container != null) {
                return container.getMods().get(0).getVersion().toString();
            }
        } catch (Throwable ignored) {}
        return "0.0.0";
    }

    private static void showUpdateNotification(String latest, String current) {
        // Показываем только один раз за сессию
        if (notifiedThisSession) return;
        notifiedThisSession = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.execute(() -> {
            if (mc.player == null) return;
            mc.player.displayClientMessage(
                    Component.literal("§6[CS-Edition] §eДоступна новая версия §av" + latest
                            + " §e(у тебя v" + current + ")"),
                    false);
            mc.player.displayClientMessage(
                    Component.literal("§7  → github.com/" + REPO + "/releases/latest"),
                    false);
            CSEditionMod.LOGGER.info("[CS-Edition] Update available: v{} (current: v{})", latest, current);
        });
    }
}
