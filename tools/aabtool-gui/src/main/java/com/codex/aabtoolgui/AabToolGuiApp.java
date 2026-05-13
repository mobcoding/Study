package com.codex.aabtoolgui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class AabToolGuiApp {
    private static final String APP_NAME = "AAB Tool GUI 2.0";
    private static final String BUNDLETOOL_NAME = "bundletool-all-1.18.3.jar";
    private static final String BUNDLED_BUNDLETOOL_RESOURCE = "/embedded/" + BUNDLETOOL_NAME;
    private static final String DEBUG_ALIAS = "aab_debug";
    private static final String DEBUG_PASSWORD = "aab_debug";
    private static final Pattern ADMOB_PATTERN = Pattern.compile("ca-app-pub-(?:\\d{16}/\\d{10}|\\d+/\\d+)");
    private static final Pattern CLASS_DESCRIPTOR_PATTERN = Pattern.compile("^L[^;]+;$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("\\p{IsHan}");
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("package:\\s+name='([^']+)'");
    private static final Pattern LAUNCHABLE_ACTIVITY_PATTERN = Pattern.compile("launchable-activity:\\s+name='([^']+)'");
    private static final Pattern USES_PERMISSION_PATTERN = Pattern.compile("uses-permission(?:-sdk-\\d+)?:\\s+name='([^']+)'");
    private static final Pattern DEBUGGABLE_XMLTREE_PATTERN = Pattern.compile("android:debuggable\\(0x0101000f\\)=\\((?:type 0x12|type 0x10)\\)0x([0-9a-fA-F]+)");
    private static final Pattern APPLICATION_LABEL_RESOURCE_PATTERN = Pattern.compile("android:label\\(0x01010001\\)=@0x([0-9a-fA-F]+)");
    private static final Pattern RESOURCE_HEADER_PATTERN = Pattern.compile("^\\s*resource\\s+0x([0-9a-fA-F]+)\\b");
    private static final Pattern RESOURCE_LOCALE_VALUE_PATTERN = Pattern.compile("^\\s*\\(([^)]*)\\)\\s+\"");
    private static final String ADMOB_TEST_UNIT_PREFIX = "ca-app-pub-3940256099942544/";
    private static final String ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    private static final Set<String> OBFUSCATION_PACKAGE_IGNORES = Set.of("google", "adjust", "firebase", "facebook", "androidx");
    private static final Set<String> STRINGFOG_MARKERS = Set.of(
        "stringfog",
        "com/github/megatronking/stringfog",
        "com/github/megatronking/stringfog/xor/stringfogimpl",
        "com/github/megatronking/stringfog/iStringfog".toLowerCase(Locale.ROOT),
        "stringfogimpl",
        "istringfog"
    );
    private static final Set<String> GENERIC_SCAN_SKIPPED_EXTENSIONS = Set.of(
        ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".heic",
        ".mp3", ".ogg", ".wav", ".aac", ".flac",
        ".mp4", ".avi", ".mkv", ".webm",
        ".so", ".ttf", ".otf", ".woff", ".woff2"
    );
    private static final int MAX_SAMPLE_COUNT = 12;
    private static final int MAX_SAMPLE_TEXT = 120;
    private static final int MAX_GENERIC_SCAN_BYTES = 4 * 1024 * 1024;

    private AabToolGuiApp() {
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            int exitCode = runCli(args);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    private enum InstallMode {
        CONNECTED_DEVICE("连接设备（推荐）", true),
        UNIVERSAL("通用 APK 集", false);

        private final String label;
        private final boolean connectedDevice;

        InstallMode(String label, boolean connectedDevice) {
            this.label = label;
            this.connectedDevice = connectedDevice;
        }

        public boolean isConnectedDevice() {
            return connectedDevice;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ToolConfig {
        private String aabPath;
        private String outputPath;
        private String bundletoolPath;
        private String adbPath;
        private String aapt2Path;
        private String deviceId;
        private String keystorePath;
        private String keystorePassword;
        private String keyAlias;
        private String keyPassword;
        private InstallMode mode;
        private boolean installAfterBuild;
        private boolean allowDowngrade;
        private boolean grantRuntimePermissions;
        private boolean runLegacyChecks;
        private boolean autoLaunchAfterInstall;
        private boolean autoUninstallOnSignatureMismatch;
    }

    private static final class ExecutionResult {
        private final boolean success;
        private final String message;

        private ExecutionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static final class CliOptions {
        private boolean showHelp;
        private final ToolConfig config = new ToolConfig();
    }

    private static final class LegacyInspectionReport {
        private final Set<String> apkNames = new LinkedHashSet<>();
        private final Set<String> chineseSamples = new LinkedHashSet<>();
        private final Set<String> admobSamples = new LinkedHashSet<>();
        private final Set<String> stringFogSamples = new LinkedHashSet<>();
        private final Set<String> aabResGuardSamples = new LinkedHashSet<>();
        private final Set<String> permissions = new LinkedHashSet<>();
        private final Set<String> locales = new LinkedHashSet<>();
        private final Set<String> logSamples = new LinkedHashSet<>();
        private int chineseCount;
        private int admobCount;
        private int stringFogCount;
        private int aabResGuardCount;
        private int logCount;
        private int totalClassCount;
        private int shortClassCount;
        private int totalResourceCount;
        private int shortResourceCount;
        private boolean bundleMetadataPresent;
        private boolean debuggable;
        private boolean debuggableResolved;

        private void addApkName(String apkName) {
            apkNames.add(apkName);
        }

        private void addChinese(String sample) {
            chineseCount++;
            addSample(chineseSamples, sample);
        }

        private void addAdmob(String sample) {
            admobCount++;
            addSample(admobSamples, sample);
        }

        private void addStringFog(String sample) {
            stringFogCount++;
            addSample(stringFogSamples, sample);
        }

        private void addAabResGuard(String sample) {
            aabResGuardCount++;
            addSample(aabResGuardSamples, sample);
        }

        private void addLog(String sample) {
            logCount++;
            addSample(logSamples, sample);
        }

        private void addPermission(String permission) {
            if (permission == null || permission.isBlank()) {
                return;
            }
            permissions.add(permission);
        }

        private void addLocale(String locale) {
            if (locale == null || locale.isBlank() || "--_--".equals(locale)) {
                return;
            }
            locales.add(locale);
        }

        private void replaceLocales(Set<String> locales) {
            this.locales.clear();
            if (locales == null) {
                return;
            }
            for (String locale : locales) {
                addLocale(locale);
            }
        }

        private void setDebuggable(boolean debuggable) {
            this.debuggable = debuggable;
            this.debuggableResolved = true;
        }

        private void addClassDescriptor(String descriptor) {
            if (!CLASS_DESCRIPTOR_PATTERN.matcher(descriptor).matches()) {
                return;
            }
            String body = descriptor.substring(1, descriptor.length() - 1);
            String[] segments = body.split("/");
            if (segments.length == 0) {
                return;
            }
            String first = segments[0].toLowerCase(Locale.ROOT);
            if (OBFUSCATION_PACKAGE_IGNORES.contains(first)) {
                return;
            }
            String simpleName = segments[segments.length - 1];
            int dollarIndex = simpleName.indexOf('$');
            if (dollarIndex > 0) {
                simpleName = simpleName.substring(0, dollarIndex);
            }
            if (simpleName.isBlank()) {
                return;
            }
            totalClassCount++;
            if (simpleName.length() <= 2) {
                shortClassCount++;
            }
        }

        private void addResourceEntry(String entryName) {
            int slash = entryName.lastIndexOf('/');
            int dot = entryName.lastIndexOf('.');
            if (slash < 0 || dot <= slash + 1) {
                return;
            }
            String stem = entryName.substring(slash + 1, dot);
            if (stem.isBlank()) {
                return;
            }
            totalResourceCount++;
            if (stem.length() <= 4) {
                shortResourceCount++;
            }
        }

        private void logTo(java.util.function.Consumer<String> logSink) {
            logSink.accept("静态检查结果：");
            logSink.accept("  APK 分包：" + (apkNames.isEmpty() ? "未找到" : String.join(", ", apkNames)));

            logSink.accept("1. AdMob 配置相关");
            emitSection(logSink, admobCount, admobSamples, "  未发现 AdMob 广告位 ID。", "  发现 %d 个 AdMob 广告位 ID：");

            logSink.accept("2. StringFog/AabResGuard/代码混淆\"");
            logSink.accept("  StringFog：" + (stringFogCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  代码混淆：" + codeObfuscationStatus());
            logSink.accept("  AabResGuard：" + (aabResGuardCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  调试模式：isDebug: " + debugStatus());

            logSink.accept("3. 中文字符串相关");
            emitSection(logSink, chineseCount, chineseSamples, "  未发现中文字符串。", "  发现 %d 处中文字符串：");

            logSink.accept("4. 已申请权限相关");
            if (permissions.isEmpty()) {
                logSink.accept("  未解析到已申请权限。");
            } else {
                logSink.accept("  共解析到 " + permissions.size() + " 个权限：");
                for (String permission : permissions) {
                    logSink.accept("  - " + permission);
                }
            }

            logSink.accept("5. 多语言支持相关");
            if (locales.isEmpty()) {
                logSink.accept("  未解析到多语言配置。");
            } else {
                logSink.accept("  共解析到 " + locales.size() + " 个语言/地区：");
                for (String locale : locales) {
                    logSink.accept("  - " + locale);
                }
            }
            logSink.accept("");
        }

        private void logToReadable(java.util.function.Consumer<String> logSink) {
            logSink.accept("静态检查结果：");
            logSink.accept("  APK 分包：" + (apkNames.isEmpty() ? "未找到" : String.join(", ", apkNames)));

            logSink.accept("1. AdMob 配置相关");
            emitReadableSection(logSink, admobCount, admobSamples, "  未发现 AdMob 广告位 ID。", "  发现 %d 个 AdMob 广告位 ID：");

            logSink.accept("2. StringFog/AabResGuard/代码混淆");
            logSink.accept("  StringFog：" + (stringFogCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  代码混淆：" + readableCodeObfuscationStatus());
            logSink.accept("  AabResGuard：" + (aabResGuardCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  调试模式：isDebug: " + readableDebugStatus());

            logSink.accept("3. 中文字符串相关");
            emitReadableSection(logSink, chineseCount, chineseSamples, "  未发现中文字符串。", "  发现 %d 处中文字符串：");

            logSink.accept("4. 已申请权限相关");
            if (permissions.isEmpty()) {
                logSink.accept("  未解析到已申请权限。");
            } else {
                logSink.accept("  共解析到 " + permissions.size() + " 个权限：");
                for (String permission : permissions) {
                    logSink.accept("  - " + permission);
                }
            }

            logSink.accept("5. 多语言支持相关");
            if (locales.isEmpty()) {
                logSink.accept("  未解析到多语言配置。");
            } else {
                logSink.accept("  共解析到 " + locales.size() + " 个语言/地区：");
                for (String locale : locales) {
                    logSink.accept("  - " + locale);
                }
            }
            logSink.accept("");
        }

        private String codeObfuscationStatus() {
            if (totalClassCount <= 0) {
                return "无法判断";
            }
            return shortClassCount * 2 > totalClassCount ? "已启用" : "不明显";
        }

        private String debugStatus() {
            if (!debuggableResolved) {
                return "unknown";
            }
            return Boolean.toString(debuggable);
        }

        private String readableCodeObfuscationStatus() {
            if (totalClassCount <= 0) {
                return "无法判断";
            }
            return shortClassCount * 2 > totalClassCount ? "已启用" : "未明显启用";
        }

        private String readableDebugStatus() {
            return Boolean.toString(debuggableResolved && debuggable);
        }

        private static void emitReadableSection(
            java.util.function.Consumer<String> logSink,
            int total,
            Set<String> samples,
            String emptyMessage,
            String foundMessage
        ) {
            if (total == 0) {
                logSink.accept(emptyMessage);
                return;
            }
            logSink.accept(String.format(foundMessage, total));
            for (String sample : samples) {
                logSink.accept("  - " + sanitizeDisplayText(sample));
            }
        }

        private static void emitSection(
            java.util.function.Consumer<String> logSink,
            int total,
            Set<String> samples,
            String emptyMessage,
            String foundMessage
        ) {
            if (total == 0) {
                logSink.accept(emptyMessage);
                return;
            }
            logSink.accept(String.format(foundMessage, total));
            for (String sample : samples) {
                logSink.accept("  - " + sample);
            }
        }

        private static void addSample(Set<String> target, String sample) {
            if (target.size() < MAX_SAMPLE_COUNT) {
                target.add(sample);
            }
        }
    }

    private static final class LaunchTarget {
        private final String packageName;
        private final String launchableActivity;

        private LaunchTarget(String packageName, String launchableActivity) {
            this.packageName = packageName;
            this.launchableActivity = launchableActivity;
        }
    }

    private static final class ExtractedApk {
        private final Path path;

        private ExtractedApk(Path path) {
            this.path = path;
        }
    }

    private static final class SettingsStore {
        private final Path settingsFile;
        private final Properties properties = new Properties();

        private SettingsStore() {
            this.settingsFile = getStateDir().resolve("settings.properties");
        }

        void load() {
            if (!Files.exists(settingsFile)) {
                return;
            }
            try (var in = Files.newInputStream(settingsFile)) {
                properties.load(in);
            } catch (IOException ignored) {
            }
        }

        String get(String key, String fallback) {
            return properties.getProperty(key, fallback);
        }

        boolean getBoolean(String key, boolean fallback) {
            return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
        }

        void put(String key, String value) {
            properties.setProperty(key, value == null ? "" : value);
        }

        void putBoolean(String key, boolean value) {
            properties.setProperty(key, Boolean.toString(value));
        }

        void save() {
            try {
                Files.createDirectories(settingsFile.getParent());
                try (var out = Files.newOutputStream(settingsFile)) {
                    properties.store(out, APP_NAME + " settings");
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static final class MainFrame extends JFrame {
        private final SettingsStore settings = new SettingsStore();

        private final JTextField aabField = new JTextField(48);
        private final JTextField outputField = new JTextField(48);
        private final JTextField adbField = new JTextField(48);
        private final JTextField aapt2Field = new JTextField(48);
        private final JComboBox<String> deviceIdBox = new JComboBox<>();
        private final JTextField keystoreField = new JTextField(48);
        private final JPasswordField keystorePasswordField = new JPasswordField(48);
        private final JTextField keyAliasField = new JTextField(48);
        private final JPasswordField keyPasswordField = new JPasswordField(48);
        private final JComboBox<InstallMode> modeBox = new JComboBox<>(InstallMode.values());
        private final JCheckBox runLegacyChecksBox = new JCheckBox("Run legacy checks", true);
        private final JCheckBox installAfterBuildBox = new JCheckBox("Install after build", true);
        private final JCheckBox autoLaunchAfterInstallBox = new JCheckBox("Auto launch after install", true);
        private final JCheckBox autoUninstallOnSignatureMismatchBox = new JCheckBox("Auto uninstall on signature mismatch", false);
        private final JCheckBox allowDowngradeBox = new JCheckBox("Allow downgrade", false);
        private final JCheckBox grantRuntimePermissionsBox = new JCheckBox("Grant runtime permissions", true);
        private final JButton refreshDevicesButton = new JButton("Refresh");
        private final JButton advancedToggleButton = new JButton("Show advanced options");
        private final JButton startButton = new JButton("Start");
        private final JTextArea logArea = new JTextArea(26, 88);
        private final JPanel advancedPanel = new JPanel(new GridBagLayout());
        private boolean advancedVisible;

        private MainFrame() {
            super(APP_NAME);
            deviceIdBox.setEditable(true);
            settings.load();
            buildUi();
            loadSettings();
            refreshDeviceChoices(false);
        }

        private void buildUi() {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout(10, 10));
            installDropSupport();

            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            root.add(buildFormPanel(), BorderLayout.NORTH);
            root.add(buildLogPanel(), BorderLayout.CENTER);
            root.add(buildActionPanel(), BorderLayout.SOUTH);

            add(root, BorderLayout.CENTER);
            pack();
            setSize(new Dimension(1020, 760));
            setLocationRelativeTo(null);
            setMinimumSize(new Dimension(920, 680));
        }

        private JPanel buildFormPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            JPanel basicPanel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            row = addFieldRow(basicPanel, c, row, "AAB file", aabField, browseFileButton(aabField, false));
            row = addFieldRow(basicPanel, c, row, "Output .apks", outputField, browseSaveButton(outputField));
            row = addDeviceRow(basicPanel, c, row);
            addModeRow(basicPanel, c, row);

            panel.add(basicPanel, BorderLayout.NORTH);
            panel.add(buildAdvancedPanel(), BorderLayout.CENTER);
            return panel;
        }

        private JPanel buildAdvancedPanel() {
            JPanel wrapper = new JPanel(new BorderLayout(0, 6));

            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            advancedToggleButton.addActionListener(e -> toggleAdvancedOptions());
            header.add(advancedToggleButton);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            row = addFieldRow(advancedPanel, c, row, "ADB path", adbField, browseFileButton(adbField, false));
            row = addFieldRow(advancedPanel, c, row, "AAPT2 path (optional)", aapt2Field, browseFileButton(aapt2Field, false));
            row = addFieldRow(advancedPanel, c, row, "Keystore (optional)", keystoreField, browseFileButton(keystoreField, false));
            row = addPasswordRow(advancedPanel, c, row, "Keystore password", keystorePasswordField);
            row = addFieldRow(advancedPanel, c, row, "Key alias", keyAliasField, null);
            addPasswordRow(advancedPanel, c, row, "Key password", keyPasswordField);

            advancedPanel.setVisible(false);

            wrapper.add(header, BorderLayout.NORTH);
            wrapper.add(advancedPanel, BorderLayout.CENTER);
            return wrapper;
        }

        private void toggleAdvancedOptions() {
            advancedVisible = !advancedVisible;
            advancedPanel.setVisible(advancedVisible);
            advancedToggleButton.setText(advancedVisible ? "Hide advanced options" : "Show advanced options");
            revalidate();
            repaint();
        }

        private JPanel buildLogPanel() {
            logArea.setEditable(false);
            logArea.setLineWrap(false);
            JScrollPane scrollPane = new JScrollPane(logArea);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(scrollPane, BorderLayout.CENTER);
            return panel;
        }

        private JPanel buildActionPanel() {
            JPanel panel = new JPanel(new BorderLayout());

            JPanel hintPanel = new JPanel();
            hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
            hintPanel.add(new JLabel("说明："));
            hintPanel.add(new JLabel("1. 签名相关留空时，会自动生成默认调试证书。"));
            hintPanel.add(new JLabel("2. 已连接设备模式会按当前连接设备的实际配置生成并安装。"));
            hintPanel.add(new JLabel("3. 可以直接把 .aab 文件拖到 AAB 输入框中。"));
            hintPanel.add(new JLabel("4. Device ID 支持下拉选择，也支持手动输入。"));
            hintPanel.add(new JLabel("5. 静态检查会检测中文、AdMob、代码混淆、StringFog 等信息。"));
            hintPanel.add(new JLabel("6. 如果日志出现 ProtoDeserialize 或 unknown compound value，通常是 AAB 包本身存在资源问题。"));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            startButton.addActionListener(e -> startExecution());
            JButton clearButton = new JButton("Clear logs");
            clearButton.addActionListener(e -> logArea.setText(""));
            buttonPanel.add(clearButton);
            buttonPanel.add(startButton);

            panel.add(hintPanel, BorderLayout.WEST);
            panel.add(buttonPanel, BorderLayout.EAST);
            return panel;
        }

        private int addFieldRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field, JButton browseButton) {
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0;
            panel.add(new JLabel(label + ":"), c);

            c.gridx = 1;
            c.weightx = 1;
            panel.add(field, c);

            c.gridx = 2;
            c.weightx = 0;
            panel.add(browseButton != null ? browseButton : Box.createHorizontalStrut(1), c);
            return row + 1;
        }

        private int addPasswordRow(JPanel panel, GridBagConstraints c, int row, String label, JPasswordField field) {
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0;
            panel.add(new JLabel(label + ":"), c);

            c.gridx = 1;
            c.weightx = 1;
            panel.add(field, c);

            c.gridx = 2;
            c.weightx = 0;
            panel.add(Box.createHorizontalStrut(1), c);
            return row + 1;
        }

        private int addDeviceRow(JPanel panel, GridBagConstraints c, int row) {
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0;
            panel.add(new JLabel("Device ID (optional):"), c);

            JPanel devicePanel = new JPanel(new BorderLayout(8, 0));
            devicePanel.add(deviceIdBox, BorderLayout.CENTER);
            refreshDevicesButton.addActionListener(e -> refreshDeviceChoices(true));
            devicePanel.add(refreshDevicesButton, BorderLayout.EAST);

            c.gridx = 1;
            c.weightx = 1;
            panel.add(devicePanel, c);

            c.gridx = 2;
            c.weightx = 0;
            panel.add(Box.createHorizontalStrut(1), c);
            return row + 1;
        }

        private int addModeRow(JPanel panel, GridBagConstraints c, int row) {
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0;
            panel.add(new JLabel("Install mode:"), c);

            JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            options.add(modeBox);
            options.add(runLegacyChecksBox);
            options.add(installAfterBuildBox);
            options.add(autoLaunchAfterInstallBox);
            options.add(autoUninstallOnSignatureMismatchBox);
            options.add(allowDowngradeBox);
            options.add(grantRuntimePermissionsBox);

            c.gridx = 1;
            c.weightx = 1;
            panel.add(options, c);

            c.gridx = 2;
            c.weightx = 0;
            panel.add(Box.createHorizontalStrut(1), c);
            return row + 1;
        }

        private JButton browseFileButton(JTextField target, boolean directoriesOnly) {
            JButton button = new JButton("Browse");
            button.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(directoriesOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
                chooser.setSelectedFile(target.getText().isBlank() ? null : Paths.get(target.getText()).toFile());
                int result = chooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    if (target == aabField) {
                        applyAabSelection(chooser.getSelectedFile().toPath());
                    } else {
                        target.setText(chooser.getSelectedFile().getAbsolutePath());
                    }
                }
            });
            return button;
        }

        private JButton browseSaveButton(JTextField target) {
            JButton button = new JButton("Browse");
            button.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(target.getText().isBlank() ? null : Paths.get(target.getText()).toFile());
                int result = chooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    target.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });
            return button;
        }

        private void refreshDeviceChoices(boolean showFeedback) {
            refreshDevicesButton.setEnabled(false);
            String currentValue = getDeviceSelection();
            String adbArg = adbField.getText().trim().isBlank() ? "adb" : adbField.getText().trim();

            new SwingWorker<List<String>, Void>() {
                private Exception error;

                @Override
                protected List<String> doInBackground() {
                    try {
                        return listDevices(adbArg, line -> {});
                    } catch (Exception e) {
                        error = e;
                        return List.of();
                    }
                }

                @Override
                protected void done() {
                    refreshDevicesButton.setEnabled(true);
                    List<String> devices = List.of();
                    try {
                        devices = get();
                    } catch (Exception e) {
                        error = e;
                    }
                    applyDeviceChoices(currentValue, devices);
                    if (showFeedback) {
                        if (error != null) {
                            JOptionPane.showMessageDialog(
                                MainFrame.this,
                                "Failed to refresh devices: " + error.getMessage(),
                                "Refresh failed",
                                JOptionPane.WARNING_MESSAGE
                            );
                        } else if (devices.isEmpty()) {
                            JOptionPane.showMessageDialog(
                                MainFrame.this,
                                "No online Android devices were found.",
                                "No devices",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    }
                }
            }.execute();
        }

        private void applyDeviceChoices(String currentValue, List<String> devices) {
            deviceIdBox.removeAllItems();
            deviceIdBox.addItem("");
            for (String device : devices) {
                deviceIdBox.addItem(device);
            }

            String selected = currentValue;
            if (devices.size() == 1) {
                selected = devices.get(0);
            } else if (!devices.isEmpty() && !devices.contains(selected)) {
                selected = "";
            }
            if (!selected.isBlank() && devices.isEmpty() && !devices.contains(selected)) {
                deviceIdBox.addItem(selected);
            }
            deviceIdBox.setSelectedItem(selected);
        }

        private String getDeviceSelection() {
            Object item = deviceIdBox.getEditor().getItem();
            return item == null ? "" : item.toString().trim();
        }

        private void installDropSupport() {
            aabField.setToolTipText("Drop a .aab file here or click Browse.");
            aabField.setTransferHandler(new TransferHandler() {
                @Override
                public boolean canImport(TransferSupport support) {
                    if (!support.isDrop()) {
                        return false;
                    }
                    if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        return false;
                    }
                    support.setDropAction(COPY);
                    return true;
                }

                @Override
                public boolean importData(TransferSupport support) {
                    if (!canImport(support)) {
                        return false;
                    }
                    try {
                        @SuppressWarnings("unchecked")
                        List<java.io.File> files = (List<java.io.File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                        if (files == null || files.isEmpty()) {
                            return false;
                        }
                        Path dropped = files.get(0).toPath().toAbsolutePath().normalize();
                        if (!dropped.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".aab")) {
                            JOptionPane.showMessageDialog(
                                MainFrame.this,
                                "Please drop a .aab file.",
                                "Unsupported file",
                                JOptionPane.WARNING_MESSAGE
                            );
                            return false;
                        }
                        applyAabSelection(dropped);
                        return true;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            MainFrame.this,
                            "Failed to import dropped file: " + e.getMessage(),
                            "Drop failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return false;
                    }
                }
            });
        }

        private void applyAabSelection(Path aabPath) {
            String normalized = aabPath.toAbsolutePath().normalize().toString();
            aabField.setText(normalized);
            if (outputField.getText().isBlank()) {
                outputField.setText(deriveOutputPath(Paths.get(normalized), (InstallMode) Objects.requireNonNull(modeBox.getSelectedItem())).toString());
            }
        }

        private void loadSettings() {
            aabField.setText(settings.get("aabPath", ""));
            outputField.setText(settings.get("outputPath", ""));
            adbField.setText(settings.get("adbPath", "adb"));
            aapt2Field.setText(settings.get("aapt2Path", detectLatestAapt2Path()));
            deviceIdBox.setSelectedItem("");
            keystoreField.setText(settings.get("keystorePath", ""));
            keyAliasField.setText(settings.get("keyAlias", ""));
            modeBox.setSelectedItem(InstallMode.valueOf(settings.get("mode", InstallMode.CONNECTED_DEVICE.name())));
            runLegacyChecksBox.setSelected(settings.getBoolean("runLegacyChecks", true));
            installAfterBuildBox.setSelected(settings.getBoolean("installAfterBuild", true));
            autoLaunchAfterInstallBox.setSelected(settings.getBoolean("autoLaunchAfterInstall", true));
            autoUninstallOnSignatureMismatchBox.setSelected(settings.getBoolean("autoUninstallOnSignatureMismatch", false));
            allowDowngradeBox.setSelected(settings.getBoolean("allowDowngrade", false));
            grantRuntimePermissionsBox.setSelected(settings.getBoolean("grantRuntimePermissions", true));
        }

        private void saveSettings(ToolConfig config) {
            settings.put("aabPath", config.aabPath);
            settings.put("outputPath", config.outputPath);
            settings.put("bundletoolPath", normalizeSavedBundletoolPath(config.bundletoolPath));
            settings.put("adbPath", config.adbPath);
            settings.put("aapt2Path", config.aapt2Path);
            settings.put("deviceId", config.deviceId);
            settings.put("keystorePath", config.keystorePath);
            settings.put("keyAlias", config.keyAlias);
            settings.put("mode", config.mode.name());
            settings.putBoolean("runLegacyChecks", config.runLegacyChecks);
            settings.putBoolean("installAfterBuild", config.installAfterBuild);
            settings.putBoolean("autoLaunchAfterInstall", config.autoLaunchAfterInstall);
            settings.putBoolean("autoUninstallOnSignatureMismatch", config.autoUninstallOnSignatureMismatch);
            settings.putBoolean("allowDowngrade", config.allowDowngrade);
            settings.putBoolean("grantRuntimePermissions", config.grantRuntimePermissions);
            settings.save();
        }

        private void startExecution() {
            ToolConfig config = collectConfig();
            if (config == null) {
                return;
            }
            saveSettings(config);
            startButton.setEnabled(false);
            logArea.setText("");

            new SwingWorker<ExecutionResult, String>() {
                @Override
                protected ExecutionResult doInBackground() {
                    try {
                        return runWorkflow(config, this::publish);
                    } catch (Exception e) {
                        return new ExecutionResult(false, e.getMessage());
                    }
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String chunk : chunks) {
                        appendLog(chunk);
                    }
                }

                @Override
                protected void done() {
                    startButton.setEnabled(true);
                    try {
                        ExecutionResult result = get();
                        appendLog("");
                        appendLog(result.message);
                        JOptionPane.showMessageDialog(
                            MainFrame.this,
                            result.message,
                            result.success ? "Success" : "Failed",
                            result.success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
                        );
                    } catch (Exception e) {
                        appendLog("Unexpected GUI error: " + e.getMessage());
                    }
                }
            }.execute();
        }

        private ToolConfig collectConfig() {
            ToolConfig config = new ToolConfig();
            config.aabPath = aabField.getText().trim();
            config.outputPath = outputField.getText().trim();
            config.bundletoolPath = "";
            config.adbPath = adbField.getText().trim();
            config.aapt2Path = aapt2Field.getText().trim();
            config.deviceId = getDeviceSelection();
            config.keystorePath = keystoreField.getText().trim();
            config.keystorePassword = new String(keystorePasswordField.getPassword()).trim();
            config.keyAlias = keyAliasField.getText().trim();
            config.keyPassword = new String(keyPasswordField.getPassword()).trim();
            config.mode = (InstallMode) Objects.requireNonNull(modeBox.getSelectedItem());
            config.runLegacyChecks = runLegacyChecksBox.isSelected();
            config.installAfterBuild = installAfterBuildBox.isSelected();
            config.autoLaunchAfterInstall = autoLaunchAfterInstallBox.isSelected();
            config.autoUninstallOnSignatureMismatch = autoUninstallOnSignatureMismatchBox.isSelected();
            config.allowDowngrade = allowDowngradeBox.isSelected();
            config.grantRuntimePermissions = grantRuntimePermissionsBox.isSelected();

            if (config.aabPath.isBlank()) {
                JOptionPane.showMessageDialog(this, "AAB file is required.", "Missing field", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            if (config.outputPath.isBlank()) {
                config.outputPath = deriveOutputPath(Paths.get(config.aabPath), config.mode).toString();
                outputField.setText(config.outputPath);
            }
            return config;
        }

        private void appendLog(String line) {
            logArea.append(line);
            logArea.append(System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private static ExecutionResult runWorkflow(ToolConfig config, java.util.function.Consumer<String> logSink) throws Exception {
        Path aab = Paths.get(config.aabPath).toAbsolutePath().normalize();
        Path output = Paths.get(config.outputPath).toAbsolutePath().normalize();
        Path bundletool = resolveExistingFile(resolveBundletool(config.bundletoolPath), "bundletool JAR");
        Path aapt2 = resolveAapt2(config.aapt2Path);

        if (!Files.exists(aab)) {
            throw new IllegalArgumentException("AAB file not found: " + aab);
        }
        Files.createDirectories(output.getParent());

        String adbArg = config.adbPath.isBlank() ? "adb" : config.adbPath;
        String resolvedDeviceId = resolveDeviceId(adbArg, config.mode, config.installAfterBuild, config.deviceId, logSink);
        Signing signing = resolveSigning(config, logSink);
        Path javaBin = resolveJavaBinary();
        String aabSha256 = sha256(aab);

        logSink.accept("当前任务：");
        logSink.accept("  AAB 文件：" + aab);
        logSink.accept("  输出 APKS：" + output);
        logSink.accept("  bundletool：" + bundletool);
        logSink.accept("  ADB：" + adbArg);
        logSink.accept("  安装模式：" + config.mode);
        logSink.accept("  静态检查：" + (config.runLegacyChecks ? "开启" : "关闭"));
        logSink.accept("  安装后自动启动：" + (config.autoLaunchAfterInstall ? "开启" : "关闭"));
        logSink.accept("  签名冲突自动卸载重试：" + (config.autoUninstallOnSignatureMismatch ? "开启" : "关闭"));
        if (aapt2 != null) {
            logSink.accept("  AAPT2：" + aapt2);
        }
        if (!resolvedDeviceId.isBlank()) {
            logSink.accept("  设备 ID：" + resolvedDeviceId);
        }
        logSink.accept("  签名方式：" + signing.summary);
        logSink.accept("");

        logSink.accept("  AAB SHA-256: " + aabSha256);
        StringBuilder buildOutput = new StringBuilder();
        List<String> buildCommand = new ArrayList<>();
        buildCommand.add(javaBin.toString());
        buildCommand.add("-jar");
        buildCommand.add(bundletool.toString());
        buildCommand.add("build-apks");
        buildCommand.add("--bundle=" + aab);
        buildCommand.add("--output=" + output);
        buildCommand.add("--overwrite");
        buildCommand.add("--verbose");
        if (config.mode.isConnectedDevice()) {
            buildCommand.add("--connected-device");
            if (!resolvedDeviceId.isBlank()) {
                buildCommand.add("--device-id=" + resolvedDeviceId);
            }
        } else {
            buildCommand.add("--mode=universal");
        }
        if (aapt2 != null) {
            buildCommand.add("--aapt2=" + aapt2);
        }
        if (!"adb".equalsIgnoreCase(adbArg)) {
            buildCommand.add("--adb=" + adbArg);
        }
        buildCommand.add("--ks=" + signing.keystore);
        buildCommand.add("--ks-pass=pass:" + signing.storePassword);
        buildCommand.add("--ks-key-alias=" + signing.keyAlias);
        buildCommand.add("--key-pass=pass:" + signing.keyPassword);

        int buildExit = runCommand(buildCommand, buildOutput, logSink);
        if (buildExit != 0) {
            emitHints(buildOutput.toString(), logSink);
            return new ExecutionResult(false, "构建失败，请查看上方日志。");
        }

        if (config.runLegacyChecks) {
            runLegacyChecks(aab, output, aapt2, logSink);
        }

        if (!config.installAfterBuild) {
            return new ExecutionResult(true, "APK 集构建完成：" + output);
        }

        LaunchTarget launchTarget = null;
        if (aapt2 != null && !isBlank(resolvedDeviceId)) {
            launchTarget = detectLaunchTarget(output, aapt2, logSink);
            if (launchTarget != null && !isBlank(launchTarget.packageName)) {
                boolean installed = isPackageInstalled(launchTarget.packageName, adbArg, resolvedDeviceId, logSink);
                if (installed) {
                    logSink.accept("设备上已安装同包名应用：" + launchTarget.packageName);
                    if (!config.autoUninstallOnSignatureMismatch) {
                        logSink.accept("提示：如果旧应用签名不同，安装时可能出现 UPDATE_INCOMPATIBLE。");
                    }
                    logSink.accept("");
                }
            }
        }

        StringBuilder installOutput = new StringBuilder();
        List<String> installCommand = new ArrayList<>();
        installCommand.add(javaBin.toString());
        installCommand.add("-jar");
        installCommand.add(bundletool.toString());
        installCommand.add("install-apks");
        installCommand.add("--apks=" + output);
        if (!resolvedDeviceId.isBlank()) {
            installCommand.add("--device-id=" + resolvedDeviceId);
        }
        if (!"adb".equalsIgnoreCase(adbArg)) {
            installCommand.add("--adb=" + adbArg);
        }
        if (config.allowDowngrade) {
            installCommand.add("--allow-downgrade");
        }
        if (config.grantRuntimePermissions) {
            installCommand.add("--grant-runtime-permissions");
        }

        int installExit = runCommand(installCommand, installOutput, logSink);
        if (installExit != 0
            && containsUpdateIncompatible(installOutput.toString())
            && config.autoUninstallOnSignatureMismatch
            && launchTarget != null
            && !isBlank(launchTarget.packageName)) {
            logSink.accept("检测到 INSTALL_FAILED_UPDATE_INCOMPATIBLE，正在卸载旧应用并重试一次...");
            boolean uninstallOk = uninstallPackage(launchTarget.packageName, adbArg, resolvedDeviceId, logSink);
            if (uninstallOk) {
                installOutput.setLength(0);
                installExit = runCommand(installCommand, installOutput, logSink);
            } else {
                logSink.accept("自动卸载失败，已跳过重试安装。");
                logSink.accept("");
            }
        }
        if (installExit != 0) {
            emitHints(installOutput.toString(), logSink);
            return new ExecutionResult(false, "安装失败，请查看上方日志。");
        }

        String launchStatus = "";
        if (config.autoLaunchAfterInstall) {
            launchStatus = launchInstalledApp(output, aapt2, adbArg, resolvedDeviceId, launchTarget, logSink);
        }

        if (!launchStatus.isBlank()) {
            return new ExecutionResult(true, "安装完成。 " + launchStatus);
        }
        return new ExecutionResult(true, "安装完成。");
    }

    private static String launchInstalledApp(
        Path apks,
        Path aapt2,
        String adbArg,
        String resolvedDeviceId,
        LaunchTarget knownTarget,
        java.util.function.Consumer<String> logSink
    ) {
        if (isBlank(resolvedDeviceId)) {
            logSink.accept("自动启动已跳过：未解析到设备 ID。");
            logSink.accept("");
            return "已跳过自动启动。";
        }
        if (aapt2 == null) {
            logSink.accept("自动启动已跳过：缺少 AAPT2，无法解析启动页。");
            logSink.accept("");
            return "已跳过自动启动。";
        }

        try {
            LaunchTarget target = knownTarget != null ? knownTarget : detectLaunchTarget(apks, aapt2, logSink);
            if (target == null || isBlank(target.packageName)) {
                logSink.accept("自动启动已跳过：无法从生成的 APK 中解析包名。");
                logSink.accept("");
                return "已跳过自动启动。";
            }

            boolean launched = false;
            if (!isBlank(target.launchableActivity)) {
                launched = startByExplicitActivity(target, adbArg, resolvedDeviceId, logSink);
            }
            if (!launched) {
                launched = startByMonkey(target.packageName, adbArg, resolvedDeviceId, logSink);
            }
            if (launched) {
                return "应用已自动启动。";
            }
            return "安装成功，但自动启动失败。";
        } catch (Exception e) {
            logSink.accept("自动启动失败：" + e.getMessage());
            logSink.accept("");
            return "安装成功，但自动启动失败。";
        }
    }

    private static boolean isPackageInstalled(
        String packageName,
        String adbArg,
        String resolvedDeviceId,
        java.util.function.Consumer<String> logSink
    ) throws Exception {
        StringBuilder output = new StringBuilder();
        List<String> command = buildAdbShellCommand(adbArg, resolvedDeviceId, "pm", "path", packageName);
        int exit = runCommand(command, output, logSink);
        if (exit != 0) {
            return false;
        }
        String text = output.toString();
        return text.contains("package:") || text.contains("package:/");
    }

    private static boolean uninstallPackage(
        String packageName,
        String adbArg,
        String resolvedDeviceId,
        java.util.function.Consumer<String> logSink
    ) throws Exception {
        StringBuilder output = new StringBuilder();
        List<String> command = buildAdbShellCommand(adbArg, resolvedDeviceId, "pm", "uninstall", packageName);
        int exit = runCommand(command, output, logSink);
        String lower = output.toString().toLowerCase(Locale.ROOT);
        return exit == 0 && lower.contains("success");
    }

    private static boolean containsUpdateIncompatible(String output) {
        return output.toLowerCase(Locale.ROOT).contains("install_failed_update_incompatible");
    }

    private static LaunchTarget detectLaunchTarget(Path apks, Path aapt2, java.util.function.Consumer<String> logSink) throws Exception {
        ExtractedApk extractedApk = extractApkForInspection(apks);
        if (extractedApk == null) {
            return null;
        }

        try {
            String badging = runCommandQuietly(List.of(aapt2.toString(), "dump", "badging", extractedApk.path.toString()));
            String packageName = findFirstGroup(PACKAGE_NAME_PATTERN, badging);
            String activityName = findFirstGroup(LAUNCHABLE_ACTIVITY_PATTERN, badging);
            if (isBlank(packageName)) {
                return null;
            }
            logSink.accept("自动启动目标：" + packageName + (isBlank(activityName) ? "" : " / " + activityName));
            logSink.accept("");
            return new LaunchTarget(packageName, activityName == null ? "" : activityName);
        } finally {
            Files.deleteIfExists(extractedApk.path);
        }
    }

    private static boolean startByExplicitActivity(
        LaunchTarget target,
        String adbArg,
        String resolvedDeviceId,
        java.util.function.Consumer<String> logSink
    ) throws Exception {
        String normalizedActivity = normalizeActivityName(target.packageName, target.launchableActivity);
        String component = target.packageName + "/" + normalizedActivity;
        StringBuilder output = new StringBuilder();
        List<String> command = buildAdbShellCommand(adbArg, resolvedDeviceId, "am", "start", "-n", component);
        int exit = runCommand(command, output, logSink);
        String lower = output.toString().toLowerCase(Locale.ROOT);
        return exit == 0 && !lower.contains("error") && !lower.contains("exception");
    }

    private static boolean startByMonkey(
        String packageName,
        String adbArg,
        String resolvedDeviceId,
        java.util.function.Consumer<String> logSink
    ) throws Exception {
        StringBuilder output = new StringBuilder();
        List<String> command = buildAdbShellCommand(
            adbArg,
            resolvedDeviceId,
            "monkey",
            "-p",
            packageName,
            "-c",
            "android.intent.category.LAUNCHER",
            "1"
        );
        int exit = runCommand(command, output, logSink);
        String lower = output.toString().toLowerCase(Locale.ROOT);
        return exit == 0 && !lower.contains("error") && !lower.contains("exception");
    }

    private static void runLegacyChecks(Path aab, Path apks, Path aapt2, java.util.function.Consumer<String> logSink) {
        logSink.accept("开始执行静态检查...");
        try {
            LegacyInspectionReport report = inspectGeneratedApks(aab, apks, aapt2);
            report.logToReadable(logSink);
        } catch (Exception e) {
            logSink.accept("静态检查已跳过：" + e.getMessage());
            logSink.accept("");
        }
    }

    private static LegacyInspectionReport inspectGeneratedApks(Path aab, Path apks, Path aapt2) throws IOException {
        LegacyInspectionReport report = new LegacyInspectionReport();
        report.bundleMetadataPresent = containsBundleMetadata(aab);

        try (ZipInputStream apksZip = new ZipInputStream(Files.newInputStream(apks))) {
            ZipEntry apkSetEntry;
            while ((apkSetEntry = apksZip.getNextEntry()) != null) {
                if (apkSetEntry.isDirectory()) {
                    continue;
                }
                String apkSetEntryName = apkSetEntry.getName();
                if (!apkSetEntryName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                    continue;
                }
                report.addApkName(apkSetEntryName);
                byte[] apkBytes = apksZip.readAllBytes();
                inspectSingleApk(apkSetEntryName, apkBytes, report);
            }
        }
        if (aapt2 != null) {
            inspectDeclaredPermissions(apks, aapt2, report);
        }
        augmentWithNearbySourceLocales(aab, report);
        augmentWithNearbyBuildMarkers(aab, report);
        return report;
    }

    private static void inspectDeclaredPermissions(Path apks, Path aapt2, LegacyInspectionReport report) throws IOException {
        ExtractedApk extractedApk = extractApkForInspection(apks);
        try {
            String output = runCommandQuietly(List.of(aapt2.toString(), "dump", "badging", extractedApk.path.toString()));
            Matcher matcher = USES_PERMISSION_PATTERN.matcher(output);
            while (matcher.find()) {
                report.addPermission(matcher.group(1));
            }
            Boolean debuggable = resolveDebuggableFromBadging(output);
            if (debuggable == null) {
                debuggable = resolveDebuggableFromManifest(extractedApk.path, aapt2);
            }
            if (debuggable != null) {
                report.setDebuggable(debuggable);
            }
            if (!collectLocalesFromAppLabelResource(extractedApk.path, aapt2, report)) {
                collectLocalesFromBadging(output, report);
                collectLocalesFromConfigurations(extractedApk.path, aapt2, report);
            }
        } catch (Exception ignored) {
            try {
                Boolean debuggable = resolveDebuggableFromManifest(extractedApk.path, aapt2);
                if (debuggable != null) {
                    report.setDebuggable(debuggable);
                }
                if (!collectLocalesFromAppLabelResource(extractedApk.path, aapt2, report)) {
                    collectLocalesFromConfigurations(extractedApk.path, aapt2, report);
                }
            } catch (Exception ignoredAgain) {
                // Keep permission reporting best-effort to avoid breaking installation flow.
            }
            // Keep permission reporting best-effort to avoid breaking installation flow.
        } finally {
            Files.deleteIfExists(extractedApk.path);
        }
    }

    private static boolean collectLocalesFromAppLabelResource(Path apk, Path aapt2, LegacyInspectionReport report)
        throws IOException, InterruptedException {
        String xmlTree = runCommandQuietly(List.of(aapt2.toString(), "dump", "xmltree", "--file", "AndroidManifest.xml", apk.toString()));
        Matcher labelMatcher = APPLICATION_LABEL_RESOURCE_PATTERN.matcher(xmlTree);
        if (!labelMatcher.find()) {
            return false;
        }
        String targetId = "0x" + labelMatcher.group(1).toLowerCase(Locale.ROOT);
        String resources = runCommandQuietly(List.of(aapt2.toString(), "dump", "resources", apk.toString()));
        boolean inTargetResource = false;
        boolean foundAny = false;
        for (String line : resources.split("\\R")) {
            Matcher headerMatcher = RESOURCE_HEADER_PATTERN.matcher(line);
            if (headerMatcher.find()) {
                String currentId = "0x" + headerMatcher.group(1).toLowerCase(Locale.ROOT);
                if (inTargetResource && !currentId.equals(targetId)) {
                    break;
                }
                inTargetResource = currentId.equals(targetId);
                continue;
            }
            if (!inTargetResource) {
                continue;
            }
            Matcher localeMatcher = RESOURCE_LOCALE_VALUE_PATTERN.matcher(line);
            if (!localeMatcher.find()) {
                continue;
            }
            String normalized = normalizeResourceLocale(localeMatcher.group(1));
            if (normalized != null) {
                report.addLocale(normalized);
                foundAny = true;
            }
        }
        return foundAny;
    }

    private static void collectLocalesFromBadging(String output, LegacyInspectionReport report) {
        boolean collecting = false;
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("locales:")) {
                collecting = true;
                collectQuotedLocales(trimmed, report);
                continue;
            }
            if (collecting) {
                if (trimmed.startsWith("'")) {
                    collectQuotedLocales(trimmed, report);
                    continue;
                }
                break;
            }
        }
    }

    private static void collectQuotedLocales(String value, LegacyInspectionReport report) {
        Matcher localeMatcher = Pattern.compile("'([^']+)'").matcher(value);
        while (localeMatcher.find()) {
            report.addLocale(localeMatcher.group(1));
        }
    }

    private static void collectLocalesFromConfigurations(Path apk, Path aapt2, LegacyInspectionReport report)
        throws IOException, InterruptedException {
        String output = runCommandQuietly(List.of(aapt2.toString(), "dump", "configurations", apk.toString()));
        for (String line : output.split("\\R")) {
            String normalized = normalizeLocaleQualifier(line.trim());
            if (normalized != null) {
                report.addLocale(normalized);
            }
        }
    }

    private static String normalizeLocaleQualifier(String qualifier) {
        if (qualifier == null || qualifier.isBlank()) {
            return null;
        }
        if (qualifier.matches("[a-z]{2,3}")) {
            return qualifier;
        }
        if (qualifier.matches("[a-z]{2,3}-r[A-Z0-9]{2,3}")) {
            return qualifier.replace("-r", "-");
        }
        if (qualifier.matches("b\\+[a-zA-Z0-9]{2,8}(\\+[a-zA-Z0-9]{2,8})+")) {
            String[] parts = qualifier.split("\\+");
            if (parts.length >= 2) {
                StringBuilder locale = new StringBuilder(parts[1]);
                for (int i = 2; i < parts.length; i++) {
                    locale.append("-").append(parts[i]);
                }
                return locale.toString();
            }
        }
        return null;
    }

    private static String normalizeResourceLocale(String qualifier) {
        if (qualifier == null) {
            return null;
        }
        String trimmed = qualifier.trim();
        if (trimmed.isEmpty()) {
            return "en";
        }
        return normalizeLocaleQualifier(trimmed);
    }

    private static String normalizeSourceValuesDirectory(String directoryName) {
        if ("values".equals(directoryName) || "values-night".equals(directoryName)) {
            return null;
        }
        if (!directoryName.startsWith("values-")) {
            return null;
        }
        String suffix = directoryName.substring("values-".length());
        if (suffix.isBlank() || suffix.contains("night") || suffix.contains("land") || suffix.contains("port")) {
            return null;
        }
        return normalizeLocaleQualifier(suffix);
    }

    private static ExtractedApk extractApkForInspection(Path apks) throws IOException {
        Path tempApk = Files.createTempFile("aabtool-launch-", ".apk");
        boolean copied = false;
        try (ZipInputStream apksZip = new ZipInputStream(Files.newInputStream(apks))) {
            ZipEntry apkSetEntry;
            String fallbackName = null;
            byte[] fallbackBytes = null;
            while ((apkSetEntry = apksZip.getNextEntry()) != null) {
                if (apkSetEntry.isDirectory()) {
                    continue;
                }
                String apkSetEntryName = apkSetEntry.getName();
                if (!apkSetEntryName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                    continue;
                }
                byte[] apkBytes = apksZip.readAllBytes();
                String lower = apkSetEntryName.toLowerCase(Locale.ROOT);
                if ("universal.apk".equals(lower) || lower.contains("standalone") || lower.contains("base-master") || lower.endsWith("/base.apk")) {
                    Files.write(tempApk, apkBytes);
                    copied = true;
                    return new ExtractedApk(tempApk);
                }
                if (fallbackBytes == null) {
                    fallbackName = apkSetEntryName;
                    fallbackBytes = apkBytes;
                }
            }
            if (fallbackBytes != null) {
                Files.write(tempApk, fallbackBytes);
                copied = true;
                return new ExtractedApk(tempApk);
            }
            throw new IOException("No APK entries found inside " + apks);
        } finally {
            if (!copied) {
                Files.deleteIfExists(tempApk);
            }
        }
    }

    private static Boolean resolveDebuggableFromBadging(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        return output.contains("application-debuggable");
    }

    private static Boolean resolveDebuggableFromManifest(Path apk, Path aapt2) throws IOException, InterruptedException {
        String xmlTree = runCommandQuietly(List.of(aapt2.toString(), "dump", "xmltree", apk.toString(), "AndroidManifest.xml"));
        Matcher matcher = DEBUGGABLE_XMLTREE_PATTERN.matcher(xmlTree);
        if (matcher.find()) {
            String raw = matcher.group(1);
            try {
                return Long.parseLong(raw, 16) != 0L;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (xmlTree.contains("android:debuggable")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private static boolean containsBundleMetadata(Path aab) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(aab))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith("BUNDLE-METADATA/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void inspectSingleApk(String apkName, byte[] apkBytes, LegacyInspectionReport report) throws IOException {
        try (ZipInputStream apkZip = new ZipInputStream(new ByteArrayInputStream(apkBytes))) {
            ZipEntry entry;
            while ((entry = apkZip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (containsChinese(entryName)) {
                    report.addChinese(apkName + "!" + entryName);
                }
                if (entryName.startsWith("res/")) {
                    report.addResourceEntry(entryName);
                }

                byte[] entryBytes = apkZip.readAllBytes();
                String lowerEntryName = entryName.toLowerCase(Locale.ROOT);
                if (lowerEntryName.endsWith(".dex")) {
                    inspectDexFile(apkName, entryName, entryBytes, report);
                    continue;
                }
                if (shouldScanBinaryEntry(lowerEntryName, entryBytes)) {
                    inspectBinaryEntry(apkName, entryName, entryBytes, report);
                }
            }
        }
    }

    private static void inspectDexFile(String apkName, String entryName, byte[] dexBytes, LegacyInspectionReport report) {
        List<String> dexStrings;
        try {
            dexStrings = readDexStrings(dexBytes);
        } catch (Exception e) {
            report.addLog(apkName + "!" + entryName + " -> dex parse skipped: " + e.getMessage());
            return;
        }

        for (String value : dexStrings) {
            inspectReadableValue(apkName + "!" + entryName, value, report);
            if (CLASS_DESCRIPTOR_PATTERN.matcher(value).matches()) {
                report.addClassDescriptor(value);
            }
        }
    }

    private static void inspectBinaryEntry(String apkName, String entryName, byte[] bytes, LegacyInspectionReport report) {
        boolean allowChinese = isTextLikeEntry(entryName);
        for (String candidate : extractAsciiStrings(bytes, 6)) {
            inspectReadableValue(apkName + "!" + entryName, candidate, report, allowChinese);
        }
        for (String candidate : extractUtf16LeStrings(bytes, 4)) {
            inspectReadableValue(apkName + "!" + entryName, candidate, report, allowChinese);
        }
    }

    private static void inspectReadableValue(String source, String rawValue, LegacyInspectionReport report) {
        inspectReadableValue(source, rawValue, report, true);
    }

    private static void inspectReadableValue(String source, String rawValue, LegacyInspectionReport report, boolean allowChinese) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            return;
        }

        if (allowChinese && containsMeaningfulChinese(value)) {
            report.addChinese(source + " -> " + abbreviate(value));
        }

        Matcher admobMatcher = ADMOB_PATTERN.matcher(value);
        while (admobMatcher.find()) {
            String adUnitId = admobMatcher.group().trim();
            if (adUnitId.startsWith(ADMOB_TEST_UNIT_PREFIX)) {
                report.addAdmob(adUnitId + "（测试 ID）");
            } else {
                report.addAdmob(adUnitId);
            }
        }

        if (value.contains(ADMOB_TEST_APP_ID)) {
            report.addAdmob(ADMOB_TEST_APP_ID + "（测试 App ID）");
        }

        String lower = value.toLowerCase(Locale.ROOT);
        for (String marker : STRINGFOG_MARKERS) {
            if (lower.contains(marker)) {
                report.addStringFog(source + " -> " + abbreviate(value));
                break;
            }
        }

        if (lower.contains("android/util/log") || lower.contains("landroid/util/log;")) {
            report.addLog(source + " -> " + abbreviate(value));
        }
    }

    private static boolean isTextLikeEntry(String entryName) {
        String lower = entryName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("assets/")) {
            return hasTextExtension(lower);
        }
        if (lower.startsWith("res/raw/")) {
            return hasTextExtension(lower);
        }
        if (lower.startsWith("meta-inf/")) {
            return hasTextExtension(lower);
        }
        return false;
    }

    private static boolean hasTextExtension(String value) {
        return value.endsWith(".txt")
            || value.endsWith(".json")
            || value.endsWith(".xml")
            || value.endsWith(".html")
            || value.endsWith(".htm")
            || value.endsWith(".js")
            || value.endsWith(".css")
            || value.endsWith(".csv")
            || value.endsWith(".properties")
            || value.endsWith(".yml")
            || value.endsWith(".yaml")
            || value.endsWith(".svg")
            || value.endsWith(".md")
            || value.endsWith(".pro");
    }

    private static String findFirstGroup(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String normalizeActivityName(String packageName, String activityName) {
        if (isBlank(activityName)) {
            return "";
        }
        if (activityName.startsWith(".")) {
            return packageName + activityName;
        }
        if (activityName.contains("/")) {
            return activityName;
        }
        if (activityName.contains(".")) {
            return activityName;
        }
        return packageName + "." + activityName;
    }

    private static List<String> buildAdbShellCommand(String adbArg, String resolvedDeviceId, String... shellArgs) {
        List<String> command = new ArrayList<>();
        command.add(adbArg);
        if (!isBlank(resolvedDeviceId)) {
            command.add("-s");
            command.add(resolvedDeviceId);
        }
        command.add("shell");
        command.addAll(List.of(shellArgs));
        return command;
    }

    private static List<String> readDexStrings(byte[] dexBytes) {
        if (dexBytes.length < 64) {
            return List.of();
        }
        if (dexBytes[0] != 'd' || dexBytes[1] != 'e' || dexBytes[2] != 'x') {
            return List.of();
        }

        int stringIdsSize = readLittleEndianInt(dexBytes, 56);
        int stringIdsOffset = readLittleEndianInt(dexBytes, 60);
        if (stringIdsSize < 0 || stringIdsOffset < 0 || stringIdsOffset + (long) stringIdsSize * 4L > dexBytes.length) {
            return List.of();
        }

        List<String> strings = new ArrayList<>(Math.max(0, stringIdsSize));
        for (int i = 0; i < stringIdsSize; i++) {
            int itemOffset = stringIdsOffset + i * 4;
            int stringDataOffset = readLittleEndianInt(dexBytes, itemOffset);
            if (stringDataOffset <= 0 || stringDataOffset >= dexBytes.length) {
                continue;
            }
            int cursor = skipUleb128(dexBytes, stringDataOffset);
            if (cursor < 0 || cursor >= dexBytes.length) {
                continue;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while (cursor < dexBytes.length && dexBytes[cursor] != 0) {
                buffer.write(dexBytes[cursor]);
                cursor++;
            }
            strings.add(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        }
        return strings;
    }

    private static int readLittleEndianInt(byte[] bytes, int offset) {
        if (offset < 0 || offset + 4 > bytes.length) {
            return -1;
        }
        return (bytes[offset] & 0xFF)
            | ((bytes[offset + 1] & 0xFF) << 8)
            | ((bytes[offset + 2] & 0xFF) << 16)
            | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static int skipUleb128(byte[] bytes, int offset) {
        int cursor = offset;
        int guard = 0;
        while (cursor < bytes.length && guard < 5) {
            int current = bytes[cursor] & 0xFF;
            cursor++;
            if ((current & 0x80) == 0) {
                return cursor;
            }
            guard++;
        }
        return -1;
    }

    private static List<String> extractAsciiStrings(byte[] bytes, int minLength) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (byte value : bytes) {
            int code = value & 0xFF;
            if (code == 9 || code == 10 || code == 13 || (code >= 32 && code <= 126)) {
                current.append((char) code);
            } else {
                flushCandidate(values, current, minLength);
            }
        }
        flushCandidate(values, current, minLength);
        return values;
    }

    private static List<String> extractUtf16LeStrings(byte[] bytes, int minLength) {
        List<String> values = new ArrayList<>();
        for (int parity = 0; parity <= 1; parity++) {
            StringBuilder current = new StringBuilder();
            for (int i = parity; i + 1 < bytes.length; i += 2) {
                int low = bytes[i] & 0xFF;
                int high = bytes[i + 1] & 0xFF;
                char value = (char) (low | (high << 8));
                if (isLikelyUtf16LeChar(low, high, value)) {
                    current.append(value);
                } else {
                    flushCandidate(values, current, minLength);
                }
            }
            flushCandidate(values, current, minLength);
        }
        return values;
    }

    private static boolean isReadableChar(char value) {
        if (value == 0) {
            return false;
        }
        if (Character.isISOControl(value) && !Character.isWhitespace(value)) {
            return false;
        }
        return !Character.isSurrogate(value);
    }

    private static boolean isLikelyUtf16LeChar(int low, int high, char value) {
        if (!isReadableChar(value)) {
            return false;
        }
        if (Character.UnicodeScript.of(value) == Character.UnicodeScript.HAN) {
            return low > 0x7F || high > 0x7F;
        }
        return high == 0;
    }

    private static void flushCandidate(List<String> values, StringBuilder current, int minLength) {
        if (current.length() >= minLength) {
            values.add(current.toString().trim());
        }
        current.setLength(0);
    }

    private static boolean shouldScanBinaryEntry(String lowerEntryName, byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_GENERIC_SCAN_BYTES) {
            return false;
        }
        if (isIgnoredBinaryMetadataEntry(lowerEntryName)) {
            return false;
        }
        for (String extension : GENERIC_SCAN_SKIPPED_EXTENSIONS) {
            if (lowerEntryName.endsWith(extension)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIgnoredBinaryMetadataEntry(String lowerEntryName) {
        if (!lowerEntryName.startsWith("meta-inf/")) {
            return false;
        }
        return lowerEntryName.endsWith(".rsa")
            || lowerEntryName.endsWith(".dsa")
            || lowerEntryName.endsWith(".ec")
            || lowerEntryName.endsWith(".sf")
            || lowerEntryName.endsWith(".mf");
    }

    private static boolean containsChinese(String value) {
        return CHINESE_PATTERN.matcher(value).find();
    }

    private static boolean containsMeaningfulChinese(String value) {
        if (!containsChinese(value)) {
            return false;
        }
        int hanCount = 0;
        int noisyCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.UnicodeScript.of(current) == Character.UnicodeScript.HAN) {
                hanCount++;
            } else if (Character.isLetterOrDigit(current) || Character.isWhitespace(current) || isCommonReadablePunctuation(current)) {
                // allowed
            } else {
                noisyCount++;
            }
        }
        return hanCount >= 2 && noisyCount == 0;
    }

    private static boolean isCommonReadablePunctuation(char value) {
        return ",.!?;:'\"()[]{}<>+-_=/#@&%*~|，。！？；：（）【】《》、".indexOf(value) >= 0;
    }

    private static void augmentWithNearbyBuildMarkers(Path aab, LegacyInspectionReport report) {
        Path current = aab.toAbsolutePath().normalize().getParent();
        List<String> candidates = List.of(
            "build.gradle",
            "build.gradle.kts",
            "settings.gradle",
            "settings.gradle.kts"
        );
        for (int level = 0; level < 6 && current != null; level++) {
            for (String candidate : candidates) {
                inspectPotentialMarkerFile(current.resolve(candidate), report);
            }
            inspectPotentialMarkerFile(current.resolve("gradle").resolve("libs.versions.toml"), report);
            current = current.getParent();
        }
    }

    private static void augmentWithNearbySourceLocales(Path aab, LegacyInspectionReport report) {
        Path current = aab.toAbsolutePath().normalize().getParent();
        for (int level = 0; level < 8 && current != null; level++) {
            Path resDir = current.resolve("src").resolve("main").resolve("res");
            if (Files.isDirectory(resDir)) {
                LinkedHashSet<String> sourceLocales = collectLocalesFromResDirectory(resDir);
                if (!sourceLocales.isEmpty()) {
                    report.replaceLocales(sourceLocales);
                    return;
                }
            }
            current = current.getParent();
        }
    }

    private static LinkedHashSet<String> collectLocalesFromResDirectory(Path resDir) {
        LinkedHashSet<String> locales = new LinkedHashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(resDir, "values*")) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                String normalized = normalizeSourceValuesDirectory(dir.getFileName().toString());
                if (normalized != null) {
                    locales.add(normalized);
                }
            }
        } catch (IOException ignored) {
        }
        return locales;
    }

    private static void inspectPotentialMarkerFile(Path file, LegacyInspectionReport report) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            String text = Files.readString(file).toLowerCase(Locale.ROOT);
            if (text.contains("stringfog")) {
                report.addStringFog("附近构建配置 -> " + file);
            }
            if (text.contains("aabresguard")) {
                report.addAabResGuard("附近构建配置 -> " + file);
            }
        } catch (IOException ignored) {
        }
    }

    private static String abbreviate(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_SAMPLE_TEXT) {
            return normalized;
        }
        return normalized.substring(0, MAX_SAMPLE_TEXT - 3) + "...";
    }

    private static String sanitizeDisplayText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
            .replace("锛堟祴璇?ID锛?", "（测试 ID）")
            .replace("锛堟祴璇?App ID锛?", "（测试 App ID）")
            .replace("闄勮繎鏋勫缓閰嶇疆 -> ", "附近构建配置 -> ");
    }

    private static int runCli(String[] args) {
        try {
            CliOptions options = parseCli(args);
            if (options.showHelp) {
                printCliHelp();
                return 0;
            }

            ToolConfig config = options.config;
            if (isBlank(config.aabPath)) {
                throw new IllegalArgumentException("Missing required option: --aab");
            }
            if (config.mode == null) {
                config.mode = InstallMode.CONNECTED_DEVICE;
            }
            if (isBlank(config.outputPath)) {
                config.outputPath = deriveOutputPath(Paths.get(config.aabPath), config.mode).toString();
            }
            if (config.bundletoolPath == null) {
                config.bundletoolPath = "";
            }
            if (config.adbPath == null) {
                config.adbPath = "adb";
            }
            if (config.aapt2Path == null) {
                config.aapt2Path = "";
            }
            if (config.deviceId == null) {
                config.deviceId = "";
            }
            if (config.keystorePath == null) {
                config.keystorePath = "";
            }
            if (config.keystorePassword == null) {
                config.keystorePassword = "";
            }
            if (config.keyAlias == null) {
                config.keyAlias = "";
            }
            if (config.keyPassword == null) {
                config.keyPassword = "";
            }

            ExecutionResult result = runWorkflow(config, System.out::println);
            System.out.println(result.message);
            return result.success ? 0 : 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            printCliHelp();
            return 1;
        }
    }

    private static CliOptions parseCli(String[] args) {
        CliOptions options = new CliOptions();
        ToolConfig config = options.config;
        config.mode = InstallMode.CONNECTED_DEVICE;
        config.installAfterBuild = true;
        config.allowDowngrade = false;
        config.grantRuntimePermissions = true;
        config.runLegacyChecks = true;
        config.autoLaunchAfterInstall = true;
        config.autoUninstallOnSignatureMismatch = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help":
                case "-h":
                    options.showHelp = true;
                    return options;
                case "--aab":
                    config.aabPath = requireValue(args, ++i, arg);
                    break;
                case "--output":
                    config.outputPath = requireValue(args, ++i, arg);
                    break;
                case "--bundletool":
                    config.bundletoolPath = requireValue(args, ++i, arg);
                    break;
                case "--adb":
                    config.adbPath = requireValue(args, ++i, arg);
                    break;
                case "--aapt2":
                    config.aapt2Path = requireValue(args, ++i, arg);
                    break;
                case "--device-id":
                    config.deviceId = requireValue(args, ++i, arg);
                    break;
                case "--ks":
                    config.keystorePath = requireValue(args, ++i, arg);
                    break;
                case "--ks-pass":
                    config.keystorePassword = requireValue(args, ++i, arg);
                    break;
                case "--key-alias":
                    config.keyAlias = requireValue(args, ++i, arg);
                    break;
                case "--key-pass":
                    config.keyPassword = requireValue(args, ++i, arg);
                    break;
                case "--mode":
                    config.mode = parseMode(requireValue(args, ++i, arg));
                    break;
                case "--no-install":
                    config.installAfterBuild = false;
                    break;
                case "--allow-downgrade":
                    config.allowDowngrade = true;
                    break;
                case "--no-grant-runtime-permissions":
                    config.grantRuntimePermissions = false;
                    break;
                case "--no-analysis":
                    config.runLegacyChecks = false;
                    break;
                case "--no-launch":
                    config.autoLaunchAfterInstall = false;
                    break;
                case "--replace-incompatible":
                    config.autoUninstallOnSignatureMismatch = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        return options;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static InstallMode parseMode(String value) {
        if ("connected-device".equalsIgnoreCase(value) || "connected".equalsIgnoreCase(value)) {
            return InstallMode.CONNECTED_DEVICE;
        }
        if ("universal".equalsIgnoreCase(value)) {
            return InstallMode.UNIVERSAL;
        }
        throw new IllegalArgumentException("Unsupported mode: " + value);
    }

    private static void printCliHelp() {
        System.out.println("AAB Tool GUI 2.0 CLI");
        System.out.println("Usage:");
        System.out.println("  java -jar aabtool-gui-2.0.jar --aab <file.aab> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --aab <path>                           Required. Input AAB file.");
        System.out.println("  --output <path>                        Optional. Output .apks path.");
        System.out.println("  --bundletool <path>                    Optional. bundletool-all jar path.");
        System.out.println("  --adb <path>                           Optional. adb path. Default: adb");
        System.out.println("  --aapt2 <path>                         Optional. Override AAPT2 binary.");
        System.out.println("  --device-id <serial>                   Optional. Target device serial.");
        System.out.println("  --mode connected-device|universal      Optional. Default: connected-device");
        System.out.println("  --no-install                           Build only, do not install.");
        System.out.println("  --allow-downgrade                      Pass through to install-apks.");
        System.out.println("  --no-grant-runtime-permissions         Disable grant-runtime-permissions.");
        System.out.println("  --no-analysis                          Skip legacy checks after build.");
        System.out.println("  --no-launch                            Do not auto launch the app after install.");
        System.out.println("  --replace-incompatible                 Uninstall existing app and retry if signature mismatch is detected.");
        System.out.println("  --ks <path>                            Optional. Custom keystore path.");
        System.out.println("  --ks-pass <password>                   Optional. Custom keystore password.");
        System.out.println("  --key-alias <alias>                    Optional. Custom key alias.");
        System.out.println("  --key-pass <password>                  Optional. Custom key password.");
        System.out.println("  --help                                 Show this help.");
    }

    private static Path resolveJavaBinary() {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        String javaName = isWindows() ? "java.exe" : "java";
        Path candidate = javaHome.resolve("bin").resolve(javaName);
        if (Files.exists(candidate)) {
            return candidate;
        }
        return Paths.get("java");
    }

    private static Path resolveBundletool(String rawPath) {
        if (rawPath != null && !rawPath.isBlank()) {
            Path explicit = Paths.get(rawPath).toAbsolutePath().normalize();
            if (Files.exists(explicit)) {
                return explicit;
            }
            Path bundled = ensureBundledBundletool();
            if (Files.exists(bundled)) {
                return bundled.toAbsolutePath().normalize();
            }
            return explicit;
        }
        Path bundled = ensureBundledBundletool();
        if (Files.exists(bundled)) {
            return bundled.toAbsolutePath().normalize();
        }
        return Paths.get(detectBundletoolPath()).toAbsolutePath().normalize();
    }

    private static String normalizeSavedBundletoolPath(String rawPath) {
        if (isBlank(rawPath)) {
            return "";
        }
        Path explicit = Paths.get(rawPath).toAbsolutePath().normalize();
        if (!Files.exists(explicit)) {
            return "";
        }
        try {
            Path bundled = ensureBundledBundletool().toAbsolutePath().normalize();
            if (Files.exists(bundled) && explicit.equals(bundled)) {
                return "";
            }
        } catch (Exception ignored) {
        }
        return explicit.toString();
    }

    private static Path resolveAapt2(String rawPath) {
        if (!isBlank(rawPath)) {
            return resolveExistingFile(Paths.get(rawPath), "AAPT2");
        }
        String detected = detectLatestAapt2Path();
        if (isBlank(detected)) {
            return null;
        }
        return resolveExistingFile(Paths.get(detected), "AAPT2");
    }

    private static Path resolveExistingFile(Path path, String label) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException(label + " not found: " + path);
        }
        return path;
    }

    private static String resolveDeviceId(
        String adbArg,
        InstallMode mode,
        boolean installAfterBuild,
        String requestedDeviceId,
        java.util.function.Consumer<String> logSink
    ) throws Exception {
        if (!mode.isConnectedDevice() && !installAfterBuild) {
            return requestedDeviceId == null ? "" : requestedDeviceId.trim();
        }
        List<String> devices = listDevices(adbArg, logSink);
        if (!requestedDeviceId.isBlank()) {
            if (devices.contains(requestedDeviceId)) {
                return requestedDeviceId;
            }
            if (devices.size() == 1) {
                String fallback = devices.get(0);
                logSink.accept("提示：已保存的 Device ID 不在线，已自动切换到当前在线设备：" + fallback);
                logSink.accept("");
                return fallback;
            }
            throw new IllegalArgumentException("Configured device is not online: " + requestedDeviceId);
        }
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("No online Android device found.");
        }
        if (devices.size() > 1) {
            throw new IllegalArgumentException("Multiple devices are online. Fill in Device ID explicitly.");
        }
        return devices.get(0);
    }

    private static List<String> listDevices(String adbArg, java.util.function.Consumer<String> logSink) throws Exception {
        StringBuilder output = new StringBuilder();
        List<String> command = new ArrayList<>();
        command.add(adbArg);
        command.add("devices");
        int exit = runCommand(command, output, logSink);
        if (exit != 0) {
            throw new IllegalArgumentException("Unable to query adb devices. Check ADB path.");
        }

        List<String> devices = new ArrayList<>();
        for (String line : output.toString().split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("List of devices attached")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2 && "device".equals(parts[1])) {
                devices.add(parts[0]);
            }
        }
        return devices;
    }

    private static Signing resolveSigning(ToolConfig config, java.util.function.Consumer<String> logSink) throws Exception {
        if (!config.keystorePath.isBlank()) {
            if (config.keystorePassword.isBlank() || config.keyAlias.isBlank() || config.keyPassword.isBlank()) {
                throw new IllegalArgumentException("When keystore is set, keystore password, key alias and key password are all required.");
            }
            Path keystore = resolveExistingFile(Paths.get(config.keystorePath), "Keystore");
            return new Signing(keystore.toString(), config.keystorePassword, config.keyAlias, config.keyPassword, "自定义 keystore");
        }

        Path stateDir = getStateDir();
        Files.createDirectories(stateDir);
        Path debugKeystore = stateDir.resolve("debug.keystore");
        if (!Files.exists(debugKeystore)) {
            logSink.accept("未配置 keystore，正在生成默认调试证书：" + debugKeystore);
            Path keytool = resolveKeytoolBinary();
            StringBuilder output = new StringBuilder();
            List<String> command = List.of(
                keytool.toString(),
                "-genkeypair",
                "-keystore",
                debugKeystore.toString(),
                "-alias",
                DEBUG_ALIAS,
                "-storepass",
                DEBUG_PASSWORD,
                "-keypass",
                DEBUG_PASSWORD,
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "36500",
                "-dname",
                "CN=AAB Tool, OU=Codex, O=Codex, L=Local, ST=Local, C=US",
                "-noprompt"
            );
            int exit = runCommand(command, output, logSink);
            if (exit != 0) {
                throw new IllegalArgumentException("默认调试证书生成失败。");
            }
        }
        return new Signing(debugKeystore.toString(), DEBUG_PASSWORD, DEBUG_ALIAS, DEBUG_PASSWORD, "默认调试证书");
    }

    private static Path resolveKeytoolBinary() {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        String keytoolName = isWindows() ? "keytool.exe" : "keytool";
        Path candidate = javaHome.resolve("bin").resolve(keytoolName);
        if (Files.exists(candidate)) {
            return candidate;
        }
        return Paths.get("keytool");
    }

    private static int runCommand(List<String> command, StringBuilder output, java.util.function.Consumer<String> logSink)
        throws IOException, InterruptedException {
        logSink.accept("$ " + maskCommand(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                logSink.accept(line);
            }
        }
        int exit = process.waitFor();
        logSink.accept("退出码: " + exit);
        logSink.accept("");
        return exit;
    }

    private static String runCommandQuietly(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("Command failed with exit code " + exit);
        }
        return output.toString();
    }

    private static void emitHints(String output, java.util.function.Consumer<String> logSink) {
        String lower = output.toLowerCase(Locale.ROOT);
        if (lower.contains("protodeserialize.cpp") || lower.contains("unknown compound value")) {
            logSink.accept("提示：这个 AAB 中存在异常或不受支持的 proto 资源。");
            logSink.accept("提示：请先对比未经过 AABResGuard 等后处理前的原始 bundle。");
            logSink.accept("提示：如果问题来自资源混淆，请缩小混淆范围后重新打包。");
            logSink.accept("");
        }
        if (lower.contains("install_failed_update_incompatible")) {
            logSink.accept("提示：设备上已安装同包名但签名不同的应用，请先卸载旧版本，或改用相同签名重新安装。");
            logSink.accept("");
        }
        if (lower.contains("install_failed_version_downgrade")) {
            logSink.accept("提示：请开启“允许降级安装”，或者安装更高 versionCode 的包。");
            logSink.accept("");
        }
        if (lower.contains("more than one device")) {
            logSink.accept("提示：检测到多台设备，请手动填写 Device ID。");
            logSink.accept("");
        }
    }

    private static String maskCommand(List<String> command) {
        List<String> masked = new ArrayList<>(command.size());
        for (int i = 0; i < command.size(); i++) {
            String token = command.get(i);
            if (token.startsWith("--ks-pass=pass:")) {
                masked.add("--ks-pass=pass:***");
            } else if (token.startsWith("--key-pass=pass:")) {
                masked.add("--key-pass=pass:***");
            } else if ("-storepass".equals(token) || "-keypass".equals(token)) {
                masked.add(token);
                if (i + 1 < command.size()) {
                    masked.add("***");
                    i++;
                }
            } else {
                masked.add(token);
            }
        }
        return String.join(" ", masked);
    }

    private static Path deriveOutputPath(Path aab, InstallMode mode) {
        String fileName = aab.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String stem = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String suffix = mode.isConnectedDevice() ? "-device.apks" : "-universal.apks";
        return aab.getParent().resolve(stem + suffix);
    }

    private static String detectBundletoolPath() {
        List<Path> candidates = new ArrayList<>();
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        candidates.add(cwd.resolve("lib").resolve(BUNDLETOOL_NAME));
        candidates.add(cwd.resolve("vendor").resolve(BUNDLETOOL_NAME));
        candidates.add(cwd.resolve("dist").resolve("lib").resolve(BUNDLETOOL_NAME));

        Path appDir = getAppDir();
        candidates.add(appDir.resolve("lib").resolve(BUNDLETOOL_NAME));
        candidates.add(appDir.resolve("vendor").resolve(BUNDLETOOL_NAME));
        candidates.add(appDir.resolve("dist").resolve("lib").resolve(BUNDLETOOL_NAME));

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }
        Path bundled = ensureBundledBundletool();
        if (Files.exists(bundled)) {
            return bundled.toString();
        }
        return bundled.toString();
    }

    private static Path ensureBundledBundletool() {
        Path extracted = getStateDir().resolve(BUNDLETOOL_NAME);
        try (var in = AabToolGuiApp.class.getResourceAsStream(BUNDLED_BUNDLETOOL_RESOURCE)) {
            if (in == null) {
                return extracted;
            }
            if (Files.exists(extracted) && Files.size(extracted) > 0) {
                return extracted;
            }
            Files.createDirectories(extracted.getParent());
            Files.copy(in, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return extracted;
        } catch (IOException ignored) {
            return extracted;
        }
    }

    private static String detectLatestAapt2Path() {
        String sdkRoot = firstNonBlank(
            System.getenv("ANDROID_SDK_ROOT"),
            System.getenv("ANDROID_HOME"),
            readSdkDirFromLocalProperties()
        );
        if (isBlank(sdkRoot)) {
            return "";
        }

        Path buildToolsDir = Paths.get(sdkRoot).resolve("build-tools");
        if (!Files.isDirectory(buildToolsDir)) {
            return "";
        }

        String executable = isWindows() ? "aapt2.exe" : "aapt2";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildToolsDir)) {
            return java.util.stream.StreamSupport.stream(stream.spliterator(), false)
                .filter(Files::isDirectory)
                .sorted(Comparator.comparing((Path path) -> path.getFileName().toString(), AabToolGuiApp::compareBuildToolsVersion).reversed())
                .map(path -> path.resolve(executable))
                .filter(Files::exists)
                .map(Path::toString)
                .findFirst()
                .orElse("");
        } catch (IOException ignored) {
            return "";
        }
    }

    private static int compareBuildToolsVersion(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int max = Math.max(l.length, r.length);
        for (int i = 0; i < max; i++) {
            int lv = i < l.length ? parseVersionPart(l[i]) : 0;
            int rv = i < r.length ? parseVersionPart(r[i]) : 0;
            if (lv != rv) {
                return Integer.compare(lv, rv);
            }
        }
        return left.compareTo(right);
    }

    private static int parseVersionPart(String raw) {
        try {
            return Integer.parseInt(raw.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(file));
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static String readSdkDirFromLocalProperties() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 4 && current != null; i++) {
            Path localProperties = current.resolve("local.properties");
            if (Files.exists(localProperties)) {
                Properties properties = new Properties();
                try (var in = Files.newInputStream(localProperties)) {
                    properties.load(in);
                    return properties.getProperty("sdk.dir", "");
                } catch (IOException ignored) {
                    return "";
                }
            }
            current = current.getParent();
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static Path getAppDir() {
        try {
            CodeSource source = AabToolGuiApp.class.getProtectionDomain().getCodeSource();
            if (source == null) {
                return Paths.get("").toAbsolutePath().normalize();
            }
            Path location = Paths.get(source.getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location.getParent();
            }
            return location;
        } catch (URISyntaxException e) {
            return Paths.get("").toAbsolutePath().normalize();
        }
    }

    private static Path getStateDir() {
        return Paths.get(System.getProperty("user.home"), ".aabtool-gui");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class Signing {
        private final String keystore;
        private final String storePassword;
        private final String keyAlias;
        private final String keyPassword;
        private final String summary;

        private Signing(String keystore, String storePassword, String keyAlias, String keyPassword, String summary) {
            this.keystore = keystore;
            this.storePassword = storePassword;
            this.keyAlias = keyAlias;
            this.keyPassword = keyPassword;
            this.summary = summary;
        }
    }
}
