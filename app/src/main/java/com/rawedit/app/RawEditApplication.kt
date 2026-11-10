package com.rawedit.app

import android.app.Application
import com.rawedit.app.data.CacheManager
import com.rawedit.app.data.RawEditDatabase
import com.rawedit.app.data.RawRepository
import com.rawedit.app.data.StylePackExporter
import com.rawedit.app.data.UserPreferencesRepository
import com.rawedit.app.domain.DeviceBudgetProfile
import com.rawedit.app.domain.DevicePerformanceOptimizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RawEditApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: RawEditDatabase
        private set
    lateinit var cacheManager: CacheManager
        private set
    lateinit var stylePackExporter: StylePackExporter
        private set
    lateinit var repository: RawRepository
        private set
    lateinit var userPreferences: UserPreferencesRepository
        private set
    lateinit var deviceProfile: DeviceBudgetProfile
        private set

    override fun onCreate() {
        super.onCreate()
        deviceProfile = DevicePerformanceOptimizer.detectProfile(this)
        database = RawEditDatabase.getInstance(this)
        cacheManager = CacheManager(this)
        stylePackExporter = StylePackExporter(this, cacheManager)
        repository = RawRepository(this, database.dao(), cacheManager, stylePackExporter)
        userPreferences = UserPreferencesRepository(this)

        appScope.launch {
            cacheManager.cleanupStaleTempFiles()
        }
    }
}
