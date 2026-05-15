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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.text.BadLocationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class AabToolGuiApp {
    private static final String APP_NAME = "AAB Tool GUI 2.0";
    private static final String BUNDLETOOL_NAME = "bundletool-all-1.18.3.jar";
    private static final String BUNDLED_BUNDLETOOL_RESOURCE = "/embedded/" + BUNDLETOOL_NAME;
    private static final String DEBUG_ALIAS = "aab_debug";
    private static final String DEBUG_PASSWORD = "aab_debug";
    private static final Pattern ADMOB_AD_UNIT_PATTERN = Pattern.compile("ca-app-pub-(?:\\d{16}/\\d{10}|\\d+/\\d+)");
    private static final Pattern ADMOB_APP_ID_PATTERN = Pattern.compile("ca-app-pub-\\d{16}~\\d+");
    private static final Pattern CLASS_DESCRIPTOR_PATTERN = Pattern.compile("^L[^;]+;$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("\\p{IsHan}");
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("package:\\s+name='([^']+)'");
    private static final Pattern LAUNCHABLE_ACTIVITY_PATTERN = Pattern.compile("launchable-activity:\\s+name='([^']+)'");
    private static final Pattern VERSION_CODE_PATTERN = Pattern.compile("versionCode='([^']+)'");
    private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("versionName='([^']+)'");
    private static final Pattern MIN_SDK_PATTERN = Pattern.compile("minSdkVersion:'([^']+)'");
    private static final Pattern TARGET_SDK_PATTERN = Pattern.compile("targetSdkVersion:'([^']+)'");
    private static final Pattern USES_PERMISSION_PATTERN = Pattern.compile("uses-permission(?:-sdk-\\d+)?:\\s+name='([^']+)'");
    private static final Pattern DEBUGGABLE_XMLTREE_PATTERN = Pattern.compile("android:debuggable\\(0x0101000f\\)=\\((?:type 0x12|type 0x10)\\)0x([0-9a-fA-F]+)");
    private static final Pattern APPLICATION_LABEL_RESOURCE_PATTERN = Pattern.compile("android:label\\(0x01010001\\)=@0x([0-9a-fA-F]+)");
    private static final Pattern RESOURCE_HEADER_PATTERN = Pattern.compile("^\\s*resource\\s+0x([0-9a-fA-F]+)\\b");
    private static final Pattern RESOURCE_LOCALE_VALUE_PATTERN = Pattern.compile("^\\s*\\(([^)]*)\\)\\s+\"");
    private static final Set<String> OBFUSCATION_PACKAGE_IGNORES = Set.of("google", "adjust", "firebase", "facebook", "androidx");
    private static final Set<String> GENERIC_SCAN_SKIPPED_EXTENSIONS = Set.of(
        ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".heic",
        ".mp3", ".ogg", ".wav", ".aac", ".flac",
        ".mp4", ".avi", ".mkv", ".webm",
        ".so", ".ttf", ".otf", ".woff", ".woff2"
    );
    private static final int[] LEGACY_STRINGFOG_OPCODE_SEQUENCE = {0x48, 0x48, 0xb7, 0x8d, 0x4f, 0xd8, 0xd8};
    private static final Map<Integer, Integer> LEGACY_STRINGFOG_OPCODE_WIDTHS = Map.of(
        0x48, 2,
        0xb7, 1,
        0x8d, 1,
        0x4f, 2,
        0xd8, 2
    );
    private static final int GUI_SECTION_PREVIEW_LIMIT = 20;
    private static final int GUI_CHINESE_PREVIEW_LIMIT = GUI_SECTION_PREVIEW_LIMIT;
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

	    private static final class SourceChineseEntry {
	        private final String source;
	        private final String value;

        private SourceChineseEntry(String source, String value) {
            this.source = source;
            this.value = value;
	        }
	    }

	    private static final class DexCursor {
	        private int offset;

	        private DexCursor(int offset) {
	            this.offset = offset;
	        }
	    }

	    private static final class DexMethodMeta {
	        private final String classDescriptor;
	        private final String methodName;
	        private final boolean legacyStringFogCandidate;

	        private DexMethodMeta(String classDescriptor, String methodName, boolean legacyStringFogCandidate) {
	            this.classDescriptor = classDescriptor;
	            this.methodName = methodName;
	            this.legacyStringFogCandidate = legacyStringFogCandidate;
	        }

	        private String displayName() {
	            return classDescriptor + "->" + methodName + "([B[B)Ljava/lang/String;";
	        }
	    }

	    private static final class CliOptions {
	        private boolean showHelp;
	        private final ToolConfig config = new ToolConfig();
	    }

    private static final class LegacyInspectionReport {
        private final Set<String> apkNames = new LinkedHashSet<>();
        private final Set<String> chineseKeys = new LinkedHashSet<>();
        private final Set<String> chineseSamples = new LinkedHashSet<>();
        private final Set<String> admobAppIds = new LinkedHashSet<>();
        private final Set<String> admobAdUnitIds = new LinkedHashSet<>();
        private final Set<String> stringFogSamples = new LinkedHashSet<>();
        private final Set<String> aabResGuardSamples = new LinkedHashSet<>();
        private final Set<String> permissions = new LinkedHashSet<>();
        private final Set<String> locales = new LinkedHashSet<>();
        private final Set<String> logSamples = new LinkedHashSet<>();
        private int chineseCount;
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
        private String packageName = "";
        private String launchableActivity = "";
        private String versionCode = "";
        private String versionName = "";
        private String minSdkVersion = "";
        private String targetSdkVersion = "";

        private void addApkName(String apkName) {
            apkNames.add(apkName);
        }

        private void addChineseValue(String source, String value) {
            String normalized = normalizeChineseValue(value);
            if (normalized.isEmpty() || !chineseKeys.add(normalized)) {
                return;
            }
            chineseCount++;
            addSample(chineseSamples, source + " -> " + abbreviate(normalized));
        }

        private void clearChinese() {
            chineseKeys.clear();
            chineseSamples.clear();
            chineseCount = 0;
        }

        private void addAdmobAppId(String sample) {
            addSample(admobAppIds, sample);
        }

        private void addAdmobAdUnitId(String sample) {
            addSample(admobAdUnitIds, sample);
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

        private void setPackageName(String packageName) {
            this.packageName = safeTrim(packageName);
        }

        private void setLaunchableActivity(String launchableActivity) {
            this.launchableActivity = safeTrim(launchableActivity);
        }

        private void setVersionCode(String versionCode) {
            this.versionCode = safeTrim(versionCode);
        }

        private void setVersionName(String versionName) {
            this.versionName = safeTrim(versionName);
        }

        private void setMinSdkVersion(String minSdkVersion) {
            this.minSdkVersion = safeTrim(minSdkVersion);
        }

        private void setTargetSdkVersion(String targetSdkVersion) {
            this.targetSdkVersion = safeTrim(targetSdkVersion);
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

        private boolean isLikelyResourceObfuscated() {
            return totalResourceCount > 0 && shortResourceCount * 2 > totalResourceCount;
        }

        private void logTo(java.util.function.Consumer<String> logSink) {
            logSink.accept("静态检查结果：");
            logSink.accept("  APK 分包：" + (apkNames.isEmpty() ? "未找到" : String.join(", ", apkNames)));

            logSink.accept("1. AdMob 配置相关");
            emitAdmobSection(logSink);

            logSink.accept("2. StringFog/AabResGuard/代码混淆\"");
            logSink.accept("  StringFog：" + (stringFogCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  代码混淆：" + codeObfuscationStatus());
            logSink.accept("  AabResGuard：" + (aabResGuardCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  调试模式：isDebug: " + debugStatus());

            logSink.accept("3. 中文字符串相关");
            emitSection(logSink, chineseCount, chineseSamples, "  未发现中文字符串。", "  发现 %d 条中文字符串（去重后）：");

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
            emitReadableBasicInfoSection(logSink);

            logSink.accept("2. AdMob 配置相关");
            emitReadableAdmobSection(logSink);

            logSink.accept("3. StringFog/AabResGuard/代码混淆");
            logSink.accept("  StringFog：" + (stringFogCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  代码混淆：" + readableCodeObfuscationStatus());
            logSink.accept("  AabResGuard：" + (aabResGuardCount > 0 ? "已启用" : "未发现"));
            logSink.accept("  调试模式：isDebug: " + readableDebugStatus());

            logSink.accept("4. 中文字符串相关");
            emitReadableSection(logSink, chineseCount, chineseSamples, "  未发现中文字符串。", "  发现 %d 条中文字符串（去重后）：");

            logSink.accept("5. 已申请权限相关");
            if (permissions.isEmpty()) {
                logSink.accept("  未解析到已申请权限。");
            } else {
                logSink.accept("  共解析到 " + permissions.size() + " 个权限：");
                for (String permission : permissions) {
                    logSink.accept("  - " + permission);
                }
            }

            logSink.accept("6. 多语言支持相关");
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

        private void emitReadableBasicInfoSection(java.util.function.Consumer<String> logSink) {
            logSink.accept("1. 基础信息");
            logSink.accept("  包名: " + displayValue(packageName));
            logSink.accept("  启动类: " + displayValue(launchableActivity));
            logSink.accept("  versionCode: " + displayValue(versionCode));
            logSink.accept("  versionName: " + displayValue(versionName));
            logSink.accept("  minSdkVersion: " + displayValue(minSdkVersion));
            logSink.accept("  targetSdkVersion: " + displayValue(targetSdkVersion));
            logSink.accept("  APK 分包: " + (apkNames.isEmpty() ? "未找到" : String.join(", ", apkNames)));
        }

        private static String displayValue(String value) {
            return isBlank(value) ? "未解析到" : value;
        }

        private static String safeTrim(String value) {
            return value == null ? "" : value.trim();
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

        private void emitReadableAdmobSection(java.util.function.Consumer<String> logSink) {
            if (admobAppIds.isEmpty() && admobAdUnitIds.isEmpty()) {
                logSink.accept("  未发现 AdMob 配置。");
                return;
            }
            for (String appId : admobAppIds) {
                logSink.accept("  AdMob app ID:  " + sanitizeDisplayText(appId));
            }
            if (admobAdUnitIds.isEmpty()) {
                logSink.accept("  未发现 admob adUnitId");
                return;
            }
            logSink.accept("  discover admob adUnitId:");
            for (String adUnitId : admobAdUnitIds) {
                logSink.accept("  " + sanitizeDisplayText(adUnitId));
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

        private void emitAdmobSection(java.util.function.Consumer<String> logSink) {
            if (admobAppIds.isEmpty() && admobAdUnitIds.isEmpty()) {
                logSink.accept("  未发现 AdMob 配置。");
                return;
            }
            for (String appId : admobAppIds) {
                logSink.accept("  AdMob app ID:  " + appId);
            }
            if (admobAdUnitIds.isEmpty()) {
                logSink.accept("  未发现 admob adUnitId");
                return;
            }
            logSink.accept("  discover admob adUnitId:");
            for (String adUnitId : admobAdUnitIds) {
                logSink.accept("  " + adUnitId);
            }
        }

        private static void addSample(Set<String> target, String sample) {
            target.add(sample);
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
        private final JCheckBox runLegacyChecksBox = new JCheckBox("静态检查", true);
        private final JCheckBox installAfterBuildBox = new JCheckBox("构建后安装", true);
        private final JCheckBox autoLaunchAfterInstallBox = new JCheckBox("安装后自动启动", true);
        private final JCheckBox autoUninstallOnSignatureMismatchBox = new JCheckBox("签名冲突时自动卸载重试", false);
        private final JCheckBox allowDowngradeBox = new JCheckBox("允许降级安装", false);
        private final JCheckBox grantRuntimePermissionsBox = new JCheckBox("自动授权运行时权限", false);
        private final JButton refreshDevicesButton = new JButton("刷新");
        private final JButton advancedToggleButton = new JButton("显示高级选项");
        private final JButton startButton = new JButton("开始");
        private final JTextArea logArea = new JTextArea(26, 88);
        private final JScrollPane logScrollPane = new JScrollPane(logArea);
        private final JPanel advancedPanel = new JPanel(new GridBagLayout());
        private final List<String> rawLogLines = new ArrayList<>();
        private final Set<String> expandedSections = new LinkedHashSet<>();
        private boolean advancedVisible;
        private boolean chineseExpanded;
        private RenderedLog renderedLog = RenderedLog.empty();

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
            setSize(new Dimension(1280, 720));
            setLocationRelativeTo(null);
            setMinimumSize(new Dimension(1024, 576));
        }

        private JPanel buildFormPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            JPanel basicPanel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            row = addFieldRow(basicPanel, c, row, "AAB 文件", aabField, browseFileButton(aabField, false));
            row = addFieldRow(basicPanel, c, row, "输出 .apks", outputField, browseSaveButton(outputField));
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
            row = addFieldRow(advancedPanel, c, row, "ADB 路径", adbField, browseFileButton(adbField, false));
            row = addFieldRow(advancedPanel, c, row, "AAPT2 路径（可选）", aapt2Field, browseFileButton(aapt2Field, false));
            row = addFieldRow(advancedPanel, c, row, "签名文件（可选）", keystoreField, browseFileButton(keystoreField, false));
            row = addPasswordRow(advancedPanel, c, row, "签名密码", keystorePasswordField);
            row = addFieldRow(advancedPanel, c, row, "签名别名", keyAliasField, null);
            row = addPasswordRow(advancedPanel, c, row, "密钥密码", keyPasswordField);
            row = addCheckBoxRow(advancedPanel, c, row, "构建选项", runLegacyChecksBox, installAfterBuildBox);
            row = addCheckBoxRow(advancedPanel, c, row, "安装选项", autoLaunchAfterInstallBox, autoUninstallOnSignatureMismatchBox);
            addCheckBoxRow(advancedPanel, c, row, "权限选项", allowDowngradeBox, grantRuntimePermissionsBox);

            advancedPanel.setVisible(false);

            wrapper.add(header, BorderLayout.NORTH);
            wrapper.add(advancedPanel, BorderLayout.CENTER);
            return wrapper;
        }

        private void toggleAdvancedOptions() {
            advancedVisible = !advancedVisible;
            advancedPanel.setVisible(advancedVisible);
            advancedToggleButton.setText(advancedVisible ? "隐藏高级选项" : "显示高级选项");
            revalidate();
            repaint();
        }

        private JPanel buildLogPanel() {
            logArea.setEditable(false);
            logArea.setLineWrap(false);
            MouseAdapter logMouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleLogAreaClick(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    updateLogCursor(e);
                }
            };
            logArea.addMouseListener(logMouseAdapter);
            logArea.addMouseMotionListener(logMouseAdapter);
            logScrollPane.setBorder(BorderFactory.createTitledBorder("日志"));

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(logScrollPane, BorderLayout.CENTER);
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
            JButton clearButton = new JButton("清空日志");
            clearButton.addActionListener(e -> resetLogArea());
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
            panel.add(new JLabel("设备 ID（可选）:"), c);

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
            panel.add(new JLabel("安装模式:"), c);

            c.gridx = 1;
            c.weightx = 1;
            panel.add(modeBox, c);

            c.gridx = 2;
            c.weightx = 0;
            panel.add(Box.createHorizontalStrut(1), c);
            return row + 1;
        }

        private int addCheckBoxRow(JPanel panel, GridBagConstraints c, int row, String label, JCheckBox first, JCheckBox second) {
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0;
            panel.add(new JLabel(label + ":"), c);

            JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            options.add(first);
            options.add(second);

            c.gridx = 1;
            c.weightx = 1;
            panel.add(options, c);

            c.gridx = 2;
            c.weightx = 0;
            panel.add(Box.createHorizontalStrut(1), c);
            return row + 1;
        }

        private JButton browseFileButton(JTextField target, boolean directoriesOnly) {
            JButton button = new JButton("浏览");
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
            JButton button = new JButton("浏览");
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
                                "刷新设备失败：" + error.getMessage(),
                                "刷新失败",
                                JOptionPane.WARNING_MESSAGE
                            );
                        } else if (devices.isEmpty()) {
                            JOptionPane.showMessageDialog(
                                MainFrame.this,
                                "未发现在线 Android 设备。",
                                "没有设备",
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
            aabField.setToolTipText("可将 .aab 文件拖到这里，或点击“浏览”。");
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
                                "请拖入 .aab 文件。",
                                "文件不支持",
                                JOptionPane.WARNING_MESSAGE
                            );
                            return false;
                        }
                        applyAabSelection(dropped);
                        return true;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            MainFrame.this,
                            "导入拖拽文件失败：" + e.getMessage(),
                            "导入失败",
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
            grantRuntimePermissionsBox.setSelected(false);
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
            resetLogArea();

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
                            result.success ? "成功" : "失败",
                            result.success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
                        );
                    } catch (Exception e) {
                        appendLog("界面异常：" + e.getMessage());
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
                JOptionPane.showMessageDialog(this, "必须选择 AAB 文件。", "缺少必填项", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            if (config.outputPath.isBlank()) {
                config.outputPath = deriveOutputPath(Paths.get(config.aabPath), config.mode).toString();
                outputField.setText(config.outputPath);
            }
            return config;
        }

        private void resetLogArea() {
            rawLogLines.clear();
            expandedSections.clear();
            renderLogArea(false, false);
        }

        private void appendLog(String line) {
            rawLogLines.add(line);
            renderLogArea(false, true);
        }

        private void renderLogArea(boolean preserveView, boolean scrollToBottom) {
            ScrollAnchor anchor = preserveView ? captureScrollAnchor() : null;
            renderedLog = buildRenderedLog();
            StringBuilder builder = new StringBuilder();
            for (String line : renderedLog.lines) {
                builder.append(line).append(System.lineSeparator());
            }
            logArea.setText(builder.toString());
            if (anchor != null) {
                SwingUtilities.invokeLater(() -> restoreScrollAnchor(anchor));
            } else if (scrollToBottom) {
                logArea.setCaretPosition(logArea.getDocument().getLength());
            } else {
                logArea.setCaretPosition(0);
            }
            logArea.setCursor(Cursor.getDefaultCursor());
        }

        private ScrollAnchor captureScrollAnchor() {
            try {
                Point viewPosition = logScrollPane.getViewport().getViewPosition();
                int offset = logArea.viewToModel2D(new Point(0, viewPosition.y));
                if (offset < 0) {
                    return null;
                }
                int line = logArea.getLineOfOffset(offset);
                Rectangle2D rect = logArea.modelToView2D(logArea.getLineStartOffset(line));
                int lineY = rect == null ? viewPosition.y : (int) rect.getY();
                return new ScrollAnchor(line, Math.max(0, viewPosition.y - lineY), viewPosition.x);
            } catch (BadLocationException ignored) {
                return null;
            }
        }

        private void restoreScrollAnchor(ScrollAnchor anchor) {
            try {
                int lineCount = Math.max(1, logArea.getLineCount());
                int targetLine = Math.min(anchor.line, lineCount - 1);
                Rectangle2D rect = logArea.modelToView2D(logArea.getLineStartOffset(targetLine));
                int baseY = rect == null ? 0 : (int) rect.getY();
                logScrollPane.getViewport().setViewPosition(new Point(anchor.x, Math.max(0, baseY + anchor.offsetWithinLine)));
            } catch (BadLocationException ignored) {
                logScrollPane.getViewport().setViewPosition(new Point(0, 0));
            }
        }

        private RenderedLog buildRenderedLog() {
            if (Boolean.TRUE.booleanValue()) {
                return buildRenderedLogV2();
            }
            List<String> lines = new ArrayList<>();
            Map<Integer, ToggleAction> toggleActions = new HashMap<>();
            int toggleLineIndex = -1;
            int chineseHeaderLineIndex = -1;
            int index = 0;
            while (index < rawLogLines.size()) {
                String line = rawLogLines.get(index);
                if (!isTopLevelSectionHeader(line)) {
                    lines.add(line);
                    index++;
                    continue;
                }

                int headerLineIndex = lines.size();
                lines.add(line);
                index++;
                List<String> sectionLines = new ArrayList<>();
                while (index < rawLogLines.size()) {
                    String candidateLine = rawLogLines.get(index);
                    if (isTopLevelSectionHeader(candidateLine) || isSectionBoundaryLine(candidateLine)) {
                        break;
                    }
                    sectionLines.add(candidateLine);
                    index++;
                }

                int chineseLineCount = 0;
                int hiddenCount = 0;
                for (String sectionLine : sectionLines) {
                    if (!isChineseSampleLine(sectionLine)) {
                        lines.add(sectionLine);
                        continue;
                    }
                    chineseLineCount++;
                    if (chineseExpanded || chineseLineCount <= GUI_CHINESE_PREVIEW_LIMIT) {
                        lines.add(sectionLine);
                    } else {
                        hiddenCount++;
                    }
                }

                if (hiddenCount > 0) {
                    toggleLineIndex = lines.size();
                    lines.add("  ... 点击展开全部（已省略 " + hiddenCount + " 条）");
                } else if (chineseExpanded && chineseLineCount > GUI_CHINESE_PREVIEW_LIMIT) {
                    toggleLineIndex = lines.size();
                    lines.add("  ... 点击收起");
                }
            }
            return new RenderedLog(lines, toggleLineIndex, chineseHeaderLineIndex);
        }

        private void handleLogAreaClick(MouseEvent event) {
            if (Boolean.TRUE.booleanValue()) {
                handleLogAreaClickV2(event);
                return;
            }
            int lineIndex = resolveLogLineIndex(event);
            if (lineIndex < 0 || lineIndex != renderedLog.toggleLineIndex) {
                return;
            }
            if (chineseExpanded) {
                chineseExpanded = false;
                int headerLineIndex = renderedLog.chineseHeaderLineIndex;
                renderLogArea(false, false);
                if (headerLineIndex >= 0) {
                    SwingUtilities.invokeLater(() -> restoreScrollAnchor(new ScrollAnchor(headerLineIndex, 0, 0)));
                }
            } else {
                chineseExpanded = true;
                renderLogArea(true, false);
            }
        }

        private void updateLogCursor(MouseEvent event) {
            if (Boolean.TRUE.booleanValue()) {
                updateLogCursorV2(event);
                return;
            }
            int lineIndex = resolveLogLineIndex(event);
            logArea.setCursor(lineIndex >= 0 && lineIndex == renderedLog.toggleLineIndex
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        }

        private RenderedLog buildRenderedLogV2() {
            List<String> lines = new ArrayList<>();
            Map<Integer, ToggleAction> toggleActions = new HashMap<>();
            int index = 0;
            while (index < rawLogLines.size()) {
                String line = rawLogLines.get(index);
                if (!isTopLevelSectionHeader(line)) {
                    lines.add(line);
                    index++;
                    continue;
                }

                int headerLineIndex = lines.size();
                lines.add(line);
                index++;
                List<String> sectionLines = new ArrayList<>();
                while (index < rawLogLines.size()) {
                    String candidateLine = rawLogLines.get(index);
                    if (isTopLevelSectionHeader(candidateLine) || isSectionBoundaryLine(candidateLine)) {
                        break;
                    }
                    sectionLines.add(candidateLine);
                    index++;
                }

                String sectionKey = resolveCollapsibleSectionKey(line);
                if (sectionKey == null) {
                    lines.addAll(sectionLines);
                    continue;
                }

                int detailLineCount = 0;
                int hiddenCount = 0;
                boolean expanded = expandedSections.contains(sectionKey);
                for (String sectionLine : sectionLines) {
                    if (!isCollapsibleDetailLine(sectionKey, sectionLine)) {
                        lines.add(sectionLine);
                        continue;
                    }
                    detailLineCount++;
                    if (expanded || detailLineCount <= GUI_SECTION_PREVIEW_LIMIT) {
                        lines.add(sectionLine);
                    } else {
                        hiddenCount++;
                    }
                }

                if (hiddenCount > 0) {
                    int toggleLineIndex = lines.size();
                    lines.add(buildExpandPrompt(hiddenCount));
                    toggleActions.put(toggleLineIndex, new ToggleAction(sectionKey, headerLineIndex, false));
                } else if (expanded && detailLineCount > GUI_SECTION_PREVIEW_LIMIT) {
                    int toggleLineIndex = lines.size();
                    lines.add(buildCollapsePrompt());
                    toggleActions.put(toggleLineIndex, new ToggleAction(sectionKey, headerLineIndex, true));
                }
            }
            return new RenderedLog(lines, toggleActions);
        }

        private void handleLogAreaClickV2(MouseEvent event) {
            int lineIndex = resolveLogLineIndex(event);
            if (lineIndex < 0) {
                return;
            }
            ToggleAction action = renderedLog.toggleActions.get(lineIndex);
            if (action == null) {
                return;
            }
            if (action.collapse) {
                expandedSections.remove(action.sectionKey);
                renderLogArea(false, false);
                if (action.headerLineIndex >= 0) {
                    SwingUtilities.invokeLater(() -> restoreScrollAnchor(new ScrollAnchor(action.headerLineIndex, 0, 0)));
                }
            } else {
                expandedSections.add(action.sectionKey);
                renderLogArea(true, false);
            }
        }

        private void updateLogCursorV2(MouseEvent event) {
            int lineIndex = resolveLogLineIndex(event);
            logArea.setCursor(lineIndex >= 0 && renderedLog.toggleActions.containsKey(lineIndex)
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        }

        private static String resolveCollapsibleSectionKey(String line) {
            if (line == null) {
                return null;
            }
            if (line.startsWith("2. ")) {
                return "admob";
            }
            if (line.startsWith("4. ")) {
                return "chinese";
            }
            if (line.startsWith("5. ")) {
                return "permissions";
            }
            if (line.startsWith("6. ")) {
                return "locales";
            }
            return null;
        }

        private static boolean isCollapsibleDetailLine(String sectionKey, String line) {
            if (sectionKey == null || line == null) {
                return false;
            }
            if ("admob".equals(sectionKey)) {
                return ADMOB_AD_UNIT_PATTERN.matcher(line.trim()).matches();
            }
            return line.startsWith("  - ");
        }

        private static boolean isSectionBoundaryLine(String line) {
            if (line == null || line.isBlank()) {
                return true;
            }
            return !Character.isWhitespace(line.charAt(0));
        }

        private static String buildExpandPrompt(int hiddenCount) {
            return "  ... \u70b9\u51fb\u5c55\u5f00\u5168\u90e8\uff08\u5df2\u7701\u7565 " + hiddenCount + " \u6761\uff09";
        }

        private static String buildCollapsePrompt() {
            return "  ... \u70b9\u51fb\u6536\u8d77";
        }

        private int resolveLogLineIndex(MouseEvent event) {
            int offset = logArea.viewToModel2D(event.getPoint());
            if (offset < 0) {
                return -1;
            }
            try {
                return logArea.getLineOfOffset(offset);
            } catch (BadLocationException ignored) {
                return -1;
            }
        }

        private static boolean isChineseSectionHeader(String line) {
            return line != null && line.matches("^\\d+\\.\\s+中文字符串相关$");
        }

        private static boolean isTopLevelSectionHeader(String line) {
            return line != null && line.matches("^\\d+\\.\\s+.+$");
        }

        private static boolean isChineseSampleLine(String line) {
            return line != null && line.startsWith("  - ");
        }

        private static final class RenderedLog {
            private final List<String> lines;
            private final int toggleLineIndex;
            private final int chineseHeaderLineIndex;
            private final Map<Integer, ToggleAction> toggleActions;

            private RenderedLog(List<String> lines, int toggleLineIndex, int chineseHeaderLineIndex) {
                this.lines = lines;
                this.toggleLineIndex = toggleLineIndex;
                this.chineseHeaderLineIndex = chineseHeaderLineIndex;
                this.toggleActions = Map.of();
            }

            private RenderedLog(List<String> lines, Map<Integer, ToggleAction> toggleActions) {
                this.lines = lines;
                this.toggleLineIndex = -1;
                this.chineseHeaderLineIndex = -1;
                this.toggleActions = toggleActions;
            }

            private static RenderedLog empty() {
                return new RenderedLog(List.of(), Map.of());
            }
        }

        private static final class ToggleAction {
            private final String sectionKey;
            private final int headerLineIndex;
            private final boolean collapse;

            private ToggleAction(String sectionKey, int headerLineIndex, boolean collapse) {
                this.sectionKey = sectionKey;
                this.headerLineIndex = headerLineIndex;
                this.collapse = collapse;
            }
        }

        private static final class ScrollAnchor {
            private final int line;
            private final int offsetWithinLine;
            private final int x;

            private ScrollAnchor(int line, int offsetWithinLine, int x) {
                this.line = line;
                this.offsetWithinLine = offsetWithinLine;
                this.x = x;
            }
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

        LaunchTarget launchTarget = resolveLaunchTarget(aab, output, aapt2, logSink);
        if (!isBlank(resolvedDeviceId) && launchTarget != null && !isBlank(launchTarget.packageName)) {
            boolean installed = isPackageInstalled(launchTarget.packageName, adbArg, resolvedDeviceId, logSink);
            if (installed) {
                logSink.accept("设备上已安装同包名应用：" + launchTarget.packageName);
                if (!config.autoUninstallOnSignatureMismatch) {
                    logSink.accept("提示：如果旧应用签名不同，安装时可能出现 UPDATE_INCOMPATIBLE。");
                }
                logSink.accept("");
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

        try {
            LaunchTarget target = knownTarget;
            if ((target == null || isBlank(target.packageName)) && aapt2 != null) {
                target = detectLaunchTarget(apks, aapt2, logSink);
            }
            if (target == null || isBlank(target.packageName)) {
                logSink.accept("自动启动已跳过：无法解析包名。");
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

    private static LaunchTarget resolveLaunchTarget(Path aab, Path apks, Path aapt2, java.util.function.Consumer<String> logSink) throws Exception {
        LaunchTarget bundleTarget = detectLaunchTargetFromBundle(aab);
        LaunchTarget apkTarget = null;
        if (aapt2 != null) {
            apkTarget = detectLaunchTarget(apks, aapt2, logSink);
        }

        LaunchTarget merged = mergeLaunchTarget(bundleTarget, apkTarget);
        if (merged != null && !isBlank(merged.packageName) && apkTarget == null) {
            logSink.accept("自动启动目标：" + merged.packageName
                + (isBlank(merged.launchableActivity) ? "（通过 AAB Manifest 解析）" : " / " + merged.launchableActivity + "（通过 AAB Manifest 解析）"));
            logSink.accept("");
        }
        return merged;
    }

    private static LaunchTarget mergeLaunchTarget(LaunchTarget preferred, LaunchTarget fallback) {
        if ((preferred == null || isBlank(preferred.packageName)) && (fallback == null || isBlank(fallback.packageName))) {
            return null;
        }
        String packageName = preferred != null && !isBlank(preferred.packageName) ? preferred.packageName : fallback.packageName;
        String launchableActivity = preferred != null && !isBlank(preferred.launchableActivity)
            ? preferred.launchableActivity
            : (fallback == null ? "" : fallback.launchableActivity);
        return new LaunchTarget(packageName, launchableActivity == null ? "" : launchableActivity);
    }

    private static LaunchTarget detectLaunchTargetFromBundle(Path aab) {
        try {
            Path bundletool = resolveBundletool("");
            String packageName = runBundleManifestXPath(bundletool, aab, "/manifest/@package");
            if (isBlank(packageName)) {
                return null;
            }
            String activityName = firstNonBlank(
                runBundleManifestXPath(
                    bundletool,
                    aab,
                    "/manifest/application/activity-alias[" +
                        "intent-filter/action[@android:name='android.intent.action.MAIN'] and " +
                        "intent-filter/category[@android:name='android.intent.category.LAUNCHER']" +
                    "]/@android:targetActivity"
                ),
                runBundleManifestXPath(
                    bundletool,
                    aab,
                    "/manifest/application/activity[" +
                        "intent-filter/action[@android:name='android.intent.action.MAIN'] and " +
                        "intent-filter/category[@android:name='android.intent.category.LAUNCHER']" +
                    "]/@android:name"
                ),
                runBundleManifestXPath(
                    bundletool,
                    aab,
                    "/manifest/application/activity-alias[" +
                        "intent-filter/action[@android:name='android.intent.action.MAIN'] and " +
                        "intent-filter/category[@android:name='android.intent.category.LAUNCHER']" +
                    "]/@android:name"
                )
            );
            if (!isBlank(activityName)) {
                activityName = normalizeActivityName(packageName, activityName);
            }
            return new LaunchTarget(packageName, activityName == null ? "" : activityName);
        } catch (Exception ignored) {
            return null;
        }
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
        collectBasicInfoFromBundle(aab, report);
        collectAdmobAppIdFromBundle(aab, report);

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
        inspectLegacyFeatureFlags(apks, report);
        augmentWithNearbySourceChinese(aab, report);
        augmentWithNearbySourceLocales(aab, report);
        return report;
    }

    private static void inspectDeclaredPermissions(Path apks, Path aapt2, LegacyInspectionReport report) throws IOException {
        ExtractedApk extractedApk = extractApkForInspection(apks);
        try {
            String output = runCommandQuietly(List.of(aapt2.toString(), "dump", "badging", extractedApk.path.toString()));
            collectBasicInfoFromBadging(output, report);
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

    private static void collectBasicInfoFromBundle(Path aab, LegacyInspectionReport report) {
        try {
            LaunchTarget launchTarget = detectLaunchTargetFromBundle(aab);
            if (launchTarget != null) {
                report.setPackageName(launchTarget.packageName);
                report.setLaunchableActivity(launchTarget.launchableActivity);
            }
            Path bundletool = resolveBundletool("");
            if (launchTarget == null || isBlank(launchTarget.packageName)) {
                report.setPackageName(runBundleManifestXPath(bundletool, aab, "/manifest/@package"));
            }
            report.setVersionCode(runBundleManifestXPath(bundletool, aab, "/manifest/@android:versionCode"));
            report.setVersionName(runBundleManifestXPath(bundletool, aab, "/manifest/@android:versionName"));
            report.setMinSdkVersion(runBundleManifestXPath(bundletool, aab, "/manifest/uses-sdk/@android:minSdkVersion"));
            report.setTargetSdkVersion(runBundleManifestXPath(bundletool, aab, "/manifest/uses-sdk/@android:targetSdkVersion"));
        } catch (Exception ignored) {
            // Keep static analysis best-effort even if bundle manifest dump is unavailable.
        }
    }

    private static void collectAdmobAppIdFromBundle(Path aab, LegacyInspectionReport report) {
        try {
            Path bundletool = resolveBundletool("");
            String appId = runBundleManifestXPath(
                bundletool,
                aab,
                "/manifest/application/meta-data[@android:name='com.google.android.gms.ads.APPLICATION_ID']/@android:value"
            );
            if (!isBlank(appId)) {
                report.addAdmobAppId(appId);
            }
        } catch (Exception ignored) {
            // Keep AdMob detection best-effort even if manifest dump is unavailable.
        }
    }

    private static String runBundleManifestXPath(Path bundletool, Path aab, String xpath) throws IOException, InterruptedException {
        return runCommandQuietly(List.of(
            "java",
            "-jar",
            bundletool.toString(),
            "dump",
            "manifest",
            "--bundle=" + aab,
            "--xpath=" + xpath
        )).trim();
    }

    private static void collectBasicInfoFromBadging(String output, LegacyInspectionReport report) {
        if (output == null || output.isBlank()) {
            return;
        }
        String packageName = findFirstGroup(PACKAGE_NAME_PATTERN, output);
        if (isBlank(report.packageName)) {
            report.setPackageName(packageName);
        }

        String activityName = findFirstGroup(LAUNCHABLE_ACTIVITY_PATTERN, output);
        if (!isBlank(packageName) && !isBlank(activityName)) {
            report.setLaunchableActivity(normalizeActivityName(packageName, activityName));
        } else {
            report.setLaunchableActivity(activityName);
        }

        if (isBlank(report.versionCode)) {
            report.setVersionCode(findFirstGroup(VERSION_CODE_PATTERN, output));
        }
        if (isBlank(report.versionName)) {
            report.setVersionName(findFirstGroup(VERSION_NAME_PATTERN, output));
        }
        if (isBlank(report.minSdkVersion)) {
            report.setMinSdkVersion(findFirstGroup(MIN_SDK_PATTERN, output));
        }
        if (isBlank(report.targetSdkVersion)) {
            report.setTargetSdkVersion(findFirstGroup(TARGET_SDK_PATTERN, output));
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

	    private static void inspectLegacyFeatureFlags(Path apks, LegacyInspectionReport report) throws IOException {
	        ExtractedApk extractedApk = extractApkForInspection(apks);
	        try {
	            inspectLegacyAabResGuard(extractedApk.path, report);
	            inspectLegacyStringFog(extractedApk.path, report);
	        } finally {
	            Files.deleteIfExists(extractedApk.path);
	        }
	    }

	    private static void inspectLegacyAabResGuard(Path apk, LegacyInspectionReport report) throws IOException {
	        try (ZipInputStream apkZip = new ZipInputStream(Files.newInputStream(apk))) {
	            ZipEntry entry;
	            while ((entry = apkZip.getNextEntry()) != null) {
	                if (entry.isDirectory()) {
	                    continue;
	                }
	                String entryName = entry.getName();
	                if (!entryName.startsWith("res/")) {
	                    continue;
	                }
	                report.addResourceEntry(entryName);
	            }
	        }
	        if (report.isLikelyResourceObfuscated()) {
	            report.addAabResGuard("旧版资源短名比例匹配");
	        }
	    }

	    private static void inspectLegacyStringFog(Path apk, LegacyInspectionReport report) throws IOException {
	        try (ZipInputStream apkZip = new ZipInputStream(Files.newInputStream(apk))) {
	            ZipEntry entry;
	            while ((entry = apkZip.getNextEntry()) != null) {
	                if (entry.isDirectory()) {
	                    continue;
	                }
	                String entryName = entry.getName().toLowerCase(Locale.ROOT);
	                if (!entryName.endsWith(".dex")) {
	                    continue;
	                }
	                byte[] dexBytes = apkZip.readAllBytes();
	                String method = findLegacyStringFogDecryptMethod(dexBytes);
	                if (method != null) {
	                    report.addStringFog("旧版解密方法模式 -> " + method);
	                    return;
	                }
	            }
	        }
	    }

	    private static String findLegacyStringFogDecryptMethod(byte[] dexBytes) {
	        if (dexBytes.length < 112 || dexBytes[0] != 'd' || dexBytes[1] != 'e' || dexBytes[2] != 'x') {
	            return null;
	        }

	        int stringIdsSize = readLittleEndianInt(dexBytes, 56);
	        int stringIdsOffset = readLittleEndianInt(dexBytes, 60);
	        int typeIdsSize = readLittleEndianInt(dexBytes, 64);
	        int typeIdsOffset = readLittleEndianInt(dexBytes, 68);
	        int protoIdsSize = readLittleEndianInt(dexBytes, 72);
	        int protoIdsOffset = readLittleEndianInt(dexBytes, 76);
	        int methodIdsSize = readLittleEndianInt(dexBytes, 88);
	        int methodIdsOffset = readLittleEndianInt(dexBytes, 92);
	        int classDefsSize = readLittleEndianInt(dexBytes, 96);
	        int classDefsOffset = readLittleEndianInt(dexBytes, 100);

	        if (!isDexRangeValid(dexBytes, stringIdsOffset, (long) stringIdsSize * 4L)
	            || !isDexRangeValid(dexBytes, typeIdsOffset, (long) typeIdsSize * 4L)
	            || !isDexRangeValid(dexBytes, protoIdsOffset, (long) protoIdsSize * 12L)
	            || !isDexRangeValid(dexBytes, methodIdsOffset, (long) methodIdsSize * 8L)
	            || !isDexRangeValid(dexBytes, classDefsOffset, (long) classDefsSize * 32L)) {
	            return null;
	        }

	        String[] strings = readDexStringPool(dexBytes, stringIdsSize, stringIdsOffset);
	        String[] typeDescriptors = readDexTypeDescriptors(dexBytes, typeIdsSize, typeIdsOffset, strings);
	        boolean[] targetProtos = readLegacyStringFogProtoFlags(dexBytes, protoIdsSize, protoIdsOffset, typeDescriptors);
	        DexMethodMeta[] methodMetas = readDexMethodMetas(dexBytes, methodIdsSize, methodIdsOffset, typeDescriptors, strings, targetProtos);

	        for (int i = 0; i < classDefsSize; i++) {
	            int classDefOffset = classDefsOffset + i * 32;
	            int classDataOffset = readLittleEndianInt(dexBytes, classDefOffset + 24);
	            if (classDataOffset <= 0 || classDataOffset >= dexBytes.length) {
	                continue;
	            }
	            String matched = scanLegacyStringFogClassData(dexBytes, classDataOffset, methodMetas);
	            if (matched != null) {
	                return matched;
	            }
	        }
	        return null;
	    }

	    private static boolean isDexRangeValid(byte[] dexBytes, int offset, long length) {
	        return offset >= 0 && length >= 0 && offset + length <= dexBytes.length;
	    }

	    private static String[] readDexStringPool(byte[] dexBytes, int stringIdsSize, int stringIdsOffset) {
	        String[] strings = new String[stringIdsSize];
	        for (int i = 0; i < stringIdsSize; i++) {
	            int itemOffset = stringIdsOffset + i * 4;
	            int stringDataOffset = readLittleEndianInt(dexBytes, itemOffset);
	            if (stringDataOffset <= 0 || stringDataOffset >= dexBytes.length) {
	                strings[i] = "";
	                continue;
	            }
	            int cursor = skipUleb128(dexBytes, stringDataOffset);
	            if (cursor < 0 || cursor >= dexBytes.length) {
	                strings[i] = "";
	                continue;
	            }
	            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	            while (cursor < dexBytes.length && dexBytes[cursor] != 0) {
	                buffer.write(dexBytes[cursor]);
	                cursor++;
	            }
	            strings[i] = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
	        }
	        return strings;
	    }

	    private static String[] readDexTypeDescriptors(byte[] dexBytes, int typeIdsSize, int typeIdsOffset, String[] strings) {
	        String[] descriptors = new String[typeIdsSize];
	        for (int i = 0; i < typeIdsSize; i++) {
	            int stringIndex = readLittleEndianInt(dexBytes, typeIdsOffset + i * 4);
	            descriptors[i] = stringIndex >= 0 && stringIndex < strings.length ? strings[stringIndex] : "";
	        }
	        return descriptors;
	    }

	    private static boolean[] readLegacyStringFogProtoFlags(byte[] dexBytes, int protoIdsSize, int protoIdsOffset, String[] typeDescriptors) {
	        boolean[] matches = new boolean[protoIdsSize];
	        for (int i = 0; i < protoIdsSize; i++) {
	            int protoOffset = protoIdsOffset + i * 12;
	            int returnTypeIndex = readLittleEndianInt(dexBytes, protoOffset + 4);
	            int parametersOffset = readLittleEndianInt(dexBytes, protoOffset + 8);
	            String returnType = returnTypeIndex >= 0 && returnTypeIndex < typeDescriptors.length ? typeDescriptors[returnTypeIndex] : "";
	            if (!"Ljava/lang/String;".equals(returnType)) {
	                continue;
	            }
	            if (!isDexRangeValid(dexBytes, parametersOffset, 8)) {
	                continue;
	            }
	            int parameterCount = readLittleEndianInt(dexBytes, parametersOffset);
	            if (parameterCount != 2 || !isDexRangeValid(dexBytes, parametersOffset + 4, 4L)) {
	                continue;
	            }
	            int firstTypeIndex = readLittleEndianUnsignedShort(dexBytes, parametersOffset + 4);
	            int secondTypeIndex = readLittleEndianUnsignedShort(dexBytes, parametersOffset + 6);
	            String firstType = firstTypeIndex >= 0 && firstTypeIndex < typeDescriptors.length ? typeDescriptors[firstTypeIndex] : "";
	            String secondType = secondTypeIndex >= 0 && secondTypeIndex < typeDescriptors.length ? typeDescriptors[secondTypeIndex] : "";
	            matches[i] = "[B".equals(firstType) && "[B".equals(secondType);
	        }
	        return matches;
	    }

	    private static DexMethodMeta[] readDexMethodMetas(
	        byte[] dexBytes,
	        int methodIdsSize,
	        int methodIdsOffset,
	        String[] typeDescriptors,
	        String[] strings,
	        boolean[] targetProtos
	    ) {
	        DexMethodMeta[] methods = new DexMethodMeta[methodIdsSize];
	        for (int i = 0; i < methodIdsSize; i++) {
	            int methodOffset = methodIdsOffset + i * 8;
	            int classTypeIndex = readLittleEndianUnsignedShort(dexBytes, methodOffset);
	            int protoIndex = readLittleEndianUnsignedShort(dexBytes, methodOffset + 2);
	            int nameIndex = readLittleEndianInt(dexBytes, methodOffset + 4);
	            String classDescriptor = classTypeIndex >= 0 && classTypeIndex < typeDescriptors.length ? typeDescriptors[classTypeIndex] : "";
	            String methodName = nameIndex >= 0 && nameIndex < strings.length ? strings[nameIndex] : "";
	            boolean targetProto = protoIndex >= 0 && protoIndex < targetProtos.length && targetProtos[protoIndex];
	            methods[i] = new DexMethodMeta(classDescriptor, methodName, targetProto);
	        }
	        return methods;
	    }

	    private static String scanLegacyStringFogClassData(byte[] dexBytes, int classDataOffset, DexMethodMeta[] methodMetas) {
	        DexCursor cursor = new DexCursor(classDataOffset);
	        int staticFields = readUleb128(dexBytes, cursor);
	        int instanceFields = readUleb128(dexBytes, cursor);
	        int directMethods = readUleb128(dexBytes, cursor);
	        int virtualMethods = readUleb128(dexBytes, cursor);
	        if (staticFields < 0 || instanceFields < 0 || directMethods < 0 || virtualMethods < 0) {
	            return null;
	        }
	        if (!skipEncodedFields(dexBytes, cursor, staticFields) || !skipEncodedFields(dexBytes, cursor, instanceFields)) {
	            return null;
	        }
	        String directMatch = scanLegacyStringFogMethods(dexBytes, cursor, directMethods, methodMetas);
	        if (directMatch != null) {
	            return directMatch;
	        }
	        return scanLegacyStringFogMethods(dexBytes, cursor, virtualMethods, methodMetas);
	    }

	    private static boolean skipEncodedFields(byte[] dexBytes, DexCursor cursor, int count) {
	        for (int i = 0; i < count; i++) {
	            if (readUleb128(dexBytes, cursor) < 0 || readUleb128(dexBytes, cursor) < 0) {
	                return false;
	            }
	        }
	        return true;
	    }

	    private static String scanLegacyStringFogMethods(byte[] dexBytes, DexCursor cursor, int count, DexMethodMeta[] methodMetas) {
	        int methodIndex = 0;
	        for (int i = 0; i < count; i++) {
	            int methodIndexDiff = readUleb128(dexBytes, cursor);
	            int accessFlags = readUleb128(dexBytes, cursor);
	            int codeOffset = readUleb128(dexBytes, cursor);
	            if (methodIndexDiff < 0 || accessFlags < 0 || codeOffset < 0) {
	                return null;
	            }
	            methodIndex += methodIndexDiff;
	            if (methodIndex < 0 || methodIndex >= methodMetas.length) {
	                return null;
	            }
	            DexMethodMeta methodMeta = methodMetas[methodIndex];
	            if (methodMeta.legacyStringFogCandidate && codeOffset > 0 && matchesLegacyStringFogCode(dexBytes, codeOffset)) {
	                return methodMeta.displayName();
	            }
	        }
	        return null;
	    }

	    private static boolean matchesLegacyStringFogCode(byte[] dexBytes, int codeOffset) {
	        if (!isDexRangeValid(dexBytes, codeOffset, 16)) {
	            return false;
	        }
	        int insnsSize = readLittleEndianInt(dexBytes, codeOffset + 12);
	        int insnsOffset = codeOffset + 16;
	        if (insnsSize <= 0 || !isDexRangeValid(dexBytes, insnsOffset, (long) insnsSize * 2L)) {
	            return false;
	        }
	        int[] codeUnits = new int[insnsSize];
	        for (int i = 0; i < insnsSize; i++) {
	            codeUnits[i] = readLittleEndianUnsignedShort(dexBytes, insnsOffset + i * 2);
	        }
	        for (int start = 0; start < codeUnits.length; start++) {
	            int cursor = start;
	            boolean matched = true;
	            for (int opcode : LEGACY_STRINGFOG_OPCODE_SEQUENCE) {
	                Integer width = LEGACY_STRINGFOG_OPCODE_WIDTHS.get(opcode);
	                if (width == null || cursor >= codeUnits.length || (codeUnits[cursor] & 0xFF) != opcode) {
	                    matched = false;
	                    break;
	                }
	                cursor += width;
	            }
	            if (matched) {
	                return true;
	            }
	        }
	        return false;
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
            inspectReadableValue(apkName + "!" + entryName, value, report, true, true);
            if (CLASS_DESCRIPTOR_PATTERN.matcher(value).matches()) {
                report.addClassDescriptor(value);
            }
        }
    }

    private static void inspectBinaryEntry(String apkName, String entryName, byte[] bytes, LegacyInspectionReport report) {
        boolean allowChinese = isTextLikeEntry(entryName);
        for (String candidate : extractAsciiStrings(bytes, 6)) {
            inspectReadableValue(apkName + "!" + entryName, candidate, report, allowChinese, false);
        }
        for (String candidate : extractUtf16LeStrings(bytes, 4)) {
            inspectReadableValue(apkName + "!" + entryName, candidate, report, allowChinese, false);
        }
    }

    private static void inspectReadableValue(String source, String rawValue, LegacyInspectionReport report) {
        inspectReadableValue(source, rawValue, report, true, false);
    }

    private static void inspectReadableValue(String source, String rawValue, LegacyInspectionReport report, boolean allowChinese) {
        inspectReadableValue(source, rawValue, report, allowChinese, false);
    }

    private static void inspectReadableValue(String source, String rawValue, LegacyInspectionReport report, boolean allowChinese, boolean strictDexChinese) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            return;
        }

        if (allowChinese && isReportableChineseText(value, strictDexChinese)) {
            report.addChineseValue(source, value);
        }

	        Matcher admobAdUnitMatcher = ADMOB_AD_UNIT_PATTERN.matcher(value);
	        while (admobAdUnitMatcher.find()) {
	            report.addAdmobAdUnitId(admobAdUnitMatcher.group().trim());
	        }

	        String lower = value.toLowerCase(Locale.ROOT);
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

	    private static int readLittleEndianUnsignedShort(byte[] bytes, int offset) {
	        if (offset < 0 || offset + 2 > bytes.length) {
	            return -1;
	        }
	        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
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

	    private static int readUleb128(byte[] bytes, DexCursor cursor) {
	        int result = 0;
	        int shift = 0;
	        int guard = 0;
	        while (cursor.offset < bytes.length && guard < 5) {
	            int current = bytes[cursor.offset] & 0xFF;
	            cursor.offset++;
	            result |= (current & 0x7F) << shift;
	            if ((current & 0x80) == 0) {
	                return result;
	            }
	            shift += 7;
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

    private static String normalizeChineseValue(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static boolean isReportableChineseText(String value, boolean strictDexChinese) {
        String normalized = normalizeChineseValue(value);
        if (!containsMeaningfulChinese(normalized)) {
            return false;
        }
        if (looksLikeTechnicalChineseString(normalized)) {
            return false;
        }
        int hanCount = countHanCharacters(normalized);
        if (strictDexChinese && hanCount < 4) {
            return false;
        }
        return hanCount >= 2;
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

    private static int countHanCharacters(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeScript.of(value.charAt(i)) == Character.UnicodeScript.HAN) {
                count++;
            }
        }
        return count;
    }

    private static boolean looksLikeTechnicalChineseString(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("android.permission.")
            || lower.contains("content://")
            || lower.contains("http://")
            || lower.contains("https://")
            || lower.contains(".xml")
            || lower.contains(".java")
            || lower.contains(".kt")
            || CLASS_DESCRIPTOR_PATTERN.matcher(value).matches();
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

    private static void augmentWithNearbySourceChinese(Path aab, LegacyInspectionReport report) {
        Path current = aab.toAbsolutePath().normalize().getParent();
        for (int level = 0; level < 8 && current != null; level++) {
            Path resDir = current.resolve("src").resolve("main").resolve("res");
            if (Files.isDirectory(resDir)) {
                List<SourceChineseEntry> sourceEntries = collectChineseFromResDirectory(resDir);
                if (!sourceEntries.isEmpty()) {
                    report.clearChinese();
                    for (SourceChineseEntry entry : sourceEntries) {
                        report.addChineseValue(entry.source, entry.value);
                    }
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

    private static List<SourceChineseEntry> collectChineseFromResDirectory(Path resDir) {
        List<SourceChineseEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> valuesDirs = Files.newDirectoryStream(resDir, "values*")) {
            for (Path valuesDir : valuesDirs) {
                if (!Files.isDirectory(valuesDir)) {
                    continue;
                }
                String dirName = valuesDir.getFileName().toString();
                if (!isLikelyChineseValuesDirectory(dirName)) {
                    continue;
                }
                try (DirectoryStream<Path> xmlFiles = Files.newDirectoryStream(valuesDir, "*.xml")) {
                    for (Path xmlFile : xmlFiles) {
                        collectChineseFromResourceFile(resDir, xmlFile, entries);
                    }
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        return entries;
    }

    private static boolean isLikelyChineseValuesDirectory(String dirName) {
        String lower = dirName.toLowerCase(Locale.ROOT);
        return lower.startsWith("values-zh")
            || lower.contains("-zh-")
            || lower.startsWith("values-b+zh");
    }

    private static void collectChineseFromResourceFile(Path resDir, Path xmlFile, List<SourceChineseEntry> entries) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
            }
            document = factory.newDocumentBuilder().parse(Files.newInputStream(xmlFile));
        } catch (Exception ignored) {
            return;
        }

        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }
        String relativeSource = "src/main/res/" + resDir.relativize(xmlFile).toString().replace('\\', '/');
        collectChineseFromResourceNode(root, relativeSource, entries);
    }

    private static void collectChineseFromResourceNode(Node node, String source, List<SourceChineseEntry> entries) {
        if (!(node instanceof Element element)) {
            return;
        }
        String tag = element.getTagName();
        if ("string".equals(tag) || "item".equals(tag)) {
            String value = normalizeChineseValue(element.getTextContent());
            if (isReportableChineseText(value, false)) {
                entries.add(new SourceChineseEntry(source, value));
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectChineseFromResourceNode(children.item(i), source, entries);
        }
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
        config.grantRuntimePermissions = false;
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
