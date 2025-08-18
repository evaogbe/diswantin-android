package io.github.evaogbe.diswantin

import android.app.Application
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class DiswantinApplication : Application() {
    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Composer.setDiagnosticStackTraceEnabled(true)
        }
    }
}
