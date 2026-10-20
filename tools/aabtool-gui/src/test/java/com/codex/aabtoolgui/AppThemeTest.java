package com.codex.aabtoolgui;

import java.awt.Color;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Standalone checks against the packaged JAR; no device or system setting changes. */
public final class AppThemeTest {
    public static void main(String[] args) throws Exception {
        verifyPlatformDetection();
        check(AppTheme.Mode.fromSetting("invalid") == AppTheme.Mode.SYSTEM, "invalid preference fallback");
        check(AppTheme.Mode.fromSetting("LIGHT") == AppTheme.Mode.LIGHT, "load saved light preference");
        check(!AppTheme.Mode.LIGHT.isDark(true), "manual light overrides dark system");
        check(AppTheme.Mode.DARK.isDark(false), "manual dark overrides light system");
        check(AppTheme.Mode.SYSTEM.isDark(true) && !AppTheme.Mode.SYSTEM.isDark(false), "system mode follows startup detection");
        check(AppTheme.isWindowsDarkMode("HKCU\\test\r\n    AppsUseLightTheme    REG_DWORD    0x0\r\n"), "dark app mode");
        check(AppTheme.isWindowsDarkMode("\tAppsUseLightTheme REG_DWORD 0x00000000\n"), "zero-padded dark mode");
        check(!AppTheme.isWindowsDarkMode("AppsUseLightTheme REG_DWORD 0x1"), "light app mode");
        check(!AppTheme.isWindowsDarkMode("SystemUsesLightTheme REG_DWORD 0x0"), "ignore taskbar theme");
        check(!AppTheme.isWindowsDarkMode("AppsUseLightTheme REG_SZ 0x0"), "ignore invalid value type");
        check(!AppTheme.isWindowsDarkMode("AppsUseLightTheme REG_DWORD 0x01"), "do not match zero prefix");
        check(!AppTheme.isWindowsDarkMode("ERROR: value not found"), "fallback on missing setting");
        check(!AppTheme.isWindowsDarkMode(""), "fallback on empty setting");
        SwingUtilities.invokeAndWait(() -> {
            verifyTheme(false);
            verifyTheme(true);
        });
        System.out.println("PASS: Windows/macOS detection, manual overrides, light/dark controls, text contrast and progress palette");
    }

    private static void verifyPlatformDetection() {
        for (String os : List.of("Mac OS X", "macOS", "Darwin")) {
            check(AppTheme.detectSystemDarkMode(os, null, command -> {
                check(command.equals(List.of("/usr/bin/defaults", "read", "-g", "AppleInterfaceStyle")), "macOS command");
                return "Dark\n";
            }), "macOS dark startup: " + os);
        }
        for (String value : List.of("", "Light\n", "The domain/default pair does not exist", "Dark mode unavailable")) {
            check(!AppTheme.detectSystemDarkMode("Mac OS X", null, command -> value), "macOS light/missing/invalid preference");
        }
        check(AppTheme.detectSystemDarkMode("Mac OS X", null, command -> "  dark\r\n"), "macOS whitespace");
        check(AppTheme.detectSystemDarkMode("Windows 11", "C:\\Windows", command -> {
            check(command.get(0).endsWith("reg.exe") && command.contains("AppsUseLightTheme"), "Windows command retained");
            return "AppsUseLightTheme REG_DWORD 0x0";
        }), "Windows dark startup");
        check(!AppTheme.detectSystemDarkMode("Windows 11", "C:\\Windows", command -> "AppsUseLightTheme REG_DWORD 0x1"), "Windows light startup");
        check(!AppTheme.detectSystemDarkMode("Linux", null, command -> {
            throw new AssertionError("Unsupported platform must not run a command");
        }), "unsupported platform fallback");
        check(!AppTheme.detectSystemDarkMode("Windows 11", null, command -> {
            throw new AssertionError("Missing Windows root must not run a command");
        }), "missing Windows root fallback");
    }

    private static void verifyTheme(boolean dark) {
        AppTheme.install(dark);
        check(UIManager.getLookAndFeel().getName().equals(dark ? "FlatLaf Dark" : "FlatLaf Light"), "theme installed");
        JPanel parent = new JPanel();
        for (var component : new javax.swing.JComponent[] {
            new JPanel(), new JTextField("AAB"), new JTextArea("日志"),
            new JButton("卸载重装"), new JFileChooser(), new JOptionPane("完成")
        }) {
            parent.add(component);
            Color background = component.getBackground();
            check((luminance(background) < 0.3) == dark, "control background: " + component.getClass().getSimpleName());
        }
        check(contrast(new JLabel().getForeground(), new JPanel().getBackground()) >= 4.5, "label contrast");
        check(contrast(new JTextArea().getForeground(), new JTextArea().getBackground()) >= 4.5, "log contrast");
        check(UIManager.getColor("Separator.foreground") != null, "separator color");
        Color track = UIManager.getColor("AabTool.progress.track");
        for (String state : new String[] {"active", "success", "failure"}) {
            check(contrast(UIManager.getColor("AabTool.progress." + state), track) >= 3, "progress contrast: " + state);
        }
    }

    private static double luminance(Color color) {
        return channel(color.getRed()) * 0.2126 + channel(color.getGreen()) * 0.7152 + channel(color.getBlue()) * 0.0722;
    }

    private static double channel(int value) {
        double c = value / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double contrast(Color a, Color b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
