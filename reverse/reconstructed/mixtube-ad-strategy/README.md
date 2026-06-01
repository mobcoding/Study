# Mixtube Ad Strategy Kotlin Rebuild

这个目录是基于 `Mixtube_v4.7.2(472).apks` 静态逆向结果整理出的可读 Kotlin 重建工程。

目标不是还原原始签名 APK，也不是绕过混淆恢复所有业务代码，而是把广告加载策略用可维护、可编译、可运行的 Kotlin 代码表达出来。

## 构建

在仓库根目录执行：

```powershell
.\gradlew.bat -p reverse\reconstructed\mixtube-ad-strategy :app:assembleDebug
```

APK 输出：

```text
reverse/reconstructed/mixtube-ad-strategy/app/build/outputs/apk/debug/app-debug.apk
```

## 逆向类映射

| 原 APK 证据 | Kotlin 重建代码 | 作用 |
|---|---|---|
| `androidx/core/widget/k.C()`、`y()` | `AssetRemoteConfigStore`、`AdPlacementResolver` | Remote Config 拉取、激活、读取 key。 |
| `Lf9/b.a/b/c()` | `RemoteConfigStore.getDouble/getLong/getString` | 从激活配置/默认配置读取类型值。 |
| `SplashActivity` 读取 `splash_open_duration`、`ad_req_placement_and` | `AdStrategyManager.bootstrapFromRemoteConfig()` | 启动期读取远程策略并预加载广告。 |
| `ha/h` | `AdStrategyManager` | 广告管理器，按场景维护缓存广告对象。 |
| `ha/b` | `LoadedAd` + `AdNetworkAdapter` | 单个广告位的加载、缓存、展示生命周期。 |
| `ToponSplashAdShowActivity` | `ToponSplashAdShowActivity` | TopOn/ThinkUp 开屏广告 Activity。 |
| `FullNativeAdActivity` | `FullNativeAdActivity` | 全屏原生广告承载页。 |
| `AdWinInterActivity`、`AdWinAdmNativeBean` | `AdWinInterActivity`、`AdWinNativeAd`、`AdWinRepository` | 自渲染广告/服务端广告链路。 |

## 策略要点

- `remote_config.json` 模拟 Firebase Remote Config，包含 `splash_open_duration`、`ad_req_placement_and`、`play_cool`、`open_cool`、`adType`、`ad_need_show` 等 key。
- `ad_config.json` 保留逆向出的广告场景、广告源、权重、广告类型和 placement id。
- `WeightedAdSelector` 使用权重展开后的轮询选择，便于稳定复现不同广告源。
- `FrequencyController` 复刻 `showCount`、`sameInterval`、`differentInterval` 频控。
- `RealGoogleMobileAdsAdapter` 已接入 Google Mobile Ads SDK，并默认使用 Google 官方测试广告位展示真实 SDK 广告效果。
- `MockAdNetworkAdapter` 保留给 MAX、TopOn、Pangle、Unity 等未接真实账号的广告源，便于后续替换。

## 真实 SDK 接入位置

保留真实 SDK 接入点：

- `SdkInitializer`: 对应 AdMob、TopOn/ThinkUp、AppLovin、Unity、BidMachine、Pangle 初始化。
- `RealGoogleMobileAdsAdapter`: 已真实请求 AdMob 测试广告，覆盖 banner、native、interstitial、rewarded、rewarded interstitial、app open。
- `AdNetworkAdapter`: 按 `adsource` 实现 `load()` 和 `show()`，MAX/TopOn/Pangle 可在这里替换为你自己的真实账号实现。
- `AdStrategyManager.adapterFor()`: 根据广告源分配 adapter。

## 关于生产广告位

原 APK 中出现了生产广告位和多个聚合平台 ID，但它们依赖服务端后台配置、包名、签名、账号权限和广告平台反作弊校验。这个重建工程默认不会请求那些第三方生产广告位，而是使用官方测试广告位保证可运行和可验证。若要上线或请求真实广告，请替换成你自己广告账号下授权给当前包名的广告位。
