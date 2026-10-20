package com.codex.aabtoolgui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;

/** Reads the application theme once at startup; no polling or change listeners. */
final class AppTheme {
    private static final Pattern WINDOWS_DARK_MODE = Pattern.compile(
        "(?im)^\\s*AppsUseLightTheme\\s+REG_DWORD\\s+0x0+\\s*$"
    );

    private AppTheme() {}

    enum Mode {
        SYSTEM("跟随系统"), LIGHT("浅色模式"), DARK("深色模式");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
        static Mode fromSetting(String value) {
            try { return valueOf(value); }
            catch (IllegalArgumentException | NullPointerException e) { return SYSTEM; }
        }
        boolean isDark(boolean systemDark) {
            return this == DARK || (this == SYSTEM && systemDark);
        }
    }

    static boolean detectSystemDarkMode() {
        return detectSystemDarkMode(
            System.getProperty("os.name", ""), System.getenv("SystemRoot"), AppTheme::readThemeSetting
        );
    }

    static boolean detectSystemDarkMode(String osName, String windowsRoot, Function<List<String>, String> query) {
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("darwin")) {
            // In light mode AppleInterfaceStyle may be absent (defaults exits nonzero).
            // An absolute path also works when launched from Finder with a minimal PATH.
            String appearance = query.apply(List.of("/usr/bin/defaults", "read", "-g", "AppleInterfaceStyle"));
            return "Dark".equalsIgnoreCase(appearance.strip());
        }
        if (os.startsWith("windows") && windowsRoot != null && !windowsRoot.isBlank()) {
            return isWindowsDarkMode(query.apply(List.of(
                Path.of(windowsRoot, "System32", "reg.exe").toString(),
                "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme"
            )));
        }
        return false;
    }

    private static String readThemeSetting(List<String> command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            // Both platform queries return a single setting. Bound startup delay on failure.
            if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return "";
            }
            try (var input = process.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (Exception e) {
            return "";
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    static boolean isWindowsDarkMode(String registryOutput) {
        return WINDOWS_DARK_MODE.matcher(registryOutput).find();
    }

    /** Called on the EDT at startup or when the user explicitly changes the theme. */
    static void install(boolean dark) {
        boolean installed = dark ? FlatDarkLaf.setup() : FlatLightLaf.setup();
        if (!installed) {
            dark = false;
            FlatLightLaf.setup();
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        UIManager.put("AabTool.progress.active", dark ? new Color(100, 174, 245) : new Color(43, 119, 192));
        UIManager.put("AabTool.progress.success", dark ? new Color(94, 196, 141) : new Color(39, 136, 93));
        UIManager.put("AabTool.progress.failure", dark ? new Color(242, 125, 125) : new Color(190, 76, 76));
        UIManager.put("AabTool.progress.track", dark ? new Color(66, 72, 81) : new Color(224, 229, 235));
    }
}
