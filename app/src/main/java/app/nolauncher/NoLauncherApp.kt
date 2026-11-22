package app.nolauncher

import android.app.Application
import app.nolauncher.data.AppRepository
import app.nolauncher.data.Prefs

class NoLauncherApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var repo: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = AppRepository(this)
    }
}
