package pl.bsk.chatapp

import android.app.Application
import android.content.Context
import timber.log.Timber

class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        CryptoManager.initialize(this.getSharedPreferences(GLOBAL_SHARED_PREFERENCES,Context.MODE_PRIVATE))



        Timber.plant(object : Timber.DebugTree() {
            override fun log(
                priority: Int, tag: String?, message: String, t: Throwable?
            ) {
                super.log(priority, "BSK_$tag", message, t)
            }
            override fun createStackElementTag(element: StackTraceElement): String? {
                return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
            }
        })
    }
}