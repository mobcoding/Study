package com.rebuild.mixtube.ads

import android.content.Context

object ServiceLocator {
    lateinit var appContext: Context
        private set

    lateinit var remoteConfig: RemoteConfigStore
        private set

    lateinit var adConfigRepository: AdConfigRepository
        private set

    lateinit var frequencyController: FrequencyController
        private set

    lateinit var placementResolver: AdPlacementResolver
        private set

    lateinit var weightedSelector: WeightedAdSelector
        private set

    lateinit var adWinRepository: AdWinRepository
        private set

    lateinit var sdkInitializer: SdkInitializer
        private set

    lateinit var adManager: AdStrategyManager
        private set

    lateinit var appForegroundTracker: AppForegroundTracker
        private set

    var hasQueuedDownloads: Boolean = false
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        remoteConfig = AssetRemoteConfigStore(appContext)
        adConfigRepository = AdConfigRepository(appContext)
        frequencyController = FrequencyController(appContext)
        placementResolver = AdPlacementResolver(remoteConfig)
        weightedSelector = WeightedAdSelector(appContext)
        adWinRepository = AdWinRepository()
        sdkInitializer = SdkInitializer(appContext)
        appForegroundTracker = AppForegroundTracker()
        adManager = AdStrategyManager(
            context = appContext,
            remoteConfig = remoteConfig,
            repository = adConfigRepository,
            frequency = frequencyController,
            resolver = placementResolver,
            selector = weightedSelector,
            lifecycle = LoggingAdLifecycleListener(),
            adapters = listOf(
                RealGoogleMobileAdsAdapter(appContext),
                MockAdNetworkAdapter("admob"),
                MockAdNetworkAdapter("MAX"),
                MockAdNetworkAdapter("max"),
                MockAdNetworkAdapter("pangle"),
                MockAdNetworkAdapter("topon"),
                MockAdNetworkAdapter("unity")
            )
        )
    }

    fun markQueuedDownloads(hasQueue: Boolean) {
        hasQueuedDownloads = hasQueue
    }
}
